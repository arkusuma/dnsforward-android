package com.grafian.dnsforward;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.widget.Toast;

import com.stericson.RootTools.Command;
import com.stericson.RootTools.CommandCapture;
import com.stericson.RootTools.RootTools;

public class Settings {

	private final static String PREF_NAME = "settings";

	int mode;

	int forwardPreset;
	String forwardServer;
	int forwardPort;
	int forwardType;

	int normalPreset;
	String normalPrimary;
	String normalSecondary;

	boolean autoWifi;
	boolean autoMobile;

	private Context mContext;

	public Settings(Context context) {
		mContext = context;
	}

	public void load() {
		SharedPreferences sp = mContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
		mode = sp.getInt("mode", 0);
		forwardPreset = sp.getInt("forward.preset", 0);
		forwardServer = sp.getString("forward.server", "");
		forwardPort = sp.getInt("forward.port", 5353);
		forwardType = sp.getInt("forward.type", 0);
		normalPreset = sp.getInt("normal.preset", 0);
		normalPrimary = sp.getString("normal.primary", "");
		normalSecondary = sp.getString("normal.secondary", "");
		autoWifi = sp.getBoolean("auto.wifi", false);
		autoMobile = sp.getBoolean("auto.mobile", false);
	}

	public void save() {
		SharedPreferences sp = mContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
		Editor ed = sp.edit();
		ed.putInt("mode", mode);
		ed.putInt("forward.preset", forwardPreset);
		ed.putString("forward.server", forwardServer);
		ed.putInt("forward.port", forwardPort);
		ed.putInt("forward.type", forwardType);
		ed.putInt("normal.preset", normalPreset);
		ed.putString("normal.primary", normalPrimary);
		ed.putString("normal.secondary", normalSecondary);
		ed.putBoolean("auto.wifi", autoWifi);
		ed.putBoolean("auto.mobile", autoMobile);
		ed.commit();
	}

	public void apply() {
		Command command;
		if (mode == 0) {
			String exe = Utils.getExePath(mContext);
			String pid = Utils.getPidPath(mContext);
			String type = forwardType == 0 ? "udp" : "tcp";
			command = new CommandCapture(0,
					String.format("chmod 755 %s", exe),
					String.format("killall %s", Utils.DAEMON),
					String.format("%s %s %d %s %s", exe, forwardServer, forwardPort, type, pid),
					"setprop net.dns1 127.0.0.1",
					"setprop net.dns2 ''");
		} else {
			command = new CommandCapture(0,
					String.format("killall %s", Utils.DAEMON),
					String.format("setprop net.dns1 '%s'", normalPrimary),
					String.format("setprop net.dns2 '%s'", normalSecondary));

		}
		try {
			RootTools.getShell(true).add(command).waitForFinish();
			Toast.makeText(mContext, R.string.applied, Toast.LENGTH_SHORT).show();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
