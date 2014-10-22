package xmu.swordbearer.audio.ui;


import org.sipdroid.codecs.Codecs;

import ssh.net.stun.DiscoveryInfo;
import ssh.net.stun.StunClient;

import xmu.swordbearer.audio.AudioWrapper;
import xmu.swordbearer.audio.NetConfig;
import xmu.swordbearer.audio.data.Receiver;
import xmu.swordbearer.audio.data.Sender;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity implements OnClickListener, OnCheckedChangeListener {
	String LOG = "MainActivity";
	private AudioWrapper audioWrapper;

	// View
	private Button btnStartRecord;
	private Button btnStopRecord;
	private Button btnStartListen;
	private Button btnStopListen;
	private Button btnExit;
	private EditText ipEditText;

	// 새로 추가된 View
	private Button btnStartEcho, btnStopEcho;
	private RadioGroup rgLocal, rgEncode, rgCodec, rgAmpl;
	private static TextView textLog;
	private static ScrollView svLog;
	
	public void onCreate(Bundle savedInstanceState) { 
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		audioWrapper = AudioWrapper.getInstance();

		initView();
		
		initNewView();
		
		DiscoveryInfo info = getStunInfo("s2.taraba.net", 3478);
		Log.d(LOG, info.toString());
		Log(info.toString());
	} 

	public static DiscoveryInfo getStunInfo(String server, int port) {
		StunClient client = new StunClient(server, port);
		DiscoveryInfo info = client.binding(null);
		if (info.getErrorCode() != 0) {
			return null;
		} else {
			return info;
		}
	}
	
	private void initNewView() {
		btnStartEcho = (Button) findViewById(R.id.startEcho);
		btnStopEcho = (Button) findViewById(R.id.stopEcho);
		rgLocal = (RadioGroup) findViewById(R.id.radioGroupLocal);
		rgEncode = (RadioGroup) findViewById(R.id.radioGroupEncoding);
		rgCodec = (RadioGroup) findViewById(R.id.radioGroupCodec);
		rgAmpl = (RadioGroup) findViewById(R.id.radioGroupAmpl);
		textLog = (TextView) findViewById(R.id.textViewLog);
		
		btnStopEcho.setEnabled(false);
		
		btnStartEcho.setOnClickListener(this);
		btnStopEcho.setOnClickListener(this);
		rgLocal.setOnCheckedChangeListener(this);
		rgEncode.setOnCheckedChangeListener(this);
		rgCodec.setOnCheckedChangeListener(this);
		rgAmpl.setOnCheckedChangeListener(this);
		
		final ScrollView svMain = (ScrollView) findViewById(R.id.ScrollViewMain);
		svLog = (ScrollView) findViewById(R.id.scrollViewLog);
		svLog.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_UP)
					svMain.requestDisallowInterceptTouchEvent(false);
				else
					svMain.requestDisallowInterceptTouchEvent(true);
				return false;
			}
		});
		
		textLog.setText("");
	}
	
	public static void Log(String str) {
		textLog.append(str+"\n");
		svLog.post(new Runnable() {
			@Override
			public void run() {
				svLog.fullScroll(ScrollView.FOCUS_DOWN);
			}
		});
	}
	
	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.startEcho:
			audioWrapper.startSend();
			audioWrapper.startReceive();
			btnStartEcho.setEnabled(false);
			btnStopEcho.setEnabled(true);
			
			Log("Echo Start");
			break;
		case R.id.stopEcho:
			audioWrapper.stopReceive();
			audioWrapper.stopSend();
			btnStartEcho.setEnabled(true);
			btnStopEcho.setEnabled(false);
			
			Log("Echo Stop");
			break;
		}
	}
	
	@Override
	public void onCheckedChanged(RadioGroup group, int checkedId) {
		boolean ongoing = btnStopEcho.isEnabled();

		if (ongoing) {
			try {
				audioWrapper.stopReceive();
				Thread.sleep(100);
				audioWrapper.stopSend();
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		switch (checkedId) {
		case R.id.radioNet:
			Sender.local = false;
			Log("패킷을 서버에서 Echo 합니다");
			break;
		case R.id.radioLocal:
			Sender.local = true;
			Log("패킷을 로컬에서 Echo 합니다");
			break;
		case R.id.radioEncode:
			Sender.encode = true;
			Log("Audio 데이터를 인코딩 합니다");
			break;
		case R.id.radioNoEncode:
			Sender.encode = false;
			Log("Audio 데이터를 인코딩하지 않습니다");
			break;
		case R.id.radioG722:
		case R.id.radioGSM:
		case R.id.radioiLBC:
		case R.id.radioOpus:
		case R.id.radioPCMA:
		case R.id.radioPCMU:
		case R.id.radioSpeex:
		case R.id.radioRAW:
			Sender.codecName = Codecs.get(Integer.parseInt(findViewById(checkedId).getTag().toString())).name();
			Log("코덱이 "+Sender.codecName+"으로 바뀌었습니다");
			break;
		case R.id.radioAmpl1:
		case R.id.radioAmpl2:
		case R.id.radioAmpl3:
		case R.id.radioAmpl5:
		case R.id.radioAmpl_2:
			Receiver.amplMode = Integer.parseInt(findViewById(checkedId).getTag().toString());
			break;
		}
		if (ongoing) {
			try {
				audioWrapper.startSend();
				Thread.sleep(100);
				audioWrapper.startReceive();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void initView() {
		btnStartRecord = (Button) findViewById(R.id.startRecord);
		btnStartListen = (Button) findViewById(R.id.startListen);
		btnStopRecord = (Button) findViewById(R.id.stopRecord);
		btnStopListen = (Button) findViewById(R.id.stopListen);
		ipEditText = (EditText) findViewById(R.id.edittext_ip);

		btnStopRecord.setEnabled(false);
		btnStopListen.setEnabled(false);

		btnExit = (Button) findViewById(R.id.btnExit);
		btnStartRecord.setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) {
				String ipString = ipEditText.getText().toString().trim();
				NetConfig.setServerHost(ipString);
				btnStartRecord.setEnabled(false);
				btnStopRecord.setEnabled(true);
				audioWrapper.startRecord();
				audioWrapper.startSend();
			}
		});

		btnStopRecord.setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) {
				btnStartRecord.setEnabled(true);
				btnStopRecord.setEnabled(false);
				audioWrapper.stopRecord();
				audioWrapper.stopSend();
			}
		});
		btnStartListen.setOnClickListener(new View.OnClickListener() {

			public void onClick(View arg0) {
				btnStartListen.setEnabled(false);
				btnStopListen.setEnabled(true);
				audioWrapper.startListen();
				audioWrapper.startReceive();
			}
		});
		btnStopListen.setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) {
				btnStartListen.setEnabled(true);
				btnStopListen.setEnabled(false);
				audioWrapper.stopListen();
				audioWrapper.stopReceive();
			}
		});
		btnExit.setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) {
				audioWrapper.stopListen();
				audioWrapper.stopRecord();
				audioWrapper.stopSend();
				audioWrapper.stopReceive();
				System.exit(0);
			}
		});
	}
}