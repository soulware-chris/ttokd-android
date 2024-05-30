package kr.co.miaps.welfare.droid;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;


import net.nshc.droidx3.engine.ScanResult;
import net.nshc.droidx3.manager.DroidXCallbackListenerV2;
import net.nshc.droidx3.manager.apk.DroidXApkManager;


import java.util.HashMap;
import java.util.Map;

import kr.co.miaps.welfare.R;


// Droid-X 의 결과를 받을 CallbackListener Class
public class DroidXServiceListener implements DroidXCallbackListenerV2 {
    private final Context mContext;
    private DroidXServiceHandler handler;
    private ProgressBar mProgressBar = null;
    private TextView mTextView = null;
    private ImageView mImageViewUpdate = null;
    private ImageView mImageViewSystem = null;
    private ImageView mImageViewMalware = null;
    private Activity mActivity;

    private Message msgobject;

    /**
     * DroidX서비스의 흐름을 도와주는 리스너
     * @param pbar 진행률을 표시할 진행바의 객체
     * @param tv 진행률을 표시할 숫자 TextView의 객체
     * @param iUp Update가 되었다는 체크표시를 할 아이콘 이미지
     * @param iSys 루팅체크가 되었다는 체크표시를 할 아이콘 이미지
     * @param iMal 바이러스 체크가 되었다는 체크표시를 할 아이콘 이미지
     * @param act 진행이 끝난 뒤 다음화면으로 진행하기 위해 현재 Activity를 필요로함
     */
    public DroidXServiceListener(ProgressBar pbar, TextView tv, ImageView iUp, ImageView iSys, ImageView iMal, Activity act) {
        mProgressBar = pbar;
        mTextView = tv;
        mImageViewUpdate = iUp;
        mImageViewSystem = iSys;
        mImageViewMalware = iMal;
        mActivity = act;
        mContext = mActivity.getApplicationContext();
        handler = new DroidXServiceHandler();
    }

    /**
     * DroidX 서비스 시작으로 수행되는 백신 초기화 작업  제일 처음 호출되는 callbacklistener Method
     * @param resultCode 초기화 후 결과값
     * 		    0: 초기화 성공
     *		    1: 초기화 실패
     *		    100: so파일 무결성 검증 실패, 이후 백신의 올바른 실행이 보장되지 않으므로 종료 필요
     *		    101: 기존에 설치된 패턴/엔진이 임의로 변조 되었음
     *		    102: 최초 엔진 설치시에 문제가 발생하였음. 이후 정상 진행되지 않을 수 있으므로 종료 필요
     */
    @Override
    public void callbackInit(int resultCode) {
        Log.e("NSHC_Listener", "callbackInit: " + resultCode + ",Build.VERSION.SDK_INT:"+Build.VERSION.SDK_INT);
        if (resultCode >= 100 && resultCode < 0) {
            //FIXME "Droid-X의 엔진 무결성 실패로 Droid-X 의 재시작 또는 재설치가 필요합니다.
            android.os.Process.killProcess(android.os.Process.myPid());
            return;
        }

        // !!!!! setDefaultRemoveDialogMode(true); 로 설정값 사용시 아래 분기 처리 필수 !!!!!
        if (Build.VERSION.SDK_INT < 33) {
            DroidXApkManager.getInstance().runUpdate();
        } else {
            // 노티피케이션 권한 획득 요청 activity 발생
            DroidXApkManager.getInstance().getGrantNotificationPermission();
        }
    }

    /**
     * DroidX 업데이트 결과 리턴
     * @param resultCode 업데이트 결과값
     *		    0: 정상
     *		    1: Offline으로 실패
     *		    2: 정책파일 파싱 실패
     *		    3: 복호화 실패
     *		    4: 파일 없음
     *		    5: 입출력 실패
     *		    6: 전자서명 실패
     *		    7: 엔진 업데이트 실패
     *		    8: 최대시간 초과
     */
    @Override
    public void callbackUpdate(int resultCode) {
        Log.e("NSHC_Listener", "callbackUpdate: " + resultCode);
        DroidXApkManager.getInstance().runRootingCheck();

        msgobject = DroidXServiceHandler.generateMessage(mImageViewUpdate, DroidXServiceHandler.VIEW_IMAGEVIEW, R.drawable.check);
        handler.sendMessage(msgobject);
    }

    /**
     * @param resultCode
     ***** runRootingCheck() 동작 완료시 실행
     *        -1: 알려지지 않은 모든 에러
     *        -2: 패턴의 무결성 검증 실패
     *        0: 정상
     *        1: su 파일 탐지
     *        2: 루팅 관련 애플리케이션 탐지
     *        3: 기타 루팅 방법 탐지
     *
     ***** runRootingScan() 동작 완료시 실행
     *       resultCode & Const.FLAG_ROOTING_SCAN_DETECTED
     *          0: 정상
     *          Const.FLAG_ROOTING_SCAN_DETECTED: 루팅 관련 파일 탐지
     */
    @Override
    public void callbackRoot(int resultCode) {
        Log.e("NSHC_Listener", "callbackRoot(int): " + resultCode);
        if (resultCode < 0) {
            //FIXME
            Log.e("NSHC_Listener", "에러 발생으로 검사 실패, 필요시 재검사");
        }

        if (resultCode > 0) {
            Log.e("NSHC_Listener", "루팅탐지");
            android.os.Process.killProcess(android.os.Process.myPid());
            return;
        }

        DroidXApkManager.getInstance().runMalwareScan();
        // 내부 저장소의 파일을 포함하여 검사를 원할 때 runMalwareScan() 대신 사용.
        // 권한 관련 UI가 발생하므로 확인 후 적용 필요
        //DroidXApkManager.getInstance().runSDCardScan();
        msgobject = DroidXServiceHandler.generateMessage(mImageViewSystem, DroidXServiceHandler.VIEW_IMAGEVIEW, R.drawable.check);
        handler.sendMessage(msgobject);
    }

    /**
     *  사용하지 않음
     */
    @Override
    public void callbackEngineVersion(String[] localVersion, String[] serverVersion) {
        // 미사용
    }

    /**
     * getGrantNotificationPermission() 호출 결과값 전달
     * @param resultCode
     *        0 : Notification 알람 표시 권한 있음 or 획득 완료
     *        1 : Notification 알람 표시 권한 없음, 권한 획득이 필요한 상황
     */
    @Override
    public void callbackNotificationPermissoinStatus(int resultCode) {
        Log.e("NSHC_Listener", "callbackNotificationPermissoinStatus(int): " + resultCode);
        if (resultCode == 1) {
            if (Build.VERSION.SDK_INT >= 33) {
                DroidXApkManager.getInstance().getGrantNotificationPermission();
            }
        } else {
            DroidXApkManager.getInstance().runUpdate();
        }
    }

    /**
     * 악성코드 탐지 결과를 Object Map 형태로 받습니다. (기본 삭체창을 띄우지 않았을 경우)
     * @param type 스캔 유형
     *		    1: 스캔중 탐지 결과
     *		    2: 실시간 탐지 결과
     * @param iResult 총 스캔 파일 수, 검사 실패 이유 에러 코드
     *           > 0 : 총 스캔 파일 수
     *		    -1: 이외의 모든 이상 상황에 대한 에러코드
     *		    -2: 패턴의 검증 실패 에러코드
     * @param mapResult 스캔결과인 ScanResult를 담은 map객체
     *		    key : Filepath(String)
     *		    value : ScanResult(3.3 ScanResult Class 설명 참조)
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void callbackMalwareResult(int type, int iResult,  Map mapResult) {
        Log.d("NSHC_Listener","callbackMalwareResult: "+type + " | "+iResult + " | "+mapResult.size());

        if (iResult < 0) {
            //FIXME
            Log.e("NSHC_Listener", "에러 발생으로 검사 실패, 필요시 재검사");
        }

        if(mapResult.size() > 0 && iResult > 0) { // 악성코드 탐지 결과 하나 이상 발견되었을 경우
            Bundle mBundle = new Bundle();
            for(Map.Entry<String, ScanResult> sre : ((HashMap<String, ScanResult>)mapResult).entrySet()) {
                ScanResult sr = sre.getValue();
                mBundle.putSerializable(sre.getKey(), sr);

                Log.d("NSHC_Listener", "-callbackMalwareResult : " +sr.getTargetPath() + " | "+sr.getPackageName() + " | "+sr.getResultCode());
            }

            Intent mIntent = new Intent(mActivity, DroidXMalwareResultsActivity.class);
            mIntent.putExtra("DX_TotalSize", iResult);
            mIntent.putExtra("DX_Malwares", mBundle);
            mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            mActivity.startActivity(mIntent);

            msgobject = DroidXServiceHandler.generateMessage(mImageViewMalware, DroidXServiceHandler.VIEW_IMAGEVIEW, R.drawable.check);
            handler.sendMessage(msgobject);

            mActivity.finish();

        } else { // 악성코드 탐지 결과 하나도 없을 경우
            msgobject = DroidXServiceHandler.generateMessage(mActivity, DroidXServiceHandler.VIEW_NEXTACTIVITY, 0);
            handler.sendMessageDelayed(msgobject,400); // 400밀리초 후 진행
        }
    }

    @Override
    public void measureUpdateEngine(String bytesize) {
        // 미사용
    }

    /**
     * 검사 중 검사 진행 단계를 callback으로 전달
     * @param percent            현재 검사 진행울
     * @param currentFilepath    정보 전달시 검사 완료한 대상 app 또는 파일
     */
    @Override
    public void updateProgress(int percent, String currentFilepath) {
        Log.e("NSHC_Listener", "updateProgress: " + currentFilepath + " | "+percent);

        msgobject = DroidXServiceHandler.generateMessage(mTextView, DroidXServiceHandler.VIEW_TEXTVIEW, percent);
        handler.sendMessage(msgobject);

        msgobject = DroidXServiceHandler.generateMessage(mProgressBar, DroidXServiceHandler.VIEW_PROGRESS, percent);
        handler.sendMessage(msgobject);
    }

    /**
     * 기본창 사용 옵션으로 설정한 경우 악성코드 탐지 결과를 받음
     * @param resultCode 검사 결과 값과 검사 실패 이유 에러 코드
     *		    -1: 이외의 모든 이상상황에 대한 에러코드
     *		    -2: 패턴의 검증 실패 에러코드
     *		    0: 정상
     *		    1이상: 발견된 악성코드의 수
     */
    @Override
    public void callbackMalware(int resultCode) {
        Log.e("NSHC_Listener", "callbackMalware: " + resultCode);

        if (resultCode < 0) {
            //FIXME
            Log.e("NSHC_Listener", "에러 발생으로 검사 실패, 필요시 재검사");
        }

        msgobject = DroidXServiceHandler.generateMessage(mImageViewMalware, DroidXServiceHandler.VIEW_IMAGEVIEW, R.drawable.check);
        handler.sendMessage(msgobject);

        if (resultCode == 0) {
            msgobject = DroidXServiceHandler.generateMessage(mActivity, DroidXServiceHandler.VIEW_NEXTACTIVITY, 0);
            handler.sendMessageDelayed(msgobject, 400); // 400밀리초 후 진행
        } else if (resultCode > 0) {
            msgobject = DroidXServiceHandler.generateMessage(mActivity, DroidXServiceHandler.VIEW_ALERT, 0);
            handler.sendMessageDelayed(msgobject, 100); // 400밀리초 후 진행
        }
    }

    /**
     * 기본창 사용 옵션으로 설정한 경우 악성코드 실시간 탐지 결과를 받음
     * @param resultCode 검사 결과 값과 검사 실패 이유 에러 코드
     *		    -1: 이외의 모든 이상상황에 대한 에러코드
     *		    -2: 패턴의 검증 실패 에러코드
     *		    0: 정상
     *		    1이상: 발견된 악성코드의 수
     */
    @Override
    public void callbackRealTimeMalware(int resultCode) {
        Log.e("NSHC_Listener", "callbackRealTimeMalware: " + resultCode);
    }

    @Override
    public void callbackDetailMalware(int resultCode) {
        //Deprecated
    }
}