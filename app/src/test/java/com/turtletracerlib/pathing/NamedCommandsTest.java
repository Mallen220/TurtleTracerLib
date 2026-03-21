package com.turtletracerlib.pathing;

import com.turtletracerlib.command.Command;
import com.turtletracerlib.command.CommandScheduler;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Collections;

public class NamedCommandsTest {

    // A foreign command class that looks like a command but implements nothing
    public static class ForeignCommand {
        public boolean init = false;
        public void initialize() { init = true; }
        public void execute() {}
        public boolean isFinished() { return true; }
        public void end(boolean interrupted) {}
    }

    @Test
    public void testRegisterForeignCommand() {
        NamedCommands.clearAllCommands();

        ForeignCommand fc = new ForeignCommand();
        NamedCommands.registerCommand("ForeignCmd", fc);

        assertTrue(NamedCommands.hasCommand("ForeignCmd"));

        Command retrieved = NamedCommands.getCommand("ForeignCmd");
        assertNotNull(retrieved);

        // Schedule it to make sure it works
        retrieved.schedule();
        CommandScheduler.getInstance().run();

        assertTrue(fc.init);
    }

    @Test
    public void testRegisterRunnable() {
        NamedCommands.clearAllCommands();
        final boolean[] ran = {false};

        Runnable r = () -> { ran[0] = true; };
        NamedCommands.registerCommand("RunCmd", r);

        Command retrieved = NamedCommands.getCommand("RunCmd");
        retrieved.schedule();
        CommandScheduler.getInstance().run();

        assertTrue(ran[0]);
    }
}
