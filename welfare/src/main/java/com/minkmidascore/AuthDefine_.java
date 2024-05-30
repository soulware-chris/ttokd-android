package com.minkmidascore;

public class AuthDefine_ {

    /**
     * 자체 Push를 사용할것인지 <= true : default
     */
    public static boolean PROPERTY_GCM_ENABLE = true;
    /**
     * FCM인지 <= true : default
     * GCM인지 <= false
     */
    public static boolean PROPERTY_FCM_MODE = true;
    /**
     * 노티바를 개별로 띄울 것인지 true
     * 노티바를 한개만 띄울 것인지 false <= default
     */
    public static boolean PROPERTY_GCM_MULTINOTI = true;
    /**
     * 프리미엄 푸시를 사용할 것인지 true
     * 일반 푸시를 사용할 것인지 false <= default
     */
    public static boolean PROPERTY_GCM_PREMIUM = true;
    /**
     * DocView 화면 캡쳐 방지 여부(화면캡쳐방지:true, 일반:false) <= false : default
     */
    public static boolean PROPERTY_DOCVIEW_SCREENLOCK = false;
    /**
     * DocView 워터마크 사용 여부(워터마크:true, 일반:false) <= false : default
     */
    public static boolean PROPERTY_DOCVIEW_WATERMARK = false;

    /**
     * MQTT 사용 여부 <= false : default
     */
    public static boolean PROPERTY_USE_MQTT = false;

    /**
     * 단말기ID를 어떤걸 사용할지
     * IMEI라면 제일먼저 IMEI값을 가져와서 사용하고 값이 없으면 uuid로 리턴한다.
     * IMEI가 아니라면 uuid로 리턴한다.
     */
    public static String PROPERTY_DEVICEID_TYPE = "UUID";// "UUID";//"IMEI";

    // 0이면 내부, 1이면 외부 저장소
    public static int STORAGE_MODE = 0;

    public static final int LOGS_LVL = 3;


    public static final int IP_LIMIT = 1;

}
