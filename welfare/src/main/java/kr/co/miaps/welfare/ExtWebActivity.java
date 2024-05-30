package kr.co.miaps.welfare;

import static io.ktor.http.URLUtilsKt.URLBuilder;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.os.Parcelable;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import com.minkmidascore.action.CMinkCamera;
import com.minkmidascore.action.CMinkGallery;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.ktor.http.Cookie;
import io.ktor.http.Url;
import kr.co.miaps.welfare.api.CookiesContinuation;
import kr.co.miaps.welfare.api.CookiesStorageManager;


public class ExtWebActivity extends Activity {
    private static final String TAG = "ExtWebActivity";
    WebView mWebView;
    Button btnClose;
    KedJavaScriptInterface kedJavaScriptInterface;
    private Context mContext;

    private boolean closeAfterLoading = false;
    //추가
    private static final String TYPE_IMAGE = "*/*";

    private ValueCallback<Uri> mUploadMessage;
    private ValueCallback<Uri[]> mFilePathCallback;
    private ValueCallback<String[]> mFilePathCallback_1;

    private static final int REQUEST_WEBVIEW_INPUT_FILE = 413;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ext_web_activity);
        mContext = this;
        mWebView = (WebView) findViewById(R.id.webview);

        setWebView();

//        mWebView.addJavascriptInterface(kedJavaScriptInterface, "MiAPS");
        String url = getIntent().getStringExtra("url");
        String post = getIntent().getStringExtra("post");

        boolean setCookies = getIntent().getBooleanExtra("setCookies", false);
        closeAfterLoading = getIntent().getBooleanExtra("closeAfterLoading", false);
        byte[] _post = post.getBytes();
        mWebView.clearHistory();
//        mWebView.clearCache(true);

        if(setCookies) {
            String sessionUrl = getIntent().getStringExtra("sessionUrl");
            setCookie(sessionUrl);
        }

        if(_post.length == 0) {
            mWebView.loadUrl(url);
        }
        else {
            mWebView.postUrl(url, _post);
        }

        btnClose = (Button)findViewById(R.id.btn_close);
        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moveBack();
            }
        });
    }

    private void setWebView() {
        mWebView.setBackgroundColor(Color.WHITE);

        //meta태그의 viewport사용 가능
        mWebView.getSettings().setLoadWithOverviewMode(true);
        mWebView.getSettings().setUseWideViewPort(true);
        mWebView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        mWebView.getSettings().setSupportMultipleWindows(true);

        //userAgent 추가 2020-12-15
        String userAgent = mWebView.getSettings().getUserAgentString();
        mWebView.getSettings().setUserAgentString("miapsHybridWebView "+ userAgent);

        mWebView.setWebChromeClient(new WebChromeClient(){
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                WebView newWebView = new WebView(ExtWebActivity.this);
                WebSettings webSettings = newWebView.getSettings();
                webSettings.setJavaScriptEnabled(true);
                webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
                webSettings.setSupportMultipleWindows(true);

                final Dialog dialog = new Dialog(ExtWebActivity.this);
                dialog.setContentView(newWebView);

                ViewGroup.LayoutParams params = dialog.getWindow().getAttributes();
                params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                params.height = ViewGroup.LayoutParams.MATCH_PARENT;

                dialog.getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);
                dialog.show();

                newWebView.setWebChromeClient(new WebChromeClient() {
                    @Override public void onCloseWindow(WebView window)
                    {
                        dialog.dismiss();
                    }

                });
                ((WebView.WebViewTransport)resultMsg.obj).setWebView(newWebView);
                resultMsg.sendToTarget();
                return true;
            }

            // For Android Version < 3.0
            public void openFileChooser(ValueCallback<Uri> uploadMsg) {
                //System.out.println("WebViewActivity OS Version : " + Build.VERSION.SDK_INT + "\t openFC(VCU), n=1");
                mUploadMessage = uploadMsg;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType(TYPE_IMAGE);
                startActivityForResult(intent, REQUEST_WEBVIEW_INPUT_FILE);
            }

            // For 3.0 <= Android Version < 4.1
            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType) {
                //System.out.println("WebViewActivity 3<A<4.1, OS Version : " + Build.VERSION.SDK_INT + "\t openFC(VCU,aT), n=2");
                openFileChooser(uploadMsg, acceptType, "");
            }

            // Work on Android 4.4.2 Zenfone 5
            public void showFileChooser(ValueCallback<String[]> filePathCallback,
                                        String acceptType, boolean paramBoolean) {
                // TODO Auto-generated method stub
                mFilePathCallback_1 = filePathCallback;
                imageChooser(FileChooserParams.MODE_OPEN);
            }
            // For 4.1 <= Android Version < 5.0
            public void openFileChooser(ValueCallback<Uri> uploadFile, String acceptType, String capture) {
                Log.d(TAG, "openFileChooser : " + acceptType + "/" + capture);
                mUploadMessage = uploadFile;
                //imageChooser(FileChooserParams.MODE_OPEN);
                openFileChooser(mUploadMessage);
            }

            // For Android Version 5.0+
            // Ref: https://github.com/GoogleChrome/chromium-webview-samples/blob/master/input-file-example/app/src/main/java/inputfilesample/android/chrome/google/com/inputfilesample/MainFragment.java
            public boolean onShowFileChooser(WebView webView,
                                             ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                System.out.println("WebViewActivity A>5, OS Version : " + Build.VERSION.SDK_INT + "\t onSFC(WV,VCUB,FCP), n=3");
                if (mFilePathCallback != null) {
                    mFilePathCallback.onReceiveValue(null);
                }
                mFilePathCallback = filePathCallback;

                if(fileChooserParams.isCaptureEnabled()) {
                    capture();
                }
                else {
                    imageChooser(fileChooserParams.getMode());
                }
                return true;
            }

            private void imageChooser(int mode) {
                Intent contentSelectionIntent = new Intent(mContext, CMinkGallery.class);
                contentSelectionIntent.putExtra("formid", "form1.text1");
                contentSelectionIntent.putExtra("params", "");

                Intent contentDirectorySelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                contentDirectorySelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                contentDirectorySelectionIntent.setType(TYPE_IMAGE);

                if (FileChooserParams.MODE_OPEN_MULTIPLE == mode) {
                    contentSelectionIntent.putExtra("multi", "1");
                    contentDirectorySelectionIntent.putExtra("multi", "1");
                }

                Parcelable[] intentArray;
                if (contentDirectorySelectionIntent != null) {
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
                        intentArray = new Parcelable[]{contentSelectionIntent};
                    }
                    else {
                        intentArray = new Parcelable[]{contentSelectionIntent};
                    }

                } else {
                    intentArray = new Parcelable[0];
                }

                //Intent chooserIntent = Intent.createChooser(contentSelectionIntent, "미디어 선택");

                Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "미디어 선택");
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentDirectorySelectionIntent);
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
                startActivityForResult(chooserIntent, REQUEST_WEBVIEW_INPUT_FILE);
            }

            private void capture() {
                Intent takePictureIntent = new Intent(mContext, CMinkCamera.class);
                takePictureIntent.putExtra("formid", "form1.text1");
                takePictureIntent.putExtra("params", "");

                startActivityForResult(takePictureIntent, REQUEST_WEBVIEW_INPUT_FILE);
            }

            /**
             * More info this method can be found at
             * http://developer.android.com/training/camera/photobasics.html
             *
             * @return
             * @throws IOException
             */
            private File createImageFile() throws IOException {
                // Create an image file name
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                String imageFileName = "JPEG_" + timeStamp + "_";
                File storageDir = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES);
                File imageFile = File.createTempFile(
                        imageFileName,  /* prefix */
                        ".jpg",         /* suffix */
                        storageDir      /* directory */
                );
                return imageFile;

            }

            @Override
            public void onCloseWindow(WebView window) {
                window.setVisibility(View.GONE);
                window.destroy();

                super.onCloseWindow(window);
            }
        });

        kedJavaScriptInterface = new KedJavaScriptInterface(this, mWebView, ExtWebActivity.this);
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if(url.equals("dpaper://close")) {
                    btnClose.callOnClick();
                    return true;
                }
                return super.shouldOverrideUrlLoading(view, request);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if(closeAfterLoading) {
                    moveBack();
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                Log.e(TAG, "onReceivedError() " + error.getErrorCode() + error.getDescription());
                super.onReceivedError(view, request, error);
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                Log.e(TAG, "onReceivedHttpError" + " " +errorResponse.getStatusCode() + " " + errorResponse.getReasonPhrase());
                super.onReceivedHttpError(view, request, errorResponse);
            }

        });
        mWebView.addJavascriptInterface(kedJavaScriptInterface, "MiAPS");
    }

    private void moveBack() {
        Intent intent = new Intent(ExtWebActivity.this, MainActivity.class);
        setResult(RESULT_OK, intent);
        ExtWebActivity.this.finish();
    }


    @Override
    public void onBackPressed() {
        if(mWebView.canGoBack()) {
            mWebView.goBack();
        }
        else {
            moveBack();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_WEBVIEW_INPUT_FILE:
                if (RESULT_OK == resultCode) {
                    try {
                        Uri uri = data.getData();
                        if (uri == null) {
                            uri = Uri.parse("file:" + data.getStringExtra("result"));

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                Uri[] results = new Uri[]{uri};
                                mFilePathCallback.onReceiveValue(results);
                                mFilePathCallback = null;
                            } else {

                                Log.d(TAG, "openFileChooser : " + uri);

                                if (null != mUploadMessage) {
                                    mUploadMessage.onReceiveValue(uri);
                                    mUploadMessage = null;
                                } else if (null != mFilePathCallback_1) {
                                    mFilePathCallback_1.onReceiveValue(new String[]{uri.getPath()});
                                    mFilePathCallback_1 = null;
                                }
                            }
                        }else {
                            String mimeType = getContentResolver().getType(uri);

                            Uri returnUri = data.getData();
                            Cursor returnCursor = getContentResolver().query(returnUri, null, null, null, null);
                            int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                            int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
                            returnCursor.moveToFirst();
                            String filename = returnCursor.getString(nameIndex);
                            String size = Long.toString(returnCursor.getLong(sizeIndex));
                            File fileSave = getExternalFilesDir(null);
                            String sourcePath = getExternalFilesDir(null).toString();
                            try {
                                copyFileStream(new File(sourcePath + "/" + filename), uri, this);

                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                Uri[] results = new Uri[]{uri};
                                mFilePathCallback.onReceiveValue(results);
                                mFilePathCallback = null;
                            } else {

                                Log.d(TAG, "openFileChooser : " + uri);

                                if (null != mUploadMessage) {
                                    mUploadMessage.onReceiveValue(uri);
                                    mUploadMessage = null;
                                } else if (null != mFilePathCallback_1) {
                                    mFilePathCallback_1.onReceiveValue(new String[]{uri.getPath()});
                                    mFilePathCallback_1 = null;
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;

            /*case IMinkDefine.MINK_SCAN:
                if (resultCode == RESULT_OK) {
                    // 자바스크립트 형식으로 변경하자.
                    String result = "javascript:" + MOBILE_CALLBACK_FUNC + "('" + data.getStringExtra("result") + "');";
                    mWebview.loadUrl(result);
                }
                break;*/
        }

        if (mUploadMessage != null) mUploadMessage.onReceiveValue(null);
        mUploadMessage = null;

        if (mFilePathCallback != null) mFilePathCallback.onReceiveValue(null);
        mFilePathCallback = null;

        if (mFilePathCallback_1 != null) mFilePathCallback_1.onReceiveValue(null);
        mFilePathCallback_1 = null;
    }
    private void copyFileStream(File dest, Uri uri, Context context)
            throws IOException {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = context.getContentResolver().openInputStream(uri);
            os = new FileOutputStream(dest);
            byte[] buffer = new byte[1024];
            int length;

            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);

            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            is.close();
            os.close();
        }
    }


    private void setCookie(String requestUrl) {
        CompletableFuture<List<Cookie>> suspendResult = new CompletableFuture<>();
        CookiesContinuation continuation = new CookiesContinuation(suspendResult);
        Url url = URLBuilder(requestUrl).build();

        List<Cookie> cookies = (List<Cookie>) CookiesStorageManager.INSTANCE.getCookiesStorage().get(url, continuation);
        //기존 쿠키 Clear
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(mWebView, true);
        cookieManager.removeAllCookies(null);
        cookieManager.flush();

        for(Cookie cookie : cookies) {
            String cookieString = cookie.getName() + "=" + cookie.getValue() + ";";
            Log.d(TAG, "setCookie(), domain : " + cookie.getDomain() + "cookie : " + cookieString);
            cookieManager.setCookie(cookie.getDomain(), cookieString);
        }
        cookieManager.flush();
    }

}
