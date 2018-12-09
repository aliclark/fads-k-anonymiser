package com.privitar;

public class OutputRecord {
    /** The time that this record was received by the filter, in seconds from the start time */
    public final int inputTime;
    /** The time that this record was released by the filter, in seconds from the start time */
    public final int outputTime;
    public final double rawValue;
    public final double anonymisedValue;

    public OutputRecord(InputRecord input, int outputTime, double anonymisedValue) {
        this.inputTime = input.getTime();
        this.rawValue = input.getRawValue();
        this.outputTime = outputTime;
        this.anonymisedValue = anonymisedValue;
    }

    public int getInputTime() {
        return inputTime;
    }

    public int getOutputTime() {
        return outputTime;
    }

    public double getRawValue() {
        return rawValue;
    }

    public double getAnonymisedValue() {
        return anonymisedValue;
    }

    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append(inputTime).append(",").append(outputTime);
        sb.append(",");
        sb.append(rawValue).append(",").append(anonymisedValue);
        return sb.toString();
    }
}


