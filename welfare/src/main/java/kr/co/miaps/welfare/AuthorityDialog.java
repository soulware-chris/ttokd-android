package kr.co.miaps.welfare;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;


public class AuthorityDialog extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.authority_dialog);

        WebView webview = findViewById(R.id.authorityWebview);
        WebSettings settings = webview.getSettings();
        settings.setJavaScriptEnabled(true);

        webview.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // .... 내용 처리....
                //return false;

                if(url.startsWith("authority://")) {
                    setResult(RESULT_OK);
                    finish();
                    return true;
                }


                return super.shouldOverrideUrlLoading(view, url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {

                if(request.getUrl().toString().startsWith("authority://")) {
                    setResult(RESULT_OK);
                    finish();
                    return true;
                }

                return super.shouldOverrideUrlLoading(view, request);
            }
        });


        webview.loadUrl(getResources().getString(R.string.auth_url));
    }

    @Override
    public void onBackPressed() {
        // 처리하지 않는다.
        //super.onBackPressed();
    }
}
