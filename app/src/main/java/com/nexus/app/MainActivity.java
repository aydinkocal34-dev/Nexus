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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public final class MainActivity extends Activity {
    private static final int BG = Color.rgb(4, 13, 25);
    private static final int PANEL = Color.rgb(11, 26, 44);
    private static final int CARD = Color.rgb(17, 35, 57);
    private static final int BLUE = Color.rgb(20, 128, 255);
    private static final int TEXT = Color.WHITE;
    private static final int MUTED = Color.rgb(158, 178, 200);
    private static final int GREEN = Color.rgb(47, 202, 119);

    private LinearLayout root;
    private int topInset;
    private TextView pageTitle;
    private TextView pageSub;

    private GradientDrawable bg(int color, float radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }

    private TextView text(String s, float size, int color) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setIncludeFontPadding(true);
        return v;
    }

    private TextView title(String s) {
        TextView v = text(s, 22, TEXT);
        v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        v.setGravity(Gravity.CENTER_VERTICAL);
        return v;
    }

    private LinearLayout screen() {
        LinearLayout s = new LinearLayout(this);
        s.setOrientation(LinearLayout.VERTICAL);
        s.setPadding(14, 12, 14, 24);
        return s;
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(18, 18, 18, 18);
        c.setBackground(bg(PANEL, 18));
        return c;
    }

    private Button blueButton(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(TEXT);
        b.setTextSize(15);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER);
        b.setBackground(bg(BLUE, 18));
        b.setPadding(8, 8, 8, 8);
        return b;
    }

    private EditText field(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setHintTextColor(MUTED);
        e.setTextColor(TEXT);
        e.setTextSize(15);
        e.setSingleLine(true);
        e.setPadding(16, 8, 16, 8);
        e.setBackground(bg(CARD, 16));
        return e;
    }

    private TextView iconButton(String icon, View.OnClickListener click) {
        TextView b = text(icon, 25, TEXT);
        b.setGravity(Gravity.CENTER);
        b.setBackground(bg(CARD, 28));
        b.setOnClickListener(click);
        return b;
    }

    private void addSpace(LinearLayout p, int h) {
        p.addView(new View(this), new LinearLayout.LayoutParams(1, h));
    }

    private void header(LinearLayout page, String titleText, String sub, final View.OnClickListener back) {
        LinearLayout h = new LinearLayout(this);
        h.setGravity(Gravity.CENTER_VERTICAL);
        if (back != null) h.addView(iconButton("‹", back), new LinearLayout.LayoutParams(46, 46));
        TextView t = title(titleText);
        h.addView(t, new LinearLayout.LayoutParams(0, 50, 1));
        TextView menu = text("⋮", 26, TEXT);
        menu.setGravity(Gravity.CENTER);
        h.addView(menu, new LinearLayout.LayoutParams(42, 46));
        page.addView(h, new LinearLayout.LayoutParams(-1, 54));
        if (sub != null && !sub.isEmpty()) {
            TextView st = text(sub, 13, MUTED);
            page.addView(st, new LinearLayout.LayoutParams(-1, -2));
        }
    }

    private void clearAndShow(LinearLayout page) {
        root.removeAllViews();
        root.addView(page, new LinearLayout.LayoutParams(-1, -1));
    }

    private TextView tile(String icon, String name, String sub, View.OnClickListener click) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(8, 14, 8, 14);
        box.setBackground(bg(CARD, 18));
        TextView i = text(icon, 27, BLUE);
        i.setGravity(Gravity.CENTER);
        box.addView(i, new LinearLayout.LayoutParams(-1, 38));
        TextView n = text(name, 15, TEXT);
        n.setGravity(Gravity.CENTER);
        n.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        box.addView(n, new LinearLayout.LayoutParams(-1, 28));
        TextView s = text(sub, 11, MUTED);
        s.setGravity(Gravity.CENTER);
        box.addView(s, new LinearLayout.LayoutParams(-1, 24));
        box.setOnClickListener(click);
        return box;
    }

    private void showHome() {
        final LinearLayout p = screen();
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView menu = text("☰", 27, TEXT);
        menu.setGravity(Gravity.CENTER);
        top.addView(menu, new LinearLayout.LayoutParams(48, 48));
        TextView brand = title("NEXUS");
        brand.setGravity(Gravity.CENTER);
        top.addView(brand, new LinearLayout.LayoutParams(0, 48, 1));
        top.addView(iconButton("♧", v -> {}), new LinearLayout.LayoutParams(48, 48));
        p.addView(top);
        addSpace(p, 12);

        TextView logo = text("N", 70, BLUE);
        logo.setGravity(Gravity.CENTER);
        logo.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        p.addView(logo, new LinearLayout.LayoutParams(-1, 90));
        TextView slogan = text("Güvenli iletişim\nGüvenli Gelecek", 13, MUTED);
        slogan.setGravity(Gravity.CENTER);
        p.addView(slogan);

        addSpace(p, 18);
        LinearLayout row1 = new LinearLayout(this);
        row1.setGravity(Gravity.CENTER);
        row1.addView(tile("◌", "Sohbet", "Güvenli mesajlaş", v -> showNewChat()),
                new LinearLayout.LayoutParams(0, 130, 1));
        row1.addView(new View(this), new LinearLayout.LayoutParams(10, 1));
        row1.addView(tile("♙", "Kişiler", "NEXUS ID ekle", v -> showNewChat()),
                new LinearLayout.LayoutParams(0, 130, 1));
        p.addView(row1);
        addSpace(p, 10);

        LinearLayout row2 = new LinearLayout(this);
        row2.addView(tile("◇", "Gizlilik", "Tam şifreleme", v -> showPrivacy()),
                new LinearLayout.LayoutParams(0, 130, 1));
        row2.addView(new View(this), new LinearLayout.LayoutParams(10, 1));
        row2.addView(tile("⚙", "Ayarlar", "Uygulama ayarları", v -> showPrivacy()),
                new LinearLayout.LayoutParams(0, 130, 1));
        p.addView(row2);

        addSpace(p, 16);
        TextView nav = text("⌂  Ana Sayfa        ◌  Sohbetler        ♙  Kişiler        ⚙  Ayarlar", 12, MUTED);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(8, 14, 8, 14);
        nav.setBackground(bg(PANEL, 20));
        p.addView(nav);

        clearAndShow(p);
    }

    private void showNewChat() {
        final LinearLayout p = screen();
        header(p, "Yeni Güvenli Sohbet", "NEXUS ID ile güvenli kişi ekle", v -> showHome());
        addSpace(p, 14);

        final EditText search = field("NEXUS ID ara...");
        p.addView(search, new LinearLayout.LayoutParams(-1, 58));

        addSpace(p, 12);
        LinearLayout qr = card();
        TextView q1 = text("▣  QR Kod ile Ekle", 16, TEXT);
        q1.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        qr.addView(q1);
        qr.addView(text("Karşı tarafın QR kodunu tara", 12, MUTED));
        qr.setOnClickListener(v -> showIdEntry());
        p.addView(qr);

        addSpace(p, 0);
        LinearLayout.LayoutParams empty = new LinearLayout.LayoutParams(-1, 0, 1);
        TextView no = text("♙\n\nHenüz kişi yok\nSohbete başlamak için bir NEXUS ID ekleyin.", 15, MUTED);
        no.setGravity(Gravity.CENTER);
        p.addView(no, empty);

        Button add = blueButton("NEXUS ID Ekle");
        add.setOnClickListener(v -> showIdEntry());
        p.addView(add, new LinearLayout.LayoutParams(-1, 58));

        clearAndShow(p);
    }

    private void showIdEntry() {
        final LinearLayout p = screen();
        header(p, "NEXUS ID Ekle", "Karşı tarafın ID'si ile arama", v -> showNewChat());
        addSpace(p, 75);

        final EditText id = field("kullanici@nexus");
        p.addView(id, new LinearLayout.LayoutParams(-1, 58));
        addSpace(p, 12);

        Button search = blueButton("Ara");
        search.setOnClickListener(v -> showFound(id.getText().toString()));
        p.addView(search, new LinearLayout.LayoutParams(-1, 58));

        TextView qr = text("Veya QR Kod Tara", 14, BLUE);
        qr.setGravity(Gravity.CENTER);
        qr.setPadding(8, 18, 8, 18);
        p.addView(qr);
        LinearLayout.LayoutParams fill = new LinearLayout.LayoutParams(-1, 0, 1);
        TextView secure = text("▣\nUçtan uca şifreleme\nBu sohbet sadece sizin ve karşı tarafın cihazında okunabilir.", 13, MUTED);
        secure.setGravity(Gravity.CENTER);
        p.addView(secure, fill);
        clearAndShow(p);
    }

    private void showFound(String id) {
        final LinearLayout p = screen();
        header(p, "Kişi Bulundu", "NEXUS ID doğrulandı", v -> showIdEntry());
        addSpace(p, 38);
        TextView avatar = text("A", 54, TEXT);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(bg(BLUE, 70));
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(110, 110);
        avatarParams.gravity = Gravity.CENTER_HORIZONTAL;
        p.addView(avatar, avatarParams);
        TextView name = text(id == null || id.isEmpty() ? "aydin@nexus" : id, 20, TEXT);
        name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        name.setGravity(Gravity.CENTER);
        p.addView(name);
        TextView online = text("● Çevrimiçi", 13, GREEN);
        online.setGravity(Gravity.CENTER);
        p.addView(online);
        addSpace(p, 28);
        LinearLayout info = card();
        TextView a = text("▢  Uçtan uca şifreleme", 15, TEXT);
        a.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        info.addView(a);
        info.addView(text("Bu sohbet sadece sizin ve karşı tarafın cihazında okunabilir.", 12, MUTED));
        p.addView(info);
        LinearLayout.LayoutParams fill = new LinearLayout.LayoutParams(-1, 0, 1);
        p.addView(new View(this), fill);
        Button chat = blueButton("Sohbete Başla");
        chat.setOnClickListener(v -> showChat(id));
        p.addView(chat, new LinearLayout.LayoutParams(-1, 58));
        clearAndShow(p);
    }

    private void showChat(String id) {
        final LinearLayout p = screen();
        header(p, id == null || id.isEmpty() ? "aydin@nexus" : id, "● Çevrimiçi", v -> showFound(id));
        addSpace(p, 12);
        TextView day = text("Bugün", 11, MUTED);
        day.setGravity(Gravity.CENTER);
        p.addView(day);
        addSpace(p, 14);

        LinearLayout messages = new LinearLayout(this);
        messages.setOrientation(LinearLayout.VERTICAL);
        TextView incoming = text("Merhaba\n09:34", 14, TEXT);
        incoming.setPadding(14, 12, 14, 12);
        incoming.setBackground(bg(CARD, 16));
        messages.addView(incoming, new LinearLayout.LayoutParams(-2, -2));
        TextView outgoing = text("Merhaba\nNasılsın?  ✓✓", 14, TEXT);
        outgoing.setPadding(14, 12, 14, 12);
        outgoing.setBackground(bg(BLUE, 16));
        LinearLayout.LayoutParams op = new LinearLayout.LayoutParams(-2, -2);
        op.gravity = Gravity.RIGHT;
        op.topMargin = 10;
        messages.addView(outgoing, op);
        TextView reply = text("İyiyim, sen nasılsın?\n09:35", 14, TEXT);
        reply.setPadding(14, 12, 14, 12);
        reply.setBackground(bg(CARD, 16));
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-2, -2);
        rp.topMargin = 10;
        messages.addView(reply, rp);
        p.addView(messages, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout composer = new LinearLayout(this);
        composer.setGravity(Gravity.CENTER_VERTICAL);
        composer.setPadding(6, 6, 6, 6);
        composer.setBackground(bg(PANEL, 22));
        EditText input = field("Mesaj yaz...");
        composer.addView(input, new LinearLayout.LayoutParams(0, 54, 1));
        TextView send = text("➤", 22, TEXT);
        send.setGravity(Gravity.CENTER);
        send.setBackground(bg(BLUE, 30));
        composer.addView(send, new LinearLayout.LayoutParams(54, 54));
        p.addView(composer);
        clearAndShow(p);
    }

    private void showPrivacy() {
        final LinearLayout p = screen();
        header(p, "Gizlilik", "NEXUS güvenlik merkezi", v -> showHome());
        addSpace(p, 18);
        LinearLayout c = card();
        TextView t = text("🔒 Uçtan uca şifreleme", 17, TEXT);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        c.addView(t);
        c.addView(text("Mesaj içerikleri yalnızca konuşmaya katılan cihazlarda okunacak şekilde tasarlanır.", 13, MUTED));
        p.addView(c);
        addSpace(p, 10);
        LinearLayout c2 = card();
        c2.addView(text("● Güvenlik durumu", 17, TEXT));
        c2.addView(text("NEXUS çekirdek iletişim katmanı hazır.", 13, GREEN));
        p.addView(c2);
        clearAndShow(p);
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        Window w = getWindow();
        w.setStatusBarColor(BG);
        w.setNavigationBarColor(BG);
        w.getDecorView().setSystemUiVisibility(0);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);
        scroll.setFillViewport(true);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(root);

        scroll.setOnApplyWindowInsetsListener((v, insets) -> {
            int top = insets.getSystemWindowInsetTop();
            int bottom = insets.getSystemWindowInsetBottom();
            root.setPadding(0, top, 0, bottom);
            return insets;
        });

        setContentView(scroll);
        showHome();
        scroll.requestApplyInsets();
    }
}
