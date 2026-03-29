package string_matcher.algorithms;

import string_matcher.core.StringNormalizer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class BusinessEntityNormalizer implements StringNormalizer {

    // Ordered: longest / most specific patterns first to avoid partial matches
    private static final LinkedHashMap<Pattern, String> SUFFIX_PATTERNS = new LinkedHashMap<>();

    static {
        // Full German legal forms (must come before abbreviations)
        SUFFIX_PATTERNS.put(
                Pattern.compile("(?i)\\bgesellschaft\\s+mit\\s+beschr[aä]nkter\\s+haftung\\b"), "");
        SUFFIX_PATTERNS.put(
                Pattern.compile("(?i)\\bgmbh\\s*&\\s*co\\.?\\s*kg\\b"), "");
        SUFFIX_PATTERNS.put(
                Pattern.compile("(?i)\\bgmbh\\s*&\\s*co\\.?\\s*ohg\\b"), "");

        // Common abbreviations & legal forms (word-boundary matched)
        String[] legalForms = {
                "gmbh", "gbr", "ohg", "kg", "ag", "se", "ug",
                "inc", "llc", "ltd", "corp", "corporation",
                "co", "holdings", "holding", "group",
                "and", "sons", "plc", "lp", "llp", "sa", "nv", "bv"
        };
        for (String form : legalForms) {
            SUFFIX_PATTERNS.put(
                    Pattern.compile("(?i)\\b" + Pattern.quote(form) + "\\b"), "");
        }

        // Abbreviation expansions
        SUFFIX_PATTERNS.put(Pattern.compile("(?i)\\bbros\\b"), "brothers");
        SUFFIX_PATTERNS.put(Pattern.compile("&"), "and");
    }

    // Unicode transliterations
    private static final Map<String, String> UNICODE_MAP = Map.ofEntries(
            Map.entry("ä", "ae"), Map.entry("ö", "oe"), Map.entry("ü", "ue"),
            Map.entry("ß", "ss"), Map.entry("é", "e"), Map.entry("è", "e"),
            Map.entry("ê", "e"), Map.entry("ë", "e"), Map.entry("à", "a"),
            Map.entry("â", "a"), Map.entry("î", "i"), Map.entry("ï", "i"),
            Map.entry("ô", "o"), Map.entry("ù", "u"), Map.entry("û", "u"),
            Map.entry("ç", "c"), Map.entry("ñ", "n")
    );

    @Override
    public String normalize(String input) {
        if (input == null) {
            return "";
        }

        String normalized = input.toLowerCase().trim();

        // 1. Unicode transliteration (before punctuation removal so we catch accented chars)
        for (Map.Entry<String, String> entry : UNICODE_MAP.entrySet()) {
            normalized = normalized.replace(entry.getKey(), entry.getValue());
        }

        // 2. Strip legal suffixes (multi-pass to handle nested suffixes like "GmbH & Co KG")
        for (int pass = 0; pass < 2; pass++) {
            for (Map.Entry<Pattern, String> entry : SUFFIX_PATTERNS.entrySet()) {
                normalized = entry.getKey().matcher(normalized).replaceAll(entry.getValue());
            }
        }

        // 3. Remove remaining punctuation (keep alphanumerics and spaces)
        normalized = normalized.replaceAll("[^a-z0-9\\s]", "");

        // 4. Collapse whitespace
        normalized = normalized.replaceAll("\\s+", " ").trim();

        return normalized;
    }
}
