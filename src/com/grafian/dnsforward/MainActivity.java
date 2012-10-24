package com.grafian.dnsforward;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.regex.Pattern;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.stericson.RootTools.CommandCapture;
import com.stericson.RootTools.RootTools;

public class MainActivity extends SherlockActivity {

	private static final Pattern IP_ADDRESS = Pattern.compile("((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(25[0-5]|2[0-4]"
			+ "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1]" + "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}"
			+ "|[1-9][0-9]|[0-9]))");

	private static final String DONATION_URL = "https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=4T78LW4UVVN2Y";

	private final Settings mSettings = new Settings(this);
	private final Handler mHandler = new Handler();
	private final MyReceiver mReceiver = new MyReceiver();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Spinner forwardSpinner = (Spinner) findViewById(R.id.mainForwardPreset);
		Spinner normalSpinner = (Spinner) findViewById(R.id.mainNormalPreset);

		((RadioGroup) findViewById(R.id.mainMode)).setOnCheckedChangeListener(mOnModeChange);
		forwardSpinner.setOnItemSelectedListener(mOnPresetChange);
		normalSpinner.setOnItemSelectedListener(mOnPresetChange);
		findViewById(R.id.mainCurrentDNS).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				loadCurrentDNS();
			}
		});

		ArrayAdapter<Preset.Forward> forwardAdapter = new ArrayAdapter<Preset.Forward>(this, android.R.layout.simple_spinner_item, Preset.FORWARD_SERVERS);
		forwardAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		forwardSpinner.setAdapter(forwardAdapter);

		ArrayAdapter<Preset.Normal> normalAdapter = new ArrayAdapter<Preset.Normal>(this, android.R.layout.simple_spinner_item, Preset.NORMAL_SERVERS);
		normalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		normalSpinner.setAdapter(normalAdapter);

		loadSettings();
		loadCurrentDNS();
		updateViews();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.apply:
			doApply();
			break;
		case R.id.revert:
			doRevert();
			break;
		case R.id.rate:
			doRate();
			break;
		case R.id.share:
			doShare();
			break;
		case R.id.donate:
			doDonate();
			break;
		case R.id.about:
			doAbout();
			break;
		default:
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	@Override
	protected void onResume() {
		super.onResume();
		IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
		registerReceiver(mReceiver, filter);
	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(mReceiver);
	}

	/***********
	 * Helpers *
	 ***********/

	private String getProp(String key) {
		String line = "";
		try {
			Process p = Runtime.getRuntime().exec(new String[] { "getprop", key });
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			line = reader.readLine();
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return line;
	}

	private void closeQueitly(InputStream is) {
		if (is != null) {
			try {
				is.close();
			} catch (IOException e) {
			}
		}
	}

	private void closeQueitly(OutputStream os) {
		if (os != null) {
			try {
				os.close();
			} catch (IOException e) {
			}
		}
	}

	private int getRunCounter() {
		return getPreferences(MODE_PRIVATE).getInt("runCounter", 0);
	}

	private void setRunCounter(int counter) {
		Editor ed = getPreferences(MODE_PRIVATE).edit();
		ed.putInt("runCounter", counter);
		ed.commit();
	}

	/******************
	 * Event Handlers *
	 ******************/

	private class MyReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					loadCurrentDNS();
				}
			}, 500);
		}
	}

	private final OnCheckedChangeListener mOnModeChange = new OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(RadioGroup group, int checkedId) {
			updateViews();
		}
	};

	private final OnItemSelectedListener mOnPresetChange = new OnItemSelectedListener() {

		@Override
		public void onItemSelected(AdapterView<?> adapter, View view, int index, long arg3) {
			updateViews();
		}

		@Override
		public void onNothingSelected(AdapterView<?> adapter) {
		}
	};

	private String readServerStatus() {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(Utils.getPidFile(this)));
			reader.readLine();
			String server = reader.readLine();
			String port = reader.readLine();
			String type = reader.readLine();
			reader.close();
			return String.format("%s:%s (%s)", server, port, type.toUpperCase());
		} catch (IOException e) {
			return "unknown";
		}
	}

	private void loadCurrentDNS() {
		ConnectivityManager conn = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo info = conn.getActiveNetworkInfo();

		TextView tv = (TextView) findViewById(R.id.mainCurrentDNS);
		String dns1 = getProp("net.dns1");
		String dns2 = getProp("net.dns2");
		if (info == null) {
			tv.setText("No active connection");
		} else if ("".equals(dns1)) {
			tv.setText("None");
		} else if ("127.0.0.1".equals(dns1)) {
			tv.setText("Forward to " + readServerStatus());
		} else {
			if ("".equals(dns2)) {
				tv.setText(dns1);
			} else {
				tv.setText(dns1 + ", " + dns2);
			}
		}
	}

	private void updateViews() {
		RadioGroup mode = (RadioGroup) findViewById(R.id.mainMode);
		if (mode.getCheckedRadioButtonId() == R.id.mainForward) {
			findViewById(R.id.mainForwardLayout).setVisibility(View.VISIBLE);
			findViewById(R.id.mainNormalLayout).setVisibility(View.GONE);
		} else {
			findViewById(R.id.mainForwardLayout).setVisibility(View.GONE);
			findViewById(R.id.mainNormalLayout).setVisibility(View.VISIBLE);
		}

		Spinner spinner = (Spinner) findViewById(R.id.mainForwardPreset);
		EditText server = (EditText) findViewById(R.id.mainServer);
		EditText port = (EditText) findViewById(R.id.mainPort);
		int index = spinner.getSelectedItemPosition();
		if (index == 0) {
			server.setEnabled(true);
			port.setEnabled(true);
		} else {
			server.setEnabled(false);
			port.setEnabled(false);
			Preset.Forward forward = Preset.FORWARD_SERVERS[index];
			server.setText(forward.server);
			port.setText(Integer.toString(forward.port));
		}

		spinner = (Spinner) findViewById(R.id.mainNormalPreset);
		EditText primary = (EditText) findViewById(R.id.mainPrimary);
		EditText secondary = (EditText) findViewById(R.id.mainSecondary);
		index = spinner.getSelectedItemPosition();
		if (index == 0) {
			primary.setEnabled(true);
			secondary.setEnabled(true);
		} else {
			primary.setEnabled(false);
			secondary.setEnabled(false);
			Preset.Normal normal = Preset.NORMAL_SERVERS[index];
			primary.setText(normal.primary);
			secondary.setText(normal.secondary);
		}
	}

	private void saveSettings() {
		RadioGroup mode = (RadioGroup) findViewById(R.id.mainMode);
		mSettings.mode = mode.getCheckedRadioButtonId() == R.id.mainForward ? 0 : 1;

		Spinner spinner = (Spinner) findViewById(R.id.mainForwardPreset);
		EditText server = (EditText) findViewById(R.id.mainServer);
		EditText port = (EditText) findViewById(R.id.mainPort);
		RadioGroup type = (RadioGroup) findViewById(R.id.mainType);
		mSettings.forwardPreset = spinner.getSelectedItemPosition();
		mSettings.forwardServer = server.getText().toString();
		mSettings.forwardPort = Integer.parseInt(port.getText().toString());
		mSettings.forwardType = type.getCheckedRadioButtonId() == R.id.mainUDP ? 0 : 1;

		spinner = (Spinner) findViewById(R.id.mainNormalPreset);
		EditText primary = (EditText) findViewById(R.id.mainPrimary);
		EditText secondary = (EditText) findViewById(R.id.mainSecondary);
		mSettings.normalPreset = spinner.getSelectedItemPosition();
		mSettings.normalPrimary = primary.getText().toString();
		mSettings.normalSecondary = secondary.getText().toString();

		CheckBox wifi = (CheckBox) findViewById(R.id.mainAutoWifi);
		CheckBox mobile = (CheckBox) findViewById(R.id.mainAutoMobile);
		mSettings.autoWifi = wifi.isChecked();
		mSettings.autoMobile = mobile.isChecked();

		mSettings.save();
	}

	private void loadSettings() {
		mSettings.load();

		RadioGroup mode = (RadioGroup) findViewById(R.id.mainMode);
		mode.check(mSettings.mode == 0 ? R.id.mainForward : R.id.mainNormal);

		Spinner spinner = (Spinner) findViewById(R.id.mainForwardPreset);
		EditText server = (EditText) findViewById(R.id.mainServer);
		EditText port = (EditText) findViewById(R.id.mainPort);
		RadioGroup type = (RadioGroup) findViewById(R.id.mainType);
		spinner.setSelection(mSettings.forwardPreset);
		server.setText(mSettings.forwardServer);
		port.setText(Integer.toString(mSettings.forwardPort));
		type.check(mSettings.forwardType == 0 ? R.id.mainUDP : R.id.mainTCP);

		spinner = (Spinner) findViewById(R.id.mainNormalPreset);
		EditText primary = (EditText) findViewById(R.id.mainPrimary);
		EditText secondary = (EditText) findViewById(R.id.mainSecondary);
		spinner.setSelection(mSettings.normalPreset);
		primary.setText(mSettings.normalPrimary);
		secondary.setText(mSettings.normalSecondary);

		CheckBox wifi = (CheckBox) findViewById(R.id.mainAutoWifi);
		CheckBox mobile = (CheckBox) findViewById(R.id.mainAutoMobile);
		wifi.setChecked(mSettings.autoWifi);
		mobile.setChecked(mSettings.autoMobile);
	}

	private void activateForwarder() {
		EditText serverView = (EditText) findViewById(R.id.mainServer);
		EditText portView = (EditText) findViewById(R.id.mainPort);

		String server = serverView.getText().toString();
		String port = portView.getText().toString();

		if (!IP_ADDRESS.matcher(server).matches()) {
			serverView.requestFocus();
			serverView.selectAll();
			return;
		}

		if (port.length() == 0) {
			portView.requestFocus();
			return;
		}

		File exeFile = Utils.getExeFile(this);
		if (!exeFile.exists()) {
			InputStream in = null;
			OutputStream out = null;
			try {
				in = getResources().openRawResource(R.raw.dnsforward);
				out = new FileOutputStream(exeFile);
				byte[] buf = new byte[8192];
				int len;
				while ((len = in.read(buf)) > 0) {
					out.write(buf, 0, len);
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				closeQueitly(in);
				closeQueitly(out);
			}
		}

		saveSettings();
		mSettings.apply();
		loadCurrentDNS();
	}

	private void activateNormal() {
		EditText primaryView = (EditText) findViewById(R.id.mainPrimary);
		EditText secondaryView = (EditText) findViewById(R.id.mainSecondary);

		String primary = primaryView.getText().toString();
		String secondary = secondaryView.getText().toString();

		if (!IP_ADDRESS.matcher(primary).matches()) {
			primaryView.requestFocus();
			primaryView.selectAll();
			return;
		}

		if (secondary.length() > 0 && !IP_ADDRESS.matcher(secondary).matches()) {
			secondaryView.requestFocus();
			secondaryView.selectAll();
			return;
		}

		saveSettings();
		mSettings.apply();
		loadCurrentDNS();
		Toast.makeText(MainActivity.this, R.string.applied, Toast.LENGTH_SHORT).show();
	}

	private void doApply() {
		if (!RootTools.isAccessGiven()) {
			Toast.makeText(this, getString(R.string.no_access), Toast.LENGTH_LONG).show();
			finish();
			return;
		}

		RadioGroup mode = (RadioGroup) findViewById(R.id.mainMode);
		if (mode.getCheckedRadioButtonId() == R.id.mainForward) {
			activateForwarder();
		} else {
			activateNormal();
		}

		int counter = getRunCounter() + 1;
		setRunCounter(counter);
		if (counter % 10 == 0) {
			String items[] = {
					getString(R.string.rate),
					getString(R.string.share),
					getString(R.string.donate) };
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			View title = getLayoutInflater().inflate(R.layout.support_title, null);
			builder.setCustomTitle(title);
			builder.setItems(items, mSupportListener);
			builder.setNeutralButton(R.string.not_now, mSupportListener);
			builder.setCancelable(true);
			builder.show();
		}
	}

	private final DialogInterface.OnClickListener mSupportListener = new DialogInterface.OnClickListener() {

		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
			case 0:
				doRate();
				break;
			case 1:
				doShare();
				break;
			case 2:
				doDonate();
				break;
			}
			dialog.dismiss();
		}
	};

	private String intToIp(int addr) {
		if (addr == 0) {
			return "";
		}
		int a = (addr >> 0) & 255;
		int b = (addr >> 8) & 255;
		int c = (addr >> 16) & 255;
		int d = (addr >> 24) & 255;
		return String.format("%d.%d.%d.%d", a, b, c, d);
	}

	private void doRevert() {
		if (!RootTools.isAccessGiven()) {
			Toast.makeText(this, getString(R.string.no_access), Toast.LENGTH_LONG).show();
			return;
		}

		ConnectivityManager conn = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo info = conn.getActiveNetworkInfo();

		String dns1 = "";
		String dns2 = "";
		if (info != null) {
			if (info.getType() == ConnectivityManager.TYPE_WIFI) {
				WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
				DhcpInfo dhcp = wifi.getDhcpInfo();
				dns1 = intToIp(dhcp.dns1);
				dns2 = intToIp(dhcp.dns2);
			} else if (info.getType() == ConnectivityManager.TYPE_MOBILE) {
				dns1 = getProp("net.rmnet0.dns1");
				dns2 = getProp("net.rmnet0.dns2");
				if ("".equals(dns1)) {
					dns1 = getProp("net.pdp0.dns1");
					dns2 = getProp("net.pdp0.dns2");
				}
			}
		}

		CommandCapture command = new CommandCapture(0,
				String.format("killall %s", Utils.DAEMON),
				String.format("setprop net.dns1 '%s'", dns1),
				String.format("setprop net.dns2 '%s'", dns2));
		try {
			RootTools.getShell(true).add(command).waitForFinish();
		} catch (Exception e) {
			e.printStackTrace();
		}
		loadCurrentDNS();
		Toast.makeText(MainActivity.this, R.string.reverted, Toast.LENGTH_SHORT).show();
	}

	private void doRate() {
		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName()));
		startActivity(intent);
	}

	private void doShare() {
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("plain/text");
		intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_subject));
		intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text));
		startActivity(Intent.createChooser(intent, getString(R.string.share)));
	}

	private void doDonate() {
		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(DONATION_URL));
		startActivity(intent);
	}

	private void doAbout() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		View about = getLayoutInflater().inflate(R.layout.about, null);
		builder.setCustomTitle(about);
		builder.setPositiveButton(R.string.visit_web, mAboutListener);
		builder.setNeutralButton(R.string.more_apps, mAboutListener);
		builder.setNegativeButton(R.string.okay, mAboutListener);
		builder.setCancelable(true);
		builder.show();
	}

	private final DialogInterface.OnClickListener mAboutListener = new DialogInterface.OnClickListener() {

		@Override
		public void onClick(DialogInterface dialog, int which) {
			Intent intent;
			switch (which) {
			case DialogInterface.BUTTON_POSITIVE:
				intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://grafian.com"));
				startActivity(intent);
				break;
			case DialogInterface.BUTTON_NEUTRAL:
				intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/search?q=pub:Grafian%20Software%20Crafter"));
				startActivity(intent);
				break;
			}
			dialog.dismiss();
		}
	};

}
