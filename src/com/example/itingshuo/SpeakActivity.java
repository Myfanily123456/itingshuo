package com.example.itingshuo;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import volley.VolleyManager;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.config.Urls;
import com.dialog.ResultDialog;
import com.dialog.UpdateDialog;
import com.entity.JSentenceList;
import com.entity.JShowMovie;
import com.entity.JShowSentence;
import com.example.itingshuo.SpeakActivity.HuiTingListener;
import com.example.itingshuo.SpeakActivity.LuYinListener;
import com.example.itingshuo.SpeakActivity.ShangChuanListener;
import com.example.itingshuo.SpeakActivity.UIHandler;
import com.player.Player;
import com.recorder.AudioFileFunc;
import com.recorder.AudioRecordFunc;
import com.recorder.ErrorCode;
import com.recorder.PlayAudioTrack;
import com.speak.JuziList;

import android.support.v7.app.ActionBarActivity;
import android.R.integer;
import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class SpeakActivity extends ActionBarActivity {
	// 音频播放控件
	private String playpath = "http://abv.cn/music/光辉岁月.mp3";// 音频地址
	private Button playBtn;
	private Player player;
	private SeekBar musicProgress;
	private int playing = -1;
	// 回听录音上传各个按钮
	private int mState = -1; // -1:没再录制，0：录制wav
	private final static int FLAG_WAV = 0;
	private LinearLayout huiTing_bg;
	private LinearLayout luYin_bg;
	private LinearLayout shangChuan_bg;
	private TextView huiTing_text;
	private TextView luYin_text;
	private TextView shangChuan_text;
	private UIHandler uiHandler;
	private Timer mTimer; // 计时器
	private TimerTask timerTask;
	private TextView tv_currentTime;
	private TextView tv_allTime;
	//服务器与intent
	public static final String TAG = "SpeakActivity";
 	private String username;
 	private String password;
 	private String courseid;
 	private String sentenceid;
 	private TextView contentTextView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_speak);
		getmIntent();
		init();
		requestDataFromServer();
		setListener();
		initRecorder();// 回听录音上传控件初始化
		setRecorderListener();// 监听回听录音上传
		uiHandler = new UIHandler();
		// 计时器
		timerTask = new TimerTask() {

			@Override
			public void run() {
				if (playing != -1) {
					Message msg = new Message();
					Bundle b = new Bundle();// 存放数据
					b.putInt("cmd", CMD_PLAYER_TIME);
					b.putString("currentTime", player.getCurrentTime());
					b.putString("allTime", player.getAllTime());
					msg.setData(b);
					uiHandler.sendMessage(msg); // 向Handler发送消息,更新UI
				}
			}
		};
		mTimer = new Timer(); // 计时器
		mTimer.schedule(timerTask, 0, 100);

	}

	public void init() {
		playBtn = (Button) findViewById(R.id.btn_online_play);
		musicProgress = (SeekBar) findViewById(R.id.music_progress);
		tv_allTime = (TextView) findViewById(R.id.all_time);
		tv_currentTime = (TextView) findViewById(R.id.current_time);
		player = new Player(musicProgress);
		musicProgress.setOnSeekBarChangeListener(new SeekBarChangeEvent());
		contentTextView = (TextView) findViewById(R.id.content);
	}

	public void setListener() {

		playBtn.setOnClickListener(new PlayListener());
	}

	// 播放监听器
	class PlayListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			// 播放录音
			// Toast.makeText(MovieActivity.this,"huiting",
			// Toast.LENGTH_SHORT).show();
			if (playing == -1) {
				new Thread(new Runnable() {

					@Override
					public void run() {
						Message msg = new Message();
						Bundle b = new Bundle();// 存放数据
						b.putInt("cmd", CMD_PLAYER_PLAY);
						msg.setData(b);
						uiHandler.sendMessage(msg); // 向Handler发送消息,更新UI
						player.playUrl(playpath);
						playing = 1;
					}
				}).start();
			} else if (playing == 1) {
				player.pause();
				Message msg = new Message();
				Bundle b = new Bundle();// 存放数据
				b.putInt("cmd", CMD_PLAYER_STOP);
				msg.setData(b);
				uiHandler.sendMessage(msg); // 向Handler发送消息,更新UI
				playing = 0;
			} else {
				player.play();
				Message msg = new Message();
				Bundle b = new Bundle();// 存放数据
				b.putInt("cmd", CMD_PLAYER_PLAY);
				msg.setData(b);
				uiHandler.sendMessage(msg); // 向Handler发送消息,更新UI
				playing = 1;
			}
		}

	}

	class SeekBarChangeEvent implements OnSeekBarChangeListener {
		int progress;

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			// 原本是(progress/seekBar.getMax())*player.mediaPlayer.getDuration()
			this.progress = progress * player.mediaPlayer.getDuration()
					/ seekBar.getMax();
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {

		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			// seekTo()的参数是相对与影片时间的数字，而不是与seekBar.getMax()相对的数字
			player.mediaPlayer.seekTo(progress);
		}

	}

	
	

	// 回听录音上传控件初始化
	private void initRecorder() {
		huiTing_bg = (LinearLayout) findViewById(R.id.huiti_bg);
		huiTing_text = (TextView) findViewById(R.id.huiti_text);
		luYin_bg = (LinearLayout) findViewById(R.id.luyin_bg);
		luYin_text = (TextView) findViewById(R.id.luyin_text);
		shangChuan_bg = (LinearLayout) findViewById(R.id.shangchuan_bg);
		shangChuan_text = (TextView) findViewById(R.id.shangchuan_text);
	}

	private void setRecorderListener() {
		huiTing_bg.setOnClickListener(new HuiTingListener());
		luYin_bg.setOnClickListener(new LuYinListener());
		shangChuan_bg.setOnClickListener(new ShangChuanListener());
	}

	// 回听监听器
	class HuiTingListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			// 播放录音
			// Toast.makeText(MovieActivity.this,"huiting",
			// Toast.LENGTH_SHORT).show();
			play();
		}

	}

	// 录音监听器
	class LuYinListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			// Toast.makeText(MovieActivity.this,"luYin",
			// Toast.LENGTH_SHORT).show();
			if (SpeakActivity.this.luYin_text.getText().equals("录音")) {
				record(FLAG_WAV);
			} else {
				stop();
			}

		}

	}

	// 上传监听器
	class ShangChuanListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			// Toast.makeText(MovieActivity.this,"shangChuan",
			// Toast.LENGTH_SHORT).show();

			getResult();
		}

	}

	private final static int CMD_RECORDING_TIME = 2000;
	private final static int CMD_RECORDFAIL = 2001;
	private final static int CMD_STOP = 2002;
	private final static int CMD_PLAYFAIL = 2003;
	private final static int CMD_PLAYER_PLAY = 2004;
	private final static int CMD_PLAYER_STOP = 2005;
	private final static int CMD_PLAYER_TIME = 2006;
	private final static int CMD_SERVER = 2008;
	

	class UIHandler extends Handler {
		public UIHandler() {
		}

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			//Log.d("MyHandler", "handleMessage......");
			super.handleMessage(msg);
			Bundle b = msg.getData();
			int vCmd = b.getInt("cmd");
			switch (vCmd) {
			case CMD_RECORDING_TIME:
				SpeakActivity.this.luYin_text.setText("完成");
				break;
			case CMD_RECORDFAIL:
				int vErrorCode = b.getInt("msg");
				String vMsg = ErrorCode.getErrorInfo(SpeakActivity.this,
						vErrorCode);
				Toast.makeText(SpeakActivity.this, "录音失败", Toast.LENGTH_SHORT)
						.show();
				SpeakActivity.this.luYin_text.setText("录音");
				break;
			case CMD_STOP:
				int vFileType = b.getInt("msg");
				switch (vFileType) {
				case FLAG_WAV:
					AudioRecordFunc mRecord_1 = AudioRecordFunc.getInstance();
					SpeakActivity.this.luYin_text.setText("录音");
					break;
				}
				break;
			case CMD_PLAYFAIL:
				Toast.makeText(SpeakActivity.this, "请先录音！", Toast.LENGTH_SHORT)
						.show();
				break;
			case CMD_PLAYER_PLAY:
				SpeakActivity.this.playBtn
						.setBackgroundResource(R.drawable.stop);
				break;
			case CMD_PLAYER_STOP:
				SpeakActivity.this.playBtn
						.setBackgroundResource(R.drawable.play);
				break;
			case CMD_PLAYER_TIME:
				String currentTimeString = b.getString("currentTime");
				String allTime = b.getString("allTime");
				tv_allTime.setText(allTime);
				tv_currentTime.setText(currentTimeString);
				break;
			case CMD_SERVER:
				//接收服务器信息
				String content = b.getString("content");
				String sentenceSrc = b.getString("sentenceSrc");
				contentTextView.setText(content);
				playpath = sentenceSrc;
				break;
			default:
				break;
			}
		}
	};

	/**
	 * 开始录音
	 * 
	 * @param mFlag
	 *            ，0：录制wav格式，1：录音amr格式
	 */
	private void record(int mFlag) {
		if (mState != -1) {
			Log.d("ces", "ggggg");
			Message msg = new Message();
			Bundle b = new Bundle();// 存放数据
			b.putInt("cmd", CMD_RECORDFAIL);
			b.putInt("msg", ErrorCode.E_STATE_RECODING);
			msg.setData(b);

			uiHandler.sendMessage(msg); // 向Handler发送消息,更新UI
			return;
		}
		int mResult = -1;
		AudioRecordFunc mRecord_1 = AudioRecordFunc.getInstance();
		mResult = mRecord_1.startRecordAndFile();
		if (mResult == ErrorCode.SUCCESS) {
			Log.d("ces", "ssss");
			Message msg = new Message();
			Bundle b = new Bundle();// 存放数据
			b.putInt("cmd", CMD_RECORDING_TIME);
			msg.setData(b);
			uiHandler.sendMessage(msg); // 向Handler发送消息,更新UI
			mState = mFlag;
		} else {
			Log.d("ces", "hhhhh");
			Message msg = new Message();
			Bundle b = new Bundle();// 存放数据
			b.putInt("cmd", CMD_RECORDFAIL);
			b.putInt("msg", mResult);
			msg.setData(b);
			uiHandler.sendMessage(msg); // 向Handler发送消息,更新UI
		}
	}

	/**
	 * 停止录音
	 */
	private void stop() {
		if (mState != -1) {
			AudioRecordFunc mRecord_1 = AudioRecordFunc.getInstance();
			mRecord_1.stopRecordAndFile();
			Message msg = new Message();
			Bundle b = new Bundle();// 存放数据
			b.putInt("cmd", CMD_STOP);
			b.putInt("msg", mState);
			msg.setData(b);
			uiHandler.sendMessage(msg); // 向Handler发送消息,更新UI
			mState = -1;
		}
	}

	/**
	 * 播放录音
	 */
	private void play() {
		if (mState != -1) {

			Message msg = new Message();
			Bundle b = new Bundle();// 存放数据
			b.putInt("cmd", CMD_PLAYFAIL);
			b.putInt("msg", mState);
			msg.setData(b);
			uiHandler.sendMessage(msg); // 向Handler发送消息,更新UI
			mState = -1;
		} else {
			if (AudioFileFunc.getWavFilePath() != "") {
				try {
					PlayAudioTrack.PlayAudioTrack(AudioFileFunc
							.getWavFilePath());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			} else {
				Log.d("play", "找不到录音文件！！！");
			}
		}
	}

	// 设置上传弹框
	public void showUpdateDialog() {

		UpdateDialog.Builder builder = new UpdateDialog.Builder(this);
		builder.create().show();

	}

	// 设置结果弹框
	public void showResultDialog() {

		ResultDialog.Builder builder = new ResultDialog.Builder(this);
		builder.create().show();

	}

	// 与服务器连接获得结果
	public void getResult() {
		UpdateDialog.Builder builder = new UpdateDialog.Builder(this);
		final Dialog updateDialog = builder.create();
		final ResultDialog.Builder builder2 = new ResultDialog.Builder(this);
		updateDialog.show();
		new Handler().postDelayed(new Runnable() {
			public void run() {
				// execute the task
				updateDialog.dismiss();
				Dialog resultDialog = builder2.create();
				resultDialog.show();
			}
		}, 3000);

	}
	//get intent
    public void getmIntent(){
    	Bundle bundle1 = getIntent().getExtras();
    	courseid = bundle1.getString("courseid");
    	sentenceid = bundle1.getString("sentenceid");
		Log.d("bundle","courseid: "+courseid); 	
		Log.d("bundle","sentenceid: "+sentenceid); 	
    }
    /*
	 * 向服务器请求数据
	 */
	private void requestDataFromServer(){
					Map<String,String> map = new HashMap<String,String>();
			        map.put("courseid", courseid);
			        map.put("sentenceid", sentenceid);
			       VolleyManager.newInstance().GsonPostRequest(TAG, map, Urls.SHOWSENTENCE, JShowSentence.class, new Response.Listener<JShowSentence>() {
			    	   
			           @Override
			           public void onResponse(JShowSentence sentence) {
			          //    Log.d("111111111111111111111", "ok" +  jmovie.getData().getMovie().get(0).getMovie_name());
			        	//   Log.d("111111111111111111111", "ok" +  jmovie.getData().getMovie().get(0).getCover_addr());   
			        	   int length = 0;
			        	   if(sentence.getData().getStatus()!=0 && sentence.getData().getSentence()!=null){
			        		   Message msg = new Message();
			       			Bundle b = new Bundle();// 存放数据
			       			b.putInt("cmd", CMD_SERVER);
			       			b.putString("content", sentence.getData().getSentence().get(0).getContent());
			       			b.putString("sentenceSrc", Urls.ROOT+sentence.getData().getSentence().get(0).getSen_addr());
			       			msg.setData(b);
			       			uiHandler.sendMessage(msg); // 向Handler发送消息,更新UI
			        	   
			        
			        	   Log.d("success", "ok" +  sentence.getData().getSentence().get(0).getContent());
			        	   }
			           }
			       }, new Response.ErrorListener() {
			           @Override
			           public void onErrorResponse(VolleyError error) {
			               Log.d("fail", "connect fail");
			      

			           }
			       });
			        Log.d(TAG, "finish");
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (player != null) {
			player.stop();
			player = null;
		}
		mTimer.cancel();
	}
}
