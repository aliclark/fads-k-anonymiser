package clark;

import com.privitar.InputRecord;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// This code could be improved by generalising the KNN method across types and distance metric
class InputRecordsKNN {
    private final Collection<InputRecord> records;

    InputRecordsKNN(Collection<InputRecord> records) {
        this.records = records;
    }

    /**
     * If there are k-1 or more records existing, find the k-1 nearest to record and return all k values including
     * record.
     * <p>
     * This is a naive implementation with O(n.log n) time complexity - O(k.n) time complexity can be achieved by
     * accumulating the best candidates and may be worth exploring after benchmarking.
     */
    Collection<InputRecord> getKNearestNeighboursInclusive(int k, InputRecord record) {
        if (records.size() < k - 1) {
            return Collections.emptyList();
        }
        return Stream.concat(Stream.of(record),
                records.stream().map(candidate -> new DistanceRecordPair(getDistance(candidate, record), candidate)).sorted()
                        .limit(k - 1).map(candidateDistanceDistanceRecordPair -> candidateDistanceDistanceRecordPair.record))
                .collect(Collectors.toList());
    }

    // Simplified distance calculation for single data point
    private double getDistance(InputRecord a, InputRecord b) {
        return Math.abs(a.getRawValue() - b.getRawValue());
    }

    private static class DistanceRecordPair implements Comparable<DistanceRecordPair> {
        private final double distance;
        private final InputRecord record;

        DistanceRecordPair(double distance, InputRecord record) {
            this.distance = distance;
            this.record = record;
        }

        @Override
        public int compareTo(@NotNull DistanceRecordPair other) {
            if (distance < other.distance) {
                return -1;
            } else if (distance > other.distance) {
                return 1;
            }
            // Prefer to use older points (as lower distance) in case of a tie to lean in favour of expunging older records
            return Integer.compare(record.getTime(), other.record.getTime());
        }
    }
}