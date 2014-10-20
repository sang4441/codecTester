package org.sipdroid.codecs;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.util.Log;

public class iLBC extends CodecBase implements Codec {

	public iLBC() {
		CODEC_NAME = "iLBC";
		CODEC_USER_NAME = "iLBC";
		CODEC_DESCRIPTION = "13~15kbit";
		CODEC_NUMBER = 97;
		CODEC_DEFAULT_SETTING = "always";
		CODEC_FRAME_SIZE = 160;	// 30ms 240, 20ms 160	
		super.update();
	}

	public void init() {
		load();
		open();
	}
	public void init(int mode) {
		load();
		open(mode);
	}
    
	void load() {
		try {
			System.loadLibrary("audiowrapper");
			Log.d("iLBC", "iLBC 로딩 완료");
			super.load();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}  
	
	public int open() { return audio_codec_init(20); }
	public int open(int mode) { CODEC_FRAME_SIZE = mode*8; return audio_codec_init(mode); }
	public int encode(short lin[], int offset, byte encoded[], int size) {
		// to turn shorts back to bytes.
		byte[] sample = new byte[size*2];
		short[] shrink = new short[size]; // shrink short array. if don't, BufferOverflow
		System.arraycopy(lin, 0, shrink, 0, size);
		ByteBuffer.wrap(sample).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(shrink);
		
//		return audio_encode(sample, offset, size*2, encoded, 0);
		
		return audio_encode(sample, offset, size*2, encoded, 12) + 12; // rtp용 헤더 12 byte를 건너뛴다.

		// 위 방식이 잘 되지 않아 추가한 방식. 원래는 50byte + 12 byte가 되어야 하나 position만 12부터 시작하고 byte는 동일하여 총 38 byte가 되어버림
//		byte[] payload = new byte[size*2];
//		int r = audio_encode(sample, offset, size*2, payload, 0);
//		System.arraycopy(payload, 0, encoded, 12, r);
//		
//		return r + 12; // rtp용 헤더 12 byte + 인코딩된 사이즈
	}
	public int decode(byte encoded[], short lin[], int size) {
		byte[] sample = new byte[lin.length*2];
		byte[] payload = new byte[size-12];
		System.arraycopy(encoded, 12, payload, 0, size-12);
		int r = audio_decode(payload, 0, size-12, sample, 0); // rtp용 헤더 12byte를 건너뛴다.

		// to turn bytes to shorts as either big endian or little endian. 
		ByteBuffer.wrap(sample).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(lin);
		
		return r/2; 
	}
	public void close() {}


	// initialize decoder and encoder
	public static native int audio_codec_init(int mode);

	// encode
	public static native int audio_encode(byte[] sample, int sampleOffset, int sampleLength, byte[] data, int dataOffset);

	// decode
	public static native int audio_decode(byte[] data, int dataOffset, int dataLength, byte[] sample, int sampleLength);
	

}
