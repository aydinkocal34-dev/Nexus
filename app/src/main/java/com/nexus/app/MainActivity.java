package com.nexus.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
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
        v.setIncludeFontPadding(true);
        return v;
    }

    private LinearLayout tile(String icon, String title, String subtitle) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(6, 16, 6, 16);
        box.setBackground(rounded(CARD_2, 22f));

        TextView i = label(icon, 21f, TEXT);
        i.setGravity(Gravity.CENTER);
        box.addView(i, new LinearLayout.LayoutParams(-1, -2));

        TextView t = label(title, 14f, TEXT);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1, -2);
        tp.topMargin = 6;
        box.addView(t, tp);

        TextView s = label(subtitle, 11f, MUTED);
        s.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.topMargin = 3;
        box.addView(s, sp);

        return box;
    }

    private LinearLayout.LayoutParams full() {
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
        scroll.setBackgroundColor(BG);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);

        final LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(18, 20, 18, 28);

        scroll.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                int top = insets.getSystemWindowInsetTop();
                int bottom = insets.getSystemWindowInsetBottom();
                root.setPadding(18, top + 18, 18, bottom + 24);
                return insets;
            }
        });

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView brand = label("NEXUS", 28f, TEXT);
        brand.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        brand.setGravity(Gravity.CENTER_VERTICAL);
        brand.setPadding(0, 4, 0, 6);
        header.addView(brand, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView profile = label("●", 19f, ACCENT);
        profile.setGravity(Gravity.CENTER);
        profile.setBackground(rounded(CARD_2, 28f));
        header.addView(profile, new LinearLayout.LayoutParams(46, 46));
        root.addView(header, full());

        TextView subtitle = label("Güvenli iletişimin yeni nesli", 15f, MUTED);
        subtitle.setPadding(0, 2, 0, 2);
        root.addView(subtitle, full());

        final LinearLayout statusCard = new LinearLayout(this);
        statusCard.setOrientation(LinearLayout.VERTICAL);
        statusCard.setPadding(16, 16, 16, 16);
        statusCard.setBackground(rounded(CARD, 22f));
        LinearLayout.LayoutParams scp = full();
        scp.topMargin = 14;
        root.addView(statusCard, scp);

        LinearLayout statusLine = new LinearLayout(this);
        statusLine.setGravity(Gravity.CENTER_VERTICAL);

        TextView dot = label("●", 13f, SUCCESS);
        dot.setGravity(Gravity.CENTER);
        statusLine.addView(dot, new LinearLayout.LayoutParams(24, -2));

        TextView ready = label("NEXUS hazır", 18f, TEXT);
        ready.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        ready.setGravity(Gravity.CENTER_VERTICAL);
        statusLine.addView(ready, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView badge = label("AKTİF", 10f, SUCCESS);
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(rounded(Color.rgb(19, 64, 48), 15f));
        badge.setPadding(10, 5, 10, 5);
        statusLine.addView(badge, new LinearLayout.LayoutParams(-2, -2));
        statusCard.addView(statusLine, full());

        TextView detail = label("Bağlantı kurulmaya hazır.", 14f, MUTED);
        LinearLayout.LayoutParams dp = full();
        dp.topMargin = 8;
        statusCard.addView(detail, dp);

        final TextView start = label("NEXUS'A BAŞLA", 16f, TEXT);
        start.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        start.setGravity(Gravity.CENTER);
        start.setBackground(rounded(ACCENT, 18f));
        start.setPadding(12, 14, 12, 14);
        LinearLayout.LayoutParams bp = full();
        bp.topMargin = 12;
        statusCard.addView(start, bp);

        TextView quickTitle = label("HIZLI ERİŞİM", 12f, MUTED);
        quickTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        quickTitle.setPadding(2, 18, 0, 8);
        root.addView(quickTitle, full());

        LinearLayout tiles = new LinearLayout(this);
        tiles.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout messages = tile("✉", "Mesajlar", "Sohbetler");
        LinearLayout contacts = tile("●", "Kişiler", "Rehber");
        LinearLayout security = tile("◆", "Güvenlik", "Kontrol");

        tiles.addView(messages, new LinearLayout.LayoutParams(0, -2, 1f));
        tiles.addView(new View(this), new LinearLayout.LayoutParams(7, 1));
        tiles.addView(contacts, new LinearLayout.LayoutParams(0, -2, 1f));
        tiles.addView(new View(this), new LinearLayout.LayoutParams(7, 1));
        tiles.addView(security, new LinearLayout.LayoutParams(0, -2, 1f));
        root.addView(tiles, full());

        TextView infoTitle = label("NEXUS", 12f, MUTED);
        infoTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        infoTitle.setPadding(2, 18, 0, 8);
        root.addView(infoTitle, full());

        final TextView info = label(
                "Özel iletişim alanın hazır.\nYeni özellikler güvenli şekilde katman katman eklenecek.",
                14f, MUTED);
        info.setGravity(Gravity.CENTER_VERTICAL);
        info.setPadding(14, 14, 14, 14);
        info.setBackground(rounded(CARD, 20f));
        root.addView(info, full());

        TextView footer = label("NEXUS  •  v0.22.2", 12f, MUTED);
        footer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams fp = full();
        fp.topMargin = 14;
        root.addView(footer, fp);

        View.OnClickListener tileAction = new View.OnClickListener() {
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
        messages.setOnClickListener(tileAction);
        contacts.setOnClickListener(tileAction);
        security.setOnClickListener(tileAction);

        scroll.addView(root);
        setContentView(scroll);
        scroll.requestApplyInsets();
    }
}
