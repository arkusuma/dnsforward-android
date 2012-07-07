package com.grafian.dnsforward;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Pattern;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.RadioButton;

public class MainActivity extends Activity {

	private static final Pattern IP_ADDRESS = Pattern
			.compile("((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(25[0-5]|2[0-4]"
					+ "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1]"
					+ "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}"
					+ "|[1-9][0-9]|[0-9]))");

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		findViewById(R.id.mainStart).setOnClickListener(mOnStartClick);
		findViewById(R.id.mainStop).setOnClickListener(mOnStopClick);

		((EditText) findViewById(R.id.mainServer)).setText("208.67.222.222");
		((EditText) findViewById(R.id.mainPort)).setText("5353");
		((RadioButton) findViewById(R.id.mainUDP)).setChecked(true);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	private final OnClickListener mOnStartClick = new OnClickListener() {
		@Override
		public void onClick(View arg0) {
			File exe = new File(getCacheDir(), "dnsforward");
			InputStream in = null;
			OutputStream out = null;
			try {
				in = getResources().openRawResource(R.raw.dnsforward);
				out = new FileOutputStream(exe);

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

			String exePath = exe.getAbsolutePath();
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

			String cmd = String.format("kill -9 `cat %s.pid`;" + "chmod +x %s;"
					+ "%s %s %s %s %s.pid;" + "setprop net.dns1 127.0.0.1",
					exePath, exePath, exePath, server, port, type, exePath);
			try {
				Runtime.getRuntime().exec(new String[] { "su", "-c", cmd });
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	};

	private final OnClickListener mOnStopClick = new OnClickListener() {
		@Override
		public void onClick(View arg0) {
			File exe = new File(getCacheDir(), "dnsforward");
			String exePath = exe.getAbsolutePath();
			String cmd = String.format("kill -9 `cat %s.pid`;"
					+ "setprop net.dns1 `getprop dhcp.wlan0.dns1`", exePath);
			try {
				Runtime.getRuntime().exec(new String[] { "su", "-c", cmd });
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	};
}
