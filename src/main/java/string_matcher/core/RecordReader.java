package string_matcher.core;

import java.util.List;

public interface RecordReader {
    List<Record> readRecords() throws Exception;
}
