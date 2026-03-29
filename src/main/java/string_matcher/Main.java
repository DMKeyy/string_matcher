package string_matcher;

import string_matcher.algorithms.BusinessEntityNormalizer;
import string_matcher.algorithms.CombinedSimilarityMetric;
import string_matcher.algorithms.UnionFindClustering;
import string_matcher.core.*;
import string_matcher.infrastructure.CsvRecordAdapter;
import string_matcher.infrastructure.CsvRecordWriter;
import string_matcher.infrastructure.LuceneCandidateGenerator;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {
        try {
            String inputPath = "data/companyNamesLarge.csv";
            String outputPath = "data/clustered_companies.csv";

            System.out.println("Starting Deduplication against: " + inputPath);

            // Dependency Injection (Wiring)
            StringNormalizer normalizer = new BusinessEntityNormalizer();
            RecordReader reader = new CsvRecordAdapter(inputPath, normalizer);
            RecordWriter writer = new CsvRecordWriter(outputPath);
            CandidateGenerator index = new LuceneCandidateGenerator();

            // Combined score >= 0.82, max relative Levenshtein distance 0.40 (40% of string length)
            SimilarityMetric metric = new CombinedSimilarityMetric(0.82, 0.40);
            ClusteringStrategy clusterer = new UnionFindClustering();

            DeduplicationEngine engine = new DeduplicationEngine(reader, writer, index, metric, clusterer);

            long start = System.currentTimeMillis();
            engine.run();
            long end = System.currentTimeMillis();

            System.out.println("Finished in " + (end - start) + "ms");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}