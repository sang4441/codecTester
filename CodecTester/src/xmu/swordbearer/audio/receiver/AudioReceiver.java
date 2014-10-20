package xmu.swordbearer.audio.receiver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import xmu.swordbearer.audio.NetConfig;
import xmu.swordbearer.audio.sender.AudioSender;
import android.util.Log;

public class AudioReceiver implements Runnable {
	String TAG = "AudioReceiver";
	int port = NetConfig.CLIENT_PORT;// 接收的端口
	DatagramSocket socket;
	DatagramPacket packet;
	boolean isRunning = false;

	private byte[] packetBuf = new byte[1024];
	private int packetSize = 1024;

	/*
	 * 开始接收数据
	 */
	public void startRecieving() {
		Log.d(TAG, "시작중, socket is "+socket);
		if (socket == null) {
			try {
				Log.d(TAG, AudioSender.localPort+1 + " 포트로 수신 시작");
				socket = new DatagramSocket(AudioSender.localPort+1);
				packet = new DatagramPacket(packetBuf, packetSize);
			} catch (SocketException e) {
				e.printStackTrace();
			}
		}
		new Thread(this).start();
	}

	/*
	 * 停止接收数据
	 */
	public void stopRecieving() {
		isRunning = false;
	}

	private void release() {
		Log.d(TAG, "종료중, socket port : "+socket.getLocalPort());
		if (packet != null) {
			packet = null;
		}
		if (socket != null) {
			socket.close();
			socket = null;
		}
		Log.d(TAG, "종료중, socket is "+socket);
	}

	public void run() {
		// 디코더 초기화
		AudioDecoder decoder = AudioDecoder.getInstance();
		decoder.startDecoding();

		isRunning = true;
		try {
			while (isRunning) {
				socket.receive(packet);
				decoder.addData(packet.getData(), packet.getLength());
				Log.d(TAG, "RECIEVE data : "+packet.getLength());
			}
		} catch (IOException e) {
			Log.e(TAG, "RECIEVE ERROR!");
		} finally {
		decoder.stopDecoding();
		release();
		Log.d(TAG, "stop recieving");
		}
	}
}
