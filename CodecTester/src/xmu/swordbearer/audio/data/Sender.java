package xmu.swordbearer.audio.data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Iterator;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;

import opensl_example.opensl_example;

import org.sipdroid.codecs.Codec;
import org.sipdroid.codecs.Codecs;
import org.sipdroid.codecs.iLBC;

import xmu.swordbearer.audio.AudioConfig;
import xmu.swordbearer.audio.AudioWrapper;
import xmu.swordbearer.audio.NetConfig;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

public class Sender implements Runnable {
	public static final String TAG = "Sender";

	private boolean isSendering = false;

	private AudioRecord audioRecord;
	
	private Codec codec;
	
	private File file;
	private FileOutputStream fos;

	/**
	 * Recorder Configure
	 */
	public static boolean local = false;
	public static boolean encode = true;
	public static boolean time_check = true;
	public static int SAMPLERATE = 8000; // 8KHZ
	public static int BUFFER_FRAME_SIZE = 160; // 160 samples = 160 short = 320byte
	public static int FRAME_PERIOD = 20; // ms, 160 samples / 8000 samples/s * 1000(ms/s)
	public static ArrayBlockingQueue <byte[]> queue = new ArrayBlockingQueue<byte[]>(100);
	public static String codecName = "PCMA"; // RAW iLBC Opus PCMA PCMU speex silk8 silk16 silk24 GSM BV16 G722
	// 실패 : silk, BV16(adjust가 32)
	
	public static DatagramSocket socket;
	public static DatagramPacket packet;

	private byte[] packetBuf = new byte[1300];
	private int packetSize = 1300;

	DatagramPacket dataPacket;
	private InetAddress ip;
	private int port = NetConfig.SERVER_PORT;

	public Sender() {
//		file = new File("/sdcard/encode.amr");
//		try {
//			if (!file.exists())
//				try {
//					file.createNewFile();
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			fos = new FileOutputStream(file);
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		}
		
		try {
			ip = InetAddress.getByName(NetConfig.SERVER_HOST);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
	
	public void startSending() {
		// local인 경우 queue 초기화
//		if (local) {
//			queue.clear();
//		} else { // socket 초기화
//			if (socket != null) {
//				socket.close();
//				socket = null;
//			}
//			try {
//				Log.d(TAG, NetConfig.CLIENT_PORT + " 포트로 송수신 시작");
//				socket = new DatagramSocket(NetConfig.CLIENT_PORT);
//				packet = new DatagramPacket(packetBuf, packetSize);
//				socket.setReceiveBufferSize(58*40); // 20개만 살려둔다.
//			} catch (SocketException e) {
//				e.printStackTrace();
//			}
//		}
		
		new Thread(this).start();
	}

	public void stopSending() {
		this.isSendering = false;
	}

	public void init() {
		// Codec 초기화
		codec = Codecs.getName(codecName);
		codec.init();
		SAMPLERATE = codec.samp_rate();
		BUFFER_FRAME_SIZE = codec.frame_size();
		FRAME_PERIOD = codec.speed();

		// AudioRecord 초기화
		int audioBufSize = AudioRecord.getMinBufferSize(SAMPLERATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
		if (audioBufSize == AudioRecord.ERROR_BAD_VALUE) {
			Log.e(TAG, "audioBufSize error");
			return;
		}
		Log.d(TAG, "audioBufSize : "+audioBufSize); // 1024
		if (audioRecord == null) {
			audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLERATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, audioBufSize);
		}
		seq = 0;
	}
	
	// 서버 or queue로 데이터 전송
	private static ByteBuffer timestamp = ByteBuffer.allocate(8);    
	private void sendData(byte[] data, int size, long start) {
		try {
			byte[] arr = new byte[size+(time_check?8:0)];
			System.arraycopy(data, 0, arr, 0, size);
			
			if (time_check) { // 시간기록 추가
				timestamp.putLong(0, start);
				System.arraycopy(timestamp.array(), 0, arr, size, 8);
			}
			
			// 맨 앞 4byte에 순서 추가
			timestamp.putInt(0, seq++);
			System.arraycopy(timestamp.array(), 0, arr, 0, 4);
			
			if (local) { // 로컬일 경우 큐에 넣는다.
				queue.add(arr); 
				if (queue.size()>10) queue.poll(); // 10개만 유지시킨다.
			} else { // 서버로 데이터그램을 보낸다.
				dataPacket = new DatagramPacket(arr, size+(time_check?8:0), ip, port);
				dataPacket.setData(arr);
				socket.send(dataPacket);
			}
			Log.d(TAG, "sendData"+(time_check?" with timestamp":"")+" : " + arr.length+" byte");
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private int seq = 0; 
	// run
	public void run() {
		Log.d(TAG, "start");
//		if (true) { codecTest(); return; }
		
//		init();
		this.isSendering = true;
		
		/*
		int frame_rate = SAMPLERATE / BUFFER_FRAME_SIZE; // 초당 프레임 수, 50
		long frame_period = 1000 / frame_rate; // 프레임의 기간, 20ms
		frame_rate *= 1.5; // 75
		short[] lin = new short[BUFFER_FRAME_SIZE * (frame_rate + 1)]; // 버퍼를 75배로
		*/
		
		short[] lin = new short[BUFFER_FRAME_SIZE]; // 읽어올 음성 데이터. 2배 정도로 잡아둔다.
		byte[] encoded; // 음성 데이터를 인코딩한 데이터. 사이즈는 코덱별로 상이
		int encodeSize; // 인코딩한 바이트 배열의 크기
		int bufferRead; // 마이크에서 읽어온 데이터수
//		this.isSendering = false;

		byte[] randb = new byte[BUFFER_FRAME_SIZE*2];
		byte[] enc = new byte[BUFFER_FRAME_SIZE*2];
		setRand(randb);
		short[] rands = new short[BUFFER_FRAME_SIZE];
		ByteBuffer.wrap(randb).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(rands);
		
		long before, start = System.currentTimeMillis();
		long reload = System.currentTimeMillis();
		
		opensl_example.start_process();
//		audioRecord.startRecording();
		while (isSendering) {
			 
			opensl_example.setBuffer(opensl_example.getBuffer());
			
			before = start;
			start = System.currentTimeMillis();
			bufferRead = audioRecord.read(lin, 0, BUFFER_FRAME_SIZE);
//			audioRecord.read(lin, 0, BUFFER_FRAME_SIZE); // discard for remove delay
			if (bufferRead > 0) { // short 기준 읽어온 사이즈, 읽어온 데이터가 있어야 전송
				encoded = new byte[BUFFER_FRAME_SIZE*2]; // 인코딩 버퍼를 원본 음성만큼 잡는다.
				if (encode) { // 인코딩을 할 경우, 인코딩 후 데이터 크기는 줄어든다.
					// sipdroid 코덱들은 RTP용 12바이트를 앞쪽에 추가하여 인코딩된다.
					encodeSize = codec.encode(lin, 0, encoded, bufferRead);
					Log.d(TAG, "(+"+(start-before)+")read size : "+(bufferRead*2)+" byte, encoded size : "+encodeSize+" byte");
				} else { // 인코딩하지 않을 경우, 원본과 사이즈가 동일하다.
					short[] buf = new short[BUFFER_FRAME_SIZE];
					System.arraycopy(lin, 0, buf, 0, bufferRead);
					ByteBuffer.wrap(encoded).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(buf);
					
					// gzip 추가
					
					encodeSize = bufferRead*2;
					Log.d(TAG, "(+"+(start-before)+")read size : "+(bufferRead*2)+" byte");
				}
				sendData(encoded, encodeSize, start);
			} else {
				Log.e(TAG, "bufferRead : "+bufferRead);
			}
			before = start;
			
			
//			if (reload < start - 10000) {
//				audioRecord.stop();
//				audioRecord.release();
//				
//				reload = start;
//				int bufferSize = AudioRecord.getMinBufferSize(Sender.SAMPLERATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
//				Log.i(TAG, "Recorder 초기화, buffersize : " + bufferSize);
//				audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLERATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
//				audioRecord.startRecording();
//			}
			
			
			try {
//				Thread.sleep(FRAME_PERIOD/2);
//				Thread.sleep(FRAME_PERIOD*3/4);
				long sleep = FRAME_PERIOD - System.currentTimeMillis() + start - 2;
				Thread.sleep(sleep > 0 ? sleep : 0);
				Log.d(TAG, "sleep : "+(sleep > 0 ? sleep : 0));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
//
//		audioRecord.stop();
//		audioRecord.release();
//		audioRecord = null;
		
		if (socket != null) {
			socket.close();
			socket = null;
		}

		Log.d(TAG, "stop");
	}
	
	
	
	/** 테스트용 **/

	public static String byteToHex(byte[] a) { return byteToHex(a, a.length); }
	public static String byteToHex(byte[] a, int len) {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<len; i++) {
			byte b = a[i];
			sb.append(String.format("%02x", b & 0xff));
		}
		return sb.toString();
	}
	
	public static String shortToHex(short[] a) { return shortToHex(a, a.length); }
	public static String shortToHex(short[] a, int len) {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<len; i++) {
			short s = a[i];
			sb.append(String.format("%02x%02x", s & 0xff, (s >> 8) & 0xff));
		}
		return sb.toString();
	}
	
	public static void setRand(byte[] arr) {
		Random r = new Random();
		for (int i=0; i<arr.length; i++) {
			arr[i] = (byte)r.nextInt(256);
		}
	}

	public static void ilbcTest() {
		// Codec 초기화
		Codec codec = Codecs.getName("PCMA");
		codec.init();
		iLBC.audio_codec_init(30);

		byte[] randb = new byte[480];
		setRand(randb);
		short[] rands = new short[240];
		ByteBuffer.wrap(randb).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(rands);
		
		Log.i(TAG, byteToHex(randb));
		Log.i(TAG, shortToHex(rands));
		
		byte[] encodeb = new byte[480];
		byte[] encodes = new byte[480];

		int encodeSizeb = iLBC.audio_encode(randb, 0, 480, encodeb, 0);
		int encodeSizes = codec.encode(rands, 0, encodes, 240);
		
		Log.w(TAG, encodeSizeb+":"+byteToHex(encodeb));
		iLBC.audio_encode(randb, 0, 480, encodeb, 0);
		Log.w(TAG, encodeSizeb+":"+byteToHex(encodeb));
		Log.w(TAG, encodeSizes+":"+byteToHex(encodes));
		codec.encode(rands, 0, encodes, 240);
		Log.w(TAG, encodeSizes+":"+byteToHex(encodes));
		
		byte[] decodeb = new byte[480];
		short[] decodes = new short[240];
		
		int decodeSizes = codec.decode(encodes, decodes, encodeSizes);
		int decodeSizeb = iLBC.audio_decode(encodeb, 0, encodeSizeb, decodeb, 0);
		
		Log.i(TAG, decodeSizeb+":"+byteToHex(decodeb));
		Log.i(TAG, decodeSizes+":"+shortToHex(decodes));
	}
	
	// iLBC 기준
	// 8000 Hz * 16bit PCM = 16000 byte / sec
	// frame size 240 = 480 byte
	// 480 byte / 16000 byte = 30ms
	// 320 byte / 16000 byte = 20ms
	
	// default 기준
	// 8000 Hz * 16bit PCM = 16000 byte / sec
	// frame size 160 = 320 byte
	// 320 byte / 16000 byte = 20ms
	public static void codecTest() {
		Vector<Codec> codecs = Codecs.get();
		
		Iterator<Codec> itr = codecs.iterator();
		while (itr.hasNext()) {
			Codec codec = itr.next();
			codec.init();
			boolean opus = codec.number() == 107;
			
			int sample = codec.samp_rate();
			int frame = codec.frame_size();
			int speed = codec.speed();
			
			Log.d(TAG, codec.getTitle()+" "+sample+" Hz, frame "+frame+" byte, interval "+speed+" ms Start");
			
			byte[] randb = new byte[frame*2];
			byte[] encode = new byte[frame*2];
			setRand(randb);
			short[] rands = new short[frame];
			ByteBuffer.wrap(randb).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(rands);
			
			Log.i(TAG, "before encode, frame:"+frame+", "+rands.length+" short: "+shortToHex(rands));
			
			int encodeSizes = codec.encode(rands, 0, encode, frame); // 12 바이트 추가됨
			Log.w(TAG, "after encode : "+encodeSizes+" byte, "+(encodeSizes*8/speed)+" kbps: "+byteToHex(encode, encodeSizes));
			
			short[] decode = new short[frame];
			// 일부 코덱은 추가된 rtp 헤더 12바이트만큼의 사이즈를 여기서는 빼줘야 한다.
			// 빼주지 않고 사이즈 동일 : G722 silk24 silk16 silk8 pcma pcmu speex(0이 많음) gsm bv16 opus(0이 일정량) ilbc
			// 빼줘도 사이즈 동일 and 문제없어 보임 : speex(0이 많음) gsm opus(0이 일정량)
			// opus는 실제 적용시 에러가 나므로 사이즈를 빼주도록 함
			int decodeSizes = codec.decode(encode, decode, encodeSizes - (opus?12:0));
			
			Log.i(TAG, "after decode : "+decodeSizes+" short: "+shortToHex(decode, decodeSizes));
		}
	}
}