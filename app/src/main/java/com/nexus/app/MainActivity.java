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
import android.widget.FrameLayout;
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
        s.setPadding(16, 14, 16, 28);
        return s;
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(18, 16, 18, 16);
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
        b.setPadding(10, 6, 10, 6);
        return b;
    }

    private EditText field(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setHintTextColor(MUTED);
        e.setTextColor(TEXT);
        e.setTextSize(15);
        e.setSingleLine(true);
        e.setPadding(17, 6, 17, 6);
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
        TextView menu = text("⋮", 24, MUTED);
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

    private LinearLayout tile(int iconRes, String name, String sub, View.OnClickListener click) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(8, 8, 8, 8);
        box.setBackground(bg(CARD, 20));

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        icon.setBackground(bg(Color.rgb(14, 40, 72), 30));
        box.addView(icon, new LinearLayout.LayoutParams(54, 54));

        TextView nameView = text(name, 15, TEXT);
        nameView.setGravity(Gravity.CENTER);
        nameView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        nameView.setPadding(0, 5, 0, 0);
        box.addView(nameView, new LinearLayout.LayoutParams(-1, -2));

        TextView subView = text(sub, 10, MUTED);
        subView.setGravity(Gravity.CENTER);
        subView.setMaxLines(1);
        box.addView(subView, new LinearLayout.LayoutParams(-1, -2));

        box.setOnClickListener(click);
        return box;
    }

    private void showHome() {
        final LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(8, 4, 8, 8);
        root.setBackgroundColor(BG);

        // NEXUS reference header
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);

        ImageView menu = new ImageView(this);
        menu.setImageResource(R.drawable.ic_menu);
        menu.setPadding(8, 8, 8, 8);
        header.addView(menu, new LinearLayout.LayoutParams(54, 58));

        TextView brand = text("NEXUS", 22, TEXT);
        brand.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        brand.setGravity(Gravity.CENTER);
        header.addView(brand, new LinearLayout.LayoutParams(0, 58, 1));

        ImageView bell = new ImageView(this);
        bell.setImageResource(R.drawable.ic_bell);
        bell.setPadding(9, 9, 9, 9);
        header.addView(bell, new LinearLayout.LayoutParams(54, 58));
        root.addView(header);

        // Large glowing NEXUS logo block
        LinearLayout logoBlock = new LinearLayout(this);
        logoBlock.setOrientation(LinearLayout.VERTICAL);
        logoBlock.setGravity(Gravity.CENTER_HORIZONTAL);

        FrameLayout logoFrame = new FrameLayout(this);
        TextView glow = text("✦", 110, Color.rgb(12, 88, 170));
        glow.setGravity(Gravity.CENTER);
        glow.setAlpha(0.18f);
        logoFrame.addView(glow, new FrameLayout.LayoutParams(210, 190, Gravity.CENTER));

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.nexus_logo);
        logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        logoFrame.addView(logo, new FrameLayout.LayoutParams(190, 190, Gravity.CENTER));
        logoBlock.addView(logoFrame, new LinearLayout.LayoutParams(220, 190));

        TextView safeTitle = text("Güvenli İletişim", 17, TEXT);
        safeTitle.setGravity(Gravity.CENTER);
        safeTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        logoBlock.addView(safeTitle, new LinearLayout.LayoutParams(-1, 30));

        TextView safeSub = text("Güvenli Gelecek", 15, MUTED);
        safeSub.setGravity(Gravity.CENTER);
        logoBlock.addView(safeSub, new LinearLayout.LayoutParams(-1, 28));

        root.addView(logoBlock, new LinearLayout.LayoutParams(-1, 248));

        // Reference 2x2 premium cards
        LinearLayout row1 = new LinearLayout(this);
        row1.setGravity(Gravity.CENTER);
        row1.addView(tile(R.drawable.ic_chat, "Sohbet", "Güvenli mesajlaş", v -> showNewChat()),
                new LinearLayout.LayoutParams(0, 184, 1));
        row1.addView(new View(this), new LinearLayout.LayoutParams(14, 1));
        row1.addView(tile(R.drawable.ic_people, "Kişiler", "NEXUS ID ekle", v -> showNewChat()),
                new LinearLayout.LayoutParams(0, 184, 1));
        root.addView(row1);

        addSpace(root, 12);

        LinearLayout row2 = new LinearLayout(this);
        row2.setGravity(Gravity.CENTER);
        row2.addView(tile(R.drawable.ic_shield, "Gizlilik", "Tam şifreleme", v -> showPrivacy()),
                new LinearLayout.LayoutParams(0, 184, 1));
        row2.addView(new View(this), new LinearLayout.LayoutParams(14, 1));
        row2.addView(tile(R.drawable.ic_settings, "Ayarlar", "Uygulama ayarları", v -> showPrivacy()),
                new LinearLayout.LayoutParams(0, 184, 1));
        root.addView(row2);

        // Neon wave at the bottom of the reference composition
        View wave = new View(this) {
            private final android.graphics.Paint p = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            @Override protected void onDraw(android.graphics.Canvas canvas) {
                super.onDraw(canvas);
                float w = getWidth(), h = getHeight();
                p.setStyle(android.graphics.Paint.Style.STROKE);
                p.setStrokeWidth(5f);
                p.setStrokeCap(android.graphics.Paint.Cap.ROUND);
                p.setColor(Color.rgb(0, 126, 255));
                p.setShadowLayer(18f, 0, 0, Color.rgb(0, 110, 255));
                android.graphics.Path path = new android.graphics.Path();
                path.moveTo(-30, h * 0.88f);
                path.cubicTo(w * 0.20f, h * 0.15f, w * 0.47f, h * 0.18f, w * 0.76f, h * 0.70f);
                path.cubicTo(w * 0.88f, h * 0.88f, w * 0.96f, h * 0.94f, w + 30, h * 0.98f);
                canvas.drawPath(path, p);
                p.clearShadowLayer();
                p.setStrokeWidth(2.5f);
                p.setColor(Color.rgb(51, 190, 255));
                android.graphics.Path p2 = new android.graphics.Path();
                p2.moveTo(-30, h * 0.98f);
                p2.cubicTo(w * 0.22f, h * 0.28f, w * 0.48f, h * 0.30f, w * 0.78f, h * 0.78f);
                p2.cubicTo(w * 0.90f, h * 0.92f, w, h * 0.96f, w + 30, h);
                canvas.drawPath(p2, p);
            }
        };
        root.addView(wave, new LinearLayout.LayoutParams(-1, 105));

        root.addView(new View(this), new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout nav = new LinearLayout(this);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(4, 5, 4, 5);
        nav.setBackground(bg(Color.rgb(7, 19, 33), 20));

        nav.addView(navItem("⌂", "Ana Sayfa", BLUE), new LinearLayout.LayoutParams(0, 70, 1));

        TextView chats = navItem("◌", "Sohbetler", MUTED);
        chats.setOnClickListener(v -> showNewChat());
        nav.addView(chats, new LinearLayout.LayoutParams(0, 70, 1));

        TextView people = navItem("♙", "Kişiler", MUTED);
        people.setOnClickListener(v -> showNewChat());
        nav.addView(people, new LinearLayout.LayoutParams(0, 70, 1));

        TextView settings = navItem("⚙", "Ayarlar", MUTED);
        settings.setOnClickListener(v -> showPrivacy());
        nav.addView(settings, new LinearLayout.LayoutParams(0, 70, 1));

        root.addView(nav);
        clearAndShow(root);
    }

    private TextView navItem(String icon, String label, int color) {
        TextView v = text(icon + "
" + label, 10, color);
        v.setGravity(Gravity.CENTER);
        v.setIncludeFontPadding(true);
        return v;
    }

    private void showNewChat() {
        final LinearLayout p = screen();
        header(p, "Yeni Güvenli Sohbet", "NEXUS ID ile güvenli kişi ekle", v -> showHome());
        addSpace(p, 14);

        final EditText search = field("NEXUS ID ara...");
        p.addView(search, new LinearLayout.LayoutParams(-1, 58));
        TextView format = text("Örnek: kullanici@nexus", 11, MUTED);
        format.setPadding(4, 5, 4, 0);
        p.addView(format);

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
        TextView no = text("♙

Henüz kişi yok
Sohbete başlamak için bir NEXUS ID ekleyin.", 15, MUTED);
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
        search.setOnClickListener(v -> {
            String value = id.getText().toString().trim().toLowerCase();
            if (value.isEmpty()) {
                id.setError("NEXUS ID gerekli");
                return;
            }
            if (!value.contains("@")) value = value + "@nexus";
            showFound(value);
        });
        p.addView(search, new LinearLayout.LayoutParams(-1, 58));

        TextView qr = text("Veya QR Kod Tara", 14, BLUE);
        qr.setGravity(Gravity.CENTER);
        qr.setPadding(8, 18, 8, 18);
        p.addView(qr);
        LinearLayout.LayoutParams fill = new LinearLayout.LayoutParams(-1, 0, 1);
        TextView secure = text("▣
Uçtan uca şifreleme
Bu sohbet sadece sizin ve karşı tarafın cihazında okunabilir.", 13, MUTED);
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
        TextView incoming = text("Merhaba
09:34", 14, TEXT);
        incoming.setPadding(14, 12, 14, 12);
        incoming.setBackground(bg(CARD, 16));
        messages.addView(incoming, new LinearLayout.LayoutParams(-2, -2));
        TextView outgoing = text("Merhaba
Nasılsın?  ✓✓", 14, TEXT);
        outgoing.setPadding(14, 12, 14, 12);
        outgoing.setBackground(bg(BLUE, 16));
        LinearLayout.LayoutParams op = new LinearLayout.LayoutParams(-2, -2);
        op.gravity = Gravity.RIGHT;
        op.topMargin = 10;
        messages.addView(outgoing, op);
        TextView reply = text("İyiyim, sen nasılsın?
09:35", 14, TEXT);
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
        send.setOnClickListener(v -> {
            String msg = input.getText().toString().trim();
            if (!msg.isEmpty()) {
                TextView sent = text(msg + "\
şimdi  ✓", 14, TEXT);
                sent.setPadding(14, 12, 14, 12);
                sent.setBackground(bg(BLUE, 16));
                LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-2, -2);
                sp.gravity = Gravity.RIGHT;
                sp.topMargin = 10;
                messages.addView(sent, sp);
                input.setText("");
            }
        });
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
        c2.addView(text("Güvenlik katmanı arayüzü hazır.", 13, GREEN));
        addSpace(c2, 8);
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView shield = text("✓", 18, GREEN);
        shield.setGravity(Gravity.CENTER);
        shield.setBackground(bg(CARD, 24));
        row.addView(shield, new LinearLayout.LayoutParams(42, 42));
        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);
        details.setPadding(12, 0, 0, 0);
        TextView key = text("Güvenlik anahtarı", 13, TEXT);
        key.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        details.addView(key);
        details.addView(text("Cihaz kimliği için güvenli altyapı hazırlanıyor.", 11, MUTED));
        row.addView(details, new LinearLayout.LayoutParams(0, 48, 1));
        c2.addView(row);
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
