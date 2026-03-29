package string_matcher.infrastructure;

import com.opencsv.CSVWriter;
import string_matcher.core.Record;
import string_matcher.core.RecordWriter;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class CsvRecordWriter implements RecordWriter {
    private final String outputPath;

    public CsvRecordWriter(String outputPath) {
        this.outputPath = outputPath;
    }

    @Override
    public void writeClusters(List<List<Record>> clusters) throws IOException {
        try (CSVWriter writer = new CSVWriter(new FileWriter(outputPath))) {
            writer.writeNext(new String[]{"ClusterId", "OriginalString"});
            
            int clusterId = 1;
            for (List<Record> cluster : clusters) {
                // We only care about clusters that have duplicates (size > 1)
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
