package com.turtletracerlib;

import static org.junit.Assert.*;

import com.pedropathing.math.Pose;
import com.turtletracerlib.auto.FieldMirror;
import com.turtletracerlib.auto.MirrorAxes;
import org.junit.Test;

public class TurtleTracerReaderMirrorTest {
    private static final String JSON = "{\"version\":\"2.2.1\","
            + "\"startPoint\":{\"x\":9,\"y\":25,\"heading\":\"linear\",\"startDeg\":0,\"endDeg\":90},"
            + "\"lines\":[{\"id\":\"l1\",\"name\":\"Drive\","
            + "\"endPoint\":{\"x\":60,\"y\":30,\"heading\":\"constant\",\"degrees\":90},"
            + "\"controlPoints\":[{\"x\":40,\"y\":20}]}],"
            + "\"sequence\":[],\"shapes\":[]}";

    @Test
    public void unmirroredByDefault() {
        TurtleTracerReader reader = new TurtleTracerReader(JSON);
        Pose start = reader.get("startPoint");
        assertEquals(9, start.x(), 1e-9);
        assertEquals(25, start.y(), 1e-9);
        assertEquals(0, start.heading(), 1e-9);
    }

    @Test
    public void mirrorsStartEndAndControlPoints() {
        TurtleTracerReader reader = new TurtleTracerReader(JSON).mirrored(FieldMirror.of(MirrorAxes.BOTH));

        Pose start = reader.get("startPoint");
        assertEquals(135, start.x(), 1e-9);
        assertEquals(119, start.y(), 1e-9);
        assertEquals(Math.PI, start.heading(), 1e-9);

        Pose drive = reader.get("Drive");
        assertEquals(84, drive.x(), 1e-9);
        assertEquals(114, drive.y(), 1e-9);
        assertEquals(Math.toRadians(270), drive.heading(), 1e-9);

        assertEquals(104, reader.get("Drive_line0_control1").x(), 1e-9);
        assertEquals(135, reader.getAllPoses().get("startPoint").x(), 1e-9);
    }

    @Test
    public void nullMirrorMeansNone() {
        TurtleTracerReader reader = new TurtleTracerReader(JSON).mirrored(null);
        assertEquals(9, reader.get("startPoint").x(), 1e-9);
        assertNull(reader.get("missing"));
    }
}
