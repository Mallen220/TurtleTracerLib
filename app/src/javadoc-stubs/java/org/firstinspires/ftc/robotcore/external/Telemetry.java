package org.firstinspires.ftc.robotcore.external;

public interface Telemetry {
    void addData(String caption, Object value);
    void addData(String caption, String format, Object... args);
    void addLine(String line);
    void update();
}
