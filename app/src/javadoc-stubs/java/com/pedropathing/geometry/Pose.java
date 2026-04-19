package com.pedropathing.geometry;

/** Minimal stub of Pose for Javadoc generation and testing */
public class Pose {
    private double x, y, heading;
    public Pose(double x, double y, double heading) { this.x = x; this.y = y; this.heading = heading; }
    public Pose(double x, double y) { this.x = x; this.y = y; }
    public Pose() {}
    public double getX() { return x; }
    public double getY() { return y; }
    public double getHeading() { return heading; }
}
