package xmu.swordbearer.audio.receiver;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.sipdroid.codecs.iLBC;

import xmu.swordbearer.audio.data.AudioData;
import android.util.Log;

public class AudioDecoder implements Runnable {

	String TAG = "AudioDecoder";
	private static AudioDecoder decoder;

	private static final int MAX_BUFFER_SIZE = 2048;

	private byte[] decodedData = new byte[1024];// data of decoded
	private boolean isDecoding = false;
	private List<AudioData> dataList = null;

	public static AudioDecoder getInstance() {
		if (decoder == null) {
			decoder = new AudioDecoder();
		}
		return decoder;
	}

	private AudioDecoder() {
		this.dataList = Collections.synchronizedList(new LinkedList<AudioData>());
	}

	/*
	 * add Data to be decoded
	 * 
	 * @ data:the data recieved from server
	 * 
	 * @ size:data size
	 */
	public void addData(byte[] data, int size) {
		AudioData adata = new AudioData();
		adata.setSize(size);
		byte[] tempData = new byte[size];
		System.arraycopy(data, 0, tempData, 0, size);
		adata.setRealData(tempData);
		dataList.add(adata);

	}

	/*
	 * start decode AMR data
	 */
	public void startDecoding() {
		System.out.println(TAG + " 시작");
		if (isDecoding) {
			return;
		}
		new Thread(this).start();
	}

	public void run() {
		// start player first
		AudioPlayer player = AudioPlayer.getInstance();
		player.startPlaying();
		//
		this.isDecoding = true;
		// init ILBC parameter:30 ,20, 15
		iLBC.audio_codec_init(30);

		Log.d(TAG, "initialized decoder");
		int decodeSize = 0;
		while (isDecoding) {
			while (dataList.size() > 0) {
				AudioData encodedData = dataList.remove(0);
				decodedData = new byte[MAX_BUFFER_SIZE];
				byte[] data = encodedData.getRealData();
				//
				decodeSize = iLBC.audio_decode(data, 0, encodedData.getSize(), decodedData, 0);
				Log.d(TAG, "받은 데이터 : " + data.length + ", 디코딩 후 데이터 : " + decodeSize + ", remaining size : "+dataList.size());
				if (decodeSize > 0) {
					// add decoded audio to player
					player.addData(decodedData, decodeSize);
				}
			}
		}
		System.out.println(TAG + " 종료");
		// stop playback audio
		player.stopPlaying();
	}

	public void stopDecoding() {
		this.isDecoding = false;
	}
}