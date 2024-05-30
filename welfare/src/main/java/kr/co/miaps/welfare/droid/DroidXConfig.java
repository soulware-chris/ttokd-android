package kr.co.miaps.welfare.droid;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class DroidXConfig {
    private static final DroidXConfig ourInstance = new DroidXConfig();
    public static DroidXConfig getInstance() {
        return ourInstance;
    }

    private DroidXConfig() {
    }

    private final static String RECORD_NAME = "FAST_SCAN_RECORD";
    private final static String FIRST_CACHING = "FIRST_CACHING";
    private final static String FIRST_CACHING_TIME = "FIRST_CACHING_TIME";
    private final static String SECOND_SCAN_TIME = "SECOND_SCAN_TIME";
    private final static String SCAN_TYPE = "SCAN_TYPE";

    public final static int DEFAULT_SCAN_TYPE = 0;
    public final static int FAST_SCAN_TYPE = 1;
    public final static int BACKGROUND_SCAN_TYPE = 2;
    public final static int SERVICE_SCAN_TYPE = 3;

    // API TEST 화면에서 API 호출 시 순차처리를 방지하는 값
    public boolean calledScenarioTest = true;

    // runRootingScan 을 호출했을 경우 '&' 연산을 처리해주기 위한 값
    public boolean calledRunRootingScan = false;
    public long start;


    public boolean isRecordFirstCachingTime(Context context) {
        SharedPreferences recordWriter = context.getSharedPreferences(DroidXConfig.RECORD_NAME, Context.MODE_PRIVATE);
        return recordWriter.getBoolean(DroidXConfig.FIRST_CACHING, false);
    }

    public void recordFirstCachingTime(Context context, long start, long end) {
        SharedPreferences recordWriter = context.getSharedPreferences(DroidXConfig.RECORD_NAME, Context.MODE_PRIVATE);
        recordWriter.edit().putLong(DroidXConfig.FIRST_CACHING_TIME, (end - start)).putBoolean(DroidXConfig.FIRST_CACHING, true).apply();
    }

    public void recordSecondScanTime(Context context, long start, long end) {
        SharedPreferences recordWriter = context.getSharedPreferences(DroidXConfig.RECORD_NAME, Context.MODE_PRIVATE);
        recordWriter.edit().putLong(DroidXConfig.SECOND_SCAN_TIME, (end - start)).apply();
    }

    long getFirstScanTime(Context context) {
        SharedPreferences recordWriter = context.getSharedPreferences(DroidXConfig.RECORD_NAME, Context.MODE_PRIVATE);
        return recordWriter.getLong(DroidXConfig.FIRST_CACHING_TIME, 0);
    }

    long getSecondScanTime(Context context) {
        SharedPreferences recordWriter = context.getSharedPreferences(DroidXConfig.RECORD_NAME, Context.MODE_PRIVATE);
        return recordWriter.getLong(DroidXConfig.SECOND_SCAN_TIME, 0);
    }

    public void setScanType(Context context, int type) {
        SharedPreferences recordWriter = context.getSharedPreferences(DroidXConfig.RECORD_NAME, Context.MODE_PRIVATE);
        recordWriter.edit().putInt(DroidXConfig.SCAN_TYPE, type).apply();
    }

    public int getScanType(Context context) {
        SharedPreferences recordWriter = context.getSharedPreferences(DroidXConfig.RECORD_NAME, Context.MODE_PRIVATE);
        return recordWriter.getInt(DroidXConfig.SCAN_TYPE, DEFAULT_SCAN_TYPE);
    }

    public void showWarningDialog(Context context,String msg) {
        Intent popupIntent = new Intent(context, DroidXAlertDialog.class);
        popupIntent.putExtra("msg", msg);
        PendingIntent pie = PendingIntent.getActivity(context, 0, popupIntent, PendingIntent.FLAG_ONE_SHOT);
        try {
            pie.send();
        } catch (PendingIntent.CanceledException e) {
            Log.e("NSHC_Listener", e.getMessage());
        }
    }
}
