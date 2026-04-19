package com.turtletracerlib.pathing;

import org.junit.Test;
import static org.junit.Assert.*;

public class NamedCommandsTest {

    @Test
    public void testRegisterRunnable() {
        NamedCommands.clearAllCommands();
        final boolean[] ran = {false};

        Runnable r = () -> { ran[0] = true; };
        NamedCommands.registerCommand("RunCmd", r);

        Runnable retrieved = NamedCommands.getCommand("RunCmd");
        assertNotNull(retrieved);
        retrieved.run();

        assertTrue(ran[0]);
    }

    @Test
    public void testGetUnregisteredCommandReturnsNoOpRunnable() {
        NamedCommands.clearAllCommands();

        Runnable retrieved = NamedCommands.getCommand("NonExistentCmd");
        assertNotNull(retrieved);

        // Should not throw an exception
        retrieved.run();
    }
}
