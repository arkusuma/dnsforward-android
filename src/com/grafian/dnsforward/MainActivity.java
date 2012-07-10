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

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.Spinner;
import android.widget.TextView;

public class MainActivity extends Activity {

	private static final Pattern IP_ADDRESS = Pattern
			.compile("((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(25[0-5]|2[0-4]"
					+ "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1]"
					+ "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}"
					+ "|[1-9][0-9]|[0-9]))");

	private static final String DONATION_URL = "https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=4T78LW4UVVN2Y";

	private Handler handler = new Handler();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Spinner forwardSpinner = (Spinner) findViewById(R.id.mainForwardPreset);
		Spinner normalSpinner = (Spinner) findViewById(R.id.mainNormalPreset);

		findViewById(R.id.mainApply).setOnClickListener(mOnApplyClick);
		findViewById(R.id.mainRevert).setOnClickListener(mOnRevertClick);
		((RadioGroup) findViewById(R.id.mainMode))
				.setOnCheckedChangeListener(mOnModeChange);
		forwardSpinner.setOnItemSelectedListener(mOnPresetChange);
		normalSpinner.setOnItemSelectedListener(mOnPresetChange);

		ArrayAdapter<Preset.Forward> forwardAdapter = new ArrayAdapter<Preset.Forward>(
				this, android.R.layout.simple_spinner_item,
				Preset.FORWARD_SERVERS);
		forwardAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		forwardSpinner.setAdapter(forwardAdapter);

		ArrayAdapter<Preset.Normal> normalAdapter = new ArrayAdapter<Preset.Normal>(
				this, android.R.layout.simple_spinner_item,
				Preset.NORMAL_SERVERS);
		normalAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		normalSpinner.setAdapter(normalAdapter);

		findViewById(R.id.mainRate).setOnClickListener(mOnRateClick);
		findViewById(R.id.mainDonate).setOnClickListener(mOnDonateClick);

		loadPreferences();
		loadCurrentDNS();
		updateViews();
	}

	/******************
	 * Event Handlers *
	 ******************/

	private final OnCheckedChangeListener mOnModeChange = new OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(RadioGroup group, int checkedId) {
			updateViews();
		}
	};

	private final OnItemSelectedListener mOnPresetChange = new OnItemSelectedListener() {

		@Override
		public void onItemSelected(AdapterView<?> adapter, View view,
				int index, long arg3) {
			updateViews();
		}

		@Override
		public void onNothingSelected(AdapterView<?> adapter) {
		}
	};

	private final OnClickListener mOnRateClick = new OnClickListener() {
		@Override
		public void onClick(View arg0) {
			Intent intent = new Intent(Intent.ACTION_VIEW,
					Uri.parse("market://details?id=" + getPackageName()));
			startActivity(intent);
		}
	};

	private final OnClickListener mOnDonateClick = new OnClickListener() {

		@Override
		public void onClick(View v) {
			Intent intent = new Intent(Intent.ACTION_VIEW,
					Uri.parse(DONATION_URL));
			startActivity(intent);
		}
	};

	private String readServerStatus() {
		try {
			File pidFile = new File(getCacheDir(), DNSFORWARD + ".pid");
			BufferedReader reader = new BufferedReader(new FileReader(pidFile));
			reader.readLine();
			String server = reader.readLine();
			String port = reader.readLine();
			String type = reader.readLine();
			return String
					.format("%s:%s (%s)", server, port, type.toUpperCase());
		} catch (IOException e) {
			return "unknown";
		}
	}

	private void loadCurrentDNS() {
		final TextView tv = (TextView) findViewById(R.id.mainCurrentDNS);
		tv.setText(R.string.please_wait);
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				String dns1 = getProp("net.dns1");
				String dns2 = getProp("net.dns2");
				if ("".equals(dns1)) {
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
		}, 1000);
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

	private final static String DNSFORWARD = "dnsforward";

	private void savePreferences() {
		SharedPreferences sp = getPreferences(MODE_PRIVATE);
		Editor ed = sp.edit();

		RadioGroup mode = (RadioGroup) findViewById(R.id.mainMode);
		ed.putInt("mode", mode.getCheckedRadioButtonId());

		Spinner spinner = (Spinner) findViewById(R.id.mainForwardPreset);
		EditText server = (EditText) findViewById(R.id.mainServer);
		EditText port = (EditText) findViewById(R.id.mainPort);
		RadioGroup type = (RadioGroup) findViewById(R.id.mainType);
		ed.putInt("forward.preset", spinner.getSelectedItemPosition());
		ed.putString("forward.server", server.getText().toString());
		ed.putString("forward.port", port.getText().toString());
		ed.putInt("forward.type", type.getCheckedRadioButtonId());

		spinner = (Spinner) findViewById(R.id.mainNormalPreset);
		EditText primary = (EditText) findViewById(R.id.mainPrimary);
		EditText secondary = (EditText) findViewById(R.id.mainSecondary);
		ed.putInt("normal.preset", spinner.getSelectedItemPosition());
		ed.putString("normal.primary", primary.getText().toString());
		ed.putString("normal.secondary", secondary.getText().toString());

		ed.commit();
	}

	private void loadPreferences() {
		SharedPreferences sp = getPreferences(MODE_PRIVATE);

		RadioGroup mode = (RadioGroup) findViewById(R.id.mainMode);
		mode.check(sp.getInt("mode", R.id.mainForward));

		Spinner spinner = (Spinner) findViewById(R.id.mainForwardPreset);
		EditText server = (EditText) findViewById(R.id.mainServer);
		EditText port = (EditText) findViewById(R.id.mainPort);
		RadioGroup type = (RadioGroup) findViewById(R.id.mainType);
		spinner.setSelection(sp.getInt("forward.preset", 0));
		server.setText(sp.getString("forward.server", ""));
		port.setText(sp.getString("forward.port", ""));
		type.check(sp.getInt("forward.type", R.id.mainUDP));

		spinner = (Spinner) findViewById(R.id.mainNormalPreset);
		EditText primary = (EditText) findViewById(R.id.mainPrimary);
		EditText secondary = (EditText) findViewById(R.id.mainSecondary);
		spinner.setSelection(sp.getInt("normal.preset", 0));
		primary.setText(sp.getString("normal.primary", ""));
		secondary.setText(sp.getString("normal.secondary", ""));
	}

	@TargetApi(9)
	private void activateForwarder() {
		File exeFile = new File(getCacheDir(), DNSFORWARD);
		String exe = exeFile.getAbsolutePath();
		EditText serverView = (EditText) findViewById(R.id.mainServer);
		EditText portView = (EditText) findViewById(R.id.mainPort);
		RadioButton udpView = (RadioButton) findViewById(R.id.mainUDP);

		String server = serverView.getText().toString();
		String port = portView.getText().toString();
		String type = udpView.isChecked() ? "udp" : "tcp";

		if (!IP_ADDRESS.matcher(server).matches()) {
			serverView.requestFocus();
			serverView.selectAll();
			return;
		}

		if (port.length() == 0) {
			portView.requestFocus();
			return;
		}

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
				try {
					if (in != null)
						in.close();
					if (out != null)
						out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			// Set executable
			if (Build.VERSION.SDK_INT >= 9) {
				exeFile.setExecutable(true, false);
			} else {
				try {
					Runtime.getRuntime().exec(
							new String[] { "chmod", "+x", exe });
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		String pid = exe + ".pid";
		String cmd = String.format("killall %s; %s %s %s %s %s;"
				+ "setprop net.dns1 127.0.0.1; setprop net.dns2 ''",
				DNSFORWARD, exe, server, port, type, pid);
		try {
			savePreferences();
			Runtime.getRuntime().exec(new String[] { "su", "-c", cmd })
					.waitFor();
			loadCurrentDNS();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
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

		String cmd = String.format("killall %s;" + "setprop net.dns1 '%s';"
				+ "setprop net.dns2 '%s'", DNSFORWARD, primary, secondary);

		try {
			savePreferences();
			Runtime.getRuntime().exec(new String[] { "su", "-c", cmd }).waitFor();
			loadCurrentDNS();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private final OnClickListener mOnApplyClick = new OnClickListener() {
		@Override
		public void onClick(View arg0) {
			RadioGroup mode = (RadioGroup) findViewById(R.id.mainMode);
			if (mode.getCheckedRadioButtonId() == R.id.mainForward) {
				activateForwarder();
			} else {
				activateNormal();
			}
		}
	};

	private final OnClickListener mOnRevertClick = new OnClickListener() {
		@Override
		public void onClick(View arg0) {
			String cmd = String.format("killall %s;" + "setprop net.dns1 '%s';"
					+ "setprop net.dns2 '%s'", DNSFORWARD,
					getProp("dhcp.wlan0.dns1"), getProp("dhcp.wlan0.dns2"));
			try {
				Runtime.getRuntime().exec(new String[] { "su", "-c", cmd }).waitFor();
				loadCurrentDNS();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	};

	private String getProp(String key) {
		String line = "";
		try {
			Process p = Runtime.getRuntime().exec(
					new String[] { "getprop", key });
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					p.getInputStream()));
			line = reader.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return line;
	}
}
