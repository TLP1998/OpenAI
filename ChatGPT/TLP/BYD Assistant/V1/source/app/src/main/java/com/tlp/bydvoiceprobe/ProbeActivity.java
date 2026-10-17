package com.tlp.bydvoiceprobe;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;
import android.content.SharedPreferences;
import java.text.DateFormat;
import java.util.Date;

/** Local-only launch probe. No BYD APIs, no network access, no background tasks. */
public final class ProbeActivity extends Activity {
    private TextView log;
    private static final String STORE="voice_probe";
    private static final String HISTORY="history";
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        String command=getString(R.string.command_text);
        SharedPreferences prefs=getSharedPreferences(STORE,MODE_PRIVATE);
        String previous=prefs.getString(HISTORY,"");
        String now=DateFormat.getDateTimeInstance().format(new Date());
        prefs.edit().putString(HISTORY,now+"  "+command+"入口启动\n"+(previous.length()>1200?previous.substring(0,1200):previous)).apply();
        LinearLayout root=new LinearLayout(this);
        root.setPadding(35,60,35,30);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(245,247,252));
        TextView title=new TextView(this);
        title.setText("已启动「"+command+"」测试入口");
        title.setTextSize(26);
        title.setTextColor(command.equals("锁车")?Color.rgb(29,78,216):Color.rgb(15,118,110));
        root.addView(title);
        TextView warning=new TextView(this);
        warning.setText("\n安全测试：只记录启动，不联网、不读取PIN、不控制车辆。\n\n观察小爱能否分别打开两个独立的测试应用。\n");
        warning.setTextSize(17);
        warning.setTextColor(Color.DKGRAY);
        root.addView(warning);
        log=new TextView(this);
        log.setTextSize(16);
        log.setTextColor(Color.BLACK);
        root.addView(log);
        Button clear=new Button(this);
        clear.setText("清空启动记录");
        clear.setOnClickListener(v->{prefs.edit().remove(HISTORY).apply();update(prefs);});
        root.addView(clear);
        android.widget.ScrollView scroll=new android.widget.ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);
        update(prefs);
    }
    private void update(SharedPreferences prefs) {
        log.setText("本机最近启动记录：\n"+prefs.getString(HISTORY,"暂无记录"));
    }
}
