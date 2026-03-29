package string_matcher.algorithms;

import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.apache.commons.text.similarity.LevenshteinDistance;
import string_matcher.core.SimilarityMetric;

public class CombinedSimilarityMetric implements SimilarityMetric {
    private final JaroWinklerSimilarity jaroWinkler = new JaroWinklerSimilarity();
    private final LevenshteinDistance levenshtein = new LevenshteinDistance();

    private final double combinedThreshold;
    private final double maxRelativeLevenshtein;

    private static final double JW_WEIGHT = 0.6;
    private static final double LEV_WEIGHT = 0.4;

    /**
     * @param combinedThreshold        minimum weighted score (0.6*JW + 0.4*normLev) to consider a match
     * @param maxRelativeLevenshtein    maximum ratio of editDistance/maxLen (e.g. 0.30 = 30% edits allowed)
     */
    public CombinedSimilarityMetric(double combinedThreshold, double maxRelativeLevenshtein) {
        this.combinedThreshold = combinedThreshold;
        this.maxRelativeLevenshtein = maxRelativeLevenshtein;
    }

    @Override
    public double calculateSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return 0.0;
        }

        double jwScore = jaroWinkler.apply(s1, s2);
        int maxLen = Math.max(s1.length(), s2.length());
        if (maxLen == 0) {
            return 1.0;
        }
        int levDist = levenshtein.apply(s1, s2);
        double normalizedLev = 1.0 - ((double) levDist / maxLen);

        return JW_WEIGHT * jwScore + LEV_WEIGHT * normalizedLev;
    }

    @Override
    public boolean isMatch(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return false;
        }

        int len1 = s1.length();
        int len2 = s2.length();

        // Early rejection: if length ratio < 0.5, strings can't possibly be similar enough
        if (len1 == 0 || len2 == 0) {
            return false;
        }
        double lengthRatio = (double) Math.min(len1, len2) / Math.max(len1, len2);
        if (lengthRatio < 0.4) {
            return false;
        }

        int maxLen = Math.max(len1, len2);
        int levDist = levenshtein.apply(s1, s2);

        // Reject if relative Levenshtein distance exceeds threshold
        double relativeLev = (double) levDist / maxLen;
        if (relativeLev > maxRelativeLevenshtein) {
            return false;
        }

        // Compute weighted combined score
        double jwScore = jaroWinkler.apply(s1, s2);
        double normalizedLev = 1.0 - relativeLev;
        double combinedScore = JW_WEIGHT * jwScore + LEV_WEIGHT * normalizedLev;

        return combinedScore >= combinedThreshold;
    }
}
