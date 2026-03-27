package com.qualcomm.robotcore.eventloop.opmode;

public abstract class LinearOpMode extends OpMode {
    public abstract void runOpMode() throws InterruptedException;

    public synchronized void waitForStart() {}
    public void idle() {}
    public void sleep(long milliseconds) {}
    public boolean opModeIsActive() { return false; }
    public boolean opModeInInit() { return false; }

    public boolean isStarted() { return false; }
    public boolean isStopRequested() { return false; }

    @Override
    public final void init() {}
    @Override
    public final void init_loop() {}
    @Override
    public final void start() {}
    @Override
    public final void loop() {}
    @Override
    public final void stop() {}
}