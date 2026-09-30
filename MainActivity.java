package com.nexus.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class MainActivity extends Activity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                              WindowManager.LayoutParams.FLAG_SECURE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(48, 72, 48, 48);
        root.setBackgroundColor(0xFF080808);

        TextView title = new TextView(this);
        title.setText("NEXUS");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(36);

        TextView body = new TextView(this);
        body.setTextColor(0xFFE0E0E0);
        body.setTextSize(17);
        body.setPadding(0, 40, 0, 0);
        body.setText("TEST BUILD\n\n" +
            "Güvenli mesajlaşma çekirdeği hazırlanıyor.\n\n" +
            "✓ FLAG_SECURE\n✓ Yedekleme kapalı\n" +
            "✓ 60 saniyelik okuma süresi çekirdeği\n" +
            "✓ CI otomatik testleri\n\n" +
            "E2EE + kör relay katmanı sonraki aşamadır.");

        root.addView(title);
        root.addView(body);
        setContentView(root);
    }
}
