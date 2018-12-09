package clark;

import com.google.common.collect.Range;
import com.privitar.InputRecord;
import com.privitar.OutputRecord;
import com.privitar.StreamingKFilter;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Implements a StreamingKFilter using the FADS algorithm.
 * <p>
 * "Fast clustering-based anonymization approaches with time constraints for data streams" by Kun Guo, Qishan Zhang
 */
public class FadsStreamingKFilter implements StreamingKFilter {

    // Parameters from the FADS paper
    private static final int DEFAULT_K = 100;
    private static final int DEFAULT_DELAY_CONSTRAINT = 2000;
    private static final int DEFAULT_REUSE_CONSTRAINT = 200;

    private final ArrayDeque<InputRecord> recordsBuffer = new ArrayDeque<>(); // Set_tp in FADS paper
    private final ArrayList<Cluster> reusableClusters = new ArrayList<>(); // Set_kc in FADS paper

    private final InputRecordsKNN inputRecordsKNN = new InputRecordsKNN(recordsBuffer);
    private final int k;
    private final int delayConstraint; // delta in FADS paper
    private final int reuseConstraint; // T_kc in FADS paper
    private Collection<OutputRecord> publishableRecords = new ArrayDeque<>();
    private int currentTime;

    private int publishedRecords = 0;
    private double averageInfoLossMetric = 0.0;
    private double averageLatencyMetric = 0.0;

    private Range<Double> domainValueRange = null;

    public FadsStreamingKFilter() {
        this(DEFAULT_K, DEFAULT_DELAY_CONSTRAINT, DEFAULT_REUSE_CONSTRAINT);
    }

    /**
     * Construct a FADS StreamingKFilter
     *
     * @param k               The anonymity set >=2. When an anonymised value is published it will be for grouping of
     *                        >=k records
     * @param delayConstraint The time lapse from a record taken as input to it being published, delayConstraint>=k
     * @param reuseConstraint The maximum age that a cluster may be reused
     */
    public FadsStreamingKFilter(int k, int delayConstraint, int reuseConstraint) {
        if (k <= 1) {
            throw new IllegalArgumentException("K must be greater than 1");
        }
        if (delayConstraint < k) {
            throw new IllegalArgumentException("Delay constraint must be greater or equal to k");
        }

        this.k = k;
        this.delayConstraint = delayConstraint;
        this.reuseConstraint = reuseConstraint;
    }

    @Override
    public void processNewRecord(InputRecord input) {
        currentTime = input.getTime();
        recordsBuffer.add(input);

        Range<Double> inputAsRange = Range.singleton(input.getRawValue());
        domainValueRange = domainValueRange != null ? domainValueRange.span(inputAsRange) : inputAsRange;

        reusableClusters.removeIf(cluster -> cluster.getAge(currentTime) >= reuseConstraint);

        while (recordsBuffer.size() >= delayConstraint) {
            publishRecord(recordsBuffer.remove());
        }
    }

    // XXX: This method would be needed to cleanly shut down without losing values
    //public void returnPublishableRecordsFlush();

    @Override
    public Collection<OutputRecord> returnPublishableRecords() {
        if (publishableRecords.isEmpty()) {
            return Collections.emptyList();
        }
        Collection<OutputRecord> publishedRecords = publishableRecords;
        publishableRecords = new ArrayDeque<>();
        return publishedRecords;
    }

    public double getAverageInfoLossRatioMetric() {
        if (domainValueRange == null || domainValueRange.upperEndpoint().equals(domainValueRange.lowerEndpoint())) {
            return 0.0;
        }
        return averageInfoLossMetric / (domainValueRange.upperEndpoint() - domainValueRange.lowerEndpoint());
    }

    public double getAverageLatencyMetric() {
        return averageLatencyMetric;
    }

    private void publishRecord(InputRecord record) {
        Optional<Cluster> reusableCluster = getLeastInfoLossReusableCluster(record);

        Collection<InputRecord> newClusterRecords = inputRecordsKNN.getKNearestNeighboursInclusive(k, record);
        Cluster newCluster = new Cluster(newClusterRecords, currentTime);

        if (reusableCluster.isPresent() && reusableCluster.get().preferThan(newCluster, record)) {
            addPublishable(reusableCluster.get(), record);
            return;
        }

        reusableClusters.add(newCluster);

        for (InputRecord newClusterRecord : newClusterRecords) {
            addPublishable(newCluster, newClusterRecord);
        }
        recordsBuffer.removeAll(newClusterRecords);
    }

    // XXX: This is very naive and will accumulate floating point errors.
    // TODO: use a proper metrics system
    private double reAverage(double average, double value) {
        if (publishedRecords <= 0) {
            return value;
        }
        return ((average * publishedRecords) + value) / (publishedRecords + 1);
    }

    private void addPublishable(Cluster cluster, InputRecord record) {
        averageInfoLossMetric = reAverage(averageInfoLossMetric, cluster.getInfoLoss(record));
        averageLatencyMetric = reAverage(averageLatencyMetric, currentTime - record.getTime());

        publishableRecords.add(new OutputRecord(record, currentTime, cluster.getAnonymisedValue()));
        publishedRecords++;
    }

    // Naive impl with O(n.log n) time complexity.
    // O(n) time complexity is achievable by accumulating the best candidate, may be worth exploring after benchmarking.
    private Optional<Cluster> getLeastInfoLossReusableCluster(InputRecord record) {
        return reusableClusters.stream().filter(cluster -> cluster.covers(record))
                .map(cluster -> new InfoLossClusterPair(cluster.getInfoLoss(record), cluster)).sorted()
                .map(infoLossClusterPair -> infoLossClusterPair.cluster).findFirst();
    }

    private static class InfoLossClusterPair implements Comparable<InfoLossClusterPair> {
        private final double infoLoss;
        private final Cluster cluster;

        InfoLossClusterPair(double infoLoss, Cluster cluster) {
            this.infoLoss = infoLoss;
            this.cluster = cluster;
        }

        @Override
        public int compareTo(@NotNull InfoLossClusterPair other) {
            return Double.compare(infoLoss, other.infoLoss);
        }
    }
}
