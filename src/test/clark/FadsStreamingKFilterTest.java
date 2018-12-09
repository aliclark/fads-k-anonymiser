package clark;

import clark.FadsStreamingKFilter;
import com.privitar.InputRecord;
import com.privitar.OutputRecord;
import org.junit.Test;

import java.util.Collection;

import static org.junit.Assert.*;

public class FadsStreamingKFilterTest {
    private int time = 0;

    @Test
    public void constructs() {
        new FadsStreamingKFilter();
        new FadsStreamingKFilter(5, 20, 100);
    }

    @Test
    public void immediateConsumption() {
        FadsStreamingKFilter filter = new FadsStreamingKFilter();
        assertTrue(filter.returnPublishableRecords().isEmpty());
    }

    @Test
    public void processSimpleRecord() {
        FadsStreamingKFilter filter = new FadsStreamingKFilter();
        filter.processNewRecord(new InputRecord(time++, 0.0));
        assertTrue(filter.returnPublishableRecords().isEmpty());
    }

    @Test
    public void sameKAndDelay() {
        FadsStreamingKFilter filter = new FadsStreamingKFilter(2, 2, 100);
        filter.processNewRecord(new InputRecord(time++, 1.0));
        filter.processNewRecord(new InputRecord(time++, 2.0));
        filter.processNewRecord(new InputRecord(time++, 3.0));
        filter.processNewRecord(new InputRecord(time++, 4.0));

        for (OutputRecord record : filter.returnPublishableRecords()) {
            switch (record.getInputTime()) {
                case 0: assertEquals(record.getAnonymisedValue(), 1.5, 0.001); continue;
                case 1: assertEquals(record.getAnonymisedValue(), 1.5, 0.001); continue;
                case 2: assertEquals(record.getAnonymisedValue(), 3.5, 0.001); continue;
                case 3: assertEquals(record.getAnonymisedValue(), 3.5, 0.001); continue;
            }
        }
    }

    @Test
    public void streamSomeValues() {
        FadsStreamingKFilter filter = new FadsStreamingKFilter(3, 6, 100);
        filter.processNewRecord(new InputRecord(time++, 4.0));
        filter.processNewRecord(new InputRecord(time++, 1.0));
        filter.processNewRecord(new InputRecord(time++, 6.0));
        filter.processNewRecord(new InputRecord(time++, 3.0));
        filter.processNewRecord(new InputRecord(time++, 8.0));
        filter.processNewRecord(new InputRecord(time++, 2.0));
        filter.processNewRecord(new InputRecord(time++, 5.0));
        filter.processNewRecord(new InputRecord(time++, 9.0));
        filter.processNewRecord(new InputRecord(time++, 1.0));
        filter.processNewRecord(new InputRecord(time++, 6.0));
        filter.processNewRecord(new InputRecord(time++, 4.0));
        filter.processNewRecord(new InputRecord(time++, 7.0));

        System.out.println(filter.getAverageInfoLossRatioMetric());
        System.out.println(filter.getAverageLatencyMetric());
    }

    // This code is not as well tested as I'd like. One area I would focus on testing first is cluster selection
    // firstly from within reusable clusters, ensuring the best cluster is always chosen even in overlapping cases.
    //
    // Cluster expiring would be another good area to test, as would the comparison between new and cached cluster,
    // and code involving the creation of a new cluster.
    //
    // It would also be good to test that metrics operate correctly and in edge cases, and other likely edge case scenarios.
}
