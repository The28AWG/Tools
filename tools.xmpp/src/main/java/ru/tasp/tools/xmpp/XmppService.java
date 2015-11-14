package ru.tasp.tools.xmpp;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class XmppService extends Service {
    public XmppService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Xmpp.get().initialize();
        stopSelf(startId);
        return super.onStartCommand(intent, flags, startId);
    }
}
