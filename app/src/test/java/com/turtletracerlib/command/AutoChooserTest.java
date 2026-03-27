package com.turtletracerlib.command;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.Gamepad;
import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.external.Func;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the {@link AutoChooser} command.
 */
public class AutoChooserTest {

    private LinearOpMode mockOpMode;
    private Gamepad mockGamepad;
    private Telemetry mockTelemetry;

    private boolean opModeStarted = false;
    private boolean opModeStopped = false;

    @Before
    public void setUp() {
        // Reset state for each test
        AutoChooser.clear();
        CommandScheduler.getInstance().reset();

        opModeStarted = false;
        opModeStopped = false;

        // Stub out LinearOpMode
        mockOpMode = new LinearOpMode() {
            @Override
            public void runOpMode() throws InterruptedException {}

            @Override
            public boolean isStarted() {
                return opModeStarted;
            }

            @Override
            public boolean isStopRequested() {
                return opModeStopped;
            }
        };

        // Stub out Gamepad
        mockGamepad = new Gamepad();

        // Stub out Telemetry
        mockTelemetry = new Telemetry() {
            @Override
            public Item addData(String caption, String format, Object... args) { return null; }
            @Override
            public Item addData(String caption, Object value) { return null; }
            @Override
            public <T> Item addData(String caption, Func<T> valueProducer) { return null; }
            @Override
            public <T> Item addData(String caption, String format, Func<T> valueProducer) { return null; }
            @Override
            public boolean removeItem(Item item) { return false; }
            @Override
            public void clear() {}
            @Override
            public void clearAll() {}
            @Override
            public Object addAction(Runnable action) { return null; }
            @Override
            public boolean removeAction(Object token) { return false; }
            @Override
            public void speak(String text) {}
            @Override
            public void speak(String text, String languageCode, String countryCode) {}
            @Override
            public boolean update() { return true; }
            @Override
            public Line addLine() { return null; }
            @Override
            public Line addLine(String lineCaption) { return null; }
            @Override
            public boolean removeLine(Line line) { return false; }
            @Override
            public boolean isAutoClear() { return true; }
            @Override
            public void setAutoClear(boolean autoClear) {}
            @Override
            public int getMsTransmissionInterval() { return 0; }
            @Override
            public void setMsTransmissionInterval(int msTransmissionInterval) {}
            @Override
            public String getItemSeparator() { return ""; }
            @Override
            public void setItemSeparator(String itemSeparator) {}
            @Override
            public String getCaptionValueSeparator() { return ""; }
            @Override
            public void setCaptionValueSeparator(String captionValueSeparator) {}
            @Override
            public void setDisplayFormat(DisplayFormat displayFormat) {}
            @Override
            public Log log() { return null; }
        };
    }

    @After
    public void tearDown() {
        AutoChooser.clear();
        CommandScheduler.getInstance().reset();
    }

    @Test
    public void testRegistrationAndClear() {
        AutoChooser.addOption("Test 1", new InstantCommand());
        AutoChooser.addOption("Test 2", new WaitCommand(1));

        AutoChooser chooser = new AutoChooser(mockOpMode, mockGamepad, mockTelemetry);
        assertNotNull("Should have a selected option initially", chooser.getSelectedOption());
        assertEquals("Initial option should be the first registered", "Test 1", chooser.getSelectedOption().getName());

        AutoChooser.clear();
        AutoChooser emptyChooser = new AutoChooser(mockOpMode, mockGamepad, mockTelemetry);
        assertNull("Should have no selected option after clear", emptyChooser.getSelectedOption());
    }

    @Test
    public void testNavigationDpadRight() throws InterruptedException {
        Command cmd1 = new InstantCommand();
        Command cmd2 = new WaitCommand(1);
        Command cmd3 = new WaitCommand(2);

        AutoChooser.addOption("Option 1", cmd1);
        AutoChooser.addOption("Option 2", cmd2);
        AutoChooser.addOption("Option 3", cmd3);

        AutoChooser chooser = new AutoChooser(mockOpMode, mockGamepad, mockTelemetry);
        chooser.initialize();

        assertEquals("Option 1", chooser.getSelectedOption().getName());

        // Simulate dpad right press, fast forward time past 300ms debounce
        mockGamepad.dpad_right = true;
        Thread.sleep(301);
        chooser.execute();

        assertEquals("Option 2", chooser.getSelectedOption().getName());

        // Simulate another dpad right press
        mockGamepad.dpad_right = false; // release
        Thread.sleep(10);
        mockGamepad.dpad_right = true; // press again
        Thread.sleep(301);
        chooser.execute();

        assertEquals("Option 3", chooser.getSelectedOption().getName());

        // Wraparound test
        mockGamepad.dpad_right = false;
        Thread.sleep(10);
        mockGamepad.dpad_right = true;
        Thread.sleep(301);
        chooser.execute();

        assertEquals("Option 1", chooser.getSelectedOption().getName());
    }

    @Test
    public void testNavigationDpadLeft() throws InterruptedException {
        Command cmd1 = new InstantCommand();
        Command cmd2 = new WaitCommand(1);
        Command cmd3 = new WaitCommand(2);

        AutoChooser.addOption("Option A", cmd1);
        AutoChooser.addOption("Option B", cmd2);
        AutoChooser.addOption("Option C", cmd3);

        AutoChooser chooser = new AutoChooser(mockOpMode, mockGamepad, mockTelemetry);
        chooser.initialize();

        assertEquals("Option A", chooser.getSelectedOption().getName());

        // Simulate dpad left press (wraparound backwards)
        mockGamepad.dpad_left = true;
        Thread.sleep(301);
        chooser.execute();

        assertEquals("Option C", chooser.getSelectedOption().getName());

        // Simulate another dpad left press
        mockGamepad.dpad_left = false;
        Thread.sleep(10);
        mockGamepad.dpad_left = true;
        Thread.sleep(301);
        chooser.execute();

        assertEquals("Option B", chooser.getSelectedOption().getName());
    }

    @Test
    public void testIsFinished() {
        AutoChooser chooser = new AutoChooser(mockOpMode, mockGamepad, mockTelemetry);

        assertFalse("Should not be finished if not started or stopped", chooser.isFinished());

        opModeStarted = true;
        assertTrue("Should be finished if OpMode started", chooser.isFinished());

        opModeStarted = false;
        opModeStopped = true;
        assertTrue("Should be finished if OpMode stop requested", chooser.isFinished());
    }

    @Test
    public void testEndSchedulesSelectedCommand() {
        // We will create a custom command to easily check if it was initialized
        class TestCommand implements Command {
            boolean isScheduled = false;
            @Override
            public void initialize() {
                isScheduled = true;
            }
            @Override
            public void execute() {}
            @Override
            public void end(boolean interrupted) {}
            @Override
            public boolean isFinished() { return false; }
        }

        TestCommand testCmd = new TestCommand();
        AutoChooser.addOption("Test Auto", testCmd);

        AutoChooser chooser = new AutoChooser(mockOpMode, mockGamepad, mockTelemetry);

        // Ensure the scheduler is clean
        CommandScheduler.getInstance().reset();

        // Simulate the chooser ending normally (OpMode started)
        chooser.end(false);

        // Run the scheduler once to process the newly scheduled command
        CommandScheduler.getInstance().run();

        assertTrue("The selected command should have been scheduled and initialized", testCmd.isScheduled);
    }
}