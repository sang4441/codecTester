package xmu.swordbearer.audio.sender;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import xmu.swordbearer.audio.NetConfig;
import xmu.swordbearer.audio.data.AudioData;
import android.util.Log;

public class AudioSender implements Runnable {
	String TAG = "AudioSender ";

	private boolean isSendering = false;
	private List<AudioData> dataList;

	DatagramSocket socket;
	DatagramPacket dataPacket;
	private InetAddress ip;
	private int port;
	public static int localPort = NetConfig.CLIENT_PORT;
	
	public AudioSender() {
		dataList = Collections.synchronizedList(new LinkedList<AudioData>());
		try {
			try {
				ip = InetAddress.getByName(NetConfig.SERVER_HOST);
				Log.d(TAG, "서버의 IP : " + ip.toString());
				port = NetConfig.SERVER_PORT;
				socket = new DatagramSocket();
				localPort = socket.getLocalPort();
				Log.d(TAG, localPort+" 포트로 송신 시작");
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}

	public void addData(byte[] data, int size) {
		AudioData encodedData = new AudioData();
		encodedData.setSize(size);
		byte[] tempData = new byte[size];
		System.arraycopy(data, 0, tempData, 0, size);
		encodedData.setRealData(tempData);
		dataList.add(encodedData);
	}

	/*
	 * send data to server
	 */
	private void sendData(byte[] data, int size) {
		try {
			dataPacket = new DatagramPacket(data, size, ip, port);
			dataPacket.setData(data);
			Log.d(TAG, "서버로 전송 : " + data.length+", remaining size : "+dataList.size()+", from port "+socket.getLocalPort());
			socket.send(dataPacket);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*
	 * start sending data
	 */
	public void startSending() {
		new Thread(this).start();
	}

	/*
	 * stop sending data
	 */
	public void stopSending() {
		this.isSendering = false;
	}

	// run
	public void run() {
		this.isSendering = true;
		System.out.println(TAG + "start....");
		while (isSendering) {
			if (dataList.size() > 0) {
				AudioData encodedData = dataList.remove(0);
				sendData(encodedData.getRealData(), encodedData.getSize());
			}
		}
		System.out.println(TAG + "stop!!!!");
	}
}