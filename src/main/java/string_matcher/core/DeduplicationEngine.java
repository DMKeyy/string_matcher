package string_matcher.core;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class DeduplicationEngine {
    private final RecordReader reader;
    private final RecordWriter writer;
    private final CandidateGenerator index;
    private final SimilarityMetric metric;
    private final ClusteringStrategy strategy;

    public DeduplicationEngine(RecordReader reader, RecordWriter writer, CandidateGenerator index,
                               SimilarityMetric metric, ClusteringStrategy strategy) {
        this.reader = reader;
        this.writer = writer;
        this.index = index;
        this.metric = metric;
        this.strategy = strategy;
    }

    public void run() throws Exception {
        System.out.println("Reading records...");
        List<Record> records = reader.readRecords();
        System.out.println("Total records read: " + records.size());

        System.out.println("Building Lucene Index...");
        index.buildIndex(records);
        System.out.println("Index built.");

        System.out.println("Finding matches...");
        AtomicInteger processed = new AtomicInteger(0);

        records.parallelStream().forEach(record -> {
            try {
                // Build a larger list to prevent truncation of identical names
                List<Record> candidates = index.findCandidates(record, 100);
                
                for (Record candidate : candidates) {
                    // UnionFind is idempotent, so we can let both A->B and B->A be checked.
                    // This ensures matches aren't missed if one side falls out of the top 100 rankings.
                    if (metric.isMatch(record.normalizedString(), candidate.normalizedString())) {
                        synchronized(strategy) {
                            strategy.addMatch(record, candidate);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            int count = processed.incrementAndGet();
            if (count % 10000 == 0) {
                System.out.println("Processed: " + count + " records.");
            }
        });

        System.out.println("Applying clusters...");
        List<List<Record>> clusters = strategy.getClusters();
        
        long duplicatedGroups = clusters.stream().filter(c -> c.size() > 1).count();
        System.out.println("Found " + duplicatedGroups + " unique clusters of duplicates.");

        System.out.println("Writing output cluster to CSV...");
        writer.writeClusters(clusters);
        System.out.println("Done.");
    }
}
