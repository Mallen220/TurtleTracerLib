package com.pedropathing.follower;
import com.pedropathing.geometry.Pose;
import com.pedropathing.paths.Path;
public interface Follower {
    int getChainIndex();
    void turnTo(double radians);
    Pose getPose();
    boolean isTurning();
    Path getCurrentPath();
    double getCurrentTValue();
    boolean isBusy();
    void breakFollowing();
}
