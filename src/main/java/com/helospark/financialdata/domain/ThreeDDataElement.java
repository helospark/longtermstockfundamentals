package com.helospark.financialdata.domain;

public class ThreeDDataElement {
    public double x;
    public double y;
    public double r;
    public String description;

    public ThreeDDataElement(double x, double y, double r, String description) {
        this.x = x;
        this.y = y;
        this.r = r;
        this.description = description;
    }

    @Override
    public String toString() {
        return "ThreeDDataElement [x=" + x + ", y=" + y + ", r=" + r + "]";
    }

}
