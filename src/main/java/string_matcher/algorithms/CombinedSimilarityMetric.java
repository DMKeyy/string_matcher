package string_matcher.algorithms;

import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.apache.commons.text.similarity.LevenshteinDistance;
import string_matcher.core.SimilarityMetric;

public class CombinedSimilarityMetric implements SimilarityMetric {

    private final JaroWinklerSimilarity jaroWinkler = new JaroWinklerSimilarity();
    private final LevenshteinDistance levenshtein = new LevenshteinDistance();

    private final double combinedThreshold;

    public CombinedSimilarityMetric(double threshold) {
        this.combinedThreshold = threshold;
    }

    @Override
    public double calculateSimilarity(String s1, String s2) {
        if (s1.isEmpty() && s2.isEmpty()) return 1.0;
        if (s1.isEmpty() || s2.isEmpty()) return 0.0;

        int maxLen = Math.max(s1.length(), s2.length());
        int minLen = Math.min(s1.length(), s2.length());
        if ((maxLen - minLen) > maxLen * 0.4) return 0.0;

        double jwScore = jaroWinkler.apply(s1, s2);

        double levScore = 1.0 - (levenshtein.apply(s1, s2) / (double) maxLen);

        return (jwScore * 0.65) + (levScore * 0.35);
    }

    @Override
    public boolean isMatch(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return false;
        }

        return calculateSimilarity(s1, s2) >= combinedThreshold;
    }
}
