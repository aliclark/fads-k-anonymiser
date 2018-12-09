package clark;

import com.google.common.collect.Range;
import com.privitar.InputRecord;

import java.util.Collection;
import java.util.stream.Collectors;

class Cluster {
    private final double anonymisedValue;
    private final int creationTime;
    private final Range<Double> range;

    Cluster(Collection<InputRecord> records, int creationTime) {
        this.anonymisedValue = records.stream().mapToDouble(record -> record.getRawValue()).average().getAsDouble();
        this.creationTime = creationTime;
        this.range = Range.encloseAll(records.stream().map(record -> record.getRawValue()).collect(Collectors.toList()));
    }

    double getAnonymisedValue() {
        return anonymisedValue;
    }

    double getAge(int currentTime) {
        return currentTime - creationTime;
    }

    boolean covers(InputRecord record) {
        return range.contains(record.getRawValue());
    }

    /**
     * XXX: Note that this is not the info loss definition according to the academic literature, which considers the
     * size of the range that the cluster represents in comparison to the domain value's range.
     * <p>
     * For the sake of accuracy focusing instead on finding the most similar anonymised value has the potential to yield
     * more accurate published records in practice
     */
    double getInfoLoss(InputRecord record) {
        return Math.abs(anonymisedValue - record.getRawValue());
    }

    boolean preferThan(Cluster other, InputRecord record) {
        double selfInfoLoss = getInfoLoss(record);
        double otherInfoLoss = other.getInfoLoss(record);
        if (selfInfoLoss < otherInfoLoss) {
            return true;
        } else if (selfInfoLoss > otherInfoLoss) {
            return false;
        }
        // Prefer to use older clusters (as lower info loss) in case of a tie to lean in favour of cached clusters
        return creationTime < other.creationTime;
    }
}
