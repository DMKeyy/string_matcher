package string_matcher.core;

public record Record(int id, String originalString, String normalizedString) {
    public Record withNormalizedString(String normalized) {
        return new Record(id, originalString, normalized);
    }
}
