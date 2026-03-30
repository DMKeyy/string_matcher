package string_matcher.infrastructure;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import string_matcher.core.Record;
import string_matcher.core.RecordReader;
import string_matcher.core.StringNormalizer;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CsvRecordAdapter implements RecordReader {
    private final String filePath;
    private final StringNormalizer normalizer;

    public CsvRecordAdapter(String filePath, StringNormalizer normalizer) {
        this.filePath = filePath;
        this.normalizer = normalizer;
    }

    @Override
    public List<Record> readRecords() throws IOException, CsvValidationException {
        List<Record> records = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] line;
            int id = 0;
            while ((line = reader.readNext()) != null) {
                if (line.length > 0) {
                    String original = String.join(",", line);
                    String normalized = normalizer.normalize(original);
                    records.add(new Record(id++, original, normalized));
                }
            }
        }
        return records;
    }
}