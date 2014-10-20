package xmu.swordbearer.audio;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;

import org.sipdroid.codecs.iLBC;

import xmu.swordbearer.audio.data.Receiver;
import xmu.swordbearer.audio.data.Sender;
import xmu.swordbearer.audio.receiver.AudioReceiver;
import xmu.swordbearer.audio.sender.AudioRecorder;
import android.provider.MediaStore.Audio;
import android.util.Log;

public class AudioWrapper {
	public static String TAG = "AudioWrapper";
	
	private AudioRecorder audioRecorder;
	private AudioReceiver audioReceiver;

	private static AudioWrapper instanceAudioWrapper;
	
	private AudioWrapper() {
//		Sender.codecTest();
	}
	
	public static AudioWrapper getInstance() {
		if (null == instanceAudioWrapper) {
			instanceAudioWrapper = new AudioWrapper();
		}
		return instanceAudioWrapper;
	}

	public void startRecord() {
		if (null == audioRecorder) {
			audioRecorder = new AudioRecorder();
		}
		audioRecorder.startRecording();
	}

	public void stopRecord() {
		if (audioRecorder != null)
			audioRecorder.stopRecording();
	}

	
	public void startListen() {
		if (null == audioReceiver) {
			audioReceiver = new AudioReceiver();
		}
		audioReceiver.startRecieving();
	}

	public void stopListen() {
		if (audioReceiver != null)
			audioReceiver.stopRecieving();
	}



	private Sender sender;
	private Receiver receiver;
	
	public Sender getSender() { return sender; }
	public Receiver getReceiver() { return receiver; }
	
	public void startSend() {
		if (null == sender) {
			sender = new Sender();
		}
		sender.startSending();
	}
	
	public void stopSend() {
		if (sender != null) {
			sender.stopSending();
		}
	}
	
	public void startReceive() {
		if (receiver == null) {
			receiver = new Receiver();
		}
		receiver.startReceive();
	}

	public void stopReceive() {
		if (receiver != null)
			receiver.stopReceive();
	}
}
