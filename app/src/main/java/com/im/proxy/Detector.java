package com.im.proxy;

public class Detector {
    static { System.loadLibrary("imdetect"); }
    public native float[] detect(byte[] rgba, int w, int h);

    public static class Box {
        public float x, y, w, h;
        public int cls;
    }
}
