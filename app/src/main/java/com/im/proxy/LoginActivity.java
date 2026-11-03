package com.im.proxy;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.net.VpnService;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.*;

public class LoginActivity extends Activity {
    private static final String VALID_KEY = "BAIM-PROXY";
    private static final int REQ_OVL = 100, REQ_CAP = 101, REQ_VPN = 102;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_login);

        EditText keyInput = findViewById(R.id.keyInput);
        Button loginBtn = findViewById(R.id.loginBtn);
        TextView status = findViewById(R.id.status);

        loginBtn.setOnClickListener(v -> {
            String input = keyInput.getText().toString().trim();
            if (!input.equals(VALID_KEY)) {
                status.setText("KEY INVALID");
                status.setTextColor(0xffff4d6d);
                return;
            }
            status.setText("KEY ACCEPTED");
            status.setTextColor(0xff00e5a0);
            requestVpn();
        });
    }

    private void requestVpn() {
        Intent intent = VpnService.prepare(this);
        if (intent != null) startActivityForResult(intent, REQ_VPN);
        else onVpnReady();
    }

    private void onVpnReady() {
        if (!Settings.canDrawOverlays(this)) {
            startActivityForResult(new Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName())), REQ_OVL);
        } else startCapture();
    }

    private void startCapture() {
        MediaProjectionManager m = (MediaProjectionManager)
            getSystemService(MEDIA_PROJECTION_SERVICE);
        startActivityForResult(m.createScreenCaptureIntent(), REQ_CAP);
    }

    @Override protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_VPN && res == RESULT_OK) onVpnReady();
        if (req == REQ_OVL) { if (Settings.canDrawOverlays(this)) startCapture(); }
        if (req == REQ_CAP && res == RESULT_OK) {
            startService(new Intent(this, ProxyVpnService.class));
            Intent s = new Intent(this, OverlayService.class);
            s.putExtra("code", res);
            s.putExtra("data", data);
            startForegroundService(s);
            finish();
            Toast.makeText(this, "IM PROXY ACTIVE", Toast.LENGTH_SHORT).show();
        }
    }
                       }
