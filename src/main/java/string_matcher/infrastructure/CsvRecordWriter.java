package string_matcher.infrastructure;

import com.opencsv.CSVWriter;
import string_matcher.core.Record;
import string_matcher.core.RecordWriter;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class CsvRecordWriter implements RecordWriter {
    private final String outputPath;
    private static final int MAX_CLUSTER_SIZE = 50;

    public CsvRecordWriter(String outputPath) {
        this.outputPath = outputPath;
    }

    @Override
    public void writeClusters(List<List<Record>> clusters) throws IOException {
        try (CSVWriter writer = new CSVWriter(new FileWriter(outputPath))) {
            writer.writeNext(new String[]{"ClusterId", "OriginalString"});
            
            int clusterId = 1;
            for (List<Record> cluster : clusters) {
                if (cluster.size() > MAX_CLUSTER_SIZE) {
                    System.err.println("WARN: Cluster of size " + cluster.size() + " detected and excluded from output. " +
                            "Sample members: " + cluster.stream().limit(3)
                            .map(Record::originalString)
                            .collect(Collectors.joining(" | ")) +
                            ". This likely indicates a false merge caused by shared suffixes " +
                            "or an overly loose similarity threshold.");
                    continue;
                }

                if (cluster.size() > 1) {
                    for (Record record : cluster) {
                        writer.writeNext(new String[]{String.valueOf(clusterId), record.originalString()});
                    }
                    clusterId++;
                }
            }
        }
    }
}
