package string_matcher.algorithms;

import string_matcher.core.StringNormalizer;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class BusinessEntityNormalizer implements StringNormalizer {

    private static final List<String> SUFFIXES = List.of(
            "gesellschaft mit beschränkter haftung",
            "gesellschaft mit beschraenkter haftung",
            "gesellschaft mit bescchraenkter haftung",
            "gmbh & co kg",
            "gmbh and co kg",
            "gmbh co kg",
            "gmbh",
            "gbr",
            "ag",
            "se",
            "kg",
            "incorporated",
            "incorporation",
            "limited liability company",
            "limited",
            "corporation",
            "company",
            "group",
            "holdings",
            "holding",
            "international",
            "inc",
            "llc",
            "ltd",
            "corp",
            "co",
            "plc",
            "sa"
    );

    // Ensure suffixes are ordered by length descending
    private static final List<String> SORTED_SUFFIXES = SUFFIXES.stream()
            .sorted((a, b) -> Integer.compare(b.length(), a.length()))
            .collect(Collectors.toList());

    private static final Map<String, String> ABBREVIATIONS = new LinkedHashMap<>();
    static {
        ABBREVIATIONS.put("bros",   "brothers");
        ABBREVIATIONS.put("intl",   "international");
        ABBREVIATIONS.put("grp",    "group");
        ABBREVIATIONS.put("svcs",   "services");
        ABBREVIATIONS.put("corp",   "corporation");
        ABBREVIATIONS.put("mfg",    "manufacturing");
        ABBREVIATIONS.put("assoc",  "associates");
        ABBREVIATIONS.put("mgmt",   "management");
        ABBREVIATIONS.put("natl",   "national");
        ABBREVIATIONS.put("amer",   "american");
        ABBREVIATIONS.put("tech",   "technology");
        ABBREVIATIONS.put("sys",    "systems");
        ABBREVIATIONS.put("sol",    "solutions");
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

        // 1. Lowercase and 2. Trim
        String normalized = input.toLowerCase().trim();

        // Transliterate Unicode (helpful before abbreviation and suffix checks)
        for (Map.Entry<String, String> entry : UNICODE_MAP.entrySet()) {
            normalized = normalized.replace(entry.getKey(), entry.getValue());
        }

        // 3. Expand abbreviations (token by token, exact match only)
        String[] tokens = normalized.split("\\s+");
        for (int i = 0; i < tokens.length; i++) {
            // Remove punctuation just for matching the token in the map
            String cleanToken = tokens[i].replaceAll("[^a-z0-9]", "");
            if (ABBREVIATIONS.containsKey(cleanToken)) {
                tokens[i] = ABBREVIATIONS.get(cleanToken);
            }
        }
        normalized = String.join(" ", tokens);

        // 4. Strip suffixes (longest first, full string match)
        for (String suffix : SORTED_SUFFIXES) {
            // Regex to match suffix at word boundaries
            // We use (?i) just in case, though we already lowercased
            // We pad with \b to ensure we match whole words
            normalized = normalized.replaceAll("(?i)\\b" + Pattern.quote(suffix) + "\\b", " ");
        }

        // 5. Remove punctuation (keep only letters, digits, spaces)
        normalized = normalized.replaceAll("[^a-z0-9\\s]", "");

        // 6. Collapse multiple spaces to single space and 7. Trim again
        normalized = normalized.replaceAll("\\s+", " ").trim();

        return normalized;
    }
}
