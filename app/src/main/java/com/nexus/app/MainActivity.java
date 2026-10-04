package com.nexus.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public final class MainActivity extends Activity {
    private static final int BG = Color.rgb(4, 13, 25);
    private static final int CARD = Color.rgb(12, 27, 45);
    private static final int CARD_2 = Color.rgb(17, 36, 58);
    private static final int ACCENT = Color.rgb(47, 145, 220);
    private static final int TEXT = Color.WHITE;
    private static final int MUTED = Color.rgb(157, 178, 199);
    private static final int SUCCESS = Color.rgb(67, 190, 125);

    private GradientDrawable rounded(int color, float radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }

    private TextView label(String text, float size, int color) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(size);
        v.setTextColor(color);
        return v;
    }

    private LinearLayout tile(String icon, String title, String subtitle) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(10, 12, 10, 10);
        box.setBackground(rounded(CARD_2, 24f));

        TextView i = label(icon, 25f, TEXT);
        i.setGravity(Gravity.CENTER);
        box.addView(i, new LinearLayout.LayoutParams(-1, 38));

        TextView t = label(title, 14f, TEXT);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setGravity(Gravity.CENTER);
        box.addView(t, new LinearLayout.LayoutParams(-1, 30));

        TextView s = label(subtitle, 11f, MUTED);
        s.setGravity(Gravity.CENTER);
        box.addView(s, new LinearLayout.LayoutParams(-1, 24));

        return box;
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 30, 24, 22);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView brand = label("NEXUS", 31f, TEXT);
        brand.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        header.addView(brand, new LinearLayout.LayoutParams(0, 62, 1f));

        TextView profile = label("●", 27f, ACCENT);
        profile.setGravity(Gravity.CENTER);
        profile.setBackground(rounded(CARD_2, 40f));
        header.addView(profile, new LinearLayout.LayoutParams(56, 56));
        root.addView(header);

        TextView subtitle = label("Güvenli iletişimin yeni nesli", 15f, MUTED);
        root.addView(subtitle, new LinearLayout.LayoutParams(-1, 34));

        LinearLayout statusCard = new LinearLayout(this);
        statusCard.setOrientation(LinearLayout.VERTICAL);
        statusCard.setPadding(24, 22, 24, 22);
        statusCard.setBackground(rounded(CARD, 28f));

        LinearLayout statusLine = new LinearLayout(this);
        statusLine.setGravity(Gravity.CENTER_VERTICAL);

        TextView dot = label("●", 16f, SUCCESS);
        statusLine.addView(dot, new LinearLayout.LayoutParams(30, 36));

        TextView ready = label("NEXUS hazır", 20f, TEXT);
        ready.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        statusLine.addView(ready, new LinearLayout.LayoutParams(0, 36, 1f));

        TextView badge = label("AKTİF", 11f, SUCCESS);
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(rounded(Color.rgb(19, 64, 48), 18f));
        statusLine.addView(badge, new LinearLayout.LayoutParams(68, 34));

        statusCard.addView(statusLine);

        TextView statusDetail = label("Bağlantı kurulmaya hazır.", 14f, MUTED);
        statusDetail.setPadding(30, 4, 0, 16);
        statusCard.addView(statusDetail);

        TextView start = label("NEXUS'A BAŞLA", 16f, TEXT);
        start.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        start.setGravity(Gravity.CENTER);
        start.setBackground(rounded(ACCENT, 22f));
        statusCard.addView(start, new LinearLayout.LayoutParams(-1, 58));

        root.addView(statusCard, new LinearLayout.LayoutParams(-1, 230));

        TextView quickTitle = label("HIZLI ERİŞİM", 12f, MUTED);
        quickTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        quickTitle.setPadding(6, 22, 0, 10);
        root.addView(quickTitle, new LinearLayout.LayoutParams(-1, 48));

        LinearLayout tiles = new LinearLayout(this);
        tiles.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout messages = tile("✉", "Mesajlar", "Sohbetler");
        LinearLayout contacts = tile("♟", "Kişiler", "Rehber");
        LinearLayout security = tile("◆", "Güvenlik", "Kontrol");
        tiles.addView(messages, new LinearLayout.LayoutParams(0, 128, 1f));
        tiles.addView(new View(this), new LinearLayout.LayoutParams(10, 1));
        tiles.addView(contacts, new LinearLayout.LayoutParams(0, 128, 1f));
        tiles.addView(new View(this), new LinearLayout.LayoutParams(10, 1));
        tiles.addView(security, new LinearLayout.LayoutParams(0, 128, 1f));
        root.addView(tiles);

        TextView infoTitle = label("NEXUS", 12f, MUTED);
        infoTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        infoTitle.setPadding(6, 20, 0, 8);
        root.addView(infoTitle, new LinearLayout.LayoutParams(-1, 38));

        TextView info = label("Özel iletişim alanın hazır.\nYeni özellikler güvenli şekilde katman katman eklenecek.", 14f, MUTED);
        info.setPadding(18, 14, 18, 14);
        info.setBackground(rounded(CARD, 22f));
        root.addView(info, new LinearLayout.LayoutParams(-1, 76));

        TextView footer = label("NEXUS  •  v0.22.2", 12f, MUTED);
        footer.setGravity(Gravity.CENTER);
        root.addView(footer, new LinearLayout.LayoutParams(-1, 54));

        View.OnClickListener action = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                info.setText(((LinearLayout) v).getChildAt(1) instanceof TextView
                        ? ((TextView) ((LinearLayout) v).getChildAt(1)).getText() + "\n\nArayüz testi başarılı."
                        : "NEXUS\n\nArayüz testi başarılı.");
            }
        };

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                info.setText("NEXUS bağlantı ekranı\n\nArayüz testi başarılı.");
            }
        });
        messages.setOnClickListener(action);
        contacts.setOnClickListener(action);
        security.setOnClickListener(action);

        scroll.addView(root);
        setContentView(scroll);
    }
}
