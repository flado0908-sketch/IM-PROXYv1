package com.im.proxy;

import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class ProxyVpnService extends VpnService {
    private ParcelFileDescriptor pfd;
    private Thread worker;

    @Override public int onStartCommand(android.content.Intent i, int f, int id) {
        if (pfd == null) startTunnel();
        return START_STICKY;
    }

    private void startTunnel() {
        Builder b = new Builder()
            .setSession("IM Proxy")
            .addAddress("10.0.0.2", 32)
            .addDnsServer("1.1.1.1")
            .addRoute("0.0.0.0", 0);
        pfd = b.establish();
        if (pfd == null) return;

        worker = new Thread(() -> {
            try (FileInputStream in = new FileInputStream(pfd.getFileDescriptor())) {
                byte[] buf = new byte[32767];
                while (true) {
                    int n = in.read(buf);
                    if (n <= 0) break;
                    // TUN2SOCKS forwarding logic — pakai proxy lokal 127.0.0.1:1080
                    // bypass UDP game lewat socket langsung di sini
                }
            } catch (Exception e) {}
        });
        worker.start();
    }

    @Override public void onDestroy() {
        try { if (pfd != null) pfd.close(); } catch (Exception e) {}
        if (worker != null) worker.interrupt();
        super.onDestroy();
    }
}
