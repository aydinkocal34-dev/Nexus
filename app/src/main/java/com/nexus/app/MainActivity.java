package com.nexus.app;

import android.Manifest;
import androidx.fragment.app.FragmentActivity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Bitmap;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.core.app.NotificationCompat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import android.text.TextUtils;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

public final class MainActivity extends FragmentActivity {
    private static final int BG=Color.rgb(4,13,25), PANEL=Color.rgb(11,26,44), CARD=Color.rgb(17,35,57);
    private static final int BLUE=Color.rgb(20,128,255), TEXT=Color.WHITE, MUTED=Color.rgb(158,178,200);
    private static final int GREEN=Color.rgb(47,202,119), RED=Color.rgb(255,82,100);
    private LinearLayout root, messages;
    private SharedPreferences prefs;
    private E2eeRuntime e2ee;
    private String localId, activePeer;
    private boolean english=false;
    private boolean relayConnected=false;
    private TextView connectionStatus;
    private final ArrayList<String> contactIds=new ArrayList<>();
    private final Map<String,TextView> deliveryViews=new HashMap<>();
    private boolean securityUnlocked=false;
    private boolean authInProgress=false;
    private BiometricPrompt biometricPrompt;
    private androidx.activity.result.ActivityResultLauncher<ScanOptions> qrScannerLauncher;

    private int dp(float v){ return Math.round(v * getResources().getDisplayMetrics().density); }
    private GradientDrawable bg(int c,float r){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(r);return d;}
    private TextView text(String s,float size,int color){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(color);v.setIncludeFontPadding(true);return v;}
    private TextView title(String s){TextView v=text(s,21,TEXT);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setGravity(Gravity.CENTER_VERTICAL);v.setMaxLines(1);v.setEllipsize(TextUtils.TruncateAt.MIDDLE);return v;}
    private LinearLayout screen(){LinearLayout s=new LinearLayout(this);s.setOrientation(LinearLayout.VERTICAL);s.setPadding(dp(16),dp(20),dp(16),dp(24));s.setBackgroundColor(BG);return s;}
    private LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(18),dp(14),dp(18),dp(14));GradientDrawable d=bg(PANEL,dp(18));d.setStroke(dp(1),Color.rgb(18,76,126));c.setBackground(d);return c;}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setTextColor(Color.WHITE);b.setTextSize(17);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setAllCaps(false);b.setGravity(Gravity.CENTER);b.setMinHeight(0);b.setMinimumHeight(0);b.setPadding(dp(12),0,dp(12),0);b.setIncludeFontPadding(true);GradientDrawable d=bg(BLUE,dp(16));d.setStroke(dp(1),Color.rgb(62,171,255));b.setBackground(d);return b;}
    private EditText field(String hint){EditText e=new EditText(this);e.setHint(hint);e.setHintTextColor(MUTED);e.setTextColor(TEXT);e.setTextSize(17);e.setSingleLine(true);e.setPadding(17,6,17,6);e.setBackground(bg(CARD,16));return e;}
    private TextView icon(String s,View.OnClickListener l){TextView v=text(s,25,TEXT);v.setGravity(Gravity.CENTER);v.setBackground(bg(CARD,26));v.setOnClickListener(l);return v;}
    private void space(LinearLayout p,int h){p.addView(new View(this),new LinearLayout.LayoutParams(1,h));}
    private void show(LinearLayout p){root.removeAllViews();root.addView(p,new LinearLayout.LayoutParams(-1,-1));}
    private void header(LinearLayout p,String t,String sub,View.OnClickListener back){
        LinearLayout h=new LinearLayout(this);h.setGravity(Gravity.CENTER_VERTICAL);
        if(back!=null)h.addView(icon("‹",back),new LinearLayout.LayoutParams(dp(46),dp(46)));
        h.addView(title(t),new LinearLayout.LayoutParams(0,dp(56),1));
        TextView home=icon("⌂",v->showHome());home.setTextColor(BLUE);h.addView(home,new LinearLayout.LayoutParams(dp(46),dp(46)));
        p.addView(h,new LinearLayout.LayoutParams(-1,dp(58))); View line=new View(this);line.setBackgroundColor(Color.rgb(18,104,180));p.addView(line,new LinearLayout.LayoutParams(-1,dp(1)));
        if(sub!=null&&!sub.isEmpty())p.addView(text(sub,13,MUTED),new LinearLayout.LayoutParams(-1,-2));
    }
    private LinearLayout tile(int res,String name,String sub,View.OnClickListener l){
        LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_VERTICAL);
        box.setPadding(dp(14),dp(9),dp(10),dp(8));
        box.setBackground(bg(CARD,dp(18))); box.setOnClickListener(l);

        LinearLayout top=new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL);
        ImageView icon=new ImageView(this); icon.setImageResource(res); icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        icon.setBackground(bg(Color.rgb(10,35,65),dp(24)));
        top.addView(icon,new LinearLayout.LayoutParams(dp(42),dp(42)));
        TextView arrow=text("›",31,Color.rgb(48,156,255)); arrow.setGravity(Gravity.CENTER);
        top.addView(arrow,new LinearLayout.LayoutParams(0,dp(42),1)); box.addView(top,new LinearLayout.LayoutParams(-1,dp(42)));

        TextView n=text(name,16,TEXT); n.setTypeface(Typeface.DEFAULT,Typeface.BOLD); box.addView(n,new LinearLayout.LayoutParams(-1,dp(23)));
        TextView q=text(sub,13,MUTED); box.addView(q,new LinearLayout.LayoutParams(-1,dp(20)));
        return box;
    }

    private TextView nav(String i,String s,int c){TextView v=text(i+"\n"+s,12,c);v.setGravity(Gravity.CENTER);return v;}

    private void lockApp(){
        if(isFinishing()||authInProgress)return;
        securityUnlocked=false;
        BiometricManager bm=BiometricManager.from(this);
        int can=bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG|BiometricManager.Authenticators.DEVICE_CREDENTIAL);
        if(can!=BiometricManager.BIOMETRIC_SUCCESS){
            toast("NEXUS için cihazda PIN, desen veya biyometri ayarlamalısın.");
            return;
        }
        authInProgress=true;
        BiometricPrompt.AuthenticationCallback cb=new BiometricPrompt.AuthenticationCallback(){
            @Override public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result){authInProgress=false;securityUnlocked=true;}
            @Override public void onAuthenticationFailed(){toast("Kimlik doğrulama başarısız");}
            @Override public void onAuthenticationError(int code,CharSequence err){authInProgress=false;securityUnlocked=false;if(code==BiometricPrompt.ERROR_USER_CANCELED||code==BiometricPrompt.ERROR_CANCELED){new android.os.Handler(getMainLooper()).postDelayed(()->lockApp(),300);}else toast("NEXUS kilidi: "+err);}
        };
        biometricPrompt=new BiometricPrompt(this,ContextCompat.getMainExecutor(this),cb);
        BiometricPrompt.PromptInfo info=new BiometricPrompt.PromptInfo.Builder()
                .setTitle("NEXUS Güvenlik Kilidi")
                .setSubtitle("Parmak izi veya cihaz PIN/deseni ile devam edin")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG|BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build();
        biometricPrompt.authenticate(info);
    }

    @Override protected void onResume(){
        super.onResume();
        if(root!=null&&!securityUnlocked&&!authInProgress)new android.os.Handler(getMainLooper()).postDelayed(()->lockApp(),120);
    }

    @Override protected void onStop(){
        super.onStop();
        if(!authInProgress)securityUnlocked=false;
    }

    private void showHome(){
        // Reference layout: normal Android dp sizing, generous spacing, no overlap.
        LinearLayout p=new LinearLayout(this);
        p.setOrientation(LinearLayout.VERTICAL);
        p.setPadding(dp(8),dp(4),dp(8),dp(8));
        p.setBackgroundColor(BG);

        LinearLayout h=new LinearLayout(this);
        h.setGravity(Gravity.CENTER_VERTICAL);
        ImageView menu=new ImageView(this); menu.setImageResource(R.drawable.ic_menu); menu.setPadding(dp(8),dp(8),dp(8),dp(8));
        h.addView(menu,new LinearLayout.LayoutParams(dp(48),dp(52)));
        TextView brand=text("NEXUS",23,TEXT); brand.setTypeface(Typeface.DEFAULT,Typeface.BOLD); brand.setGravity(Gravity.CENTER);
        h.addView(brand,new LinearLayout.LayoutParams(0,dp(52),1));
        ImageView bell=new ImageView(this); bell.setImageResource(R.drawable.ic_bell); bell.setPadding(dp(8),dp(8),dp(8),dp(8)); bell.setOnClickListener(v->showNotifications());
        h.addView(bell,new LinearLayout.LayoutParams(dp(48),dp(52)));
        p.addView(h);

        LinearLayout hero=new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL); hero.setGravity(Gravity.CENTER_HORIZONTAL);
        ImageView logo=new ImageView(this); logo.setImageResource(R.drawable.nexus_logo); logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        hero.addView(logo,new LinearLayout.LayoutParams(dp(92),dp(82)));
        TextView s1=text(english?"Secure Communication":"Güvenli İletişim",19,TEXT); s1.setTypeface(Typeface.DEFAULT,Typeface.BOLD); s1.setGravity(Gravity.CENTER);
        hero.addView(s1,new LinearLayout.LayoutParams(-1,dp(30)));
        TextView s2=text(english?"Secure Future":"Güvenli Gelecek",16,Color.rgb(190,210,235)); s2.setGravity(Gravity.CENTER);
        hero.addView(s2,new LinearLayout.LayoutParams(-1,dp(27)));
        p.addView(hero,new LinearLayout.LayoutParams(-1,dp(139)));

        View earth=new View(this){
            final android.graphics.Paint paint=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            final android.graphics.Path path=new android.graphics.Path();
            protected void onDraw(android.graphics.Canvas c){
                float w=getWidth(),h=getHeight();
                paint.setStyle(android.graphics.Paint.Style.FILL); paint.setColor(Color.rgb(2,18,42));
                path.reset(); path.moveTo(0,h*.72f); path.quadTo(w*.50f,-h*.80f,w,h*.72f); path.lineTo(w,h); path.lineTo(0,h); path.close(); c.drawPath(path,paint);
                paint.setStyle(android.graphics.Paint.Style.STROKE); paint.setStrokeWidth(dp(1.5f)); paint.setColor(Color.rgb(0,150,255));
                paint.setShadowLayer(dp(7),0,0,Color.rgb(0,120,255));
                path.reset(); path.moveTo(-dp(12),h*.76f); path.quadTo(w*.50f,-h*.72f,w+dp(12),h*.76f); c.drawPath(path,paint); paint.clearShadowLayer();
            }
        };
        p.addView(earth,new LinearLayout.LayoutParams(-1,dp(46)));

        LinearLayout r1=new LinearLayout(this); r1.setGravity(Gravity.CENTER);
        r1.addView(tile(R.drawable.ic_chat,english?"Chat":"Sohbet",english?"Secure messaging":"Güvenli mesajlaşma",v->showChats()),new LinearLayout.LayoutParams(0,dp(108),1));
        r1.addView(new View(this),new LinearLayout.LayoutParams(dp(12),1));
        r1.addView(tile(R.drawable.ic_people,english?"People":"Kişiler","NEXUS ID ekle",v->showContacts()),new LinearLayout.LayoutParams(0,dp(108),1));
        p.addView(r1);
        space(p,dp(10));
        LinearLayout r2=new LinearLayout(this); r2.setGravity(Gravity.CENTER);
        r2.addView(tile(R.drawable.ic_shield,english?"Privacy":"Gizlilik",english?"Full encryption":"Tam şifreleme",v->showPrivacy()),new LinearLayout.LayoutParams(0,dp(108),1));
        r2.addView(new View(this),new LinearLayout.LayoutParams(dp(12),1));
        r2.addView(tile(R.drawable.ic_settings,english?"Settings":"Ayarlar",english?"App settings":"Uygulama ayarları",v->showSettings()),new LinearLayout.LayoutParams(0,dp(108),1));
        p.addView(r2);

        View wave=new View(this){
            final android.graphics.Paint paint=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            protected void onDraw(android.graphics.Canvas c){
                float w=getWidth(),h=getHeight(); paint.setStyle(android.graphics.Paint.Style.STROKE); paint.setStrokeCap(android.graphics.Paint.Cap.ROUND);
                paint.setStrokeWidth(dp(1.7f)); paint.setColor(Color.rgb(0,126,255)); paint.setShadowLayer(dp(6),0,0,Color.rgb(0,110,255));
                android.graphics.Path q=new android.graphics.Path(); q.moveTo(-dp(20),h*.30f); q.cubicTo(w*.18f,h*.45f,w*.40f,h*.90f,w*.62f,h*.66f); q.cubicTo(w*.80f,h*.48f,w*.90f,h*.42f,w+dp(20),h*.72f); c.drawPath(q,paint); paint.clearShadowLayer();
            }
        };
        p.addView(wave,new LinearLayout.LayoutParams(-1,dp(58)));
        p.addView(new View(this),new LinearLayout.LayoutParams(-1,0,1));

        LinearLayout n=new LinearLayout(this); n.setGravity(Gravity.CENTER); n.setPadding(dp(4),dp(3),dp(4),dp(3)); n.setBackground(bg(Color.rgb(7,19,33),dp(20)));
        n.addView(nav("⌂",english?"Home":"Ana Sayfa",BLUE),new LinearLayout.LayoutParams(0,dp(66),1));
        TextView ch=nav("◌",english?"Chats":"Sohbetler",MUTED); ch.setOnClickListener(v->showChats()); n.addView(ch,new LinearLayout.LayoutParams(0,dp(66),1));
        TextView pe=nav("♙",english?"People":"Kişiler",MUTED); pe.setOnClickListener(v->showContacts()); n.addView(pe,new LinearLayout.LayoutParams(0,dp(66),1));
        TextView se=nav("⚙",english?"Settings":"Ayarlar",MUTED); se.setOnClickListener(v->showSettings()); n.addView(se,new LinearLayout.LayoutParams(0,dp(66),1));
        p.addView(n,new LinearLayout.LayoutParams(-1,dp(72)));
        show(p);
    }

    private void showChats(){
        LinearLayout p=screen();header(p,english?"Chats":"Sohbetler",english?"Encrypted conversations":"Şifreli konuşmalar",v->showHome());
        space(p,12);
        if(contactIds.isEmpty()){LinearLayout empty=card();empty.setGravity(Gravity.CENTER);TextView e=text(english?"◌\nNo conversations yet\nAdd a NEXUS ID to start.":"◌\nHenüz sohbet yok\nBaşlamak için NEXUS ID ekleyin.",15,MUTED);e.setGravity(Gravity.CENTER);empty.addView(e,new LinearLayout.LayoutParams(-1,dp(130)));p.addView(empty,new LinearLayout.LayoutParams(-1,0,1));}
        else{
            LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);
            for(String id:contactIds){LinearLayout c=card();TextView a=text("●  "+id,16,TEXT);a.setTypeface(Typeface.DEFAULT,Typeface.BOLD);c.addView(a);c.addView(text(english?"Tap to open encrypted chat":"Şifreli sohbeti açmak için dokunun",12,MUTED));c.setOnClickListener(v->showChat(id));c.setOnLongClickListener(v->{confirmDeleteChat(id);return true;});list.addView(c,new LinearLayout.LayoutParams(-1,dp(78)));space(list,dp(8));}
            p.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        }
        Button add=button(english?"New secure chat":"Yeni güvenli sohbet");add.setOnClickListener(v->showIdEntry());p.addView(add,new LinearLayout.LayoutParams(-1,dp(56)));show(p);
    }

    private void confirmDeleteChat(String id){
        new android.app.AlertDialog.Builder(this)
                .setTitle(english?"Delete conversation":"Sohbeti sil")
                .setMessage(english?"Delete this contact and its local encrypted conversation?":"Bu kişiyi ve bu kişiyle olan yerel şifreli sohbeti tamamen silmek istiyor musun?")
                .setNegativeButton(english?"Cancel":"İptal",null)
                .setPositiveButton(english?"Delete":"Sil",(d,w)->deleteChat(id))
                .show();
    }

    private void deleteChat(String id){
        contactIds.remove(id);
        prefs.edit().putStringSet("contacts",new HashSet<>(contactIds))
                .remove("msg_secure_"+id)
                .remove("msg_"+id)
                .apply();
        if(id.equals(activePeer)){activePeer=null;messages=null;}
        showChats();
        toast(english?"Conversation deleted from this device":"Sohbet bu cihazdan tamamen silindi");
    }

    private void showContacts(){
        LinearLayout p=screen();header(p,english?"People":"Kişiler",english?"NEXUS ID contacts":"NEXUS ID rehberi",v->showHome());
        space(p,12);
        LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);
        if(contactIds.isEmpty()){LinearLayout empty=card();empty.setGravity(Gravity.CENTER);TextView e=text(english?"♙\nNo contacts yet\nAdd a NEXUS ID below.":"♙\nHenüz kişi yok\nAşağıdan bir NEXUS ID ekleyin.",15,MUTED);e.setGravity(Gravity.CENTER);empty.addView(e,new LinearLayout.LayoutParams(-1,dp(130)));list.addView(empty,new LinearLayout.LayoutParams(-1,dp(150)));space(list,8);}
        for(String id:contactIds){
            LinearLayout c=card();c.setOnClickListener(v->showChat(id));
            c.addView(text("♙  "+id,16,TEXT));c.addView(text(english?"Encrypted contact":"Uçtan uca şifreli kişi",12,MUTED));
            list.addView(c,new LinearLayout.LayoutParams(-1,dp(74)));space(list,8);
        }
        p.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        Button add=button(english?"Add NEXUS ID":"NEXUS ID Ekle");add.setOnClickListener(v->showIdEntry());p.addView(add,new LinearLayout.LayoutParams(-1,58));show(p);
    }

    private void showIdEntry(){
        LinearLayout p=screen();header(p,english?"Add NEXUS ID":"NEXUS ID Ekle",english?"Find a secure contact":"Güvenli kişi bul",v->showHome());space(p,60);
        EditText id=field("kullanici@nexus");p.addView(id,new LinearLayout.LayoutParams(-1,58));space(p,12);
        Button add=button(english?"Add contact":"Kişiyi Ekle");
        add.setOnClickListener(v->{String x=id.getText().toString().trim().toLowerCase();if(x.isEmpty()){id.setError(english?"NEXUS ID required":"NEXUS ID gerekli");return;}if(!x.contains("@"))x+="@nexus";addContact(x);showChat(x);});p.addView(add,new LinearLayout.LayoutParams(-1,58));
        space(p,18);
        LinearLayout q=card();
        TextView qt=text("▣  "+(english?"QR CONTACT EXCHANGE":"QR KİŞİ DEĞİŞİMİ"),18,TEXT);qt.setTypeface(Typeface.DEFAULT,Typeface.BOLD);q.addView(qt);
        q.addView(text(english?"Show your NEXUS ID as a QR code or scan another device.":"NEXUS ID'nizi QR kod olarak gösterin veya diğer cihazın QR kodunu okutun.",15,MUTED));
        space(q,dp(12));
        LinearLayout qrRow=new LinearLayout(this);qrRow.setGravity(Gravity.CENTER);
        Button showQr=button(english?"Show my QR":"QR Kodumu Göster");
        Button scanQr=button(english?"Scan QR":"QR Kod Tara");
        showQr.setOnClickListener(v->showMyQr());
        scanQr.setOnClickListener(v->scanNexusQr());
        qrRow.addView(showQr,new LinearLayout.LayoutParams(0,dp(54),1));
        qrRow.addView(new View(this),new LinearLayout.LayoutParams(dp(10),1));
        qrRow.addView(scanQr,new LinearLayout.LayoutParams(0,dp(54),1));
        q.addView(qrRow);
        p.addView(q);
        p.addView(new View(this),new LinearLayout.LayoutParams(-1,0,1));p.addView(text("🔒 "+(english?"Messages are encrypted on the device.":"Mesajlar cihaz üzerinde şifrelenir."),13,MUTED));show(p);
    }

    private void showMyQr(){
        LinearLayout p=screen();
        header(p,english?"My NEXUS QR":"NEXUS QR Kodum",english?"Scan this code on the other device":"Diğer cihazdan bu kodu okutun",v->showIdEntry());
        space(p,20);
        LinearLayout box=card();box.setGravity(Gravity.CENTER);
        TextView idText=text(localId,18,BLUE);idText.setTypeface(Typeface.DEFAULT,Typeface.BOLD);idText.setGravity(Gravity.CENTER);box.addView(idText,new LinearLayout.LayoutParams(-1,dp(40)));
        ImageView qr=new ImageView(this);qr.setScaleType(ImageView.ScaleType.CENTER_INSIDE);qr.setPadding(dp(12),dp(12),dp(12),dp(12));
        try{
            BitMatrix matrix=new MultiFormatWriter().encode(localId,BarcodeFormat.QR_CODE,dp(280),dp(280));
            Bitmap bitmap=Bitmap.createBitmap(matrix.getWidth(),matrix.getHeight(),Bitmap.Config.ARGB_8888);
            for(int x=0;x<matrix.getWidth();x++) for(int y=0;y<matrix.getHeight();y++) bitmap.setPixel(x,y,matrix.get(x,y)?Color.BLACK:Color.WHITE);
            qr.setImageBitmap(bitmap);
        }catch(Exception e){ toast(english?"QR generation failed":"QR oluşturulamadı"); }
        box.addView(qr,new LinearLayout.LayoutParams(-1,dp(320)));
        p.addView(box,new LinearLayout.LayoutParams(-1,dp(380)));
        p.addView(new View(this),new LinearLayout.LayoutParams(-1,0,1));
        Button scan=button(english?"Scan another device":"Diğer cihazın QR Kodunu Tara");scan.setOnClickListener(v->scanNexusQr());p.addView(scan,new LinearLayout.LayoutParams(-1,dp(56)));
        show(p);
    }

    private void scanNexusQr(){
        if(qrScannerLauncher==null){toast(english?"QR scanner is not ready":"QR tarayıcı hazır değil");return;}
        ScanOptions options=new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        options.setPrompt(english?"Point the camera at a NEXUS QR code":"Kamerayı NEXUS QR koduna doğrultun");
        options.setBeepEnabled(false);
        options.setOrientationLocked(true);
        qrScannerLauncher.launch(options);
    }

    private void addContact(String id){
        if(!contactIds.contains(id)){contactIds.add(id);prefs.edit().putStringSet("contacts",new HashSet<>(contactIds)).apply();}
    }

    private void showChat(String id){
        activePeer=id;LinearLayout p=screen();header(p,id,"● "+(english?"Secure":"Güvenli"),v->showChats());
        connectionStatus=text(relayConnected?(english?"● Secure relay connected • E2EE ready":"● Güvenli relay bağlı • E2EE hazır"):(english?"Connecting to secure relay...":"Güvenli relay bağlantısı kuruluyor..."),11,relayConnected?GREEN:MUTED);connectionStatus.setGravity(Gravity.CENTER);p.addView(connectionStatus);
        messages=new LinearLayout(this);messages.setOrientation(LinearLayout.VERTICAL);p.addView(messages,new LinearLayout.LayoutParams(-1,0,1));
        loadLocalMessages(id);
        markPeerRead(id);
        LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setPadding(6,6,6,6);bar.setBackground(bg(PANEL,22));
        EditText input=field(english?"Message...":"Mesaj yaz...");bar.addView(input,new LinearLayout.LayoutParams(0,dp(54),1));
        TextView send=text("➤",22,TEXT);send.setGravity(Gravity.CENTER);send.setBackground(bg(BLUE,30));bar.addView(send,new LinearLayout.LayoutParams(dp(54),dp(54)));
        send.setOnClickListener(v->{String msg=input.getText().toString().trim();if(msg.isEmpty())return;input.setText("");if(e2ee!=null){try{String messageId=e2ee.sendText(id,1,msg);appendOutgoingMessage(msg,messageId);saveMessage(id,msg,true);}catch(Exception ex){toast("E2EE: "+ex.getMessage());}}else{appendOutgoingMessage(msg,null);saveMessage(id,msg,true);}});
        p.addView(bar);show(p);
    }

    private void appendMessage(String s,boolean mine){
        TextView m=text(s,17,TEXT); m.setGravity(Gravity.CENTER_VERTICAL); m.setPadding(dp(16),dp(11),dp(16),dp(11));
        GradientDrawable bubble=bg(mine?BLUE:Color.rgb(18,38,61),dp(20));
        bubble.setStroke(dp(1),mine?Color.rgb(54,164,255):Color.rgb(31,76,117)); m.setBackground(bubble);
        m.setElevation(dp(2));
        LinearLayout.LayoutParams q=new LinearLayout.LayoutParams(-2,-2); q.gravity=mine?Gravity.RIGHT:Gravity.LEFT; q.topMargin=dp(8); q.leftMargin=mine?dp(48):dp(4); q.rightMargin=mine?dp(4):dp(48); messages.addView(m,q);
    }

    private void appendOutgoingMessage(String s,String messageId){
        LinearLayout wrap=new LinearLayout(this);wrap.setOrientation(LinearLayout.VERTICAL);wrap.setGravity(Gravity.RIGHT);
        TextView body=text(s,17,TEXT);body.setGravity(Gravity.CENTER_VERTICAL);body.setPadding(dp(16),dp(11),dp(16),dp(3));
        GradientDrawable bubble=bg(BLUE,dp(20));bubble.setStroke(dp(1),Color.rgb(54,164,255));body.setBackground(bubble);
        TextView state=text("Gönderiliyor",11,Color.rgb(210,225,245));state.setGravity(Gravity.RIGHT);state.setPadding(dp(10),0,dp(12),dp(8));
        wrap.addView(body,new LinearLayout.LayoutParams(-2,-2));wrap.addView(state,new LinearLayout.LayoutParams(-2,-2));
        LinearLayout.LayoutParams q=new LinearLayout.LayoutParams(-2,-2);q.gravity=Gravity.RIGHT;q.topMargin=dp(8);q.leftMargin=dp(48);q.rightMargin=dp(4);messages.addView(wrap,q);
        if(messageId!=null)deliveryViews.put(messageId,state);
    }

    private void updateDelivery(String id,String status){
        TextView v=deliveryViews.get(id);if(v==null)return;
        String label=status;
        if("queued".equals(status)||"relayed".equals(status)) label="✓ Relay'e ulaştı";
        else if("delivered".equals(status)) label="✓✓ Teslim edildi";
        else if("read".equals(status)) label="✓✓ Okundu";
        else if("offline".equals(status)) label="Bekliyor • karşı cihaz çevrimdışı";
        else if("queue_full".equals(status)) label="Kuyruk dolu • tekrar denenecek";
        v.setText(label);
        v.setTextColor("read".equals(status)?BLUE:Color.rgb(210,225,245));
        if("delivered".equals(status)||"read".equals(status)) v.setTextColor(BLUE);
    }

    private SecretKey localMessageKey() throws Exception{
        KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);
        if(!ks.containsAlias("nexus_local_messages")){
            KeyGenerator kg=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
            kg.init(new KeyGenParameterSpec.Builder("nexus_local_messages",KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build());
            kg.generateKey();
        }
        return ((KeyStore.SecretKeyEntry)ks.getEntry("nexus_local_messages",null)).getSecretKey();
    }
    private String encryptLocalMessages(String plain) throws Exception{
        byte[] iv=new byte[12];new java.security.SecureRandom().nextBytes(iv);
        Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,localMessageKey(),new GCMParameterSpec(128,iv));
        byte[] out=c.doFinal(plain.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeToString(iv,Base64.NO_WRAP)+"."+Base64.encodeToString(out,Base64.NO_WRAP);
    }
    private String decryptLocalMessages(String blob) throws Exception{
        String[] p=blob.split("\\.",2);if(p.length!=2)throw new IllegalArgumentException("invalid local message blob");
        byte[] iv=Base64.decode(p[0],Base64.NO_WRAP),data=Base64.decode(p[1],Base64.NO_WRAP);
        Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,localMessageKey(),new GCMParameterSpec(128,iv));
        return new String(c.doFinal(data),StandardCharsets.UTF_8);
    }
    private void saveMessage(String peer,String msg,boolean mine){
        saveMessage(peer,msg,mine,null);
    }

    private void saveMessage(String peer,String msg,boolean mine,String messageId){
        try{
            String key="msg_secure_"+peer;
            String encrypted=prefs.getString(key,"");
            String plain=encrypted.isEmpty()?"":decryptLocalMessages(encrypted);
            String encoded=Base64.encodeToString(msg.getBytes(StandardCharsets.UTF_8),Base64.NO_WRAP);
            String record;
            if(!mine && messageId!=null && !messageId.isEmpty()) record="0:"+messageId+":"+encoded;
            else record=(mine?"1":"0")+":"+encoded;
            plain=plain.isEmpty()?record:plain+"\\n"+record;
            prefs.edit().putString(key,encryptLocalMessages(plain)).remove("msg_"+peer).apply();
        }catch(Exception e){
            toast("Yerel güvenli kayıt hatası: "+e.getClass().getSimpleName());
        }
    }

    private void addUnread(String peer,String messageId){
        if(messageId==null||messageId.isEmpty())return;
        String key="unread_"+peer;
        Set<String> ids=new HashSet<>(prefs.getStringSet(key,new HashSet<>()));
        ids.add(messageId);
        prefs.edit().putStringSet(key,ids).apply();
    }

    private void markPeerRead(String peer){
        if(peer==null||e2ee==null||!relayConnected)return;
        String key="unread_"+peer;
        Set<String> ids=new HashSet<>(prefs.getStringSet(key,new HashSet<>()));
        if(ids.isEmpty())return;
        for(String id:ids)e2ee.markRead(id,peer);
        prefs.edit().remove(key).apply();
    }
    private void loadLocalMessages(String peer){
        try{
            String secure=prefs.getString("msg_secure_"+peer,"");
            if(!secure.isEmpty()){
                for(String x:decryptLocalMessages(secure).split("\\n")){
                    int i=x.indexOf(':');if(i>0)appendMessage(new String(Base64.decode(x.substring(i+1),Base64.NO_WRAP),StandardCharsets.UTF_8),x.charAt(0)=='1');
                }
                return;
            }
            Set<String> legacy=prefs.getStringSet("msg_"+peer,new HashSet<>());
            if(!legacy.isEmpty()){
                for(String x:legacy){int i=x.indexOf('|');if(i>0)appendMessage(x.substring(i+1),x.charAt(0)=='1');}
                for(String x:legacy){int i=x.indexOf('|');if(i>0)saveMessage(peer,x.substring(i+1),x.charAt(0)=='1');}
                prefs.edit().remove("msg_"+peer).apply();
            }
        }catch(Exception e){toast("Yerel güvenli kayıt açılamadı");}
    }

    private void showPrivacy(){
        LinearLayout p=screen();header(p,english?"Privacy & Security":"Gizlilik ve Güvenlik",english?"Device security center":"NEXUS güvenlik merkezi",v->showHome());
        space(p,12);
        LinearLayout a=card();a.addView(text("🔒  "+(english?"End-to-end encryption":"Uçtan uca şifreleme"),17,TEXT));a.addView(text(english?"Signal protocol + encrypted persistent state":"Signal protokolü + şifreli kalıcı oturum deposu",13,MUTED));p.addView(a);
        space(p,10);LinearLayout b=card();b.addView(text("✓  "+(english?"Device identity":"Cihaz kimliği"),17,TEXT));b.addView(text(localId,12,MUTED));p.addView(b);
        space(p,10);LinearLayout c=card();c.addView(text("●  "+(english?"Connection":"Bağlantı"),17,TEXT));c.addView(text(e2ee==null?(english?"Not connected":"Bağlı değil"):(english?"Relay runtime ready":"Relay motoru hazır"),13,e2ee==null?MUTED:GREEN));p.addView(c);
        p.addView(new View(this),new LinearLayout.LayoutParams(-1,0,1));Button devices=button(english?"Reset local secure state":"Yerel güvenli durumu sıfırla");devices.setOnClickListener(v->{prefs.edit().clear().apply();toast(english?"Local data cleared":"Yerel veriler temizlendi");showHome();});p.addView(devices,new LinearLayout.LayoutParams(-1,58));show(p);
    }

    private void showSettings(){
        LinearLayout p=screen();header(p,english?"Settings":"Ayarlar",english?"NEXUS configuration":"NEXUS yapılandırması",v->showHome());space(p,12);
        LinearLayout id=card();id.addView(text("NEXUS ID",15,TEXT));id.addView(text(localId,14,BLUE));p.addView(id);space(p,10);
        EditText relay=field("");relay.setText(prefs.getString("relay","wss://nexus-relay-0dd3.onrender.com"));relay.setHint("wss://...");p.addView(relay,new LinearLayout.LayoutParams(-1,58));space(p,8);
        Button connect=button(english?"Save & connect relay":"Kaydet ve relay'e bağlan");connect.setOnClickListener(v->{String url=relay.getText().toString().trim();prefs.edit().putString("relay",url).apply();connectRelay(url);});p.addView(connect,new LinearLayout.LayoutParams(-1,58));space(p,10);
        Button lang=button(english?"Türkçe":"English");lang.setOnClickListener(v->{english=!english;prefs.edit().putBoolean("english",english).apply();showSettings();});p.addView(lang,new LinearLayout.LayoutParams(-1,58));
        p.addView(new View(this),new LinearLayout.LayoutParams(-1,0,1));p.addView(text(english?"Notifications, device identity, contacts and encrypted message state are persisted locally.":"Bildirimler, cihaz kimliği, kişiler ve şifreli mesaj durumu cihazda saklanır.",12,MUTED));show(p);
    }

    private void showNotifications(){LinearLayout p=screen();header(p,english?"Notifications":"Bildirimler",english?"Message alerts":"Mesaj bildirimleri",v->showHome());space(p,20);TextView t=text(english?"Notifications are enabled by Android permission.\nNew encrypted messages are surfaced here.":"Bildirimler Android izni ile çalışır.\nYeni şifreli mesajlar burada gösterilir.",15,MUTED);t.setGravity(Gravity.CENTER);p.addView(t,new LinearLayout.LayoutParams(-1,0,1));show(p);}

    private void connectRelay(String url){
        if(!url.startsWith("wss://")){toast(english?"Relay must use WSS/TLS":"Relay WSS/TLS kullanmalı");return;}
        try{
            if(e2ee!=null){try{e2ee=null;}catch(Exception ignored){}}
            e2ee=new E2eeRuntime(this,localId,(new E2eeRuntime.Listener(){
                public void onReady(){relayConnected=true;runOnUiThread(()->{if(connectionStatus!=null){connectionStatus.setText("●  "+(english?"Secure relay connected • E2EE ready":"Güvenli relay bağlı • E2EE hazır"));connectionStatus.setTextColor(GREEN);}toast(english?"NEXUS secure relay connected":"NEXUS güvenli relay bağlandı");});}
                public void onMessage(String from,String message,String id){runOnUiThread(()->{addContact(from);boolean open=activePeer!=null&&activePeer.equals(from)&&messages!=null;if(open){appendMessage(message,false);saveMessage(from,message,false,id);if(e2ee!=null)e2ee.markRead(id,from);}else{saveMessage(from,message,false,id);addUnread(from,id);}notifyIncoming(from);});}
                public void onDelivery(String id,String status){runOnUiThread(()->updateDelivery(id,status));}
                public void onClosed(){relayConnected=false;runOnUiThread(()->{if(connectionStatus!=null){connectionStatus.setText(english?"Reconnecting securely...":"Güvenli bağlantı yeniden kuruluyor...");connectionStatus.setTextColor(MUTED);}});}
                public void onError(String m){final String msg=(m==null?"Relay connection failed":m); final String low=msg.toLowerCase(java.util.Locale.ROOT); if(low.contains("software caused connection abort")||low.contains("connection abort")||low.contains("connection reset")||low.contains("broken pipe")||low.contains("canceled"))return; runOnUiThread(()->{if(connectionStatus!=null){connectionStatus.setText((english?"Relay error: ":"Relay hatası: ")+msg);connectionStatus.setTextColor(RED);}toast("Relay: "+msg);});}
            }));
            e2ee.connect(url);
        }catch(Exception ex){toast("E2EE: "+ex.getMessage());}
    }

    private static final String EXTRA_NOTIFICATION_PEER="nexus_notification_peer";

    private void openNotificationChat(Intent intent){
        if(intent==null)return;
        String peer=intent.getStringExtra(EXTRA_NOTIFICATION_PEER);
        if(peer==null||peer.trim().isEmpty())return;
        peer=peer.trim().toLowerCase(java.util.Locale.ROOT);
        if(!peer.endsWith("@nexus"))return;
        final String target=peer;
        runOnUiThread(()->{
            addContact(target);
            showChat(target);
        });
    }

    private void notifyIncoming(String from){
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
        NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);String ch="messages";
        if(Build.VERSION.SDK_INT>=26){NotificationChannel c=new NotificationChannel(ch,"NEXUS Messages",NotificationManager.IMPORTANCE_DEFAULT);c.setDescription("NEXUS güvenli mesaj bildirimleri");nm.createNotificationChannel(c);}
        Intent intent=new Intent(this,MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(EXTRA_NOTIFICATION_PEER,from);
        int requestCode=(from==null?0:from.hashCode())&0x7fffffff;
        PendingIntent pi=PendingIntent.getActivity(this,requestCode,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder n=new NotificationCompat.Builder(this,ch)
                .setSmallIcon(android.R.drawable.ic_dialog_email)
                .setContentTitle("NEXUS")
                .setContentText(english?"New secure message":"Yeni güvenli mesaj")
                .setSubText("E2EE")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .setOnlyAlertOnce(true);
        int id=requestCode;
        nm.notify(id,n.build());
    }

    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);Window w=getWindow();w.setStatusBarColor(BG);w.setNavigationBarColor(BG);
        prefs=getSharedPreferences("nexus_app",MODE_PRIVATE);english=prefs.getBoolean("english",false);
        String androidId=Settings.Secure.getString(getContentResolver(),Settings.Secure.ANDROID_ID);
        if(androidId==null||androidId.length()<12) androidId=UUID.randomUUID().toString().replace("-","");
        localId="nx-"+androidId.substring(0,12).toLowerCase(java.util.Locale.ROOT)+"@nexus";
        prefs.edit().putString("localId",localId).apply();
        contactIds.addAll(prefs.getStringSet("contacts",new HashSet<>()));
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},100);
        qrScannerLauncher=registerForActivityResult(new ScanContract(), result->{
            String value=result.getContents();
            if(value==null||value.trim().isEmpty()) return;
            String id=value.trim().toLowerCase(java.util.Locale.ROOT);
            if(!id.endsWith("@nexus")){ toast(english?"Invalid NEXUS QR code":"Geçersiz NEXUS QR kodu"); return; }
            addContact(id); showChat(id);
        });
        ScrollView sv=new ScrollView(this);sv.setBackgroundColor(BG);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);sv.addView(root);setContentView(sv);showHome();
        String relay=prefs.getString("relay","");if(!"wss://nexus-relay-0dd3.onrender.com".equals(relay)){relay="wss://nexus-relay-0dd3.onrender.com";prefs.edit().putString("relay",relay).apply();}connectRelay(relay);
        openNotificationChat(getIntent());
    }

    @Override protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);
        setIntent(intent);
        openNotificationChat(intent);
    }
}
