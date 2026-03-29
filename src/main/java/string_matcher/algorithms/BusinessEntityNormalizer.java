package string_matcher.algorithms;

import string_matcher.core.StringNormalizer;

public class BusinessEntityNormalizer implements StringNormalizer {

    @Override
    public String normalize(String input) {
        if (input == null) return "";
        String normalized = input.toLowerCase().trim();
        // Remove common business terms and punctuation
        normalized = normalized.replaceAll("(?i)\\b(inc|llc|gmbh|ag|se|ltd|corp|corporation|co|holdings|holding|group|and|sons|bros|brothers)\\b", "");
        normalized = normalized.replaceAll("[^a-z0-9\\s]", "");
        normalized = normalized.replaceAll("\\s+", " ").trim();
        return normalized;
    }
}
