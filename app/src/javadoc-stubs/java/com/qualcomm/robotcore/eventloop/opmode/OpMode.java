package com.qualcomm.robotcore.eventloop.opmode;

public abstract class OpMode {
    public com.qualcomm.robotcore.hardware.Gamepad gamepad1;
    public com.qualcomm.robotcore.hardware.Gamepad gamepad2;
    public org.firstinspires.ftc.robotcore.external.Telemetry telemetry;
    public com.qualcomm.robotcore.hardware.HardwareMap hardwareMap;

    public abstract void init();
    public void init_loop() {}
    public abstract void loop();
    public void start() {}
    public void stop() {}
}