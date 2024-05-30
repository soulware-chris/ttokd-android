/*
 * Copyrightⓒ 2018 NSHC.
 * All Rights Reserved.
 *
 * Droid-X에서 검출된 악성코드 List에서 삭제를 수행하기 위하여 표현하는 Dialog Acitivity 샘플
 *
 * 2018. 03. 08
 */

package kr.co.miaps.welfare.droid.list;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import net.nshc.droidx3.engine.ScanResult;

import java.io.File;
import java.util.ArrayList;

import kr.co.miaps.welfare.R;
import kr.co.miaps.welfare.droid.DroidXServiceHandler;


public class DroidXServiceListDel extends Activity implements OnClickListener {
	private TextView tvScanResult = null;
	private ListView lvMalwareList = null;

	private ArrayList<String> mKeyList = null;
	private ArrayList<String> mVList = null;
	private ArrayList<ScanResult> mSCList = null;
	private ArrayList<String> applist = null;
	
	public static final int DX_REQUEST_CODE = 0x01;
	private int uninstallAppsCurrentCount = 0;
	private int uninstallAppsTotalCount = 0;
	
	private DroidXServiceListAdapter arradapter = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_malware_list);
		
		Intent mIntent = getIntent();
		
		setUI();
		setData(mIntent);
		
		arradapter = new DroidXServiceListAdapter(this, R.layout.activity_malware_row_list, mVList);
		lvMalwareList.setAdapter(arradapter);
		lvMalwareList.setFastScrollEnabled(true);
	}
	
	private void setUI() {
		tvScanResult = findViewById(R.id.tv_scanresult_bar_summary);
		CheckBox allCheckBox = findViewById(R.id.cb_scanresult_bar_all_check);
		allCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
				if(arradapter != null) {
					arradapter.setAllChecked(isChecked);
					arradapter.notifyDataSetChanged();
			}
		});
		lvMalwareList = findViewById(R.id.lview_detail_scanresult_vlist);
		Button btnCancel = findViewById(R.id.btn_scanresult_cancel);
		Button btnDel = findViewById(R.id.btn_scanresult_remove_file);
		btnCancel.setOnClickListener(this);
		btnDel.setOnClickListener(this);
	}

	private void setData(Intent mIntent) {
		Bundle mBundle = mIntent.getBundleExtra("DX_Malwares");
		if(mKeyList == null) mKeyList = new ArrayList<>();
		if(mVList == null) mVList = new ArrayList<>();
		if(mSCList == null) mSCList = new ArrayList<>();
		
		int count = 1;
		for(String key : mBundle.keySet()) {
			ScanResult sr = (ScanResult) mBundle.get(key);
			String line = "["+Integer.toString(count++)+"]\n";
			if(sr != null)
				line += "Package: " + sr.getPackageName() + "\nDesc: " + sr.getDescription() + "\nPath: " + sr.getTargetPath();
			else
				line += "Package: NO\nDesc: NO\nPath: NO";
			
			mKeyList.add(key);
			mSCList.add(sr);
			mVList.add(line);
		}

		int iTotalSize = mIntent.getIntExtra("DX_TotalSize", 0);
		tvScanResult.setText(Html.fromHtml(getString(R.string.txt_scanresult_scan_number)+"<b>"+iTotalSize+"</b>&nbsp;&nbsp;"+getString(R.string.txt_scanresult_detected_number)+"<font color='red'><b>"+mVList.size()+"</b></font>"));
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		// 취소를 선택하였을 때
		case R.id.btn_scanresult_cancel:
			malwareCheck();
			break;
		// 삭제를 선택하였을 때
		case R.id.btn_scanresult_remove_file:
			applist = new ArrayList<>();
			ArrayList<String> filelist = new ArrayList<>();
			for(int i=0; i<arradapter.getCount(); i++) {
				if(arradapter.getChecked(i)) {
					
					if(mKeyList.get(i).startsWith("/data/app/")) { // 설치된 앱일 경우
						applist.add(mSCList.get(i).getPackageName());
					} else { // 기타 나머지의 경우
						filelist.add(mKeyList.get(i));
					}
				}
			}
			
			if(filelist.size() + applist.size() < 1 ) {
				Toast.makeText(this, R.string.msg_scanresult_need_to_select, Toast.LENGTH_SHORT).show();
				break;
			}
			
			if(filelist.size() > 0) {
				if(!removeFiles(filelist)) {
					Toast.makeText(this, R.string.msg_scanresult_delete_wrong, Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(this, R.string.msg_scanresult_complete_del, Toast.LENGTH_SHORT).show();
					if(applist.size() == 0) {
						malwareCheck();
					}
				}
			}
			if(applist.size() > 0) {
				removeApps(applist);
			}
			
			break;
		default:
			break;
		}
	}
	
	public boolean removeFiles(ArrayList<String> arrlistFilepath) {
		for(String sFilepath : arrlistFilepath) {
			File mFile = new File(sFilepath);
			if(mFile.exists()) {
				if(!mFile.delete()) return false;
			}
		}
		return true;
	}
	
	public void removeApps(ArrayList<String> arrlistPackagename) {
		uninstallAppsTotalCount = arrlistPackagename.size();
		uninstallAppsCurrentCount = 0;
		
		removeApp();
	}
	
	private void removeApp() {
		Intent mIntent = new Intent(Intent.ACTION_DELETE);
		Uri mUri = Uri.fromParts("package", applist.get(uninstallAppsCurrentCount), null);
		mIntent.setData(mUri);
		startActivityForResult(mIntent, DX_REQUEST_CODE);
		uninstallAppsCurrentCount++;
	}
	
	// 삭제 후 결과를 받는 함수
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == DX_REQUEST_CODE) {
			if(uninstallAppsCurrentCount < uninstallAppsTotalCount) {	
				removeApp();
			} else {
				malwareCheck();
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	@Override
	public void onBackPressed() {}
	
	public void malwareCheck() {
		int iMalwareCount = 0;
		
		for(int i=0; i<arradapter.getCount(); i++) {
			if(mKeyList.get(i).startsWith("/data/app/")) { // 설치된 앱일 경우
				try {
					getApplicationContext().getPackageManager().getPackageInfo(mSCList.get(i).getPackageName(), 0);
					iMalwareCount++;
				} catch(NameNotFoundException e) {
					Log.d("DroidX_Sample", e.getMessage());
				}
			} else { // 기타 나머지의 경우
				File mFile = new File(mSCList.get(i).getTargetPath());
				if(mFile.exists()) {
					iMalwareCount++;
				}
			}
		}

		DroidXServiceHandler handler = new DroidXServiceHandler();
		handler.sendMessage(handler.obtainMessage(0, 0, 0, this)); // 진행
		
		String msg = getString(R.string.msg_scanresult_remain_malware).replace("%@1", Integer.toString(iMalwareCount));
		
		Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
		
		finish();
	}
}
