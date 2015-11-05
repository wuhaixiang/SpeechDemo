package com.iflytek.voicedemo;

import com.iflytek.speech.util.ApkInstaller;
import com.wu.service.AsrService;
import com.wu.service.TtsService;

import android.R.array;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener {

	private Toast mToast;
	private TtsService ttsService ;
	private AsrService asrService;
	// 语记安装助手类
	public ApkInstaller mInstaller ;
	@SuppressLint("ShowToast")
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);
		
		mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
		SimpleAdapter listitemAdapter = new SimpleAdapter();
		((ListView) findViewById(R.id.listview_main)).setAdapter(listitemAdapter);
		mInstaller = new  ApkInstaller(MainActivity.this);
		ttsService = new TtsService(MainActivity.this,mInstaller);
		asrService = new AsrService(MainActivity.this, mInstaller);
		
	}
	private void test_asr() {
		asrService.isr_grammar();
		asrService.isr_recognize();
	}
	@Override
	public void onClick(View view) {
		int tag = Integer.parseInt(view.getTag().toString());
		Intent intent = null;
		switch (tag) {
		case 0:
			this.test_asr();
			// 语法识别
		//	intent = new Intent(MainActivity.this, AsrDemo.class);
			break;
		case 1:
			ttsService.tts_cfg();
			// 语音合成
			//intent = new Intent(MainActivity.this, TtsDemo.class);
			break;
		default:
			showTip("在IsvDemo中哦，为了代码简洁，就不放在一起啦，^_^");
			break;
		}
		
		if (intent != null) {
			startActivity(intent);
		}
	}
		
	// Menu 列表
	String items[] = { "进入语音识别",  "进入语音合成",};

	private class SimpleAdapter extends BaseAdapter {
		public View getView(int position, View convertView, ViewGroup parent) {
			if (null == convertView) {
				LayoutInflater factory = LayoutInflater.from(MainActivity.this);
				View mView = factory.inflate(R.layout.list_items, null);
				convertView = mView;
			}
			
			Button btn = (Button) convertView.findViewById(R.id.btn);
			btn.setOnClickListener(MainActivity.this);
			btn.setTag(position);
			btn.setText(items[position]);
			
			return convertView;
		}
		
		@Override
		public int getCount() {
			return items.length;
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}
	}
	
	private void showTip(final String str) {
		mToast.setText(str);
		mToast.show();
	}
	
}
