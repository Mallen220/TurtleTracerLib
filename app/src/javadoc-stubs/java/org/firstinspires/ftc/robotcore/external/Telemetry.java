package org.firstinspires.ftc.robotcore.external;

/**
 * Minimal stub of the FTC Telemetry interface for Javadoc generation.
 * This file is only used to allow Javadoc to compile and is not part of the real SDK.
 */
public interface Telemetry {
    void addData(String caption, Object value);
    void addLine(String lineCaption);
    void update();
}
