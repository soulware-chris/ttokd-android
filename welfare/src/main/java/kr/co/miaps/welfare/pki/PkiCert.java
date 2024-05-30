package kr.co.miaps.welfare.pki;

import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import com.google.android.gms.common.util.IOUtils;
import com.minkcomm.CMinkLogMan;
import com.minkmidascore.comm.CMinkUtils;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import kr.co.miaps.bridge.MiapsBridge;
import tradesign.certificate.CertificateInfo;
import tradesign.certificate.media.IStorage;
import tradesign.pki.pkix.X509Certificate;

import static tradesign.certificate.CertificateConstants.CertType.SIGN_CERT;
import static tradesign.certificate.CertificateConstants.CertType.SIGN_CERT_PRIV;

public class PkiCert {
    private Handler mHandlerMiAPS;
    private JSONObject mJsonMiAPS;
    private String mCallbackFunc;


    public void init(JSONObject obj, final String callbackfunc, Handler h){
        mJsonMiAPS= obj;
        mCallbackFunc = callbackfunc;
        mHandlerMiAPS = h;
    }

    public void getSignCertList(String _listType){
        String listType = _listType;

        ArrayList<CertificateInfo> certs = new ArrayList<>();
        try {
            IStorage.loadCert("");
            List<IStorage> storageList = IStorage.getAllStorageList();

            for (int i = 0; i < storageList.size(); i++) {
                List<CertificateInfo> DNList = storageList.get(i).getCertInfoList();
                IStorage storage = storageList.get(i);

                // 모든 인증서 전달
                if (listType.equalsIgnoreCase("all")) {
                    certs.addAll(storage.getCertInfoList());
                } else {
                    for (int j = 0; j < DNList.size(); j++) {
                        CertificateInfo cert = DNList.get(j);

                        Date date = cert.getNotAfter();
                        //오늘 날짜와 비교해서
                        final int compare = date.compareTo(new Date());

                        //인증서가 만료되었는지 아닌지 구분
                        if (compare < 0) {
                            //만료
                            if (listType.equalsIgnoreCase("unavailable")) {
                                certs.add(cert);
                            }
                        } else {
                            //사용가능
                            if (listType.equalsIgnoreCase("available")) {
                                certs.add(cert);
                            }
                        }
                    }
                }

            }
        }catch (Exception e) {
            e.printStackTrace();
            resultErrorCode("인증서가 올바르지 않습니다.");
          //  Toast.makeText(PSend.this, "인증서가 올바르지 않습니다.", Toast.LENGTH_SHORT).show();
        }
        if(certs.size() == 0){
            // 인증서가 없는 경우
            Message m = mHandlerMiAPS.obtainMessage();
            try {
                JSONObject j = new JSONObject();
                JSONArray ja = new JSONArray();

                j.put("signCertList", ja);
                mJsonMiAPS.put("code", 200);
                mJsonMiAPS.put("res", j);

            } catch (Exception e){
                e.printStackTrace();
            }


            String s = "javascript:" + mCallbackFunc + "(" + mJsonMiAPS.toString() + ");";


            m.what = MiapsBridge.MIAPS_CALL_RETURN;
            m.arg1 = 200;
            m.obj = s;
            mHandlerMiAPS.sendMessage(m);
        } else {
            resultGetSignCertList(certs);
        }
    }

    /**
     * PKI 공인인증서 리스트 처리 메서드
     * @param certs
     */
    private void resultGetSignCertList(ArrayList<CertificateInfo> certs) {
        Message m = mHandlerMiAPS.obtainMessage();

        try {
            JSONObject j = new JSONObject();
            JSONArray ja = new JSONArray();

            for (int i=0 ; i<certs.size() ; i++) {

                CertificateInfo selected = certs.get(i);

                String storageID = selected.storageInfo().getID();
                IStorage storage = IStorage.getStorageWithID(storageID);
                if (storage !=null){
                    String sDN = selected.getSubjectDNStr();
                    X509Certificate signCert  = storage.getCert(sDN, SIGN_CERT);

                    if (signCert != null) {
                        JSONObject jp = new JSONObject();
                        String verifyCert;

                        Date date = signCert.getNotAfter();
                        //오늘 날짜와 비교해서
                        final int compare = date.compareTo(new Date());

                        //인증서가 만료되었는지 아닌지 구분
                        if(compare < 0){
                            //만료
                            verifyCert = "true";
                        }else{
                            verifyCert = "false";
                        }

                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                        String validityAfter = dateFormat.format(date);

                        date = signCert.getNotBefore();
                        String validityBefore = dateFormat.format(date);


                        jp.put("version", String.valueOf(signCert.getVersion()));           // 인증서 버전
                        jp.put("subjectDN", String.valueOf(signCert.getSubjectDN()));       // 인증서 subject DN
                        jp.put("issuerDN",String.valueOf(signCert.getIssuerDN()));          // 인증서 issuer DN
                        jp.put("serealNumber", String.valueOf(signCert.getSerialNumber())); // 인증서 serial serealNumber
                        jp.put("validityBefore", validityBefore);  // 인증서 유효기간 시작일
                        jp.put("validityAfter", validityAfter);    // 인증서 유효기간 만료일
                        jp.put("signalgoorithm", String.valueOf(signCert.getSignatureAlgorithm())); // 인증서 서명 알고리즘
                        jp.put("verifyCert", verifyCert); // 인증서 유효성 (만료가 아닌 경우 false / 만료인 경우 : true)
                        jp.put("policyoid", signCert.getSigAlgOID()); //정책
                        jp.put("storage", selected.storageInfo().getDisplayName());

                        ja.put(jp);
                    }
                }
            }
            j.put("signCertList", ja);
            mJsonMiAPS.put("code", 200);
            mJsonMiAPS.put("res", j);

        } catch (Exception e){
            e.printStackTrace();
        }


        String s = "javascript:" + mCallbackFunc + "(" + mJsonMiAPS.toString() + ");";

        m.what = MiapsBridge.MIAPS_CALL_RETURN;
        m.arg1 = 200;
        m.obj = s;
        mHandlerMiAPS.sendMessage(m);

    }


    public void getDetailSignCert(String _subjectDN){
        String subjectDN = _subjectDN;
        CertificateInfo cert = null;

        PkiSearchCert pkiSearchCert = new PkiSearchCert();
        cert = pkiSearchCert.searchCert(subjectDN);


        if(cert == null){
            // 입력한 인증서가 없는 경우
            Message m = mHandlerMiAPS.obtainMessage();

            JSONObject j = new JSONObject();
            try {
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

        } else {
            resultGetDetailSignCert(cert);
        }
    }

    /**
     * PKI 공인인증서 상세정보 처리 메서드
     * @param cert
     */
    private void resultGetDetailSignCert (CertificateInfo cert) {
        Message m = mHandlerMiAPS.obtainMessage();

        try {

            CertificateInfo selected = cert;

            String storageID = selected.storageInfo().getID();
            IStorage storage = IStorage.getStorageWithID(storageID);
            if (storage != null) {
                String sDN = selected.getSubjectDNStr();
                X509Certificate signCert = storage.getCert(sDN, SIGN_CERT);

                JSONObject j = new JSONObject();
                String verifyCert;

                Date date = signCert.getNotAfter();
                //오늘 날짜와 비교해서
                final int compare = date.compareTo(new Date());

                //인증서가 만료되었는지 아닌지 구분
                if (compare < 0) {
                    //만료
                    verifyCert = "true";
                } else {
                    verifyCert = "false";
                }

                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                String validityAfter = dateFormat.format(date);

                date = signCert.getNotBefore();
                String validityBefore = dateFormat.format(date);


                j.put("version", String.valueOf(signCert.getVersion()));                    // 인증서 버전
                j.put("subjectDN", String.valueOf(signCert.getSubjectDN()));                // 인증서 subject DN
                j.put("issuerDN", String.valueOf(signCert.getIssuerDN()));                  // 인증서 issuer DN
                j.put("serealNumber", String.valueOf(signCert.getSerialNumber()));          // 인증서 serial serealNumber
                j.put("validityBefore", validityBefore);           // 인증서 유효기간 시작일
                j.put("validityAfter", validityAfter);             // 인증서 유효기간 만료일
                j.put("signalgoorithm", String.valueOf(signCert.getSignatureAlgorithm()));  // 인증서 서명 알고리즘
                j.put("verifyCert", verifyCert); // 인증서 유효성 (만료가 아닌 경우 false / 만료인 경우 : true)

                mJsonMiAPS.put("code", 200);
                mJsonMiAPS.put("res", j);
            }
        } catch (Exception e){
            e.printStackTrace();
        }


        String s = "javascript:" + mCallbackFunc + "(" + mJsonMiAPS.toString() + ");";

        m.what = MiapsBridge.MIAPS_CALL_RETURN;
        m.arg1 = 200;
        m.obj = s;
        mHandlerMiAPS.sendMessage(m);

    }


    public void resultErrorCode(String msg){
        Message m = mHandlerMiAPS.obtainMessage();

        try {

            String errorMsg = msg;

            mJsonMiAPS.put("code", 201);
            mJsonMiAPS.put("res", errorMsg);

        } catch (Exception e){
            e.printStackTrace();
        }

        String s = "javascript:" + mCallbackFunc + "(" + mJsonMiAPS.toString() + ");";

        m.what = MiapsBridge.MIAPS_CALL_RETURN;
        m.arg1 = 201;
        m.obj = s;
        mHandlerMiAPS.sendMessage(m);
    }

    // 인증서 바이너리 데이터 추출
    public void getBinaryCert(String _subjectDN){
        String subjectDN = _subjectDN;
        CertificateInfo cert = null;
        String _der = null;
        String _key = null;

        PkiSearchCert pkiSearchCert = new PkiSearchCert();

        cert = pkiSearchCert.searchCert(subjectDN);

        if (cert!=null) {
            String derPath = cert.storageInfo().getPath(subjectDN, SIGN_CERT);// der path
            String keyPath = cert.storageInfo().getPath(subjectDN, SIGN_CERT_PRIV);// key path

            String derData = CMinkUtils.FileToBase64(derPath);
            String keyData = CMinkUtils.FileToBase64(keyPath);

            resultGetBinaryCert(derData, keyData);


        } else {
            // 입력한 인증서가 없는 경우
            Message m = mHandlerMiAPS.obtainMessage();

            JSONObject j = new JSONObject();
            try {
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


    private String readData(String _path){
        String path = _path;
        File file = new File(path) ;
        FileInputStream fis = null ;
        int data = 0 ;
        String resultData = "";

        if (file.exists() && file.canRead()) {
            try {
                // open file.
                fis = new FileInputStream(file) ;

                // read file.
                while ((data = fis.read()) != -1) {
                    // TODO : use data
                    resultData = resultData + data;
                }

                // close file.
                fis.close() ;
            } catch (Exception e) {
                e.printStackTrace() ;
            }
        }
        return resultData;
    }


    private byte[] toBase64(String input) throws FileNotFoundException, IOException {
        byte[] base64 = Base64.encodeBase64(IOUtils.toByteArray(new FileInputStream(input)));
        return base64;
    }

    private void resultGetBinaryCert(String _der, String _key){
        Message m = mHandlerMiAPS.obtainMessage();
        String der = _der;
        String key = _key;

        try {
            JSONObject j = new JSONObject();
            JSONObject jp = new JSONObject();
            jp.put("der", der);       // 인증서 바이너리 데이터
            jp.put("key", key);       // 인증서 키 바이너리 데이터
            j.put("getBinaryCert", jp);
            mJsonMiAPS.put("code", 200);
            mJsonMiAPS.put("res", j);

        } catch (Exception e){
            e.printStackTrace();
        }


        String s = "javascript:" + mCallbackFunc + "(" + mJsonMiAPS.toString() + ");";

        m.what = MiapsBridge.MIAPS_CALL_RETURN;
        m.arg1 = 200;
        m.obj = s;
        mHandlerMiAPS.sendMessage(m);
    }

    // 인증서 삭제 2021.09.01
    public void deleteCert(String _subjectDN){
        String subjectDN = _subjectDN;
        CertificateInfo cert = null;

        PkiSearchCert pkiSearchCert = new PkiSearchCert();

        cert = pkiSearchCert.searchCert(subjectDN);

        if (cert!=null) {
            String storageID = cert.storageInfo().getID();
            IStorage storage = IStorage.getStorageWithID(storageID);
            if (storage != null) {
                Boolean delete_check = false;
                try {
                    delete_check = storage.deleteCert(subjectDN);
                } catch (Exception e) {
                    delete_check = false;
                }

                Message m = mHandlerMiAPS.obtainMessage();

                try {
                    JSONObject j = new JSONObject();
                    if (delete_check) {
                        j.put("code", 0);
                        j.put("msg", "인증서가 삭제되었습니다.");
                    } else {
                        j.put("code", 201);
                        j.put("msg", "인증서삭제에 실패하였습니다.");
                    }
                    mJsonMiAPS.put("code", 200);
                    mJsonMiAPS.put("res", j);

                } catch (Exception e) {
                    e.printStackTrace();
                }


                String s = "javascript:" + mCallbackFunc + "(" + mJsonMiAPS.toString() + ");";

                m.what = MiapsBridge.MIAPS_CALL_RETURN;
                m.arg1 = 200;
                m.obj = s;
                mHandlerMiAPS.sendMessage(m);
            }
        } else {
            // 입력한 인증서가 없는 경우
            Message m = mHandlerMiAPS.obtainMessage();

            JSONObject j = new JSONObject();
            try {
                j.put("code", 202);
                j.put("msg", "해당 인증서가 없습니다.");
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
