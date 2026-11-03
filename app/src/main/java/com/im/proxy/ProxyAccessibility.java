package com.im.proxy;

import android.accessibilityservice.*;
import android.accessibilityservice.GestureDescription.*;
import android.graphics.Path;
import android.view.accessibility.AccessibilityEvent;

public class ProxyAccessibility extends AccessibilityService {
    static ProxyAccessibility inst;
    @Override protected void onServiceConnected() { inst = this; }

    public static void swipe(float x1, float y1, float x2, float y2, long ms) {
        if (inst == null) return;
        Path p = new Path();
        p.moveTo(x1, y1);
        p.lineTo(x2, y2);
        StrokeDescription s = new StrokeDescription(p, 0, ms);
        inst.dispatchGesture(new GestureDescription.Builder().addStroke(s).build(), null, null);
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent e) {}
    @Override public void onInterrupt() {}
}
