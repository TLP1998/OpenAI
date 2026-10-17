package com.tlp.bydvoiceprobe;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Build;
import android.view.WindowManager;
import android.widget.TextView;

/** Voice launcher contains no credentials, PIN, network code, or vehicle SDK. */
public final class LauncherActivity extends Activity {
    private boolean launched = false;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        // Allow a launched voice entry to display while the screen is locked when HyperOS permits.
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
        TextView t = new TextView(this);
        t.setTextSize(19);
        t.setTextColor(Color.DKGRAY);
        t.setPadding(30,80,30,30);
        t.setText("正在打开车控主程序…\n\n请先安装「比亚迪车控主程序」，完成登录、车辆选择和 PIN 验证。");
        setContentView(t);
        if (state != null) return;
        try {
            Intent action = new Intent(getString(R.string.action_name));
            action.setClassName("dev.vehiclebridge.byd.xiaoai", "dev.vehiclebridge.byd.VoiceCommandActivity");
            startActivity(action);
            launched = true;
            finish();
        } catch (ActivityNotFoundException | SecurityException e) {
            t.setText("无法启动车控主程序。\n\n请先安装同一批次构建的车控主程序 APK，再打开一次。\n\n如果更新了主程序，需要同时安装本批次三个语音入口 APK。");
        }
    }
}
