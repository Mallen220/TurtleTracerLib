package com.turtletracerlib.auto;

import static org.junit.Assert.*;

import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;

/** Each auto (path) decides which alliance it is drawn for, and so whether it is mirrored. */
public class PerPathMirroringTest {
    @Before
    public void reset() {
        AutoSelection.reset();
    }

    @TurtleAuto(name = "Blue path", drawnFor = Alliance.BLUE)
    public static class BlueAuto {
        public BlueAuto() {}
    }

    private static AutoContext context(Alliance alliance, MirrorOverride override, MirrorPolicy policy) {
        return new AutoContext(alliance, Alliance.RED, override, policy, MirrorAxes.BOTH, 144);
    }

    @Test
    public void drawnForReDecidesForThatSide() {
        AutoContext red = context(Alliance.BLUE, MirrorOverride.AUTO, MirrorPolicy.FOLLOW_ALLIANCE);
        assertTrue(red.isMirrored());

        AutoContext blue = red.drawnFor(Alliance.BLUE);
        assertFalse(blue.isMirrored());
        assertEquals(Alliance.BLUE, blue.baseAlliance());
        assertEquals(Alliance.BLUE, blue.alliance());
        assertEquals(9, blue.x(9), 1e-9);

        assertTrue(context(Alliance.RED, MirrorOverride.AUTO, MirrorPolicy.FOLLOW_ALLIANCE)
                .drawnFor(Alliance.BLUE).isMirrored());
    }

    @Test
    public void drawnForKeepsTheDriversOverrideAndPolicy() {
        assertTrue(context(Alliance.BLUE, MirrorOverride.ON, MirrorPolicy.FOLLOW_ALLIANCE)
                .drawnFor(Alliance.BLUE).isMirrored());
        assertFalse(context(Alliance.RED, MirrorOverride.OFF, MirrorPolicy.FOLLOW_ALLIANCE)
                .drawnFor(Alliance.BLUE).isMirrored());
        assertFalse(context(Alliance.RED, MirrorOverride.AUTO, MirrorPolicy.NEVER)
                .drawnFor(Alliance.BLUE).isMirrored());
        assertTrue(context(Alliance.BLUE, MirrorOverride.AUTO, MirrorPolicy.ALWAYS)
                .drawnFor(Alliance.BLUE).isMirrored());
    }

    @Test
    public void noAllianceSelectedNeverMirrorsWhicheverSideTheAutoIsDrawnFor() {
        AutoContext none = AutoContext.unmirrored();
        assertFalse(none.drawnFor(Alliance.BLUE).isMirrored());
        assertSame(none, none.drawnFor(Alliance.NONE));
    }

    @Test
    public void eachEntryCanBeDrawnForItsOwnAlliance() {
        AutoRegistry<AutoContext> r = new AutoRegistry<AutoContext>()
                .add("Red path", c -> c)
                .add("Blue path", c -> c, Alliance.BLUE);
        AutoSelector<AutoContext> s = new AutoSelector<>(r, AutoConfig.defaults());
        s.setAlliance(Alliance.BLUE);

        assertTrue(s.isMirrored());
        assertTrue(s.buildSelected().isMirrored());

        AutoEntry<AutoContext> blue = r.entries().get(1);
        assertFalse(AutoSelector.resolveMirrored(AutoConfig.defaults(), blue, Alliance.BLUE, MirrorOverride.AUTO));
        assertTrue(AutoSelector.resolveMirrored(AutoConfig.defaults(), blue, Alliance.RED, MirrorOverride.AUTO));
    }

    @Test
    public void createContextUsesTheSelectedEntrysSide() {
        AutoRegistry<AutoContext> r = new AutoRegistry<AutoContext>().add("Blue path", c -> c, Alliance.BLUE);
        AutoSelector<AutoContext> s = new AutoSelector<>(r, AutoConfig.defaults());
        s.setAlliance(Alliance.RED);
        AutoContext ctx = s.createContext();
        assertEquals(Alliance.BLUE, ctx.baseAlliance());
        assertTrue(ctx.isMirrored());
        assertEquals(135, ctx.x(9), 1e-9);
    }

    @Test
    public void annotationSetsTheSide() {
        AutoRegistry<Object> r = new AutoRegistry<>()
                .scan(Object.class, Arrays.<Class<?>>asList(BlueAuto.class));
        assertEquals(Alliance.BLUE, r.entries().get(0).getDrawnFor());
    }

    @Test
    public void anAutoRecordsItsAllianceAndWhetherItMirrors() {
        AutoSelection.record(Alliance.BLUE, false);
        assertEquals(Alliance.BLUE, AutoSelection.alliance());
        assertFalse(AutoSelection.isMirrored());
        // Nothing else about the selection changes.
        assertFalse(AutoSelection.hasSelection());
        assertNull(AutoSelection.autoName());
        AutoSelection.record((Alliance) null, false);
        assertEquals(Alliance.NONE, AutoSelection.alliance());

        // From a context: the alliance picked, or the one it's drawn for if none was.
        AutoSelection.record(AutoContext.forAlliance(Alliance.RED, Alliance.BLUE));
        assertEquals(Alliance.RED, AutoSelection.alliance());
        assertTrue(AutoSelection.isMirrored());
        AutoSelection.record(AutoContext.forAlliance(Alliance.NONE, Alliance.BLUE));
        assertEquals(Alliance.BLUE, AutoSelection.alliance());
        assertFalse(AutoSelection.isMirrored());
    }

    @Test
    public void anAutoCanSetItsOwnMirrorPolicy() {
        // Registered never to mirror, but the auto's own code says it follows the alliance.
        AutoContext never = new AutoContext(
                Alliance.RED, Alliance.BLUE, MirrorOverride.AUTO, MirrorPolicy.NEVER, MirrorAxes.BOTH, 144);
        assertFalse(never.isMirrored());
        AutoContext follows = never.mirroring(MirrorPolicy.FOLLOW_ALLIANCE);
        assertTrue(follows.isMirrored());
        assertEquals(MirrorPolicy.FOLLOW_ALLIANCE, follows.mirrorPolicy());
        assertEquals(135, follows.x(9), 1e-9);

        // And the other way: a config that always mirrors, an auto that never does.
        AutoContext always = new AutoContext(
                Alliance.BLUE, Alliance.BLUE, MirrorOverride.AUTO, MirrorPolicy.ALWAYS, MirrorAxes.BOTH, 144);
        assertTrue(always.isMirrored());
        assertFalse(always.mirroring(MirrorPolicy.NEVER).isMirrored());

        // The driver's override still wins over the policy the code asks for.
        AutoContext forcedOff = new AutoContext(
                Alliance.RED, Alliance.BLUE, MirrorOverride.OFF, MirrorPolicy.NEVER, MirrorAxes.BOTH, 144);
        assertFalse(forcedOff.mirroring(MirrorPolicy.FOLLOW_ALLIANCE).isMirrored());
        assertSame(never, never.mirroring(null));
    }
}
