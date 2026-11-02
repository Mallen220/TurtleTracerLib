package com.turtletracerlib.auto;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;

public class AutoRegistryTest {
    public interface FakeCommand {}

    @TurtleAuto(name = "Via Ctor", group = "Near")
    public static class CtorAuto implements FakeCommand {
        final AutoContext ctx;
        public CtorAuto(AutoContext ctx) { this.ctx = ctx; }
    }

    @TurtleAuto
    public static class DefaultAuto implements FakeCommand {
        public DefaultAuto() {}
    }

    @TurtleAuto(name = "Fixed", overrideMirror = true, mirror = MirrorPolicy.NEVER)
    public static class FixedAuto implements FakeCommand {
        public static FixedAuto create(AutoContext ctx) { return new FixedAuto(); }
    }

    @TurtleAuto(name = "Other type")
    public static class NotACommand {}

    public static class NotAnnotated implements FakeCommand {}

    @Test(expected = IllegalArgumentException.class)
    public void duplicateNamesAreRejected() {
        new AutoRegistry<String>().add("A", c -> "a").add("A", c -> "b");
    }

    @Test
    public void scanFindsOnlyAnnotatedClassesOfTheRightType() {
        Collection<Class<?>> candidates = Arrays.<Class<?>>asList(
                CtorAuto.class, DefaultAuto.class, FixedAuto.class, NotACommand.class, NotAnnotated.class);
        AutoRegistry<FakeCommand> r = new AutoRegistry<FakeCommand>().scan(FakeCommand.class, candidates);

        assertEquals(3, r.size());
        assertTrue(r.indexOf("Via Ctor") >= 0);
        assertTrue(r.indexOf("DefaultAuto") >= 0);
        assertEquals(-1, r.indexOf("Other type"));
    }

    @Test
    public void scannedAutosAreBuiltWithTheirPreferredConstructionPath() {
        AutoRegistry<FakeCommand> r = new AutoRegistry<FakeCommand>().scan(FakeCommand.class,
                Arrays.<Class<?>>asList(CtorAuto.class, DefaultAuto.class, FixedAuto.class));
        AutoContext ctx = AutoContext.unmirrored();

        FakeCommand ctor = r.entries().get(r.indexOf("Via Ctor")).create(ctx);
        assertSame(ctx, ((CtorAuto) ctor).ctx);
        assertEquals("Near: Via Ctor", r.entries().get(r.indexOf("Via Ctor")).getDisplayName());
        assertTrue(r.entries().get(r.indexOf("DefaultAuto")).create(ctx) instanceof DefaultAuto);
        assertTrue(r.entries().get(r.indexOf("Fixed")).create(ctx) instanceof FixedAuto);
        assertEquals(MirrorPolicy.NEVER, r.entries().get(r.indexOf("Fixed")).getMirrorPolicy());
        assertNull(r.entries().get(r.indexOf("DefaultAuto")).getMirrorPolicy());
    }

    @Test
    public void explicitEntriesWinOverScannedOnesWithTheSameName() {
        AutoRegistry<FakeCommand> r = new AutoRegistry<FakeCommand>()
                .add("DefaultAuto", c -> new FakeCommand() {})
                .scan(FakeCommand.class, Arrays.<Class<?>>asList(DefaultAuto.class));
        assertEquals(1, r.size());
        assertFalse(r.entries().get(0).create(AutoContext.unmirrored()) instanceof DefaultAuto);
    }
}
