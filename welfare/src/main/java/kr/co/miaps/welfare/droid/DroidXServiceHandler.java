package kr.co.miaps.welfare.droid;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import kr.co.miaps.welfare.MainActivity;


public class DroidXServiceHandler extends Handler {
    public static final int VIEW_TEXTVIEW = 0x01;
    public static final int VIEW_PROGRESS = 0x02;
    public static final int VIEW_IMAGEVIEW = 0x03;
    public static final int VIEW_NEXTACTIVITY = 0x10;
    public static final int VIEW_ALERT = 0x11;


    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);

        if(msg.arg1 == VIEW_TEXTVIEW) {
            ((TextView)msg.obj).setText(msg.arg2+"%");
        } else if(msg.arg1 == VIEW_PROGRESS) {
            ((ProgressBar)msg.obj).setProgress(msg.arg2);
        } else if(msg.arg1 == VIEW_IMAGEVIEW) {
            ((ImageView)msg.obj).setImageResource(msg.arg2);
        } else if(msg.arg1 == VIEW_NEXTACTIVITY) {
            // 새로운액티비티 실행
            Intent intent = new Intent((Activity)msg.obj, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            ((Activity)msg.obj).startActivity(intent);
            ((Activity)msg.obj).finish();
        } else if (msg.arg1 == VIEW_ALERT) {
            AlertDialog.Builder alert_internet_status = new AlertDialog.Builder((Activity)msg.obj);
            alert_internet_status.setTitle("바이러스가 탐지되었습니다.");
            alert_internet_status.setMessage("탐지된 악성 앱을 삭제 후 종료 버튼을 눌러주세요.\n앱을 종료합니다.\n");
            alert_internet_status.setPositiveButton("종료", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    android.os.Process.killProcess(android.os.Process.myPid());
                }
            });
            alert_internet_status.show();
        }
    }

    public static Message generateMessage(Object obj, int kind, int value) {
        Message msg = new Message();
        msg.obj = obj;
        msg.arg1 = kind;
        msg.arg2 = value;
        return msg;

    }
}
