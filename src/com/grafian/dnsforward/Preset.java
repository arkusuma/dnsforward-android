package com.grafian.dnsforward;

public class Preset {

	public static final Normal[] NORMAL_SERVERS = {
			new Normal("Custom", "", ""),
			new Normal("Google", "8.8.8.8", "8.8.4.4"),
			new Normal("OpenDNS", "208.67.222.222", "208.67.220.220"),
			new Normal("Comodo Secure DNS", "8.26.56.26", "8.20.247.20"),
			new Normal("DNS Advantage", "156.154.70.1", "156.154.71.1"),
			new Normal("Level3", "209.244.0.3", "209.244.0.4"),
			new Normal("Norton DNS", "198.153.192.1", "198.153.194.1"),
			new Normal("ScrubIT", "67.138.54.120", "207.225.209.77"),
			new Normal("SmartViper", "208.76.50.50", "208.76.51.51") };

	public static final Forward[] FORWARD_SERVERS = {
			new Forward("Custom", "", 0),
			new Forward("OpenDNS", "208.67.222.222", 5353),
			new Forward("German Privacy Foundation", "87.118.100.175", 110),
			new Forward("Swiss Privacy Foundation", "87.118.104.203", 110) };

	public static class Normal {
		String name;
		String primary;
		String secondary;

		public Normal(String name, String primary, String secondary) {
			this.name = name;
			this.primary = primary;
			this.secondary = secondary;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	public static class Forward {
		String name;
		String server;
		int port;

		public Forward(String name, String server, int port) {
			this.name = name;
			this.server = server;
			this.port = port;
		}

		@Override
		public String toString() {
			return name;
		}
	}
}
