package model;

/**
 * Sealed interface reprezentujący pojedyncze wykrycie.
 * Każdy typ wykrycia to oddzielny record.
 * Sealed gwarantuje, że znamy wszystkie możliwe typy -
 * dzięki temu pattern matching w switch jest wyczerpujący.
 */
public sealed interface Finding
        permits KeywordFinding, UrlFinding, MetadataFinding, PunycodeFinding {

    /**
     * Każde wykrycie ma wagę - używamy jej do obliczania score ryzyka.
     */
    int weight();

    /**
     * Czytelny opis wykrycia do raportu.
     */
    String description();
}
