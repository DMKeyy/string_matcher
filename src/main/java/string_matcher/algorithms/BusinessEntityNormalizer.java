package string_matcher.algorithms;

import string_matcher.core.StringNormalizer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class BusinessEntityNormalizer implements StringNormalizer {

    private static final List<String> SUFFIXES = List.of(
            "gesellschaft mit beschrÃ¤nkter haftung",
            "gesellschaft mit beschraenkter haftung",
            "gesellschaft mit bescchraenkter haftung",
            "gmbh & co kg",
            "gmbh and co kg",
            "gmbh co kg",
            "gmbhcokg",
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
            "and sons",
            "sons",
            "inc",
            "llc",
            "ltd",
            "corp",
            "co",
            "plc",
            "sa"
    );

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

    private static final Map<String, String> UNICODE_MAP = Map.ofEntries(
            Map.entry("Ã¤", "ae"), Map.entry("Ã¶", "oe"), Map.entry("Ã¼", "ue"),
            Map.entry("ÃŸ", "ss"), Map.entry("Ã©", "e"), Map.entry("Ã¨", "e"),
            Map.entry("Ãª", "e"), Map.entry("Ã«", "e"), Map.entry("Ã ", "a"),
            Map.entry("Ã¢", "a"), Map.entry("Ã®", "i"), Map.entry("Ã¯", "i"),
            Map.entry("Ã´", "o"), Map.entry("Ã¹", "u"), Map.entry("Ã»", "u"),
            Map.entry("Ã§", "c"), Map.entry("Ã±", "n")
    );

    @Override
    public String normalize(String input) {
        if (input == null) return "";
        String normalized = input.replaceAll("(?i)GmbH\s*&\s*Co\s*KG", "GmbHCoKG");
        String left = normalized;
        String right = "";
        if (normalized.contains("&")) {
            String[] parts = normalized.split("&", 2);
            left = parts[0];
            right = parts[1];
        }
        left = processText(left);
        right = processText(right);
        String brandName = "";
        if (!right.isEmpty()) {
            int firstSpace = right.indexOf(' ');
            if (firstSpace != -1) {
                brandName = right.substring(firstSpace + 1).trim();
            }
        }
        return (left + " " + brandName).replaceAll("\s+", " ").trim();
    }

    private String processText(String text) {
        String normalized = text.toLowerCase().trim();
        for (Map.Entry<String, String> entry : UNICODE_MAP.entrySet()) {
            normalized = normalized.replace(entry.getKey(), entry.getValue());
        }
        String[] tokens = normalized.split("\s+");
        for (int i = 0; i < tokens.length; i++) {
            String cleanToken = tokens[i].replaceAll("[^a-z0-9]", "");
            if (ABBREVIATIONS.containsKey(cleanToken)) {
                tokens[i] = ABBREVIATIONS.get(cleanToken);
            }
        }
        normalized = String.join(" ", tokens);
        for (String suffix : SORTED_SUFFIXES) {
            normalized = normalized.replaceAll("(?i)\\b" + Pattern.quote(suffix) + "\\b", " ");
        }
        normalized = normalized.replaceAll("[^a-z0-9\s]", "");
        return normalized.replaceAll("\s+", " ").trim();
    }
}
