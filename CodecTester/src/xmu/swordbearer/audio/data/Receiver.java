package xmu.swordbearer.audio.data;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.TimeUnit;

import org.sipdroid.codecs.Codec;
import org.sipdroid.codecs.Codecs;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.os.SystemClock;
import android.util.Log;

public class Receiver implements Runnable {
	String TAG = "Receiver";
	
	boolean isRunning = false;
	
	DatagramSocket socket;
	DatagramPacket packet;

	private AudioTrack audioTrack;

	private File file;
	private FileOutputStream fos;

	private Codec codec;
	public static int amplMode = 1; // -2면 1/2
	
	public Receiver() {
//		file = new File("/sdcard/decode.amr");
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
	}
	
	
	private boolean init() {
		
		// Codec 초기화
		codec = Codecs.getName(Sender.codecName);
		codec.init();
		
		Sender.SAMPLERATE = codec.samp_rate();
		Sender.BUFFER_FRAME_SIZE = codec.frame_size();
		Sender.FRAME_PERIOD = codec.speed();
		
		int bufferSize = AudioRecord.getMinBufferSize(Sender.SAMPLERATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
		if (bufferSize < 0) {
			Log.e(TAG, "initialize error!");
			return false;
		}
		Log.i(TAG, "Player 초기화, buffersize : " + bufferSize);
		// AudioManager.STREAM_DTMF : 빠름
		// AudioManager.STREAM_MUSIC : 빠름
		// AudioManager.STREAM_VOICE_CALL : 느림
		// AudioManager.STREAM_ALARM : 빠름
		// AudioManager.STREAM_NOTIFICATION : 빠름, 이어폰 안들림
		// AudioManager.STREAM_RING : 빠름
		// AudioManager.STREAM_SYSTEM : 빠름
		int outputStream = AudioManager.STREAM_SYSTEM;
		
		audioTrack = new AudioTrack(outputStream, Sender.SAMPLERATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);
		audioTrack.setStereoVolume(1.0f, 1.0f);
		audioTrack.play();
		
		if (Sender.local) {
			Sender.queue.clear();
		} else {
			socket = Sender.socket;
			packet = Sender.packet;
		}

		return true;
	}
	
	public void startReceive() {
		new Thread(this).start();
	}

	public void stopReceive() {
		isRunning = false;
	}

	// 서버 or queue에서 데이터 받기
	private static ByteBuffer timestamp = ByteBuffer.allocate(8);  
	private byte[] receiveData(long[] end) {
		byte[] data, buf = new byte[0];
		int len = 0;
		try {
			if (Sender.local) { // 로컬일 경우 큐에서 가져온다.
				data = Sender.queue.poll(Sender.FRAME_PERIOD*20, TimeUnit.MILLISECONDS);
				if (data == null) {
					return null;
				}
				len = data.length;
				Log.d(TAG, "get "+data.length+" bytes from queue");
			} else {
				socket.receive(packet);
				data = packet.getData();
				len = packet.getLength();
				Log.d(TAG, "get "+len+" bytes from server");
			}

			// 맨 앞 4byte는 순서이다.
			timestamp.rewind();
			timestamp.put(data, 0, 4);
			timestamp.rewind();
			seq = timestamp.getInt();

			if (Sender.time_check) { // 시간을 가져온다.
				timestamp.rewind();
				timestamp.put(data, len-8, 8);
				timestamp.flip();//need flip 
		        end[0] = timestamp.getLong();
		        buf = new byte[len-8];
		        System.arraycopy(data, 0, buf, 0, buf.length);
			} else {
		        buf = new byte[len];
		        System.arraycopy(data, 0, buf, 0, len);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return buf;
	}
	
	private int befseq = 0, seq = 0;
	private int jitter = 0, loss = 0;
	
	public void run() {
		Log.d(TAG, "start");
		
		isRunning = true;
		
		if (!init()) {
			Log.e(TAG, "초기화에 실패했습니다");
			return;
		}
		Log.d(TAG, "decoding with "+codec.name()+" number : "+codec.number());
		boolean adjust = codec.number() != 97 && codec.number() != 88 && codec.number() != 97; // ilbc, RAW
		Log.d(TAG, "adjust : "+adjust);
		
		int decodeSize = 0;
		long[] end = new long[1];
		short[] lin = new short[Sender.BUFFER_FRAME_SIZE];
		long before, start = System.currentTimeMillis();
		long reload = System.currentTimeMillis();
		
		while (isRunning) {
			before = start;
			start = System.currentTimeMillis();

			befseq = seq;
			byte[] data = receiveData(end);
			if (data == null) {
				continue;
			}
//			Log.d(TAG, "received data : "+Sender.byteToHex(data));
			
			if (seq <= befseq) {
				jitter++;
				Log.e(TAG, "jitter : "+jitter+", bef : "+befseq+", seq : "+seq);
			}
			if (seq != befseq + 1) {
				loss++;
				Log.e(TAG, "loss : "+loss+", bef : "+befseq+", seq : "+seq);
			}
			
//			Log.d(TAG, "getPlaybackHeadPosition : "+audioTrack.getPlaybackHeadPosition()); // 플레이 위치?
			
			// 사이즈가 맞지 않으면 무시
			if (Sender.encode && data.length == Sender.BUFFER_FRAME_SIZE*2 || !Sender.encode && data.length < Sender.BUFFER_FRAME_SIZE*2 || data.length < 12 || data.length > Sender.BUFFER_FRAME_SIZE*2) {
				audioTrack.write(new short[Sender.BUFFER_FRAME_SIZE], 0, Sender.BUFFER_FRAME_SIZE);
			} else {
				if (Sender.encode) { // 디코딩을 해야 하는 경우
					Log.d(TAG, "(+"+(start-before)+") decoding, data : "+data.length+", to lin : "+lin.length);
					try {
						decodeSize = codec.decode(data, lin, data.length - (adjust?12:0)); // sipdroid 코덱들은 rtp 12바이트를 앞쪽에 달고 있다. opus만이 그만큼을 제거한 사이즈를 제공해야 한다.
						Log.d(TAG, "decoded from "+data.length+" byte to "+decodeSize*2+" byte, write to Track"+(Sender.time_check?", elapsed time : "+(System.currentTimeMillis() - end[0]):""));
					} catch (Exception e) {
						Log.e(TAG, "decoded from "+data.length+" byte error : "+e+(Sender.time_check?", elapsed time : "+(System.currentTimeMillis() - end[0]):""));
					}
				} else {
					ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(lin);
					decodeSize = data.length/2;
					Log.d(TAG, "(+"+(start-before)+") received "+decodeSize*2+" byte, write to Track"+(Sender.time_check?", elapsed time : "+(System.currentTimeMillis() - end[0]):""));
				}
				
				switch(amplMode) {
				case -2:
					calc_2(lin, 0, decodeSize);
					break;
				case 2:
					calc2(lin, 0, decodeSize);
					break;
				case 3:
					calc3(lin, 0, decodeSize);
					break;
				case 5:
					calc5(lin, 0, decodeSize);
					break;
				}
				Log.i(TAG, "Sender.queue.size() : "+Sender.queue.size());

				audioTrack.write(lin, 0, decodeSize);
				
//				if (reload < start - 10000) {
//					audioTrack.stop();
//					audioTrack.release();
//					
//					
//					reload = start;
//					int bufferSize = AudioRecord.getMinBufferSize(Sender.SAMPLERATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
//					Log.i(TAG, "Player 초기화, buffersize : " + bufferSize);
//					int outputStream = AudioManager.STREAM_MUSIC;
//					audioTrack = new AudioTrack(outputStream, Sender.SAMPLERATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);
//					audioTrack.setStereoVolume(1.0f, 1.0f);
//					audioTrack.play();
//				}
			}

			// 파일에 기록
//			fos.write(decodedData);
//			fos.flush();
			
	        /* receiveData를 할 때 block 되므로 sleep을 할 필요가 없다. */
			try {
//				Thread.sleep(Sender.FRAME_PERIOD*2/4);
//				Thread.sleep(Sender.FRAME_PERIOD*3/4);
//				Thread.sleep(1);
				long sleep = Sender.FRAME_PERIOD - System.currentTimeMillis() + start;
				if (Sender.queue.size() > 0) sleep -= 2;
				Thread.sleep(sleep>0?sleep:0);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			//*/
		}
	
		Log.d(TAG, "stop");
		
		if (audioTrack != null) {
			if (audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
				audioTrack.stop();
				audioTrack.release();
			}
			audioTrack = null;
		}
	}

	void calc_2(short[] lin, int off, int len) { // 1/2로 줄임
		int i, j;

		for (i = 0; i < len; i++) {
			j = lin[i + off];
			lin[i + off] = (short) (j >> 1);
		}
	}

	void calc2(short[] lin, int off, int len) { // 2배 증폭
		int i, j;

		for (i = 0; i < len; i++) {
			j = lin[i + off];
			if (j > 16350)
				lin[i + off] = 16350 << 1;
			else if (j < -16350)
				lin[i + off] = -16350 << 1;
			else
				lin[i + off] = (short) (j << 1);
		}
	}

	void calc3(short[] lin, int off, int len) { // 3배 증폭
		int i, j;

		for (i = 0; i < len; i++) {
			j = lin[i + off];
			if (j > 10900)
				lin[i + off] = 10900 * 3;
			else if (j < -10900)
				lin[i + off] = -10900 * 3;
			else
				lin[i + off] = (short) (j * 3);
		}
	}

	void calc5(short[] lin, int off, int len) { // 5배 증폭
		int i, j;

		for (i = 0; i < len; i++) {
			j = lin[i + off];
			if (j > 6540)
				lin[i + off] = 6540 * 5;
			else if (j < -6540)
				lin[i + off] = -6540 * 5;
			else
				lin[i + off] = (short) (j * 5);
		}
	}
}