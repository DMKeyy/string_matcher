package string_matcher.algorithms;

import string_matcher.core.ClusteringStrategy;
import string_matcher.core.Record;
import string_matcher.core.SimilarityMetric;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class UnionFindClustering implements ClusteringStrategy {
    private final ConcurrentHashMap<Integer, Integer> parent = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Integer> rank = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Integer> size = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Record> recordMap = new ConcurrentHashMap<>();

    private final int maxClusterSize;
    private final SimilarityMetric metric;

    private static final int LOCK_STRIPES = 64;
    private final ReentrantLock[] locks = new ReentrantLock[LOCK_STRIPES];

    public UnionFindClustering(int maxClusterSize, SimilarityMetric metric) {
        this.maxClusterSize = maxClusterSize;
        this.metric = metric;
        for (int i = 0; i < LOCK_STRIPES; i++) {
            locks[i] = new ReentrantLock();
        }
    }

    private ReentrantLock lockFor(int id) {
        return locks[Math.abs(id % LOCK_STRIPES)];
    }

    public void addRecord(Record r) {
        parent.putIfAbsent(r.id(), r.id());
        rank.putIfAbsent(r.id(), 0);
        size.putIfAbsent(r.id(), 1);
        recordMap.putIfAbsent(r.id(), r);
    }

    private int find(int i) {
        int current = i;
        while (true) {
            Integer p = parent.get(current);
            if (p == null || p == current) {
                break;
            }
            current = p;
        }
        int root = current;

        current = i;
        while (current != root) {
            Integer next = parent.get(current);
            parent.put(current, root);
            if (next == null) break;
            current = next;
        }
        return root;
    }

    @Override
    public void addMatch(Record r1, Record r2) {
        addRecord(r1);
        addRecord(r2);

        int id1 = r1.id();
        int id2 = r2.id();
        int stripe1 = Math.abs(id1 % LOCK_STRIPES);
        int stripe2 = Math.abs(id2 % LOCK_STRIPES);
        ReentrantLock lock1 = locks[Math.min(stripe1, stripe2)];
        ReentrantLock lock2 = locks[Math.max(stripe1, stripe2)];

        lock1.lock();
        try {
            if (lock1 != lock2) {
                lock2.lock();
            }
            try {
                int root1 = find(id1);
                int root2 = find(id2);

                if (root1 != root2) {
                    int size1 = size.getOrDefault(root1, 1);
                    int size2 = size.getOrDefault(root2, 1);
                    if (size1 + size2 > maxClusterSize) {
                        return;
                    }

                    int rank1 = rank.getOrDefault(root1, 0);
                    int rank2 = rank.getOrDefault(root2, 0);
                    if (rank1 < rank2) {
                        parent.put(root1, root2);
                        size.put(root2, size1 + size2);
                    } else if (rank1 > rank2) {
                        parent.put(root2, root1);
                        size.put(root1, size1 + size2);
                    } else {
                        parent.put(root2, root1);
                        rank.put(root1, rank1 + 1);
                        size.put(root1, size1 + size2);
                    }
                }
            } finally {
                if (lock1 != lock2) {
                    lock2.unlock();
                }
            }
        } finally {
            lock1.unlock();
        }
    }

    @Override
    public List<List<Record>> getClusters() {
        Map<Integer, List<Record>> rawClusters = new HashMap<>();
        for (int id : parent.keySet()) {
            int root = find(id);
            rawClusters.computeIfAbsent(root, k -> new ArrayList<>()).add(recordMap.get(id));
        }

        List<List<Record>> refined = new ArrayList<>();

        for (List<Record> cluster : rawClusters.values()) {
            if (cluster.size() <= 2) {
                refined.add(cluster);
                continue;
            }

            refined.addAll(refineCluster(cluster));
        }

        return refined;
    }

    private List<List<Record>> refineCluster(List<Record> cluster) {
        List<List<Record>> result = new ArrayList<>();

        Record centroid = findCentroid(cluster);

        List<Record> kept = new ArrayList<>();
        kept.add(centroid);

        List<Record> ejected = new ArrayList<>();

        for (Record r : cluster) {
            if (r.id() == centroid.id()) {
                continue;
            }
            if (metric.isMatch(centroid.normalizedString(), r.normalizedString())) {
                kept.add(r);
            } else {
                ejected.add(r);
            }
        }

        result.add(kept);

        for (Record r : ejected) {
            result.add(List.of(r));
        }

        return result;
    }

    private Record findCentroid(List<Record> cluster) {
        Record best = cluster.get(0);
        double bestAvg = -1;

        for (Record candidate : cluster) {
            double sum = 0;
            for (Record other : cluster) {
                if (candidate.id() != other.id()) {
                    sum += metric.calculateSimilarity(
                            candidate.normalizedString(), other.normalizedString());
                }
            }
            double avg = sum / (cluster.size() - 1);
            if (avg > bestAvg) {
                bestAvg = avg;
                best = candidate;
            }
        }
        return best;
    }
}
