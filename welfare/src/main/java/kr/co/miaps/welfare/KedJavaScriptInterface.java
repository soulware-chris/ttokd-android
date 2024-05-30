package kr.co.miaps.welfare;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

public class KedJavaScriptInterface {
    private Context mContext;
    private WebView mWebview;
    private Activity mActivity;
    private Handler mHandler = null;

    public KedJavaScriptInterface(Context context, WebView webView, Activity activity) {
        mContext = context;
        mWebview = webView;
        mActivity = activity;
    }

    @JavascriptInterface
    public void close() {
        ((ExtWebActivity) mActivity).finish();
    }

    @JavascriptInterface
    public void mobile(final String data) { // must be final
        ((ExtWebActivity) mActivity).finish();


    }
}
