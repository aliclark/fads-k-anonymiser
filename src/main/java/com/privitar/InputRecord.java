package com.privitar;

public class InputRecord {

    /** The time that this record was received by the filter, in seconds from the start time */
    private final int time;
    private final double rawValue;

    public InputRecord(int time, double rawValue) {
        this.time = time;
        this.rawValue = rawValue;
    }

    public int getTime() {
        return time;
    }

    public double getRawValue() {
        return rawValue;
    }

    @Override
    public String toString() {
        return time + "," + rawValue;
    }
}
