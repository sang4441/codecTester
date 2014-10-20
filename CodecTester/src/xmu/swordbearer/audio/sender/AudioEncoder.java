package xmu.swordbearer.audio.sender;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.sipdroid.codecs.iLBC;

import xmu.swordbearer.audio.data.AudioData;
import android.util.Log;

public class AudioEncoder implements Runnable {
	String TAG = "AudioEncoder";

	private static AudioEncoder encoder;
	private boolean isEncoding = false;

	private List<AudioData> dataList = null;

	public static AudioEncoder getInstance() {
		if (encoder == null) {
			encoder = new AudioEncoder();
		}
		return encoder;
	}

	private AudioEncoder() {
		dataList = Collections.synchronizedList(new LinkedList<AudioData>());
	}

	public void addData(byte[] data, int size) {
		AudioData rawData = new AudioData();
		rawData.setSize(size);
		byte[] tempData = new byte[size];
		System.arraycopy(data, 0, tempData, 0, size);
		rawData.setRealData(tempData);
		dataList.add(rawData);
	}

	/*
	 * start encoding
	 */
	public void startEncoding() {
		System.out.println(TAG + "start encode thread");
		if (isEncoding) {
			Log.d(TAG, "encoder has been started  !!!");
			return;
		}
		new Thread(this).start();
	}

	/*
	 * end encoding
	 */
	public void stopEncoding() {
		this.isEncoding = false;
	}

	public void run() {
		// start sender before encoder
		AudioSender sender = new AudioSender();
		sender.startSending();

		int encodeSize = 0;
		byte[] encodedData = new byte[256];

		// initialize audio encoder:mode is 30
		iLBC.audio_codec_init(30);

		isEncoding = true;
		while (isEncoding) {
			if (dataList.size() == 0) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				continue;
			}
			if (isEncoding) {
				AudioData rawData = dataList.remove(0);
				encodedData = new byte[rawData.getSize()];
				Log.d(TAG, "before audio_encode");
				encodeSize = iLBC.audio_encode(rawData.getRealData(), 0, rawData.getSize(), encodedData, 0);
				Log.d(TAG, "before sender.addData");
				if (encodeSize > 0) {
					sender.addData(encodedData, encodeSize);
				}
				Log.d(TAG, "after sender.addData");
				Log.d(TAG, "encoding dataList remaining size : "+dataList.size());
			}
		}
		System.out.println(TAG + "end encoding");
		sender.stopSending();
	}

}