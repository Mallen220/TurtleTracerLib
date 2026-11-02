package com.turtletracerlib.auto;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/**
 * Annotated classes whose constructors take more than a context, as code exported from Turtle
 * Tracer does, are built with what the registry is given, or left out with a reason: never added
 * and then failing when the driver presses Start.
 */
public class ScanDependenciesTest {
    public interface FakeCommand {}
    public static class FakeFollower {}
    public static class FakeHardwareMap {}

    @Before
    public void reset() {
        AutoSelection.reset();
    }

    @TurtleAuto(name = "Exported", drawnFor = Alliance.BLUE)
    public static class Exported implements FakeCommand {
        final FakeFollower follower;
        final FakeHardwareMap hw;
        final AutoContext ctx;
        public Exported(FakeFollower follower, FakeHardwareMap hw, AutoContext ctx) {
            this.follower = follower;
            this.hw = hw;
            this.ctx = ctx;
        }
    }

    @TurtleAuto(name = "Two constructors")
    public static class TwoConstructors implements FakeCommand {
        final boolean full;
        public TwoConstructors() { full = false; }
        public TwoConstructors(FakeFollower follower) { full = true; }
    }

    @TurtleAuto(name = "Throws")
    public static class Throws implements FakeCommand {
        public Throws(FakeFollower follower) { throw new IllegalArgumentException("file missing"); }
    }

    private static List<Class<?>> all() {
        return Arrays.<Class<?>>asList(Exported.class, TwoConstructors.class);
    }

    @Test
    public void leavesOutWhatItCantBuildAndSaysWhy() {
        AutoRegistry<FakeCommand> r = new AutoRegistry<FakeCommand>().scan(FakeCommand.class, all());
        assertEquals(-1, r.indexOf("Exported"));
        assertEquals(1, r.skipped().size());
        assertEquals("Skipped Exported: needs FakeFollower, FakeHardwareMap (give it to using(...))", r.skipped().get(0));
        // The no-argument constructor still works without anything given.
        assertFalse(((TwoConstructors) r.entries().get(r.indexOf("Two constructors")).create(AutoContext.unmirrored())).full);
    }

    @Test
    public void buildsWithWhatItIsGiven() {
        FakeFollower follower = new FakeFollower();
        FakeHardwareMap hw = new FakeHardwareMap();
        AutoRegistry<FakeCommand> r = new AutoRegistry<FakeCommand>()
                .using(follower, hw, null)
                .scan(FakeCommand.class, all());
        assertTrue(r.skipped().isEmpty());

        AutoEntry<FakeCommand> entry = r.entries().get(r.indexOf("Exported"));
        assertEquals(Alliance.BLUE, entry.getDrawnFor());
        AutoContext ctx = AutoContext.forAlliance(Alliance.RED, Alliance.BLUE);
        Exported built = (Exported) entry.create(ctx);
        assertSame(follower, built.follower);
        assertSame(hw, built.hw);
        assertSame(ctx, built.ctx);
        // The constructor that takes the most it can be given is used.
        assertTrue(((TwoConstructors) r.entries().get(r.indexOf("Two constructors")).create(ctx)).full);
    }

    @Test
    public void reportsWhatTheAutosOwnConstructorThrew() {
        AutoRegistry<FakeCommand> r = new AutoRegistry<FakeCommand>()
                .using(new FakeFollower())
                .scan(FakeCommand.class, Arrays.<Class<?>>asList(Throws.class));
        try {
            r.entries().get(0).create(AutoContext.unmirrored());
            fail();
        } catch (IllegalStateException e) {
            assertTrue(e.getCause() instanceof IllegalArgumentException);
            assertEquals("file missing", e.getCause().getMessage());
        }
    }

    @Test
    public void theMenuSaysWhatWasSkipped() {
        AutoRegistry<FakeCommand> r = new AutoRegistry<FakeCommand>()
                .add("Manual", c -> new FakeCommand() {})
                .scan(FakeCommand.class, all());
        AutoSelector<FakeCommand> s = new AutoSelector<>(r, AutoConfig.defaults());
        assertTrue(s.menuLines().contains(
                "! Skipped Exported: needs FakeFollower, FakeHardwareMap (give it to using(...))"));
    }
}
