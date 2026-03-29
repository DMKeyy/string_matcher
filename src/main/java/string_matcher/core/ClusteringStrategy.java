package string_matcher.core;

import java.util.List;

public interface ClusteringStrategy {
    void addMatch(Record r1, Record r2);
    List<List<Record>> getClusters();
}
