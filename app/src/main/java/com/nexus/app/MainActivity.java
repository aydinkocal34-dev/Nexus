package com.nexus.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class MainActivity extends Activity {
    private static final int BG = Color.rgb(4, 13, 25);
    private static final int CARD = Color.rgb(12, 27, 45);
    private static final int TEXT = Color.WHITE;
    private static final int MUTED = Color.rgb(160, 180, 200);

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(48, 80, 48, 48);
        root.setBackgroundColor(BG);

        TextView logo = new TextView(this);
        logo.setText("NEXUS");
        logo.setTextColor(TEXT);
        logo.setTextSize(34f);
        logo.setGravity(Gravity.CENTER);
        root.addView(logo, new LinearLayout.LayoutParams(-1, 90));

        TextView subtitle = new TextView(this);
        subtitle.setText("Güvenli iletişimin yeni nesli");
        subtitle.setTextColor(MUTED);
        subtitle.setTextSize(16f);
        subtitle.setGravity(Gravity.CENTER);
        root.addView(subtitle, new LinearLayout.LayoutParams(-1, 70));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(36, 36, 36, 36);
        card.setBackgroundColor(CARD);

        TextView status = new TextView(this);
        status.setText("NEXUS hazır\n\nİlk bağlantını başlat.");
        status.setTextColor(TEXT);
        status.setTextSize(20f);
        status.setGravity(Gravity.CENTER);
        card.addView(status, new LinearLayout.LayoutParams(-1, 150));

        Button start = new Button(this);
        start.setText("NEXUS'A BAŞLA");
        start.setTextSize(16f);
        start.setAllCaps(false);
        card.addView(start, new LinearLayout.LayoutParams(-1, 64));

        root.addView(card, new LinearLayout.LayoutParams(-1, 330));

        TextView footer = new TextView(this);
        footer.setText("Mesajlar • Kişiler • Güvenlik");
        footer.setTextColor(MUTED);
        footer.setTextSize(14f);
        footer.setGravity(Gravity.CENTER);
        root.addView(footer, new LinearLayout.LayoutParams(-1, 90));

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                status.setText("NEXUS bağlantı ekranı\n\nArayüz testi başarılı.");
            }
        });

        setContentView(root);
    }
}
