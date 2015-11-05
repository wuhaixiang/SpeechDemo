package com.wu.service;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.GrammarListener;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.LexiconListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.speech.util.ApkInstaller;
import com.iflytek.speech.util.FucUtil;
import com.iflytek.speech.util.JsonParser;

public class AsrService {
	private static String TAG = "AsrService";
	// 语音识别对象
	private SpeechRecognizer mAsr;
	private Toast mToast;
	// 缓存
	//private SharedPreferences mSharedPreferences;
	// 本地语法文件
	private String mLocalGrammar = null;
	// 本地词典
	private String mLocalLexicon = null;
	// 云端语法文件
//	private String mCloudGrammar = null;
//	private static final String KEY_GRAMMAR_ABNF_ID = "grammar_abnf_id";
	//private static final String GRAMMAR_TYPE_ABNF = "abnf";
	private static final String GRAMMAR_TYPE_BNF = "bnf";
	private String mEngineType = SpeechConstant.TYPE_LOCAL;
	// 语记安装助手类
	private ApkInstaller mInstaller;
	private Context this_Context;
	
	/**************构造函数********************/
	@SuppressLint("ShowToast")
	public AsrService(Context context,ApkInstaller apkInstaller) {
		super();
		// TODO 自动生成的构造函数存根
		mEngineType = SpeechConstant.TYPE_LOCAL;
		this_Context=context;
		mInstaller=apkInstaller;
		mAsr = SpeechRecognizer.createRecognizer(this_Context, mInitListener);
		mLocalLexicon = "郝宁\n汪伟\n王伟\n";
		mLocalGrammar = FucUtil.readFile(this_Context,"call.bnf", "utf-8");
		//ContactManager mgr = ContactManager.createManager(this_Context, mContactListener);	
		//mgr.asyncQueryAllContactsName();
		//mSharedPreferences = getSharedPreferences(this_Context.getPackageName(),this_Context.MODE_PRIVATE);
		mToast = Toast.makeText(this_Context,"",Toast.LENGTH_SHORT);
		if (!SpeechUtility.getUtility().checkServiceInstalled()) {
			mInstaller.install();
		}
	}
	
	// 语法、词典临时变量
		String mContent;
		// 函数调用返回值
	    int ret = 0;
	    /******************构建语法*********************/
	    public int isr_grammar() {
	    	int result = 0;
	    //	((EditText)findViewById(R.id.isr_text)).setText(mLocalGrammar); //设置输出页面字符
			mContent = new String(mLocalGrammar);
			mAsr.setParameter(SpeechConstant.TEXT_ENCODING,"utf-8");
			//指定引擎类型
			mAsr.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);
			ret = mAsr.buildGrammar(GRAMMAR_TYPE_BNF, mContent, mLocalGrammarListener);
			if(ret != ErrorCode.SUCCESS){
				if(ret == ErrorCode.ERROR_COMPONENT_NOT_INSTALLED){
					//未安装则跳转到提示安装页面
					mInstaller.install();
				}else {
					showTip("语法构建失败,错误码：" + ret);
					result =-1;
				}
		}
			return result;
	    }
	   /*************更新本地词典*******************/
	    public int isr_lexcion() {
			int result=0;
		//((EditText)findViewById(R.id.isr_text)).setText(mLocalLexicon);
		mContent = new String(mLocalLexicon);
		//指定引擎类型
		mAsr.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL);
		mAsr.setParameter(SpeechConstant.GRAMMAR_LIST, "call");
		ret = mAsr.updateLexicon("<contact>", mContent, mLexiconListener);
		if(ret != ErrorCode.SUCCESS){
			if(ret == ErrorCode.ERROR_COMPONENT_NOT_INSTALLED){
				//未安装则跳转到提示安装页面
				mInstaller.install();
			}else {
				showTip("更新词典失败,错误码：" + ret);
				result=-1;
			}
			
		}
		return result;
	    }
	    /*********************开始识别**************************/
	    public int isr_recognize() {
			
		int result=0;
	  //  ((EditText)findViewById(R.id.isr_text)).setText(null);// 清空显示内容
		// 设置参数
		if (!setParam()) {
			showTip("请先构建语法。");
			result =-1;
			return  result;
		};
		
		ret = mAsr.startListening(mRecognizerListener);
		if (ret != ErrorCode.SUCCESS) {
			if(ret == ErrorCode.ERROR_COMPONENT_NOT_INSTALLED){
				//未安装则跳转到提示安装页面
				mInstaller.install();
			}else {
				showTip("识别失败,错误码: " + ret);	
				result =-1;
			}
			
		}
		return  result;
	    }
	   public int isr_stop() {
		   int result=0;
		   mAsr.stopListening();
			showTip("停止识别");
			return result;
	}
	   public int isr_cancel() {
		   int result=0;
		   mAsr.cancel();
			showTip("停止识别");
			return result;
	}
	/**
     * 初始化监听器。
     */
    private InitListener mInitListener = new InitListener() {

		@Override
		public void onInit(int code) {
			Log.d(TAG, "SpeechRecognizer init() code = " + code);
			if (code != ErrorCode.SUCCESS) {
        		showTip("初始化失败,错误码："+code);
        	}
		}
    };
    	
	/**
     * 更新词典监听器。
     */
	private LexiconListener mLexiconListener = new LexiconListener() {
		@Override
		public void onLexiconUpdated(String lexiconId, SpeechError error) {
			if(error == null){
				showTip("词典更新成功");
			}else{
				showTip("词典更新失败,错误码："+error.getErrorCode());
			}
		}
	};
	
	/**
     * 本地构建语法监听器。
     */
	private GrammarListener mLocalGrammarListener = new GrammarListener() {
		@Override
		public void onBuildFinish(String grammarId, SpeechError error) {
			if(error == null){
				showTip("语法构建成功：" + grammarId);
			}else{
				showTip("语法构建失败,错误码：" + error.getErrorCode());
			}			
		}
	};
	
	/**
     * 识别监听器。
     */
    private RecognizerListener mRecognizerListener = new RecognizerListener() {
        
        @Override
        public void onVolumeChanged(int volume, byte[] data) {
        	showTip("当前正在说话，音量大小：" + volume);
        	Log.d(TAG, "返回音频数据："+data.length);
        }
        
        @Override
        public void onResult(final RecognizerResult result, boolean isLast) {
        	if (null != result) {
        		Log.d(TAG, "recognizer result：" + result.getResultString());
        		String text ;
        		if("cloud".equalsIgnoreCase(mEngineType)){
        			text = JsonParser.parseGrammarResult(result.getResultString());
        		}else {
        			text = JsonParser.parseLocalGrammarResult(result.getResultString());
        		}
        		Log.d(TAG, "recognizer result : "+text);
        		// 显示
        		//((EditText)findViewById(R.id.isr_text)).setText(text);                
        	} else {
        		Log.d(TAG, "recognizer result : null");
        	}	
        }
        
        @Override
        public void onEndOfSpeech() {
        	// 此回调表示：检测到了语音的尾端点，已经进入识别过程，不再接受语音输入
        	showTip("结束说话");
        }
        
        @Override
        public void onBeginOfSpeech() {
        	// 此回调表示：sdk内部录音机已经准备好了，用户可以开始语音输入
        	showTip("开始说话");
        }

		@Override
		public void onError(SpeechError error) {
			showTip("onError Code："	+ error.getErrorCode());
		}

		@Override
		public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
			// 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
			// 若使用本地能力，会话id为null
			//	if (SpeechEvent.EVENT_SESSION_ID == eventType) {
			//		String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
			//		Log.d(TAG, "session id =" + sid);
			//	}
		}

    };
    
	

	private void showTip(final String str) {
		mToast.setText(str);
		mToast.show();
	}

	/**
	 * 参数设置
	 * @param param
	 * @return 
	 */
	public boolean setParam(){
		boolean result = false;
		//设置识别引擎
		mAsr.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);
		//设置返回结果为json格式
		mAsr.setParameter(SpeechConstant.RESULT_TYPE, "json");
			//设置本地识别使用语法id
			mAsr.setParameter(SpeechConstant.LOCAL_GRAMMAR, "call");
			//设置本地识别的门限值
			mAsr.setParameter(SpeechConstant.ASR_THRESHOLD, "30");
			result = true;
		// 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
		// 注：AUDIO_FORMAT参数语记需要更新版本才能生效
		mAsr.setParameter(SpeechConstant.AUDIO_FORMAT,"wav");
		mAsr.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory()+"/msc/asr.wav");
		return result;
	}
}
