package com.wu.service;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.content.Context;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.SynthesizerListener;
import com.iflytek.speech.setting.TtsSettings;
import com.iflytek.speech.util.ApkInstaller;
import com.iflytek.sunflower.FlowerCollector;
import com.iflytek.voicedemo.R;
import com.iflytek.voicedemo.TtsDemo;

@SuppressWarnings("unused")
public class TtsService {
	private static String TAG = "TTs_service";
	// 语音合成对象
	private SpeechSynthesizer mTts;
	// 语记安装助手类
	private ApkInstaller mInstaller;
	// 缓冲进度
	private int mPercentForBuffering = 0;
	// 播放进度
	private int mPercentForPlaying = 0;
	// 云端/本地单选按钮
	private RadioGroup mRadioGroup;
	// 引擎类型
	private String mEngineType = SpeechConstant.TYPE_LOCAL;
	private String testString = "您好，我是奇客智能语音管家，语音合成初始化成功！";
	private Toast mToast;
	private SharedPreferences mSharedPreferences;
	private Context this_Context;

	/************* 构造函数- *****************/
	@SuppressWarnings("static-access")
	public TtsService(Context context, ApkInstaller apkInstaller) {
		this_Context = context;
		mEngineType = SpeechConstant.TYPE_LOCAL;
		mTts = SpeechSynthesizer.createSynthesizer(context, mTtsInitListener);
		mSharedPreferences = context.getSharedPreferences(
				TtsSettings.PREFER_NAME, this_Context.MODE_PRIVATE);
		mToast = Toast.makeText(context, "", Toast.LENGTH_SHORT);
		mInstaller = apkInstaller;
	}

	/************ 调用接口 ***************/
	public void tts_cfg() {
		if (!SpeechUtility.getUtility().checkServiceInstalled()) {
			showTip("没有检测到语记，请先按照语记哦！");
			mInstaller.install();
		} else {
			SpeechUtility.getUtility().openEngineSettings(null);
		}
	}

	public void tts_play(String stringToPlay) {
		setParam();
		int code = mTts.startSpeaking(stringToPlay, mTtsListener);
		// /**
		// * 只保存音频不进行播放接口,调用此接口请注释startSpeaking接口
		// * text:要合成的文本，uri:需要保存的音频全路径，listener:回调接口
		// */
		// String path = Environment.getExternalStorageDirectory()+"/tts.pcm";
		// int code = mTts.synthesizeToUri(text, path, mTtsListener);
		if (code != ErrorCode.SUCCESS) {
			if (code == ErrorCode.ERROR_COMPONENT_NOT_INSTALLED) {
				// 未安装则跳转到提示安装页面
				showTip("没有检测到语记，请先按照语记哦！");
				mInstaller.install();
			} else {
				showTip("语音合成失败,错误码: " + code);
			}
		}
	}

	public void tts_cancel() {
		mTts.stopSpeaking();
	}

	public void tts_pause() {
		mTts.pauseSpeaking();
	}

	public void tts_resume() {
		mTts.resumeSpeaking();
	}

	public void tts_person_select() {
		showPresonSelectDialog();
	}

	/**
	 * 发音人选择。
	 */
	private void showPresonSelectDialog() {
		if (!SpeechUtility.getUtility().checkServiceInstalled()) {
			showTip("没有检测到语记，请先按照语记哦！");
			mInstaller.install();
		} else {
			SpeechUtility.getUtility().openEngineSettings(
					SpeechConstant.ENG_TTS);
		}
	}

	/**
	 * 初始化监听。
	 */
	private InitListener mTtsInitListener = new InitListener() {
		@Override
		public void onInit(int code) {
			Log.d(TAG, "InitListener init() code = " + code);
			if (code != ErrorCode.SUCCESS) {
				showTip("初始化失败,错误码：" + code);
			} else {
				showTip("语音合成初始化成功");
				tts_play(testString);
				// 初始化成功，之后可以调用startSpeaking方法
				// 注：有的开发者在onCreate方法中创建完合成对象之后马上就调用startSpeaking进行合成，
				// 正确的做法是将onCreate中的startSpeaking调用移至这里
			}
		}
	};

	/**
	 * 合成回调监听。
	 */
	private SynthesizerListener mTtsListener = new SynthesizerListener() {

		@Override
		public void onSpeakBegin() {
			showTip("开始播放");
		}

		@Override
		public void onSpeakPaused() {
			showTip("暂停播放");
		}

		@Override
		public void onSpeakResumed() {
			showTip("继续播放");
		}

		@Override
		public void onBufferProgress(int percent, int beginPos, int endPos,
				String info) {
			// 合成进度
			mPercentForBuffering = percent;
			showTip(String.format(
					this_Context.getString(R.string.tts_toast_format),
					mPercentForBuffering, mPercentForPlaying));
		}

		@Override
		public void onSpeakProgress(int percent, int beginPos, int endPos) {
			// 播放进度
			mPercentForPlaying = percent;
			showTip(String.format(
					this_Context.getString(R.string.tts_toast_format),
					mPercentForBuffering, mPercentForPlaying));
		}

		@Override
		public void onCompleted(SpeechError error) {
			if (error == null) {
				showTip("播放完成");
			} else if (error != null) {
				showTip(error.getPlainDescription(true));
			}
		}

		@Override
		public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
			// 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
			// 若使用本地能力，会话id为null
			// if (SpeechEvent.EVENT_SESSION_ID == eventType) {
			// String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
			// Log.d(TAG, "session id =" + sid);
			// }
		}
	};

	private void showTip(final String str) {
		mToast.setText(str);
		mToast.show();
	}

	/**
	 * 参数设置
	 * 
	 * @param param
	 * @return
	 */
	private void setParam() {
		// 清空参数
		mTts.setParameter(SpeechConstant.PARAMS, null);
		// 根据合成引擎设置相应参数
		mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL);
		// 设置本地合成发音人 voicer为空，默认通过语记界面指定发音人。
		mTts.setParameter(SpeechConstant.VOICE_NAME, "");
		// 设置合成语速
		mTts.setParameter(SpeechConstant.SPEED,
				mSharedPreferences.getString("speed_preference", "50"));
		// 设置合成音调
		mTts.setParameter(SpeechConstant.PITCH,
				mSharedPreferences.getString("pitch_preference", "50"));
		// 设置合成音量
		mTts.setParameter(SpeechConstant.VOLUME,
				mSharedPreferences.getString("volume_preference", "50"));
		// 设置播放器音频流类型
		mTts.setParameter(SpeechConstant.STREAM_TYPE,
				mSharedPreferences.getString("stream_preference", "3"));
		// 设置播放合成音频打断音乐播放，默认为true
		mTts.setParameter(SpeechConstant.KEY_REQUEST_FOCUS, "true");
		// 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
		// 注：AUDIO_FORMAT参数语记需要更新版本才能生效
		mTts.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
		mTts.setParameter(SpeechConstant.TTS_AUDIO_PATH,
				Environment.getExternalStorageDirectory() + "/msc/tts.wav");
	}

}
