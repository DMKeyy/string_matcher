package string_matcher.core;

import java.util.List;
import java.util.concurrent.ForkJoinPool;
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
        long totalStart = System.currentTimeMillis();

        System.out.println("Reading records...");
        List<Record> records = reader.readRecords();
        int recordsLoaded = records.size();

        long indexStart = System.currentTimeMillis();
        System.out.println("Building Lucene Index...");
        index.buildIndex(records);
        long indexTime = System.currentTimeMillis() - indexStart;

        long matchStart = System.currentTimeMillis();
        System.out.println("Finding matches...");
        AtomicInteger processed = new AtomicInteger(0);

        int availableProcessors = Runtime.getRuntime().availableProcessors();
        int threadsToUse = Math.max(1, availableProcessors - 2);
        System.out.println("Using " + threadsToUse + " threads for " + recordsLoaded + " records.");

        ForkJoinPool customThreadPool = new ForkJoinPool(threadsToUse);
        try {
            customThreadPool.submit(() -> {
                records.parallelStream().forEach(record -> {
                    try {
                        List<Record> candidates = index.findCandidates(record, 100);

                        for (Record candidate : candidates) {
                            // Length-ratio pre-filter: skip obviously dissimilar pairs
                            int len1 = record.normalizedString().length();
                            int len2 = candidate.normalizedString().length();
                            if (len1 > 0 && len2 > 0) {
                                double lengthRatio = (double) Math.min(len1, len2) / Math.max(len1, len2);
                                if (lengthRatio < 0.5) {
                                    continue;
                                }
                            }

                            if (metric.isMatch(record.normalizedString(), candidate.normalizedString())) {
                                // ClusteringStrategy handles its own thread safety
                                strategy.addMatch(record, candidate);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    int count = processed.incrementAndGet();
                    if (count % 10000 == 0) {
                        long elapsed = System.currentTimeMillis() - matchStart;
                        double rate = count / (elapsed / 1000.0);
                        int remaining = recordsLoaded - count;
                        double etaSeconds = remaining / rate;
                        System.out.printf("Processed: %d / %d (%.1f%%) — ETA: %.0fs%n",
                                count, recordsLoaded,
                                (100.0 * count / recordsLoaded),
                                etaSeconds);
                    }
                });
            }).get();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            customThreadPool.shutdown();
        }

        System.out.println("Applying clusters...");
        List<List<Record>> clusters = strategy.getClusters();

        long duplicatedGroups = clusters.stream().filter(c -> c.size() > 1).count();
        long matchTime = System.currentTimeMillis() - matchStart;

        System.out.println("Writing output cluster to CSV...");
        writer.writeClusters(clusters);
        long totalTime = System.currentTimeMillis() - totalStart;

        System.out.println("\n--- Performance Summary ---");
        System.out.println("Records loaded: " + recordsLoaded);
        System.out.println("Index time: " + (indexTime / 1000.0) + " sec");
        System.out.println("Matching time: " + (matchTime / 1000.0) + " sec");
        System.out.println("Clusters found: " + duplicatedGroups);
        System.out.println("Total time: " + (totalTime / 1000.0) + " sec");
    }
}
