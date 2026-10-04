package com.nexus.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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
        v.setIncludeFontPadding(false);
        return v;
    }

    private LinearLayout tile(String icon, String title, String subtitle) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(8, 14, 8, 14);
        box.setBackground(rounded(CARD_2, 22f));

        TextView i = label(icon, 22f, TEXT);
        i.setGravity(Gravity.CENTER);
        box.addView(i, new LinearLayout.LayoutParams(-1, 30));

        TextView t = label(title, 14f, TEXT);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setGravity(Gravity.CENTER);
        box.addView(t, new LinearLayout.LayoutParams(-1, 24));

        TextView s = label(subtitle, 11f, MUTED);
        s.setGravity(Gravity.CENTER);
        box.addView(s, new LinearLayout.LayoutParams(-1, 20));

        return box;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(-1, -2);
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        Window window = getWindow();
        window.setStatusBarColor(BG);
        window.setNavigationBarColor(BG);
        window.getDecorView().setSystemUiVisibility(0);

        final ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        scroll.setClipToPadding(false);

        final LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(20, 24, 20, 24);

        // Android 15/16 enforces edge-to-edge for targetSdk 35.
        // Read the real system-bar insets so content never sits underneath
        // the clock/status icons or the navigation gesture area.
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(20, bars.top + 24, 20, bars.bottom + 24);
            return insets;
        });

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, 8);

        TextView brand = label("NEXUS", 28f, TEXT);
        brand.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        brand.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(brand, new LinearLayout.LayoutParams(0, 44, 1f));

        TextView profile = label("●", 20f, ACCENT);
        profile.setGravity(Gravity.CENTER);
        profile.setBackground(rounded(CARD_2, 30f));
        header.addView(profile, new LinearLayout.LayoutParams(46, 46));
        root.addView(header, matchWrap());

        TextView subtitle = label("Güvenli iletişimin yeni nesli", 15f, MUTED);
        subtitle.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(subtitle, matchWrap());

        final LinearLayout statusCard = new LinearLayout(this);
        statusCard.setOrientation(LinearLayout.VERTICAL);
        statusCard.setPadding(18, 16, 18, 16);
        statusCard.setBackground(rounded(CARD, 24f));
        LinearLayout.LayoutParams statusParams = matchWrap();
        statusParams.topMargin = 14;
        root.addView(statusCard, statusParams);

        LinearLayout statusLine = new LinearLayout(this);
        statusLine.setGravity(Gravity.CENTER_VERTICAL);

        TextView dot = label("●", 14f, SUCCESS);
        dot.setGravity(Gravity.CENTER);
        statusLine.addView(dot, new LinearLayout.LayoutParams(24, 30));

        TextView ready = label("NEXUS hazır", 18f, TEXT);
        ready.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        ready.setGravity(Gravity.CENTER_VERTICAL);
        statusLine.addView(ready, new LinearLayout.LayoutParams(0, 30, 1f));

        TextView badge = label("AKTİF", 10f, SUCCESS);
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(rounded(Color.rgb(19, 64, 48), 16f));
        statusLine.addView(badge, new LinearLayout.LayoutParams(62, 30));
        statusCard.addView(statusLine, matchWrap());

        TextView statusDetail = label("Bağlantı kurulmaya hazır.", 14f, MUTED);
        statusDetail.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams detailParams = matchWrap();
        detailParams.topMargin = 8;
        statusCard.addView(statusDetail, detailParams);

        TextView start = label("NEXUS'A BAŞLA", 16f, TEXT);
        start.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        start.setGravity(Gravity.CENTER);
        start.setBackground(rounded(ACCENT, 20f));
        LinearLayout.LayoutParams startParams = matchWrap();
        startParams.topMargin = 12;
        statusCard.addView(start, startParams);
        start.setPadding(0, 15, 0, 15);

        TextView quickTitle = label("HIZLI ERİŞİM", 12f, MUTED);
        quickTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        quickTitle.setGravity(Gravity.CENTER_VERTICAL);
        quickTitle.setPadding(4, 16, 0, 8);
        root.addView(quickTitle, matchWrap());

        LinearLayout tiles = new LinearLayout(this);
        tiles.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout messages = tile("✉", "Mesajlar", "Sohbetler");
        LinearLayout contacts = tile("♟", "Kişiler", "Rehber");
        LinearLayout security = tile("◆", "Güvenlik", "Kontrol");
        tiles.addView(messages, new LinearLayout.LayoutParams(0, -2, 1f));
        View gap1 = new View(this);
        tiles.addView(gap1, new LinearLayout.LayoutParams(7, 1));
        tiles.addView(contacts, new LinearLayout.LayoutParams(0, -2, 1f));
        View gap2 = new View(this);
        tiles.addView(gap2, new LinearLayout.LayoutParams(7, 1));
        tiles.addView(security, new LinearLayout.LayoutParams(0, -2, 1f));
        root.addView(tiles, matchWrap());

        TextView infoTitle = label("NEXUS", 12f, MUTED);
        infoTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        infoTitle.setPadding(4, 16, 0, 8);
        root.addView(infoTitle, matchWrap());

        final TextView info = label(
                "Özel iletişim alanın hazır.\nYeni özellikler güvenli şekilde katman katman eklenecek.",
                14f, MUTED);
        info.setGravity(Gravity.CENTER_VERTICAL);
        info.setPadding(16, 14, 16, 14);
        info.setBackground(rounded(CARD, 20f));
        root.addView(info, matchWrap());

        TextView footer = label("NEXUS  •  v0.22.2", 12f, MUTED);
        footer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams footerParams = matchWrap();
        footerParams.topMargin = 14;
        footerParams.bottomMargin = 8;
        root.addView(footer, footerParams);

        View.OnClickListener action = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView title = (TextView) ((LinearLayout) v).getChildAt(1);
                info.setText(title.getText() + "\n\nArayüz testi başarılı.");
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
        scroll.requestApplyInsets();
    }
}
