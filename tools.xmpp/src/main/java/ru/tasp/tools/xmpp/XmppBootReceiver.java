package ru.tasp.tools.xmpp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class XmppBootReceiver extends BroadcastReceiver {
    public XmppBootReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == null) {
            return;
        }

        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            // run!
            if (Xmpp.get().getConfiguration().getServiceStart(false)) {
                Intent xmppService = new Intent(context, XmppService.class);
                context.startService(xmppService);
            }
        }
    }
}
