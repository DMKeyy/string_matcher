package string_matcher.algorithms;

import string_matcher.core.ClusteringStrategy;
import string_matcher.core.Record;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class UnionFindClustering implements ClusteringStrategy {
    private final ConcurrentHashMap<Integer, Integer> parent = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Integer> rank = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Record> recordMap = new ConcurrentHashMap<>();

    // Stripe locks for fine-grained concurrency (avoid a single global lock)
    private static final int LOCK_STRIPES = 64;
    private final ReentrantLock[] locks = new ReentrantLock[LOCK_STRIPES];

    public UnionFindClustering() {
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
        recordMap.putIfAbsent(r.id(), r);
    }

    /**
     * Iterative find with path compression — prevents stack overflow on large datasets.
     */
    private int find(int i) {
        // Walk up to root
        int current = i;
        while (true) {
            Integer p = parent.get(current);
            if (p == null || p == current) {
                break;
            }
            current = p;
        }
        int root = current;

        // Path compression: point all nodes on the path directly to root
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

        // Acquire locks in consistent order to prevent deadlocks
        int id1 = r1.id();
        int id2 = r2.id();
        ReentrantLock lock1 = lockFor(Math.min(id1, id2));
        ReentrantLock lock2 = lockFor(Math.max(id1, id2));

        lock1.lock();
        try {
            if (lock1 != lock2) {
                lock2.lock();
            }
            try {
                int root1 = find(id1);
                int root2 = find(id2);

                if (root1 != root2) {
                    // Union by rank for balanced trees
                    int rank1 = rank.getOrDefault(root1, 0);
                    int rank2 = rank.getOrDefault(root2, 0);
                    if (rank1 < rank2) {
                        parent.put(root1, root2);
                    } else if (rank1 > rank2) {
                        parent.put(root2, root1);
                    } else {
                        parent.put(root2, root1);
                        rank.put(root1, rank1 + 1);
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
        Map<Integer, List<Record>> clusters = new HashMap<>();

        for (int id : parent.keySet()) {
            int root = find(id);
            clusters.computeIfAbsent(root, k -> new ArrayList<>()).add(recordMap.get(id));
        }

        return new ArrayList<>(clusters.values());
    }
}
