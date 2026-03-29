package string_matcher;

import string_matcher.algorithms.BusinessEntityNormalizer;
import string_matcher.algorithms.CombinedSimilarityMetric;
import string_matcher.algorithms.UnionFindClustering;
import string_matcher.core.*;
import string_matcher.infrastructure.CsvRecordAdapter;
import string_matcher.infrastructure.CsvRecordWriter;
import string_matcher.infrastructure.LuceneCandidateGenerator;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        try {
            Scanner scanner = new Scanner(System.in);
            
            // Default configuration
            String inputPath = "data/companyNamesLarge.csv";
            String outputPath = "data/clustered_companies.csv";
            double threshold = 0.92;
            double maxDistance = 0.28;
            int maxClusterSize = 50;

            // Interactive configuration loop
            while (true) {
                System.out.println("\n=== String Matcher Configuration ===");
                System.out.println("  Input Path       : " + inputPath);
                System.out.println("  Output Path      : " + outputPath);
                System.out.println("  Threshold        : " + threshold);
                System.out.println("  Max Distance     : " + maxDistance);
                System.out.println("  Max Cluster Size : " + maxClusterSize);
                System.out.println("====================================");
                System.out.println("1. Customize settings");
                System.out.println("2. Execute with current settings");
                System.out.print("Select an option (1 or 2): ");
                
                String choice = scanner.nextLine().trim();
                if ("1".equals(choice)) {
                    System.out.print("Enter Input Path [" + inputPath + "]: ");
                    String in = scanner.nextLine().trim();
                    if (!in.isEmpty()) inputPath = in;

                    System.out.print("Enter Output Path [" + outputPath + "]: ");
                    String out = scanner.nextLine().trim();
                    if (!out.isEmpty()) outputPath = out;

                    System.out.print("Enter Threshold matching score (0.0 to 1.0) [" + threshold + "]: ");
                    String th = scanner.nextLine().trim();
                    if (!th.isEmpty()) threshold = Double.parseDouble(th);

                    System.out.print("Enter Max relative Distance (e.g., 0.25) [" + maxDistance + "]: ");
                    String md = scanner.nextLine().trim();
                    if (!md.isEmpty()) maxDistance = Double.parseDouble(md);

                    System.out.print("Enter Max Cluster Size [" + maxClusterSize + "]: ");
                    String ms = scanner.nextLine().trim();
                    if (!ms.isEmpty()) maxClusterSize = Integer.parseInt(ms);
                    
                } else if ("2".equals(choice)) {
                    break;
                } else {
                    System.out.println("\n*** Invalid option. Please enter 1 or 2. ***");
                }
            }

            System.out.println("\nStarting Deduplication against: " + inputPath);
            System.out.println(String.format("Settings: Threshold=%.2f, MaxDistance=%.2f, MaxClusterSize=%d", threshold, maxDistance, maxClusterSize));

            // Dependency Injection (Wiring)
            StringNormalizer normalizer = new BusinessEntityNormalizer();
            RecordReader reader = new CsvRecordAdapter(inputPath, normalizer);
            RecordWriter writer = new CsvRecordWriter(outputPath);
            CandidateGenerator index = new LuceneCandidateGenerator();

            // Apply configured settings
            SimilarityMetric metric = new CombinedSimilarityMetric(threshold, maxDistance);
            ClusteringStrategy clusterer = new UnionFindClustering(maxClusterSize, metric);

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