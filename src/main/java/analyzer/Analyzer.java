package analyzer;

import model.EmailMessage;
import model.Finding;

import java.util.List;
import java.util.function.Function;

/**
 * Własny funkcyjny interfejs analizatora.
 *
 * Extends Function<EmailMessage, List<Finding>> żeby mieć dostęp
 * do compose() i andThen() z interfejsu Function.
 *
 * To jest własna funkcja wyższego rzędu - Analyzer to typ który
 * reprezentuje funkcję przyjmującą EmailMessage i zwracającą listę wykryć.
 */
@FunctionalInterface
public interface Analyzer extends Function<EmailMessage, List<Finding>> {

    /**
     * Kompozycja dwóch analizatorów - wyniki są łączone.
     * To jest własna metoda kompozycji funkcji.
     */
    default Analyzer andAlso(Analyzer other) {
        return email -> {
            List<Finding> first  = this.apply(email);
            List<Finding> second = other.apply(email);
            return java.util.stream.Stream
                    .concat(first.stream(), second.stream())
                    .toList();
        };
    }
}
