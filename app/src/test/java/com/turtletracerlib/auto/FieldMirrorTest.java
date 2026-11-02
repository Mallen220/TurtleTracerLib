package com.turtletracerlib.auto;

import static org.junit.Assert.*;

import com.pedropathing.math.Pose;
import org.junit.Test;

public class FieldMirrorTest {
    private static final double EPS = 1e-9;

    @Test
    public void identityChangesNothing() {
        FieldMirror m = FieldMirror.none();
        Pose p = new Pose(10, 20, 4.0);
        assertSame(p, m.pose(p));
        assertEquals(4.0, m.heading(4.0), EPS);
        assertTrue(m.isIdentity());
    }

    @Test
    public void bothAxesIsPointReflection() {
        FieldMirror m = FieldMirror.of(MirrorAxes.BOTH);
        Pose out = m.pose(new Pose(9, 25, 0));
        assertEquals(135, out.x(), EPS);
        assertEquals(119, out.y(), EPS);
        assertEquals(Math.PI, out.heading(), EPS);
    }

    @Test
    public void xOnlyFlipsXAndMirrorsHeading() {
        FieldMirror m = FieldMirror.of(MirrorAxes.X);
        Pose out = m.pose(new Pose(9, 25, Math.PI / 4));
        assertEquals(135, out.x(), EPS);
        assertEquals(25, out.y(), EPS);
        assertEquals(3 * Math.PI / 4, out.heading(), EPS);
    }

    @Test
    public void yOnlyFlipsYAndNegatesHeading() {
        FieldMirror m = FieldMirror.of(MirrorAxes.Y);
        Pose out = m.pose(new Pose(9, 25, Math.PI / 2));
        assertEquals(9, out.x(), EPS);
        assertEquals(119, out.y(), EPS);
        assertEquals(3 * Math.PI / 2, out.heading(), EPS);
    }

    @Test
    public void headingWrapsIntoZeroToTwoPi() {
        FieldMirror m = FieldMirror.of(MirrorAxes.BOTH);
        assertEquals(Math.PI / 2, m.heading(3 * Math.PI / 2), EPS);
        assertEquals(180, m.degrees(0), 1e-9);
        assertEquals(90, m.degrees(270), 1e-9);
    }

    @Test
    public void mirroringTwiceRestoresThePose() {
        FieldMirror m = FieldMirror.of(MirrorAxes.BOTH, 144);
        Pose p = new Pose(30, 40, 1.0);
        Pose back = m.pose(m.pose(p));
        assertEquals(30, back.x(), EPS);
        assertEquals(40, back.y(), EPS);
        assertEquals(1.0, back.heading(), EPS);
    }

    @Test
    public void customFieldSize() {
        FieldMirror m = FieldMirror.of(MirrorAxes.X, 100);
        assertEquals(70, m.x(30), EPS);
    }
}
