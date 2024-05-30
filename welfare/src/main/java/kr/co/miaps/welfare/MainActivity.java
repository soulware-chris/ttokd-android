package kr.co.miaps.welfare;

import static io.ktor.http.URLUtilsKt.URLBuilder;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import android.os.Looper;
import android.util.Log;
import android.webkit.WebView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.ktnet.certrelay.android.global.CertCommUtil;
import com.ktnet.certrelay.android.global.PKIUtil;
import com.ktnet.certrelay.android.storage.StorageInstallHelper;
import com.minkcomm.CMinkLogMan;
import com.minkmidascore.comm.CMinkLog;

import net.nshc.droidx3.manager.apk.DroidXApkManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;
import io.snplab.myd.core.did.DidSignature;
import io.snplab.myd.core.did.DidStore;
import kr.co.miaps.miapslibaar.BaseWebFragment;
import kr.co.miaps.miapslibaar.HActivity;
import kr.co.miaps.miapslibaar.IMiAPSCallback;
import kr.co.miaps.miapslibaar.MWebView;
import kr.co.miaps.miapslibaar.MiAPS;
import kr.co.miaps.welfare.api.OneId;
import kr.co.miaps.welfare.api.StringContinuation;
import kr.co.miaps.welfare.pki.Option;
import kr.co.miaps.welfare.pki.PkiCert;
import kr.co.miaps.welfare.pki.PkiCheckCert;
import kr.co.miaps.welfare.droid.DroidXServiceListener;
import kr.co.miaps.welfare.nfilter.NFilterOpen;
import kr.co.miaps.welfare.pki.receive.PGet;
import tradesign.certificate.CertificateInfo;
import tradesign.certificate.media.IStorage;

@AndroidEntryPoint
public class MainActivity extends HActivity implements IMiAPSCallback {
    private DroidXApkManager mDroidXApkManager = null;
    private DroidXServiceListener mServiceListener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    MainActivity mContext;

    // NFilter 관련 변수
    NFilterOpen nFilterOpen;

    //Did generator
    @Inject
    DidStore didStore;

    ActivityResultLauncher<Intent> launcher;

    private String loginUrlForCookie = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setMiAPSCallback(this);
        super.onCreate(savedInstanceState);

        MiAPS.processIntent(this, getIntent()); // 앱실행시 인텐츠값 저장

        // 개발앱일 경우
//        if(BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true);
            CMinkLogMan.InitLog(new CMinkLog(this, 3));
//        }
//        else {
//            //운영앱인 경우
//            CMinkLogMan.InitLog(new CMinkLog(this, 0));
//        }

        mContext = this;
        // NFilter 보안키패드 초기화
        nFilterOpen = new NFilterOpen();
        nFilterOpen.initNFilter(mContext);

        initActivityLauncher();

        Log.d("MainActivity", "onCreate(), did: " + didStore.load());

        if(BuildConfig.FLAVOR.equals("local")) {
            Log.d("MainActivity", "Local Server Version, appName : " + mContext.getString(R.string.app_name));
        }
        
        startDroidX();
        mServiceListener = new DroidXServiceListener(null,null,null,null,null,this);
    }

    private void initActivityLauncher() {
        launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {

                if(result.getResultCode() == RESULT_OK) {
                    Log.d("MainActivity", "onActivityResult(), result OK");
                }
            }
        });
    }

    private void initPKI(){
        PKIUtil.initpki();
        StorageInstallHelper.installAllStorage(this);
        try {
            IStorage.loadCert("");
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 공인인증서 가져오기 관련 초기화
        CertCommUtil.setURL(Option.gBaseURL);
        CertCommUtil.setGroupID(Option.gGroupID);
    }

    private void startDroidX(){
        // Droid-X Manager 객체 획득
        if (DroidXApkManager.getInstance() == null) {
            // Droid-X 매니저 최초 실행시
            mDroidXApkManager = DroidXApkManager.getInstance(getApplicationContext());
            // Notification 표시 유무
            mDroidXApkManager.setNotificationUse(true);
            // Droid-X 동작 결과를 받을 CallbackListener 설정
            mDroidXApkManager.setDXCallbackListener(mServiceListener);
            // Log 표시 설정
            mDroidXApkManager.setLogView(true);
            // Intro 팝업 표시 설정
            mDroidXApkManager.setIntroView(false);
            // 업데이트 최대 시간 설정 (기본: 5000ms)
            mDroidXApkManager.setUpdateMaxTime(5000);
            // 기본 삭제창 이용 여부 설정
            mDroidXApkManager.setDefaultRemoveDialogMode(true);
        } else if (mDroidXApkManager == null) {
            // Activity가 다시 Create 되었을 경우
            mDroidXApkManager = DroidXApkManager.getInstance();
        }

        if(mDroidXApkManager.getInstalled()) {
            //해당 구간이 짧은 시간내에 과도한 반복 실행이 될 경우가 있다면 stopService() 와 startService() 사이에 2초 정도의 간격을 유지하는 것이 안전합니다
            if (mDroidXApkManager.chkProc()) {
                mDroidXApkManager.stopService();
            }

            // Droid-X 실행
            mDroidXApkManager.startService();
        } else {
            // 앱설치 화면 이동
            Toast.makeText(mContext, getString(R.string.dx_install_required), Toast.LENGTH_SHORT).show();
            mDroidXApkManager.setInstall(this);
        }
    }


    private void stopDroidX() {
        if(mDroidXApkManager != null) {
            // Droid-X 서비스 종료
            mDroidXApkManager.stopService();
            mDroidXApkManager = null;
        }
    }

    private void showWebView() {
        Intent intent = new Intent(MainActivity.this, ExtWebActivity.class);
        //Intent intent = new Intent(this, ExtWebActivity1.class);
        intent.putExtra("url", "https://google.com");
        intent.putExtra("post", "");
        //startActivity(intent);
        launcher.launch(intent);
    }


    @Override
    protected void onNewIntent(Intent intent) {

        // 앱실행시 인텐츠값 저장
        MiAPS.processIntent(this, intent);

        super.onNewIntent(intent);
    }

    @Override
    public String setValue(String key, String value) {
        String result = MiAPSProvider.putEx(getContext(), key, value);
        Log.d("MainActivity", "setValue(), key : " + key + " value : " + value + " result : " + result);

        return result;
        //return MiAPSProvider.putEx(getContext(), key, value);
    }

    @Override
    public String getValue(String key) {

        String result = MiAPSProvider.getEx(getContext(), key);
        Log.d("MainActivity", "getValue(), key : " + key + " result : " + result);
        return result;
        //return MiAPSProvider.getEx(getContext(), key);
    }

    @Override
    public void clearValue() {
        MiAPSProvider.clear(getContext());
    }

    @Override
    public boolean onPreLoaded() {
        return false;
    }

    @Override
    public void onLoaded() {
        Log.e("MainActivity", "onLoaded()");
    }

    @Override
    public void onErrorPage(int code, String message) {
        Log.e("MainActivity", "onErrorPage(), code : " + code + " message : " + message + " currentTag : " + getCurTag());
        if(code == 404) {
            String s = getCurTag();
            Fragment fragment = getSupportFragmentManager().findFragmentByTag(s);
            if (fragment != null) {
                ((BaseWebFragment) fragment).back();
            }
        }
    }


    @Override
    public int requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, new String[] {
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.POST_NOTIFICATIONS,
            }, REQUEST_ALL_PERMISSION);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
            }, REQUEST_ALL_PERMISSION);
        }
        return -1;
    }


    @Override
    public boolean overrideUrl(String url) {
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_ALL_PERMISSION:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {  // 권한 요청이 성공하였을 경우 실행됨.
                    ///////////////////////////////////////////////////
                    //Droid-X 모바일 백신
                    ///////////////////////////////////////////////////
                    // DroidX 초기화
                    startDroidX();
                    break;
                }

                break;
        }

    }

    @Override
    public String extlib(JSONObject obj, final String callbackfunc, final Handler h) {
        final JSONObject jsonObj = obj;

        Log.d("MainActivity", "extlib(), obj " + obj.toString() + " callbackfunc : " + callbackfunc);
        try {

            if (true == jsonObj.isNull("param")) {
                return "";
            }

            JSONObject _jsonObjParam = (JSONObject) jsonObj.get("param");
            String _name = _jsonObjParam.get(IMiAPSCallback.MOBILE_EXTLIB_NAME).toString();
            String _method = _jsonObjParam.get(IMiAPSCallback.MOBILE_EXTLIB_METHOD).toString();
            String _param = _jsonObjParam.get(IMiAPSCallback.MOBILE_EXTLIB_PARAM).toString();
            int code = MiAPS.CODE_SUCCESS;

            if(_name.equalsIgnoreCase("NFILTER")) {
                if (_method.equalsIgnoreCase("showKeypad")){
                    String _publickey = "";
                    String _maxlength = "64";
                    String _type = "char";
                    String _e2e = "";

                    try {
                        JSONObject _p = new JSONObject(_param);
                        _type = _p.getString("mode");
                        _e2e = _p.getString("e2e");
                        _maxlength = _p.getString("maxlength");
                        _publickey = _p.getString("publickey");
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }

                    nFilterOpen.init(jsonObj, callbackfunc, h);
                    nFilterOpen.showKeypad(_type, _e2e, _maxlength, _publickey);
                }

                // view형태의 키패드를 닫는 함수
                if (_method.equalsIgnoreCase("closeKeypad")) {
                    nFilterOpen.init(jsonObj, callbackfunc, h);
                    nFilterOpen.closeKeypad();
                    return "";
                }
                //키패드 유무
                if (_method.equalsIgnoreCase("isKeypad")) {
                    nFilterOpen.init(jsonObj, callbackfunc, h);
                    nFilterOpen.isKeypad();
                    return "";
                }
            }
            else if(_name.equalsIgnoreCase("PKICERT")){
                initPKI();
                if (_method.equalsIgnoreCase("getSignCertList")){
                    JSONArray _policyoid = null;
                    String _listType = "";
                    // 디바이스에 있는 인증서 리스트 조회
                    try {
                        JSONObject _p = new JSONObject(_param);
                        _policyoid = _p.getJSONArray("policyoid");
                        _listType = _p.getString("listType");
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (_policyoid.length() != 0) {
                        for(int i=0 ; i<_policyoid.length() ; i++){
                            IStorage.addPolicyConditions(_policyoid.getString(i));
                        }
                    }

                    PkiCert pkiCert = new PkiCert();
                    pkiCert.init(jsonObj, callbackfunc, h);
                    pkiCert.getSignCertList(_listType);

                }
                else if(_method.equalsIgnoreCase("getDetailSignCert")){
                    // 선택한 인증서 상세정보 조회
                    String _subjectDN = "";
                    CertificateInfo cert = null;

                    try {
                        JSONObject _p = new JSONObject(_param);
                        _subjectDN = _p.getString("subjectDN");
                    }catch (Exception e){
                        e.printStackTrace();
                    }

                    PkiCert pkiCert = new PkiCert();
                    pkiCert.init(jsonObj, callbackfunc, h);
                    pkiCert.getDetailSignCert(_subjectDN);


                }
                else if(_method.equalsIgnoreCase("getSignCertCode")){
                    // 인증서 코드 가져오기
                    PGet pGet = new PGet();
                    pGet.init(jsonObj, callbackfunc, h);
                    pGet.PkiGetInit();
                    pGet.getSignCode();

                    return "";
                }
                else if(_method.equalsIgnoreCase("getSignCert")){
                    // 인증서 가져오기
                    String _certCode = "";
                    String _certPassword = "";
                    String _publickey = "";

                    try {
                        JSONObject _p = new JSONObject(_param);
                        _certCode = _p.getString("certCode");
                        _certPassword = _p.getString("certPassword");
                        _publickey = _p.getString("publickey");
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    PGet pGet = new PGet();
                    pGet.init(jsonObj, callbackfunc, h);
                    pGet.PkiGetInit();


                    pGet.PKIgetCert(_certCode, _certPassword, _publickey);

                    return "";
                }
                else if(_method.equalsIgnoreCase("checkSignCert")){
                    PkiCheckCert pkiCheckCert = new PkiCheckCert();
                    pkiCheckCert.init(jsonObj, callbackfunc, h);

                    String _subjectDN = "";
                    String _certPassword = "";
                    String _publickey = "";

                    try {
                        JSONObject _p = new JSONObject(_param);
                        _subjectDN = _p.getString("subjectDN");
                        _certPassword = _p.getString("certPassword");
                        _publickey = _p.getString("publickey");
                    }catch (Exception e){
                        e.printStackTrace();
                    }

                    pkiCheckCert.checkSignCert(_subjectDN, _certPassword, _publickey);

                    return "";
                }
                else if(_method.equalsIgnoreCase("getBinaryCert")){
                    // 인증서 바이너리코드 넘기기(.der / .key)
                    String _subjectDN = "";

                    try{
                        JSONObject _p = new JSONObject(_param);
                        _subjectDN = _p.getString("subjectDN");
                    }catch (Exception e){
                        e.printStackTrace();
                    }

                    PkiCert pkiCert = new PkiCert();
                    pkiCert.init(jsonObj, callbackfunc, h);
                    pkiCert.getBinaryCert(_subjectDN);

                }
                else if(_method.equalsIgnoreCase("deleteCert")){
                    String _subjectDN = "";

                    try{
                        JSONObject _p = new JSONObject(_param);
                        _subjectDN = _p.getString("subjectDN");
                    }catch (Exception e){
                        e.printStackTrace();
                    }

                    PkiCert pkiCert = new PkiCert();
                    pkiCert.init(jsonObj, callbackfunc, h);
                    pkiCert.deleteCert(_subjectDN);
                }


            } else if(_name.equalsIgnoreCase("addon")) {
                if (_method.equalsIgnoreCase("exit")) {
                    finish();
                    return "";
                }
            } else if(_name.equalsIgnoreCase("ked")) {
                //JSONObject _p = new JSONObject(_param);
                if (_method.equalsIgnoreCase("ked_show")) {

                    String _url = "";
                    String _post = "";
                    boolean _show = false;
                    boolean _closeAfterLoading = false;
                    try {
                        JSONObject _p = new JSONObject(_param);
                        _url = _p.getString("url");
                        _post = _p.getString("post");
                        _show = _p.getBoolean("show");
                        _closeAfterLoading = _p.getBoolean("closeAfterLoading");
                    }catch (Exception e){
                        e.printStackTrace();
                    }

                    if(_closeAfterLoading) {
                        boolean saveCookies = _url.contains("login.do?");
                        if(saveCookies) {
                            loginUrlForCookie = _url;
                        }
                        JSONObject resJson = new JSONObject();
                        try {
                            String response = fetchGet(_url, saveCookies);
                            if(response != null) {
                                resJson = new JSONObject(response);
                            }
                            Log.d("MainActivity", "extLib ked_show() " + resJson);
                            MWebView.callReturn(obj, 200, resJson, h);
                        } catch (Exception e) {
                            Log.e("MainActivity", "extLib, ked_show() " + Log.getStackTraceString(e));
                            MWebView.callReturn(obj, 0, resJson, h);
                        }
                    } else {
                        if (_show) {
                            Intent intent = new Intent(this, ExtWebActivity.class);
                            intent.putExtra("url", _url);
                            intent.putExtra("post", _post);
                            try {
                                boolean setCookies = URLBuilder(_url).build().getHost().equals(URLBuilder(loginUrlForCookie).build().getHost());
                                intent.putExtra("sessionUrl", loginUrlForCookie);
                                intent.putExtra("setCookies", setCookies);
                            } catch(Exception e) {
                                Log.e("MainActivity", "extLib, ked_show() " + Log.getStackTraceString(e));
                            }

                        //intent.putExtra("closeAfterLoading", _closeAfterLoading);
                            launcher.launch(intent);
                        }
                    }

                    return "";
                }
            }
            //Did
            else if(_name.equalsIgnoreCase("snplab")) {

                if (_method.equalsIgnoreCase("getDid")) {
                    JSONObject resJson = new JSONObject();

                    try {
                        DidSignature didSignature = didStore.load();
                        resJson.put("did", didSignature.getDid());
                        resJson.put("verKey", didSignature.getVerKey());

                        Log.d("MainActivity", "extLib getDid() " + didSignature.getDid());
                    } catch (Exception e) {
                        Log.e("MainActivity", "extLib, getDid() " + Log.getStackTraceString(e));
                    } finally {
                        MWebView.callReturn(obj, 200, resJson, h);
                    }

                }
            }

            return "";
        } catch (JSONException e) {
            e.printStackTrace();
            return "";
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopDroidX();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }


    // 화면 재입전 Droid-X의 서비스가 강제 종료 되었을 경우 서비스 재시작을 위한 부분
    @Override
    protected void onRestart() {
        super.onRestart();
        // 일정 간격 후 Droid-X 서비스 실행 요청 (즉각 실행시 타이밍 이슈로 서비스 시작 실패 가능성이 있습니다.)
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mDroidXApkManager != null && !mDroidXApkManager.chkProc()) {
                    startDroidX();
                }
            }
        }, 100);
    }

    private String fetchGet(String url, Boolean saveCookies) throws ExecutionException, InterruptedException {
        CompletableFuture<String> suspendResult = new CompletableFuture<>();
        StringContinuation continuation = new StringContinuation(suspendResult);
        OneId oneId = new OneId(saveCookies);
        // call the Kotlin suspend function using the Kotlin coroutines API
        oneId.fetchGet(url, null, continuation);
        return (String)suspendResult.get();
    }
}
