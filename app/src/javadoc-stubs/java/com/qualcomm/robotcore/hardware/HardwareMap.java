package com.qualcomm.robotcore.hardware;

public interface HardwareMap {
    <T> T get(Class<? extends T> classOrInterface, String deviceName);
    <T> T get(String deviceName);
    Iterable<Object> getAll(Class<?> classOrInterface);
}