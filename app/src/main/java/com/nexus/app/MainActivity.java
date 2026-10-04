package com.nexus.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class MainActivity extends Activity {
    private static final int BG = Color.rgb(4, 13, 25);
    private static final int CARD = Color.rgb(12, 27, 45);
    private static final int ACCENT = Color.rgb(52, 152, 219);
    private static final int TEXT = Color.WHITE;
    private static final int MUTED = Color.rgb(160, 180, 200);

    private GradientDrawable rounded(int color, float radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }

    private Button navButton(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(TEXT);
        b.setTextSize(14f);
        b.setAllCaps(false);
        b.setBackground(rounded(CARD, 28f));
        b.setPadding(8, 0, 8, 0);
        return b;
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(32, 54, 32, 24);
        root.setBackgroundColor(BG);

        TextView logo = new TextView(this);
        logo.setText("NEXUS");
        logo.setTextColor(TEXT);
        logo.setTextSize(36f);
        logo.setGravity(Gravity.CENTER);
        root.addView(logo, new LinearLayout.LayoutParams(-1, 72));

        TextView subtitle = new TextView(this);
        subtitle.setText("Güvenli iletişimin yeni nesli");
        subtitle.setTextColor(MUTED);
        subtitle.setTextSize(16f);
        subtitle.setGravity(Gravity.CENTER);
        root.addView(subtitle, new LinearLayout.LayoutParams(-1, 52));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(28, 28, 28, 28);
        card.setBackground(rounded(CARD, 32f));

        TextView status = new TextView(this);
        status.setText("NEXUS hazır\n\nGüvenli bağlantını başlat.");
        status.setTextColor(TEXT);
        status.setTextSize(20f);
        status.setGravity(Gravity.CENTER);
        card.addView(status, new LinearLayout.LayoutParams(-1, 130));

        Button start = new Button(this);
        start.setText("NEXUS'A BAŞLA");
        start.setTextColor(TEXT);
        start.setTextSize(16f);
        start.setAllCaps(false);
        start.setBackground(rounded(ACCENT, 30f));
        card.addView(start, new LinearLayout.LayoutParams(-1, 64));

        root.addView(card, new LinearLayout.LayoutParams(-1, 270));

        TextView section = new TextView(this);
        section.setText("HIZLI ERİŞİM");
        section.setTextColor(MUTED);
        section.setTextSize(12f);
        section.setGravity(Gravity.CENTER_VERTICAL);
        section.setPadding(8, 20, 8, 4);
        root.addView(section, new LinearLayout.LayoutParams(-1, 48));

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        Button messages = navButton("💬  Mesajlar");
        Button contacts = navButton("👥  Kişiler");
        Button security = navButton("🔐  Güvenlik");
        nav.addView(messages, new LinearLayout.LayoutParams(0, 58, 1f));
        LinearLayout.LayoutParams gap = new LinearLayout.LayoutParams(0, 1, 0.05f);
        nav.addView(contacts, new LinearLayout.LayoutParams(0, 58, 1f));
        nav.addView(security, new LinearLayout.LayoutParams(0, 58, 1f));
        root.addView(nav, new LinearLayout.LayoutParams(-1, 70));

        TextView footer = new TextView(this);
        footer.setText("NEXUS • Güvenli iletişim");
        footer.setTextColor(MUTED);
        footer.setTextSize(13f);
        footer.setGravity(Gravity.CENTER);
        root.addView(footer, new LinearLayout.LayoutParams(-1, 54));

        View.OnClickListener quickAction = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                status.setText(((Button) v).getText().toString() + "\n\nArayüz testi başarılı.");
            }
        };

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                status.setText("NEXUS bağlantı ekranı\n\nArayüz testi başarılı.");
            }
        });
        messages.setOnClickListener(quickAction);
        contacts.setOnClickListener(quickAction);
        security.setOnClickListener(quickAction);

        setContentView(root);
    }
}
