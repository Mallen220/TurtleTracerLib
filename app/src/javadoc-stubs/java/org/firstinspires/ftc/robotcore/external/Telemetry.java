package org.firstinspires.ftc.robotcore.external;

/**
 * Minimal stub of the FTC Telemetry interface for Javadoc generation and Testing.
 * This file is only used to allow Javadoc and tests to compile and is not part of the real SDK.
 */
public interface Telemetry {

    interface Item {
        Item addData(String caption, String format, Object... args);
        Item setRetained(boolean retained);
    }

    interface Line {}

    Item addData(String caption, String format, Object... args);
    Item addData(String caption, Object value);
    <T> Item addData(String caption, Func<T> valueProducer);
    <T> Item addData(String caption, String format, Func<T> valueProducer);

    boolean removeItem(Item item);
    void clear();
    void clearAll();

    Object addAction(Runnable action);
    boolean removeAction(Object token);

    void speak(String text);
    void speak(String text, String languageCode, String countryCode);

    boolean update();

    Line addLine();
    Line addLine(String lineCaption);
    boolean removeLine(Line line);

    boolean isAutoClear();
    void setAutoClear(boolean autoClear);

    int getMsTransmissionInterval();
    void setMsTransmissionInterval(int msTransmissionInterval);

    String getItemSeparator();
    void setItemSeparator(String itemSeparator);

    String getCaptionValueSeparator();
    void setCaptionValueSeparator(String captionValueSeparator);

    enum DisplayFormat {
        CLASSIC,
        MONOSPACE,
        HTML
    }
    void setDisplayFormat(DisplayFormat displayFormat);

    Log log();

    interface Log {
        int getCapacity();
        void setCapacity(int capacity);
        DisplayOrder getDisplayOrder();
        void setDisplayOrder(DisplayOrder displayOrder);
        void add(String entry);
        void add(String format, Object... args);
        void clear();

        enum DisplayOrder {
            NEWEST_FIRST,
            OLDEST_FIRST
        }
    }
}