package com.grafian.dnsforward;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetworkMonitor extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		ConnectivityManager conn = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = conn.getActiveNetworkInfo();
		if (info != null && info.isConnected()) {
			Settings settings = new Settings(context);
			settings.load();
			boolean isWifi = info.getType() == ConnectivityManager.TYPE_WIFI;
			boolean isMobile = info.getType() == ConnectivityManager.TYPE_MOBILE;
			if ((isWifi && settings.autoWifi) || (isMobile && settings.autoMobile)) {
				settings.apply();
			}
		}
	}
}
