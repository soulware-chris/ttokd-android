package kr.co.miaps.welfare.pki.receive;

import android.os.Build;
import android.os.Handler;
import android.os.Message;

import com.ktnet.certrelay.android.global.CertCommEventHandler;
import com.ktnet.certrelay.android.global.CertCommUtil;
import com.ktnet.certrelay.android.global.CommState;
import com.ktnet.certrelay.android.global.PKIUtil;
import com.ktnet.certrelay.android.global.ResponseItem;
import com.ktnet.certrelay.android.global.ResponseItem.CertInfo;
import com.ktnet.certrelay.android.global.ResponseItem.ClientProperty;
import com.ktnet.certrelay.android.storage.AppInternalStorage;
import com.ktnet.certrelay.android.storage.SDStorage;
import com.minkcomm.CMinkLogMan;
import com.nshc.nfilter.util.NFilterUtils;

import org.json.JSONObject;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;

import kr.co.miaps.bridge.MiapsBridge;
import tradesign.certificate.media.IStorage;
import tradesign.pki.exception.CertPasswordFormatException;
import tradesign.pki.oss.pkcs.PFX;
import tradesign.pki.pkix.X509Certificate;


public class PGet {

	private final int M_GET_OK=1;
	private final int M_GET_FAIL=2;
	private final int M_GET_NETWORK_FAIL=3;
	private final int M_PFX_LOAD_OK=4;
	private final int M_GET_CERTCODE_FAIL=5;

	private String mRtnCode = "";
    private String certPassword = null;
    private String publickey = "";

    private int pollingTime = 3000;
    private int timeOut = 180 * 1000;

    private byte[] mBytePfx = null;

    private Handler mHandlerMiAPS;
    private JSONObject mJsonMiAPS;
    private String mCallbackFunc;

    private String signCode = "";

	public void init(JSONObject obj, final String callbackfunc, Handler h){
		mJsonMiAPS= obj;
		mCallbackFunc = callbackfunc;
		mHandlerMiAPS = h;
	}

	public void PKIgetCert(String _certCode, String _password, String _publickey){
		certPassword = _password;
		signCode = _certCode;
		publickey = _publickey;

		if(!signCode.equals("")) {
			// 인증서 요청 대기
			Message message = Message.obtain();
			message.what = M_GET_OK;
			mHandler.sendMessage(message);
		} else {
			resultErrorCode("인증코드를 받으세요.");
		}
	}
	public void PkiGetInit(){


		CertCommUtil.setRequestHandler(new CertCommEventHandler() {

			public void onResponse(int type, String code, String msg, Object obj) {
				// TODO Auto-generated method stub
				if(type == CertCommUtil.TYPE_GET_CERTCODE){

					ResponseItem item=(ResponseItem)obj;
					if(code.equalsIgnoreCase(CommState.RSP_SUCCESS)) // 테스트 용   :   CommState.RSP_SUCCESS
					{
						List<ClientProperty> propertyList = item.ClientPropertyList;
						if(propertyList.size() > 0){
							try{
								pollingTime = Integer.parseInt(propertyList.get(0).polling_time) * 1000;
								CertCommUtil.setPollingTime(pollingTime); //CertCommUtil.Net_RcvPFX : 인증서받기시 서버접속 주기
                                CertCommUtil.setTimeOut(timeOut); //CertCommUtil.Net_RcvPFX : 인증서받기시 서버접속 및 대기 timeout
							}catch (NumberFormatException e){
								e.printStackTrace();
							}
						}

						List<CertInfo> infoLIst = item.CertInfoList;
						if(infoLIst.size() > 0){
							mRtnCode = infoLIst.get(0).CERT_CODE;

							String random1 = PKIUtil.getRandom(4);
							String random2 = PKIUtil.getRandom(4);


							String [] certCode = new String[3];
							certCode[0] = random1;
							certCode[1] = random2;
							certCode[2] = mRtnCode;

							resultSignCertCode(certCode);

						}
					}
					else
					{
						Message message = Message.obtain();
						message.what = M_GET_CERTCODE_FAIL;
						message.obj = msg;
						mHandler.sendMessage(message);
					}

				}else if(type == CertCommUtil.TYPE_NET_RCVPFX){

					mBytePfx = (byte[])obj;

					Message message = Message.obtain();
					message.what = M_PFX_LOAD_OK;
					message.obj = msg;
					mHandler.sendMessage(message);
				}
			}

			public void onError(int type, String code, String msg) {
				// TODO Auto-generated method stub
				if(type == CertCommUtil.TYPE_GET_CERTCODE){

					Message message = Message.obtain();
					message.what = M_GET_NETWORK_FAIL;
					message.obj = "네트워크 에러.";
					mHandler.sendMessage(message);

				}else if(type == CertCommUtil.TYPE_NET_RCVPFX){

					Message message = Message.obtain();
					message.what = M_GET_FAIL;
					message.obj = msg;
					mHandler.sendMessage(message);

				}
			}
		});

	}

	public void getSignCode(){
		Thread thread = new Thread(new Runnable_req());
		thread.setDaemon(true);
		thread.start();
	}



	class Runnable_req implements Runnable {

		public void run() {

			try {
				CertCommUtil.GetCertCode();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				Message message = Message.obtain();
				message.what = M_GET_NETWORK_FAIL;
				message.obj = "인증코드 요청에 실패하였습니다.";
				mHandler.sendMessage(message);
			}

		}
	}

    private int count = 0;
	class Runnable_get implements Runnable {
		public void run() {
			try {
				CMinkLogMan.WriteT("signCode --- : " + signCode);
				CertCommUtil.Net_RcvPFX(signCode.substring(8));
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				Message message = Message.obtain();
				message.what = M_GET_FAIL;
				message.obj = "인증서 요청에 실패하였습니다.";
				mHandler.sendMessage(message);
			}

		} // run()

	} // Runnable_get


	// 인증서 파일 가져오기
    private void start_get() {
		Thread thread = new Thread(new Runnable_get());
		thread.setDaemon(true);
		thread.start();
	}
	//--

	Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case M_GET_OK:
					start_get();
					break;
				case M_GET_FAIL:

					count = 0;
                    resultErrorCode("인증서 요청실패");

					break;
				case M_GET_NETWORK_FAIL:
					resultErrorCode("인증코드 요청실패");

					break;
				case M_GET_CERTCODE_FAIL:
                    resultErrorCode("인증코드 요청실패");

					break;
				case M_PFX_LOAD_OK:

					///mTv_board.setText("인증서를 다운 받았습니다.");

                    String code = signCode;

					PFX pfx = null;
					try {
						pfx = new PFX(mBytePfx);
						pfx.load(code.toCharArray());
						X509Certificate cert = new X509Certificate(pfx.getCert(PFX.KEYUSAGE_SIGNATURE));
						//cert info
						BigInteger sn = cert.getSerialNumber();
						Date na = cert.getNotAfter();
						Date nb = cert.getNotBefore();
						String dn = cert.getSubjectDNStr();
					} catch (Exception e) {
						e.printStackTrace();
					}

                    makeCert(mBytePfx, code);

					break;
			}
		}
	};



    private  void resultSignCertCode(String[] certCode){
		Message m = mHandlerMiAPS.obtainMessage();

		try {
			if(certCode != null) {
				String[] code = certCode;

				JSONObject j = new JSONObject();
				JSONObject jp = new JSONObject();
				jp.put("code1", code[0]);
				jp.put("code2", code[1]);
				jp.put("code3", code[2]);
				j.put("signCertCode", jp);

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

    private void resultErrorCode(String msg){
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



    private void makeCert(byte[] bytePfx, String code){
	    String mCertCode = code;
	    byte [] mBytePfx = bytePfx;
        try
        {
			String strRet = "";
			// 패스워드 복호화
			byte[] plain = NFilterUtils.getInstance().nSaferDecryptWithBase64(certPassword);
			CMinkLogMan.WriteT("certPassword : "+certPassword+"\nplain : "+plain+"\nnew String(plain) : "+ new String(plain));

			if(Build.VERSION.SDK_INT > 29)
				strRet = PKIUtil.extractPFX(mBytePfx, new String(plain), mCertCode, IStorage.getStorageWithID(AppInternalStorage.ID));
            else {
				strRet = PKIUtil.extractPFX(mBytePfx, new String(plain), mCertCode, IStorage.getStorageWithID(SDStorage.ID));
			}

			if(plain != null) { // plain 초기화.
				for( int i = 0; i < plain.length; i++) { // plain 초기화를 안할 시에는 평문 data가 남을 수 있습니다.
					plain[i] = 0x00;
				}
			}
			plain = null;


            if ( strRet == null || strRet.equals(""))
            {
                success_get(true);

            }
            else
            {
				success_get(true);
				//resultErrorCode("---- 이미 존재.");
            }
        } catch (CertPasswordFormatException e) {
            resultErrorCode("암호형식이 맞지 않습니다." + e.getMessage());
            //Toast.makeText(GetInputPassword.this, "암호형식이 맞지 않습니다." + e.getMessage(), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
			//PFX 복호화 실패
            e.printStackTrace();
            resultErrorCode("PFX 복호화에 실패하였습니다.(extractPFX)");
        }



    }
    private void success_get(boolean bSelect)
    {
        if ( bSelect )
        {
            try
            {
                //성공
				if(Build.VERSION.SDK_INT > 29)
					PKIUtil.copyPFX(IStorage.getStorageWithID(AppInternalStorage.ID));
				else {
					PKIUtil.copyPFX(IStorage.getStorageWithID(SDStorage.ID));
				}

				Message m = mHandlerMiAPS.obtainMessage();

				try {
					mJsonMiAPS.put("code", 200);
					mJsonMiAPS.put("res", "success");
				} catch (Exception e){
					e.printStackTrace();
				}

				String s = "javascript:" + mCallbackFunc + "(" + mJsonMiAPS.toString() + ");";

				m.what = MiapsBridge.MIAPS_CALL_RETURN;
				m.arg1 = 200;
				m.obj = s;
				mHandlerMiAPS.sendMessage(m);

			//	StorageInstallHelper.installAllStorage(m_Context);
            } catch (Exception e) {
            	//PFX 복호화 실패
                e.printStackTrace();
				resultErrorCode("PFX 복호화에 실패하였습니다.(copyPFX)");
            }
        }
        else
        {
        	//인증서 다운 취소
			resultErrorCode("인증서 다운을 취소하였습니다.");
        }

    }
}