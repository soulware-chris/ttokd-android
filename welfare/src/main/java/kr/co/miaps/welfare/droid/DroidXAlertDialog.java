/*
 * Copyrightⓒ 2018 NSHC.
 * All Rights Reserved.
 *
 * Droid-X에서 AlertDialog를 PendingIntent를 통하여 띄우기 위하여 구현된 Activity 샘플
 *
 * 2018. 03. 08
 */

package kr.co.miaps.welfare.droid;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import net.nshc.droidx3.manager.apk.DroidXApkManager;


public class DroidXAlertDialog extends Activity{

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

		this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
				| WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
				| WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
				| WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

		String msg = getIntent().getStringExtra("msg");

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("경고");
		builder.setMessage(msg);
		builder.setPositiveButton("종료", (dialog, which) -> killProgram());
		builder.setCancelable(false);
		builder.show();
	}

	private void killProgram() {
		DroidXApkManager.getInstance().stopService();
		this.moveTaskToBack(true);
		this.finish();
		android.os.Process.killProcess(android.os.Process.myPid());
	}
}