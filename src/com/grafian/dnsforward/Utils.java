package com.grafian.dnsforward;

import java.io.File;

import android.content.Context;

public class Utils {

	public static final String DAEMON = "dnsforward";

	public static File getExeFile(Context context) {
		return new File(context.getCacheDir(), DAEMON);
	}

	public static File getPidFile(Context context) {
		return new File(context.getCacheDir(), DAEMON + ".pid");
	}

	public static String getExePath(Context context) {
		return getExeFile(context).getAbsolutePath();
	}

	public static String getPidPath(Context context) {
		return getPidFile(context).getAbsolutePath();
	}
}
