package kr.co.miaps.welfare.nfilter;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.View;

import com.minkcomm.CMinkLogMan;
import com.nshc.nfilter.NFilter;
import com.nshc.nfilter.command.view.NFilterOnClickListener;
import com.nshc.nfilter.command.view.NFilterTO;
import com.nshc.nfilter.util.NFilterUtils;

import org.json.JSONException;
import org.json.JSONObject;

import kr.co.miaps.bridge.MiapsBridge;

public class NFilterOpen {
    NFilter nfilter;
    String sNfilter_enctype = "def";
    int nfilter_maxlength = 0;


    String aesencdata = "";

    String encdata_char = "";
    int plndatalength_char = 0;
    String dummydata_char = "";
    String plainNormalData_char = "";
    String plaindata_char = "";

    private Handler mHandlerMiAPS;
    private JSONObject mJsonMiAPS;
    private String mCallbackFunc;
    public void init(JSONObject obj, final String callbackfunc, Handler h){
        mJsonMiAPS= obj;
        mCallbackFunc = callbackfunc;
        mHandlerMiAPS = h;
    }

    public void initNFilter(Context mContext) {
        nfilter = new NFilter(mContext);
        nfilter.setNoPadding(true);
        nfilter.setPlainDataEnable(true);
        nfilter.setMasking(NFilter.MASKING_ALL);
        nfilter.setPlainDataEnable(true);  //클라이언트에서 복호화 할수 있는 암호문 데이터 반환 true 설정.
    }

    public void showKeypad(String _type, String _e2e, String _maxlength, String _publickey ){
        encdata_char = "";
        plndatalength_char = 0;
        dummydata_char = "";
        plainNormalData_char = "";
        plaindata_char = "";

        //공개키를 받지 못한 경우
        if (_publickey.equalsIgnoreCase("")) {
            nfilter.setPublicKey("MDIwGhMABBYDBeMqMlebEzcxfXjbhvS73Ff+aNCtBBRvNh0rzMSq8OKxJoh15wDPqNZTNw==");
        } else {
            nfilter.setPublicKey(_publickey);
        }
        CMinkLogMan.WriteT("nFilter 공개키:" + _publickey);


        String _enctype = "";
        if (_e2e.equalsIgnoreCase("true")) {
            _enctype = "enc";
        } else {
            _enctype = "def";
        }

        sNfilter_enctype = _enctype;


        nfilter.setMaxLength(Integer.parseInt(_maxlength));
        // max 길이를 저장하고 있다가 값이 입력될때 같은 길이면 키보드를 자동으로 종료시키자.
        nfilter_maxlength = Integer.parseInt(_maxlength);

        if (_type.equalsIgnoreCase("char")) {
            if (nfilter.isNFilterViewVisibility() == View.VISIBLE)
                nfilter.nFilterClose(View.GONE);
            nfilter.setFieldName("nFilterEditText");   //EditText
            nfilter.setCharKeyPadButtonHeight(55);
            //입력값을 listener로 받는다.
            nfilter.setOnClickListener(new NFilterOnClickListener() {
                @Override
                public void onNFilterClick(NFilterTO nFilterTO) {
                    nFilterResult(nFilterTO);
                }
            });
            //nFilter 실행 메서드 모든 옵션 설정이 끝나면 호출해준다.
            nfilter.onViewNFilter(NFilter.KEYPADCHAR);
        } else if (_type.equalsIgnoreCase("num")) {
            if (nfilter.isNFilterViewVisibility() == View.VISIBLE)
                nfilter.nFilterClose(View.GONE);
            nfilter.setFieldName("nFilterEditText");
            //입력값을 listener로 받는다.
            nfilter.setOnClickListener(new NFilterOnClickListener() {
                @Override
                public void onNFilterClick(NFilterTO nFilterTO) {
                    nFilterResult(nFilterTO);
                }
            });
            //nFilter 실행 메서드 모든 옵션 설정이 끝나면 호출해준다.
            nfilter.onViewNFilter(NFilter.KEYPADSERIALNUM);
        }
    }


    public void closeKeypad(){
        Message m = mHandlerMiAPS.obtainMessage();

        try {
            nfilter.nFilterClose(View.GONE);
        } catch (Exception e) {}

        try {
            JSONObject j = new JSONObject();
            j.put("cmd", "close");
            mJsonMiAPS.put("code", 200);
            mJsonMiAPS.put("res", j);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String s = "javascript:" + mCallbackFunc + "(" + mJsonMiAPS.toString() + ");";

        m.what = MiapsBridge.MIAPS_CALL_RETURN;
        m.arg1 = 200;
        m.obj = s;
        mHandlerMiAPS.sendMessage(m);
    }

    public void isKeypad(){
        Message m = mHandlerMiAPS.obtainMessage();

        String isKeypad = "false";

        if (nfilter.isNFilterViewVisibility() == View.VISIBLE){
            isKeypad = "true";
        }
        try {
            mJsonMiAPS.put("code", 200);
            mJsonMiAPS.put("res", isKeypad);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String s = "javascript:" + mCallbackFunc + "(" + mJsonMiAPS.toString() + ");";

        m.what = MiapsBridge.MIAPS_CALL_RETURN;
        m.arg1 = 200;
        m.obj = s;
        mHandlerMiAPS.sendMessage(m);

    }

    /**
     * nFilter 리턴값 처리 메서드
     * sample 예제 이므로 상황에 맞게 변경하여 사용하시면됩니다.
     * @param nFilterTO
     */
    private void nFilterResult(NFilterTO nFilterTO) {

        String plainData = new String(NFilterUtils.getInstance().nSaferDecryptWithBase64(plaindata_char));
        System.out.println("plainData: " + plainData);

        if( nFilterTO.getFocus() == NFilter.NEXTFOCUS ){
            if( new String(  nFilterTO.getFieldName() ).equals("et1") ){
            }else if( new String(  nFilterTO.getFieldName() ).equals("et2") ){
            }

            nfilter.nFilterClose(View.GONE); //nFilter 닫기
        }else if( nFilterTO.getFocus() == NFilter.PREFOCUS ){
            if( new String(  nFilterTO.getFieldName() ).equals("et1") ){
            }else if( new String(  nFilterTO.getFieldName() ).equals("et2") ){
            }

            nfilter.nFilterClose(View.GONE);
        }else if( nFilterTO.getFocus() == NFilter.DONEFOCUS ){
            // Done 누른 경우
            if( new String(  nFilterTO.getFieldName() ).equals("nFilterEditText") ){
                Message m = mHandlerMiAPS.obtainMessage();

                try {
                    String plainData = new String(NFilterUtils.getInstance().nSaferDecryptWithBase64(plaindata_char));
                    JSONObject j = new JSONObject();
                    j.put("cmd", "done");
                    j.put("length", plndatalength_char);
                    j.put("encdata", encdata_char);
                    j.put("aesencdata", plaindata_char); //Android는 Plaindata를 사용하여 복호화 가능하기에 AES대신에 Plaindata 넣음
                    //TODO : 응답값으로 plain을 넘기는 부분에 대해서 고민해보자
                    j.put("plaindata" , plainData);
                    j.put("dummy", dummydata_char);

                    mJsonMiAPS.put("code", 200);
                    mJsonMiAPS.put("res", j);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                String s = "javascript:" + mCallbackFunc + "(" + mJsonMiAPS.toString() + ");";

                m.what = MiapsBridge.MIAPS_CALL_RETURN;
                m.arg1 = 200;
                m.obj = s;
                mHandlerMiAPS.sendMessage(m);
            }

            nfilter.nFilterClose(View.GONE);
        }else{
            if( nFilterTO.getPlainLength() > 0 ){
                CMinkLogMan.WriteT("nFilterResult FieldName : " + new String(  nFilterTO.getFieldName() ) );
                //리턴 값을 해당 TextView에 넣는다.
                if( new String(  nFilterTO.getFieldName() ).equals("nFilterEditText") ){

                    if(sNfilter_enctype.equalsIgnoreCase("enc"))
                        encdata_char = nFilterTO.getEncData();
                    else if(sNfilter_enctype.equalsIgnoreCase("aes"))
                        encdata_char = nFilterTO.getAESEncData();
                    else
                        encdata_char = nFilterTO.getPlainData();
                    plndatalength_char = nFilterTO.getPlainLength();
                    dummydata_char = nFilterTO.getDummyData();
                    plainNormalData_char = nFilterTO.getPlainNormalData();
                    plaindata_char = nFilterTO.getPlainData();

                    aesencdata = nFilterTO.getAESEncData();

                    byte[] b = NFilterUtils.getInstance().encrypt("test", null);

                    CMinkLogMan.WriteT("nFilterResult char PlainLength : " + nFilterTO.getPlainLength() );
                    CMinkLogMan.WriteT("nFilterResult char DummyData : " +  nFilterTO.getDummyData()  );
                    CMinkLogMan.WriteT("nFilterResult char EncData : " + encdata_char  );
                    CMinkLogMan.WriteT("nFilterResult char AESEncData : " + nFilterTO.getAESEncData()  );
                    CMinkLogMan.WriteT("nFilterResult char getPlainData : " + nFilterTO.getPlainData()  );

                    // 입력필드가 가상키보드에 가려서 보이지 않을 경우
                    // 임시로 값을 보여주는 editText
                    // nfilter_char_key_view.xml 32라인에서 직접 수정 가능

                    Message m = mHandlerMiAPS.obtainMessage();

                    try {
                        String plainData = new String(NFilterUtils.getInstance().nSaferDecryptWithBase64(plaindata_char));
                        JSONObject j = new JSONObject();
                        j.put("cmd", "changing");
                        j.put("length", plndatalength_char);
                        j.put("dummy", dummydata_char);

                        if(plndatalength_char == nfilter_maxlength) {
                            j.put("cmd", "done");
                            j.put("length", plndatalength_char);
                            j.put("encdata", encdata_char);
                            j.put("aesencdata", plaindata_char);
                            j.put("plaindata" , plainData);
                            j.put("dummy", dummydata_char);
                            nfilter.nFilterClose(View.GONE);
                        }

                        mJsonMiAPS.put("code", 200);
                        mJsonMiAPS.put("res", j);


                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    String s = "javascript:" + mCallbackFunc + "(" + mJsonMiAPS.toString() + ");";

                    m.what = MiapsBridge.MIAPS_CALL_RETURN;
                    m.arg1 = 200;
                    m.obj = s;
                    mHandlerMiAPS.sendMessage(m);

                }

            }else{
                // 글자가 입력 되었을 때
                CMinkLogMan.WriteT("nFilterResult Else : " +  nFilterTO.getFieldName() );
                //리턴 값을 해당 TextView에 넣는다.
                if( new String(  nFilterTO.getFieldName() ).equals("nFilterEditText") ){

                    encdata_char = "";
                    plndatalength_char = 0;
                    dummydata_char = "";
                    plainNormalData_char = "";
                    plaindata_char = "";


                    Message m = mHandlerMiAPS.obtainMessage();

                    try {

                        JSONObject j = new JSONObject();
                        j.put("cmd", "changing");
                        j.put("length", 0);
                        j.put("dummy", "");

                        mJsonMiAPS.put("code", 200);
                        mJsonMiAPS.put("res", j);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    String s = "javascript:" + mCallbackFunc + "(" + mJsonMiAPS.toString() + ");";

                    m.what = MiapsBridge.MIAPS_CALL_RETURN;
                    m.arg1 = 200;
                    m.obj = s;
                    mHandlerMiAPS.sendMessage(m);
                }
            }

        }
    }

}
