package kr.co.miaps.welfare.pki;

import android.os.Handler;
import android.os.Message;

import com.ktnet.certrelay.android.exception.KeyPairValidationException;
import com.ktnet.certrelay.android.global.CertCommUtil;
import com.minkcomm.CMinkLogMan;
import com.nshc.nfilter.util.NFilterUtils;

import org.json.JSONObject;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;

import kr.co.miaps.bridge.MiapsBridge;
import tradesign.certificate.CertificateConstants;
import tradesign.certificate.CertificateInfo;
import tradesign.certificate.media.IStorage;
import tradesign.certificate.wrapper.PrivateKeyContainer;

public class PkiCheckCert {
    private Handler mHandlerMiAPS;
    private JSONObject mJsonMiAPS;
    private String mCallbackFunc;


    private CertificateInfo cert = null;

    private java.security.cert.X509Certificate signCert = null;
    private PrivateKeyContainer signPriv = null;

    public void init(JSONObject obj, final String callbackfunc, Handler h){
        mJsonMiAPS= obj;
        mCallbackFunc = callbackfunc;
        mHandlerMiAPS = h;
    }

    public void checkSignCert(String _subjectDN, String _certPassword, String _publickey){
        String subjectDN = _subjectDN;
        String password = _certPassword;
        String publickey = _publickey;

        // 인증서 가져오기
        PkiSearchCert pkiSearchCert = new PkiSearchCert();
        cert = pkiSearchCert.searchCert(subjectDN);

        String storageID = cert.storageInfo().getID();
        IStorage storage = IStorage.getStorageWithID(storageID);
        if (storage != null) {
            String sDN = cert.getSubjectDNStr();
            signCert = storage.getCert(sDN, CertificateConstants.CertType.SIGN_CERT);
            signPriv = storage.getEncPrivateKey(sDN, CertificateConstants.CertType.SIGN_CERT_PRIV);

            boolean bSuccess = false;
            try {
                // 패스워드 복호화
                byte[] plain = NFilterUtils.getInstance().nSaferDecryptWithBase64(password);
                CMinkLogMan.WriteT("password : " + password + "\nplain : " + plain + "\nnew String(plain) : " + new String(plain));

                bSuccess = checkValidateKeyPair(new String(plain));

                if (plain != null) { // plain 초기화.
                    for (int i = 0; i < plain.length; i++) { // plain 초기화를 안할 시에는 평문 data가 남을 수 있습니다.
                        plain[i] = 0x00;
                    }
                }
                plain = null;

                //성공
                Message m = mHandlerMiAPS.obtainMessage();

                try {
                    mJsonMiAPS.put("code", 200);
                    mJsonMiAPS.put("res", "success");
                } catch (Exception e) {
                    e.printStackTrace();
                }

                String s = "javascript:" + mCallbackFunc + "(" + mJsonMiAPS.toString() + ");";

                m.what = MiapsBridge.MIAPS_CALL_RETURN;
                m.arg1 = 200;
                m.obj = s;
                mHandlerMiAPS.sendMessage(m);


            } catch (KeyPairValidationException e) {


                if (e.getCode() == KeyPairValidationException.ERROR_CERT_PASSWORD_FAIL_NUM) {
                    resultErrorCode("비밀번호가 틀립니다. 다시 입력하세요.");
                } else if (e.getCode() == KeyPairValidationException.ERROR_CERT_KEY_PAIR_VALIDATION_FAIL_NUM) {
                    resultErrorCode("인증서 키쌍 검증에 실패하였습니다.");
                }
            }
        }
    }



    private boolean checkValidateKeyPair(String PINPassword) throws KeyPairValidationException
    {
        boolean bRet=false;

        try {
            bRet = PasswordVerify(PINPassword, signCert, signPriv);
        } catch (KeyPairValidationException e) {
            if(e.getCode() == KeyPairValidationException.ERROR_PASSWORD_FAIL_NUM){
                throw new KeyPairValidationException(KeyPairValidationException.ERROR_CERT_PASSWORD_FAIL_NUM);
            }else if(e.getCode() == KeyPairValidationException.ERROR_KEY_PAIR_VALIDATION_FAIL_NUM){
                throw new KeyPairValidationException(KeyPairValidationException.ERROR_CERT_KEY_PAIR_VALIDATION_FAIL_NUM);
            }
        } catch (Exception e){
            throw new KeyPairValidationException(KeyPairValidationException.ERROR_CERT_PASSWORD_FAIL_NUM);
        }

        return bRet;
    }

    public boolean PasswordVerify(String password, java.security.cert.X509Certificate x509, PrivateKeyContainer encKey) throws IOException, GeneralSecurityException, KeyPairValidationException{

        // 키쌍검증 루틴

        PrivateKey privateKey=null;

        try{
            privateKey = encKey.decrypt(password.toCharArray());
        }catch(Exception e){
            throw new KeyPairValidationException(KeyPairValidationException.ERROR_PASSWORD_FAIL_NUM);
        }

        if(privateKey!=null)
        {
            boolean bRet = true ;
            //	if(isSign)
            try{
                KeyPair pair = new KeyPair(x509.getPublicKey(), privateKey);
                CertCommUtil.RSAESCorePairWise(pair); //키쌍 암호화 검증
                bRet = CertCommUtil.RSASSACorePairWise(pair); //키쌍 전자서명 검증
            }catch(Exception e){
                throw new KeyPairValidationException(KeyPairValidationException.ERROR_KEY_PAIR_VALIDATION_FAIL_NUM);
            }

            return bRet;
        }

        return false;
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
        CMinkLogMan.WriteT("resultErrorCode --- : " + mJsonMiAPS.toString());

        m.what = MiapsBridge.MIAPS_CALL_RETURN;
        m.arg1 = 201;
        m.obj = s;
        mHandlerMiAPS.sendMessage(m);
    }

}
