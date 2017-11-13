package edu.stanford.futuredata.macrobase.analysis.summary.itemset.result;

import java.util.Set;

public class ItemsetResult {
    private double support;
    private double numRecords;
    private double inlierCount;
    private double ratioToInliers;
    private Set<Integer> items;

    public ItemsetResult(double support,
                         double numRecords,
                         double inlierCount,
                         double ratioToInliers,
                         Set<Integer> items) {
        this.support = support;
        this.numRecords = numRecords;
        this.inlierCount = inlierCount;
        this.ratioToInliers = ratioToInliers;
        this.items = items;
    }

    public double getSupport() {
        return support;
    }

    public double getNumRecords() {
        return numRecords;
    }

    public double getInlierCount() { return inlierCount; }

    public double getRatioToInliers() {
        return ratioToInliers;
    }

    public Set<Integer> getItems() {
        return items;
    }

    @Override
    public String toString() {
        return "ItemsetResult{" +
                "support=" + support +
                ", numRecords=" + numRecords +
                ", ratioToInliers=" + ratioToInliers +
                ", items=" + items +
                '}';
    }
}