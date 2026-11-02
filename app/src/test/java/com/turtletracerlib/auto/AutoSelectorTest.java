package com.turtletracerlib.auto;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class AutoSelectorTest {
    private static class FakeInput implements AutoInput {
        boolean up, down, left, right, mirror, lock;
        public boolean up() { return up; }
        public boolean down() { return down; }
        public boolean left() { return left; }
        public boolean right() { return right; }
        public boolean mirrorToggle() { return mirror; }
        public boolean lock() { return lock; }
    }

    private long now = 0;
    private final FakeInput in = new FakeInput();

    @Before
    public void reset() {
        AutoSelection.reset();
        now = 0;
    }

    private AutoRegistry<String> registry(int count) {
        AutoRegistry<String> r = new AutoRegistry<>();
        for (int i = 0; i < count; i++) {
            final String name = "Auto" + i;
            r.add(name, ctx -> name);
        }
        return r;
    }

    private AutoSelector<String> selector(AutoRegistry<String> r, AutoConfig config) {
        return new AutoSelector<>(r, config, () -> now);
    }

    private void tapDown(AutoSelector<String> s) {
        in.down = true; s.update(in);
        in.down = false; s.update(in);
    }

    private void tapUp(AutoSelector<String> s) {
        in.up = true; s.update(in);
        in.up = false; s.update(in);
    }

    private void tapRight(AutoSelector<String> s) {
        in.right = true; s.update(in);
        in.right = false; s.update(in);
    }

    private void tapLeft(AutoSelector<String> s) {
        in.left = true; s.update(in);
        in.left = false; s.update(in);
    }

    private void tapMirror(AutoSelector<String> s) {
        in.mirror = true; s.update(in);
        in.mirror = false; s.update(in);
    }

    private void tapLock(AutoSelector<String> s) {
        in.lock = true; s.update(in);
        in.lock = false; s.update(in);
    }

    @Test
    public void scrollsAndWraps() {
        AutoSelector<String> s = selector(registry(3), AutoConfig.defaults());
        tapDown(s);
        assertEquals(1, s.getSelectedIndex());
        tapDown(s);
        tapDown(s);
        assertEquals(0, s.getSelectedIndex());
        tapUp(s);
        assertEquals(2, s.getSelectedIndex());
    }

    @Test
    public void windowShowsOnlyMaxRowsAndFollowsSelection() {
        AutoSelector<String> s = selector(registry(10), AutoConfig.builder().maxVisibleRows(4).build());
        List<String> lines = s.menuLines();
        assertTrue(lines.contains(" > Auto0"));
        assertTrue(lines.contains("   Auto3"));
        assertFalse(lines.contains("   Auto4"));
        assertTrue(lines.contains("  v 6 more"));

        for (int i = 0; i < 5; i++) {
            tapDown(s);
        }
        lines = s.menuLines();
        assertTrue(lines.contains(" > Auto5"));
        assertFalse(lines.contains("   Auto1"));
        assertTrue(lines.contains("  ^ 2 more"));
        assertTrue(lines.contains("  v 4 more"));

        tapUp(s);
        tapUp(s);
        tapUp(s);
        tapUp(s);
        tapUp(s);
        tapUp(s);
        // wrapped from Auto0 to Auto9: window shows the last rows
        lines = s.menuLines();
        assertTrue(lines.contains(" > Auto9"));
        assertTrue(lines.contains("  ^ 6 more"));
    }

    @Test
    public void holdingARepeatsAfterDelay() {
        AutoSelector<String> s = selector(registry(10), AutoConfig.defaults());
        in.down = true;
        s.update(in);
        assertEquals(1, s.getSelectedIndex());
        now = 200;
        s.update(in);
        assertEquals(1, s.getSelectedIndex());
        now = 450;
        s.update(in);
        assertEquals(2, s.getSelectedIndex());
        now = 500;
        s.update(in);
        assertEquals(2, s.getSelectedIndex());
        now = 580;
        s.update(in);
        assertEquals(3, s.getSelectedIndex());
    }

    @Test
    public void allianceCyclesNoneRedBlue() {
        AutoSelector<String> s = selector(registry(2), AutoConfig.defaults());
        assertEquals(Alliance.NONE, s.getAlliance());
        tapRight(s);
        assertEquals(Alliance.RED, s.getAlliance());
        tapRight(s);
        assertEquals(Alliance.BLUE, s.getAlliance());
        tapRight(s);
        assertEquals(Alliance.NONE, s.getAlliance());
        tapLeft(s);
        assertEquals(Alliance.BLUE, s.getAlliance());
    }

    @Test
    public void allianceSelectionCanBeDisabled() {
        AutoSelector<String> s = selector(registry(2), AutoConfig.builder().allowAllianceSelection(false).build());
        tapRight(s);
        assertEquals(Alliance.NONE, s.getAlliance());
    }

    @Test
    public void lockFreezesChoices() {
        AutoSelector<String> s = selector(registry(3), AutoConfig.defaults());
        tapDown(s);
        tapLock(s);
        assertTrue(s.isLocked());
        tapDown(s);
        tapRight(s);
        assertEquals(1, s.getSelectedIndex());
        assertEquals(Alliance.NONE, s.getAlliance());
        tapLock(s);
        assertFalse(s.isLocked());
    }

    @Test
    public void defaultPolicyMirrorsOnlyWhenAllianceDiffersFromBase() {
        AutoConfig c = AutoConfig.defaults(); // base RED
        AutoEntry<String> e = new AutoEntry<>("a", ctx -> "a");
        assertFalse(AutoSelector.resolveMirrored(c, e, Alliance.NONE, MirrorOverride.AUTO));
        assertFalse(AutoSelector.resolveMirrored(c, e, Alliance.RED, MirrorOverride.AUTO));
        assertTrue(AutoSelector.resolveMirrored(c, e, Alliance.BLUE, MirrorOverride.AUTO));
    }

    @Test
    public void blueBaseFlipsTheOtherWay() {
        AutoConfig c = AutoConfig.builder().baseAlliance(Alliance.BLUE).build();
        AutoEntry<String> e = new AutoEntry<>("a", ctx -> "a");
        assertTrue(AutoSelector.resolveMirrored(c, e, Alliance.RED, MirrorOverride.AUTO));
        assertFalse(AutoSelector.resolveMirrored(c, e, Alliance.BLUE, MirrorOverride.AUTO));
    }

    @Test
    public void driverOverrideBeatsPolicy() {
        AutoConfig c = AutoConfig.defaults();
        AutoEntry<String> e = new AutoEntry<>("a", ctx -> "a");
        assertTrue(AutoSelector.resolveMirrored(c, e, Alliance.RED, MirrorOverride.ON));
        assertFalse(AutoSelector.resolveMirrored(c, e, Alliance.BLUE, MirrorOverride.OFF));
    }

    @Test
    public void entryNeverIsNotOverridable() {
        AutoConfig c = AutoConfig.defaults();
        AutoEntry<String> e = new AutoEntry<>("a", ctx -> "a").mirror(MirrorPolicy.NEVER);
        assertFalse(AutoSelector.resolveMirrored(c, e, Alliance.BLUE, MirrorOverride.ON));
    }

    @Test
    public void configPoliciesAreIndependentOfAlliance() {
        AutoEntry<String> e = new AutoEntry<>("a", ctx -> "a");
        AutoConfig never = AutoConfig.builder().mirrorPolicy(MirrorPolicy.NEVER).build();
        assertFalse(AutoSelector.resolveMirrored(never, e, Alliance.BLUE, MirrorOverride.AUTO));
        AutoConfig always = AutoConfig.builder().mirrorPolicy(MirrorPolicy.ALWAYS).build();
        assertTrue(AutoSelector.resolveMirrored(always, e, Alliance.NONE, MirrorOverride.AUTO));
    }

    @Test
    public void mirrorToggleCyclesOverrideAndIsIgnoredForNeverEntries() {
        AutoRegistry<String> r = new AutoRegistry<String>()
                .add("flippable", ctx -> "f")
                .add("fixed", ctx -> "x", MirrorPolicy.NEVER);
        AutoSelector<String> s = selector(r, AutoConfig.defaults());
        tapMirror(s);
        assertEquals(MirrorOverride.ON, s.getMirrorOverride());
        tapDown(s);
        tapMirror(s);
        assertEquals(MirrorOverride.ON, s.getMirrorOverride());
        assertFalse(s.isMirrored());
    }

    @Test
    public void buildSelectedPassesContext() {
        AutoRegistry<AutoContext> r = new AutoRegistry<AutoContext>().add("ctx", c -> c);
        AutoSelector<AutoContext> s = new AutoSelector<>(r, AutoConfig.defaults(), () -> now);
        s.setAlliance(Alliance.BLUE);
        AutoContext ctx = s.buildSelected();
        assertEquals(Alliance.BLUE, ctx.alliance());
        assertTrue(ctx.isMirrored());
        assertEquals(135, ctx.x(9), 1e-9);
        assertEquals("b", ctx.pick("r", "b"));
    }

    @Test
    public void noneAlliancePicksBaseAllianceFile() {
        AutoContext ctx = AutoContext.unmirrored();
        assertEquals("red.turt", ctx.pick("red.turt", "blue.turt"));
    }

    @Test
    public void selectionPersistsAndIsRestored() {
        AutoSelector<String> s = selector(registry(4), AutoConfig.defaults());
        tapDown(s);
        tapDown(s);
        tapRight(s);
        tapRight(s);
        assertEquals(Alliance.BLUE, AutoSelection.alliance());
        assertEquals("Auto2", AutoSelection.autoName());

        AutoSelector<String> again = selector(registry(4), AutoConfig.defaults());
        assertEquals(2, again.getSelectedIndex());
        assertEquals(Alliance.BLUE, again.getAlliance());
    }

    @Test(expected = IllegalStateException.class)
    public void buildWithNoAutosThrows() {
        selector(new AutoRegistry<String>(), AutoConfig.defaults()).buildSelected();
    }

    @Test
    public void emptyRegistryStillRendersAndIgnoresScroll() {
        AutoSelector<String> s = selector(new AutoRegistry<String>(), AutoConfig.defaults());
        tapDown(s);
        assertTrue(s.menuLines().contains("No autos registered"));
        assertNull(s.getSelected());
    }
}
