package string_matcher.core;

import java.util.List;

public interface RecordWriter {
    void writeClusters(List<List<Record>> clusters) throws Exception;
}
