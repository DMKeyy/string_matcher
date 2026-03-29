package string_matcher.algorithms;

import string_matcher.core.StringNormalizer;
import java.util.regex.Pattern;

public class BusinessEntityNormalizer implements StringNormalizer {

    private static final Pattern BUSINESS_TERMS = Pattern.compile("(?i)\\b(inc|llc|gmbh|ag|se|ltd|corp|corporation|co|holdings|holding|group|and|sons|bros|brothers)\\b");
    private static final Pattern PUNCTUATION = Pattern.compile("[^a-z0-9\\s]");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    @Override
    public String normalize(String input) {
        if (input == null) return "";
        String normalized = input.toLowerCase().trim();
        // Remove common business terms and punctuation
        normalized = BUSINESS_TERMS.matcher(normalized).replaceAll("");
        normalized = PUNCTUATION.matcher(normalized).replaceAll("");
        normalized = WHITESPACE.matcher(normalized).replaceAll(" ").trim();
        return normalized;
    }
}
