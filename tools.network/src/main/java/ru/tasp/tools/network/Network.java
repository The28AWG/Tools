package ru.tasp.tools.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import ru.tasp.tools.Tools;

/**
 * Created by the28awg on 11.11.15.
 */
public class Network {

    public static boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) Tools.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }
}
