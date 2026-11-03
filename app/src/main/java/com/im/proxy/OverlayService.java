package com.im.proxy;

import android.app.*;
import android.content.Intent;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.*;
import android.os.*;
import android.util.DisplayMetrics;
import android.view.*;
import android.widget.*;
import java.nio.ByteBuffer;
import java.util.*;

public class OverlayService extends Service {
    private static final int NOTIF = 1;
    private WindowManager wm;
    private View espView, menuView, fab;
    private boolean menuOpen = false;
    private MediaProjection proj;
    private ImageReader reader;
    private int W, H;
    private volatile boolean espOn = true, aimOn = true, vpnOn = true;
    private volatile List<Detector.Box> boxes = new ArrayList<>();
    private final Detector det = new Detector();

    @Override public int onStartCommand(Intent i, int f, int id) {
        startForeground(NOTIF, notif());
        DisplayMetrics dm = getResources().getDisplayMetrics();
        W = dm.widthPixels; H = dm.heightPixels;
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        addEsp();
        addFab();
        startCapture(i);
        return START_STICKY;
    }

    private void addEsp() {
        espView = new View(this) {
            @Override protected void onDraw(Canvas c) {
                if (!espOn) return;
                Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(3f);
                Paint t = new Paint(Paint.ANTI_ALIAS_FLAG);
                t.setTextSize(24f); t.setFakeBoldText(true); t.setTypeface(Typeface.MONOSPACE);
                for (Detector.Box b : boxes) {
                    if (b.cls == 0) { p.setColor(0xff00e5a0); c.drawRect(b.x, b.y, b.x+b.w, b.y+b.h, p); }
                    else { p.setColor(0xffff4d6d); c.drawRect(b.x, b.y, b.x+b.w, b.y+b.h, p);
                           t.setColor(0xffff4d6d); c.drawText("NECK", b.x, b.y-6, t); }
                }
            }
        };
        espView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
            W, H, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.LEFT;
        wm.addView(espView, lp);
    }

    private void addFab() {
        TextView fab = new TextView(this);
        fab.setText("IM");
        fab.setTextColor(Color.WHITE); fab.setTextSize(13); fab.setTypeface(null, Typeface.BOLD);
        fab.setGravity(Gravity.CENTER);
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL); g.setColor(0xff141122); g.setStroke(dp(2), 0xffa855f7);
        fab.setBackground(g);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
            dp(52), dp(52), WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START; lp.x = dp(12); lp.y = dp(80);
        fab.setOnClickListener(v -> toggleMenu());
        wm.addView(fab, lp); this.fab = fab;
    }

    private void toggleMenu() {
        if (menuOpen) { if (menuView != null) wm.removeView(menuView); menuOpen = false; }
        else { buildMenu(); menuOpen = true; }
    }

    private void buildMenu() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16), dp(16), dp(16), dp(16));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xff141122); bg.setCornerRadius(dp(16)); bg.setStroke(dp(1), 0xff3a2f5c);
        panel.setBackground(bg);

        TextView title = new TextView(this);
        title.setText("XREG // IM PROXY");
        title.setTextColor(0xffa855f7); title.setTextSize(14); title.setTypeface(null, Typeface.BOLD);
        panel.addView(title);

        View div = new View(this); div.setBackgroundColor(0xff2a2145);
        LinearLayout.LayoutParams dvp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        dvp.topMargin = dp(8); dvp.bottomMargin = dp(8); panel.addView(div, dvp);

        addToggle(panel, "VPN ANTI-BANNED", vpnOn, c -> vpnOn = c);
        addToggle(panel, "ESP BOX", espOn, c -> espOn = c);
        addToggle(panel, "AIM NECK", aimOn, c -> aimOn = c);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
            dp(300), WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.CENTER;
        wm.addView(panel, lp); menuView = panel;
    }

    private void addToggle(LinearLayout parent, String label, boolean init, java.util.function.Consumer<Boolean> cb) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));
        TextView lbl = new TextView(this);
        lbl.setText(label); lbl.setTextColor(Color.WHITE); lbl.setTextSize(13);
        row.addView(lbl, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Switch sw = new Switch(this); sw.setChecked(init); sw.setOnCheckedChangeListener((b, c) -> cb.accept(c));
        row.addView(sw); parent.addView(row);
    }

    private void startCapture(Intent intent) {
        MediaProjectionManager m = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        proj = m.getMediaProjection(intent.getIntExtra("code", 0), (Intent) intent.getParcelableExtra("data"));
        reader = ImageReader.newInstance(W, H, PixelFormat.RGBA_8888, 2);
        proj.createVirtualDisplay("im", W, H, getResources().getDisplayMetrics().densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, reader.getSurface(), null, null);
        new Thread(this::detectLoop).start();
    }

    private void detectLoop() {
        while (true) {
            Image img = reader.acquireLatestImage();
            if (img == null) { sleep(8); continue; }
            try {
                ByteBuffer b = img.getPlanes()[0].getBuffer();
                byte[] px = new byte[b.remaining()]; b.get(px);
                float[] raw = det.detect(px, W, H);
                int n = (int) raw[0];
                List<Detector.Box> list = new ArrayList<>();
                for (int i = 0; i < n; i++) {
                    int o = 1 + i * 5;
                    Detector.Box bx = new Detector.Box();
                    bx.x = raw[o]; bx.y = raw[o+1]; bx.w = raw[o+2]; bx.h = raw[o+3]; bx.cls = (int) raw[o+4];
                    list.add(bx);
                }
                boxes = list;
                if (aimOn) {
                    float cxs = W / 2f, cys = H / 2f;
                    Detector.Box best = null; float bd = Float.MAX_VALUE;
                    for (Detector.Box bx : list) {
                        if (bx.cls != 1) continue;
                        float x = bx.x + bx.w/2f, y = bx.y + bx.h/2f;
                        float d = (float) Math.hypot(x-cxs, y-cys);
                        if (d < bd && d < 400) { bd = d; best = bx; }
                    }
                    if (best != null) ProxyAccessibility.swipe(cxs, cys, best.x + best.w/2f, best.y + best.h/2f, 20);
                }
                espView.postInvalidate();
            } catch (Exception e) {} finally { img.close(); }
            sleep(30);
        }
    }

    private void sleep(int ms) { try { Thread.sleep(ms); } catch (Exception e) {} }

    private Notification notif() {
        String ch = "im";
        NotificationChannel c = new NotificationChannel(ch, "IM PROXY", NotificationManager.IMPORTANCE_LOW);
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(c);
        return new Notification.Builder(this, ch).setContentTitle("IM PROXY").setSmallIcon(android.R.drawable.ic_menu_view).build();
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
    @Override public IBinder onBind(Intent i) { return null; }
  }
