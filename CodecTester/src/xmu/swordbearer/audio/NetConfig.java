package xmu.swordbearer.audio;

import android.util.Log;

/*
 * UDP configure
 */
public class NetConfig {
	public static String SERVER_HOST = "192.168.1.10";// server ip
//	public static String SERVER_HOST = "1.234.45.226";// server ip
//	public static String SERVER_HOST = "54.238.63.194";// server ip
	public static final int SERVER_PORT = 5656;// server port
	public static final int CLIENT_PORT = 5656;// client port

	public static void setServerHost(String ip) {
		Log.d("NetConfig", "서버 IP 설정 : " + ip);
		SERVER_HOST = ip;
	}
}
