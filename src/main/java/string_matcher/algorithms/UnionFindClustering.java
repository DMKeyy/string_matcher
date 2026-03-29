package string_matcher.algorithms;

import string_matcher.core.ClusteringStrategy;
import string_matcher.core.Record;

import java.util.*;

public class UnionFindClustering implements ClusteringStrategy {
    private final Map<Integer, Integer> parent = new HashMap<>();
    private final Map<Integer, Record> recordMap = new HashMap<>();

    public void addRecord(Record r) {
        parent.putIfAbsent(r.id(), r.id());
        recordMap.putIfAbsent(r.id(), r);
    }

    private int find(int i) {
        if (parent.get(i) == i) {
            return i;
        }
        int root = find(parent.get(i));
        parent.put(i, root);
        return root;
    }

    @Override
    public void addMatch(Record r1, Record r2) {
        addRecord(r1);
        addRecord(r2);
        
        int root1 = find(r1.id());
        int root2 = find(r2.id());
        
        if (root1 != root2) {
            parent.put(root1, root2);
        }
    }

    @Override
    public List<List<Record>> getClusters() {
        Map<Integer, List<Record>> clusters = new HashMap<>();
        
        for (int id : parent.keySet()) {
            int root = find(id);
            clusters.computeIfAbsent(root, k -> new ArrayList<>()).add(recordMap.get(id));
        }
        
        return new ArrayList<>(clusters.values());
    }
}
