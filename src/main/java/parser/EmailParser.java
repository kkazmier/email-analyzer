package parser;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import model.EmailMessage;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser plików .eml.
 * Odpowiada wyłącznie za wczytanie i wyciągnięcie danych z pliku.
 * Nie zawiera żadnej logiki analizy - to jest zgodne z zasadą
 * pojedynczej odpowiedzialności.
 */
public class EmailParser {

    /**
     * Regex do ekstrakcji URL-i z treści wiadomości.
     */
    private static final Pattern URL_PATTERN = Pattern.compile(
            "https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+"
    );

    /**
     * Parsuje plik .eml i zwraca niemutowalny EmailMessage.
     */
    public EmailMessage parse(Path path) throws IOException, MessagingException {
        try (InputStream is = new FileInputStream(path.toFile())) {
            return parse(is);
        }
    }

    /**
     * Parsuje InputStream - łatwiejsze do testowania.
     */
    public EmailMessage parse(InputStream is) throws MessagingException, IOException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);
        MimeMessage message = new MimeMessage(session, is);

        String from    = extractAddress(message, Message.RecipientType.TO)
                .orElse(extractFrom(message).orElse(""));
        String actualFrom = extractFrom(message).orElse("");
        String replyTo = extractReplyTo(message).orElse("");
        String subject = Optional.ofNullable(message.getSubject()).orElse("");
        String body    = extractBody(message);

        List<String> urls    = extractUrls(body);
        Map<String, String> headers = extractHeaders(message);

        return new EmailMessage(actualFrom, replyTo, subject, body, urls, headers);
    }

    // -------------------------------------------------------------------------
    // Metody pomocnicze - prywatne, czyste funkcje ekstrakcji
    // -------------------------------------------------------------------------

    private Optional<String> extractFrom(MimeMessage message) {
        try {
            jakarta.mail.Address[] addresses = message.getFrom();
            if (addresses == null || addresses.length == 0) return Optional.empty();
            return Optional.of(addresses[0].toString());
        } catch (MessagingException e) {
            return Optional.empty();
        }
    }

    private Optional<String> extractReplyTo(MimeMessage message) {
        try {
            jakarta.mail.Address[] addresses = message.getReplyTo();
            if (addresses == null || addresses.length == 0) return Optional.empty();
            return Optional.of(addresses[0].toString());
        } catch (MessagingException e) {
            return Optional.empty();
        }
    }

    private Optional<String> extractAddress(
            MimeMessage message,
            Message.RecipientType type
    ) {
        try {
            jakarta.mail.Address[] addresses = message.getRecipients(type);
            if (addresses == null || addresses.length == 0) return Optional.empty();
            return Optional.of(addresses[0].toString());
        } catch (MessagingException e) {
            return Optional.empty();
        }
    }

    /**
     * Rekurencyjne wyciąganie treści - obsługuje multipart.
     */
    private String extractBody(Object part) throws MessagingException, IOException {
        if (part instanceof MimeMessage msg) {
            return extractBody(msg.getContent());
        }
        if (part instanceof jakarta.mail.Multipart mp) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mp.getCount(); i++) {
                sb.append(extractBody(mp.getBodyPart(i)));
            }
            return sb.toString();
        }
        if (part instanceof jakarta.mail.BodyPart bp) {
            Object content = bp.getContent();
            if (content instanceof String s) return s;
            return extractBody(content);
        }
        if (part instanceof String s) return s;
        return "";
    }

    /**
     * Wyciąga wszystkie URL-e z tekstu przy użyciu regex.
     */
    private List<String> extractUrls(String text) {
        List<String> urls = new ArrayList<>();
        Matcher matcher = URL_PATTERN.matcher(text);
        while (matcher.find()) {
            urls.add(matcher.group());
        }
        return List.copyOf(urls);
    }

    /**
     * Wyciąga wszystkie nagłówki jako niemutowalną mapę.
     * Klucze zamieniane na lowercase dla spójności.
     */
    private Map<String, String> extractHeaders(MimeMessage message)
            throws MessagingException {
        Map<String, String> result = new HashMap<>();
        Enumeration<jakarta.mail.Header> headers = message.getAllHeaders();
        while (headers.hasMoreElements()) {
            jakarta.mail.Header h = headers.nextElement();
            result.put(h.getName().toLowerCase(), h.getValue());
        }
        return Map.copyOf(result);
    }
}
