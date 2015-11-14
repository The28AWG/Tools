package ru.tasp.tools.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import ru.tasp.tools.bus.Bus;
import ru.tasp.tools.logger.L;

/**
 * Created by the28awg on 11.11.15.
 */
public class NetworkStateReceiver  extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        L.debug("isNetworkAvailable = %s", Network.isNetworkAvailable());
        Bus.get().fire(new NetworkChangeEvent());
    }

}