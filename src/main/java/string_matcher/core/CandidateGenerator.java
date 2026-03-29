package string_matcher.core;

import java.util.List;

public interface CandidateGenerator {
    void buildIndex(List<Record> records) throws Exception;
    List<Record> findCandidates(Record queryRecord, int maxCandidates) throws Exception;
}
