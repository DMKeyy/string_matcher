package string_matcher.algorithms;

import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.apache.commons.text.similarity.LevenshteinDistance;
import string_matcher.core.SimilarityMetric;

public class CombinedSimilarityMetric implements SimilarityMetric {
    private final JaroWinklerSimilarity jaroWinkler = new JaroWinklerSimilarity();
    private final LevenshteinDistance levenshtein;
    
    private final double jaroWinklerThreshold;
    private final int maxLevenshteinDistance;

    public CombinedSimilarityMetric(double jaroWinklerThreshold, int maxLevenshteinDistance) {
        this.jaroWinklerThreshold = jaroWinklerThreshold;
        this.maxLevenshteinDistance = maxLevenshteinDistance;
        this.levenshtein = new LevenshteinDistance(maxLevenshteinDistance);
    }

    @Override
    public double calculateSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0.0;
        return jaroWinkler.apply(s1, s2);
    }

    @Override
    public boolean isMatch(String s1, String s2) {
        if (s1 == s2) return true;
        if (s1 == null || s2 == null) return false;
        
        double similarity = jaroWinkler.apply(s1, s2);
        if (similarity >= jaroWinklerThreshold) {
            return true;
        }
        
        if (Math.abs(s1.length() - s2.length()) > maxLevenshteinDistance) {
            return false;
        }
        
        Integer distance = levenshtein.apply(s1, s2);
        return distance != null && distance >= 0 && distance <= maxLevenshteinDistance;
    }
}
