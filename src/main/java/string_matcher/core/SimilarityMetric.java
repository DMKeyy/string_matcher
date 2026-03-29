package string_matcher.core;

public interface SimilarityMetric {
    double calculateSimilarity(String s1, String s2);
    boolean isMatch(String s1, String s2);
}
