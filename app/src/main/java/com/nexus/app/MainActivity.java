package com.nexus.app;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.lang.reflect.Method;

public class MainActivity extends Activity {
    private static final int BG=Color.rgb(4,13,25), CARD=Color.rgb(12,28,48), CARD2=Color.rgb(18,39,65);
    private static final int TEXT=Color.WHITE, MUTED=Color.rgb(166,184,207), BLUE=Color.rgb(18,139,255);
    private LinearLayout root, content;
    private final List<String> messages=new ArrayList<>();
    private String activePeer="";
    private String localPeerId="";
    private String relayWss="wss://nexus-blind-relay.onrender.com";
    private TextView connectionStatus;
    private Object e2eeRuntime;
    private Method e2eeConnectMethod;
    private Method e2eeSendMethod;
    private final android.os.Handler handler=new android.os.Handler();
    private static final String PREFS="nexus_runtime";
    private static final String CHANNEL="nexus_messages";

    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
    private TextView text(String s,float size,int color,boolean bold){
        TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        t.setTypeface(bold?Typeface.DEFAULT_BOLD:Typeface.DEFAULT); t.setGravity(Gravity.CENTER_VERTICAL); return t;
    }
    private GradientDrawable bg(int color,float radius){
        GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }
    private View iconText(String icon,String title,String sub){
        LinearLayout box=new LinearLayout(this); box.setGravity(Gravity.CENTER_VERTICAL); box.setPadding(dp(12),dp(10),dp(12),dp(10));
        TextView i=text(icon,25,TEXT,true); i.setGravity(Gravity.CENTER); i.setBackground(bg(Color.rgb(16,67,111),18));
        box.addView(i,new LinearLayout.LayoutParams(dp(48),dp(48)));
        LinearLayout tx=new LinearLayout(this); tx.setOrientation(LinearLayout.VERTICAL); tx.setPadding(dp(12),0,0,0);
        tx.addView(text(title,15,TEXT,true)); tx.addView(text(sub,12,MUTED,false));
        box.addView(tx,new LinearLayout.LayoutParams(0,-2,1)); return box;
    }
    private Button button(String s){
        Button b=new Button(this); b.setText(s); b.setTextColor(TEXT); b.setTextSize(15); b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setAllCaps(false); b.setGravity(Gravity.CENTER); b.setBackground(bg(BLUE,18)); return b;
    }
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,WindowManager.LayoutParams.FLAG_SECURE);
        android.content.SharedPreferences p=getSharedPreferences(PREFS,MODE_PRIVATE);
        localPeerId=p.getString("peerId",null);
        if(localPeerId==null){
            localPeerId="NX-"+UUID.randomUUID().toString().replace("-","").substring(0,12).toUpperCase();
            p.edit().putString("peerId",localPeerId).apply();
        }
        relayWss=p.getString("relayWss","wss://nexus-blind-relay.onrender.com");
        createNotificationChannel();
        showHome();
    }
    private boolean ensureTransport(){
        if(e2eeRuntime!=null) return true;
        try {
            Class<?> bridge=Class.forName("com.nexus.app.E2eeBridge",true,getClassLoader());
            Class<?> listener=Class.forName("com.nexus.app.E2eeBridge$Listener",true,getClassLoader());
            Object proxy=java.lang.reflect.Proxy.newProxyInstance(listener.getClassLoader(),new Class<?>[]{listener},
                (obj,method,args)->{
                    String n=method.getName();
                    if("onReady".equals(n)) runOnUiThread(()->{if(connectionStatus!=null)connectionStatus.setText("● GÜVENLİ BAĞLI");});
                    else if("onMessage".equals(n)&&args!=null&&args.length>=3) runOnUiThread(()->onIncomingMessage(String.valueOf(args[0]),String.valueOf(args[1]),String.valueOf(args[2])));
                    else if("onError".equals(n)&&args!=null&&args.length>=1) runOnUiThread(()->onTransportError(String.valueOf(args[0])));
                    return null;
                });
            java.lang.reflect.Constructor<?> ctor=bridge.getDeclaredConstructor(android.content.Context.class,String.class,listener);
            ctor.setAccessible(true);
            e2eeRuntime=ctor.newInstance(this,localPeerId,proxy);
            e2eeConnectMethod=bridge.getDeclaredMethod("connect",String.class);
            e2eeSendMethod=bridge.getDeclaredMethod("sendText",String.class,int.class,String.class);
            return true;
        } catch(Throwable e) {
            if(connectionStatus!=null) connectionStatus.setText("● E2EE HAZIR DEĞİL");
            String detail=e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();
            android.widget.Toast.makeText(this,"E2EE başlatılamadı: "+detail,android.widget.Toast.LENGTH_LONG).show();
            e2eeRuntime=null; e2eeConnectMethod=null; e2eeSendMethod=null;
            return false;
        }
    }
    private void connectRelay(){
        if(ensureTransport() && !relayWss.isEmpty()){
            try { e2eeConnectMethod.invoke(e2eeRuntime,relayWss); }
            catch(Throwable e){ onTransportError(String.valueOf(e.getCause()==null?e.getMessage():e.getCause().getMessage())); }
        }
    }
    private void onIncomingMessage(String from,String message,String id){
        messages.add(message); notifyNewMessage(from); if(from.equals(activePeer))showChat(from);
        handler.postDelayed(()->{messages.remove(message);if(from.equals(activePeer))showChat(from);},60000L);
    }
    private void onTransportError(String message){
        if(connectionStatus!=null)connectionStatus.setText("● BAĞLANTI HATASI");
        android.widget.Toast.makeText(this,message,android.widget.Toast.LENGTH_LONG).show();
    }

    private void base(String title){
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(BG);
        LinearLayout bar=new LinearLayout(this); bar.setGravity(Gravity.CENTER_VERTICAL); bar.setPadding(dp(14),dp(8),dp(14),dp(4));
        TextView back=text("‹",34,TEXT,false); back.setGravity(Gravity.CENTER);
        back.setVisibility(title.equals("NEXUS")?View.GONE:View.VISIBLE); back.setOnClickListener(v->showHome());
        bar.addView(back,new LinearLayout.LayoutParams(dp(42),dp(52)));
        TextView t=text(title,22,TEXT,true); bar.addView(t,new LinearLayout.LayoutParams(0,dp(52),1));
        connectionStatus=text(title.equals("NEXUS")?"● BAĞLANIYOR":"● ÇEVRİMİÇİ",11,title.equals("NEXUS")?MUTED:Color.rgb(48,225,130),true);
        bar.addView(connectionStatus);
        root.addView(bar);
        ScrollView scroll=new ScrollView(this); content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16),dp(4),dp(16),dp(18)); scroll.addView(content);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1)); setContentView(root);
    }
    private void add(View v,int bottom){
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.setMargins(0,0,0,dp(bottom)); content.addView(v,p);
    }
    private void card(View v){
        LinearLayout c=new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); c.setBackground(bg(CARD,18));
        c.setPadding(dp(14),dp(12),dp(14),dp(12)); add(c,12); c.addView(v);
    }
    private void sectionTitle(String s){add(text(s,17,TEXT,true),10);}
    private void nav(){
        LinearLayout nav=new LinearLayout(this); nav.setGravity(Gravity.CENTER); nav.setPadding(dp(6),dp(4),dp(6),dp(4)); nav.setBackgroundColor(Color.rgb(5,17,31));
        String[] labels={"⌂\nAna Sayfa","◌\nSohbetler","♙\nKişiler","⚙\nAyarlar"};
        for(String s:labels){TextView n=text(s,11,s.startsWith("⌂")?BLUE:MUTED,false); n.setGravity(Gravity.CENTER); nav.addView(n,new LinearLayout.LayoutParams(0,dp(60),1));}
        root.addView(nav);
    }
    private void showHome(){
        base("NEXUS");

        LinearLayout hero=new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.CENTER);
        hero.setPadding(0,dp(12),0,dp(10));

        ImageView logo=new ImageView(this);
        logo.setImageResource(com.nexus.app.R.drawable.nexus_logo);
        logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        hero.addView(logo,new LinearLayout.LayoutParams(-1,dp(112)));

        TextView tagline=text("Güvenli İletişim\nGüvenli Gelecek",16,TEXT,true);
        tagline.setGravity(Gravity.CENTER);
        hero.addView(tagline);

        TextView hs=text(localPeerId,12,MUTED,false);
        hs.setGravity(Gravity.CENTER);
        hero.addView(hs);

        add(hero,14);

        LinearLayout grid=new LinearLayout(this); grid.setOrientation(LinearLayout.VERTICAL);
        LinearLayout r1=new LinearLayout(this); r1.setWeightSum(2);
        View chat=iconText("◉","Sohbet","Güvenli mesajlaş");
        View people=iconText("♙","Kişiler","NEXUS ID ekle");
        r1.addView(chat,new LinearLayout.LayoutParams(0,dp(92),1)); r1.addView(people,new LinearLayout.LayoutParams(0,dp(92),1));
        grid.addView(r1); add(grid,10);
        chat.setOnClickListener(v->showChat(activePeer.isEmpty()?"Yeni sohbet":activePeer));
        people.setOnClickListener(v->showNewChat());

        LinearLayout r2=new LinearLayout(this); r2.setWeightSum(2);
        r2.addView(iconText("◇","Gizlilik","Tam şifreleme"),new LinearLayout.LayoutParams(0,dp(92),1));
        View settings=iconText("⚙","Ayarlar","Uygulama ayarları");
        r2.addView(settings,new LinearLayout.LayoutParams(0,dp(92),1));
        settings.setOnClickListener(v->showSettings());
        grid.addView(r2); add(grid,12);

        card(iconText("▣","Verileriniz güvende","Uçtan uca şifreleme ile sadece sizin kontrolünüzde."));

        Button b=button("+  Yeni güvenli sohbet");
        b.setOnClickListener(v->showNewChat());
        add(b,12);

        sectionTitle("Sohbetler");
        card(text(messages.isEmpty()?"Henüz sohbet yok\nBir NEXUS ID ekleyerek başlayın.":activePeer+"\nSon mesaj: şifreli",14,MUTED,false));
        nav();
    }
    private void showNewChat(){
        base("Yeni Güvenli Sohbet");
        card(iconText("⌕","NEXUS ID ile Ekle","Karşı tarafın NEXUS ID'si ile sohbet başlat"));
        card(iconText("▦","QR Kod ile Ekle","Karşı tarafın QR kodunu tara"));
        sectionTitle("NEXUS ID");
        EditText id=new EditText(this); id.setHint("kullanici@nexus"); id.setHintTextColor(Color.rgb(100,125,150)); id.setTextColor(TEXT);
        id.setSingleLine(true); id.setTextSize(16); id.setPadding(dp(16),0,dp(16),0); id.setBackground(bg(Color.rgb(7,20,35),18));
        add(id,12);
        Button search=button("Ara"); search.setOnClickListener(v->{String peer=id.getText().toString().trim();if(peer.length()<3){id.setError("NEXUS ID gir");return;}showFound(peer);}); add(search,12);
        TextView qr=text("Veya QR Kod Tara",14,Color.rgb(25,166,255),false); qr.setGravity(Gravity.CENTER); add(qr,12);
    }
    private void showFound(String peer){
        base("Kişi Bulundu");
        LinearLayout profile=new LinearLayout(this); profile.setOrientation(LinearLayout.VERTICAL); profile.setGravity(Gravity.CENTER); profile.setPadding(0,dp(24),0,dp(20));
        TextView avatar=text(peer.substring(0,1).toUpperCase(),42,TEXT,true); avatar.setGravity(Gravity.CENTER); avatar.setBackground(bg(BLUE,100));
        profile.addView(avatar,new LinearLayout.LayoutParams(dp(110),dp(110)));
        TextView name=text(peer,22,TEXT,true); name.setGravity(Gravity.CENTER); profile.addView(name);
        TextView online=text("●  Çevrimiçi",14,Color.rgb(45,225,130),false); online.setGravity(Gravity.CENTER); profile.addView(online);
        add(profile,12);
        card(iconText("♧","Uçtan uca şifreleme","Bu sohbet sadece sizin ve karşı tarafın cihazında okunabilir."));
        Button start=button("Sohbete Başla"); start.setOnClickListener(v->{activePeer=peer;showChat(peer);}); add(start,12);
    }
    private void showSettings(){
        base("Ayarlar");
        card(text("NEXUS ID\n"+localPeerId,14,TEXT,false));
        sectionTitle("WSS Relay");
        EditText url=new EditText(this); url.setHint("wss://sunucu-adresi"); url.setHintTextColor(Color.rgb(100,125,150)); url.setTextColor(TEXT);
        url.setSingleLine(true); url.setText(relayWss); url.setBackground(bg(Color.rgb(7,20,35),18)); add(url,12);
        Button save=button("WSS Relay'e Bağlan");
        save.setOnClickListener(v->{String value=url.getText().toString().trim();if(!value.startsWith("wss://")){url.setError("wss:// adresi gerekli");return;}relayWss=value;
            getSharedPreferences(PREFS,MODE_PRIVATE).edit().putString("relayWss",value).apply();
            if(e2eeRuntime!=null){ try { e2eeConnectMethod.invoke(e2eeRuntime,value); } catch(Throwable ignored){} } showHome();});
        add(save,12);
        card(text("Bildirimler yalnızca “Yeni mesajınız var” bilgisini gösterir; mesaj metni bildirimde yer almaz.",13,MUTED,false));
    }
    private void createNotificationChannel(){
        NotificationManager nm=getSystemService(NotificationManager.class);
        if(nm!=null) nm.createNotificationChannel(new NotificationChannel(CHANNEL,"NEXUS Mesajları",NotificationManager.IMPORTANCE_HIGH));
    }
    private void notifyNewMessage(String from){
        if(android.os.Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},42); return;
        }
        NotificationCompat.Builder n=new NotificationCompat.Builder(this,CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("🔒 NEXUS").setContentText("Yeni mesajınız var")
            .setSubText("Gönderen: "+from).setAutoCancel(true).setPriority(NotificationCompat.PRIORITY_HIGH);
        NotificationManagerCompat.from(this).notify(Math.abs(from.hashCode()),n.build());
    }
    private void showChat(String peer){
        activePeer=peer; base(peer);
        for(String m:messages){
            LinearLayout row=new LinearLayout(this); row.setGravity(Gravity.RIGHT);
            TextView bubble=text(m,15,TEXT,false); bubble.setPadding(dp(14),dp(10),dp(14),dp(10)); bubble.setBackground(bg(BLUE,18));
            row.addView(bubble,new LinearLayout.LayoutParams(-2,-2)); add(row,8);
        }
        if(messages.isEmpty()){
            TextView empty=text("🔒  Uçtan uca şifreli\nMesajlar yalnızca iki cihazda okunabilir.",14,MUTED,false);
            empty.setGravity(Gravity.CENTER); add(empty,12);
        }
        LinearLayout composer=new LinearLayout(this); composer.setGravity(Gravity.CENTER_VERTICAL); composer.setPadding(dp(8),dp(6),dp(8),dp(6)); composer.setBackground(bg(CARD,24));
        EditText input=new EditText(this); input.setHint("Mesaj yaz..."); input.setHintTextColor(Color.rgb(100,125,150)); input.setTextColor(TEXT); input.setSingleLine(true);
        composer.addView(input,new LinearLayout.LayoutParams(0,dp(52),1));
        Button send=button("➤"); composer.addView(send,new LinearLayout.LayoutParams(dp(60),dp(52)));
        root.addView(composer,new LinearLayout.LayoutParams(-1,dp(64)));
        send.setOnClickListener(v->{String msg=input.getText().toString().trim();if(msg.isEmpty())return;
            if(!ensureTransport()) return;
            connectRelay();
            try { e2eeSendMethod.invoke(e2eeRuntime,peer,1,msg); messages.add(msg);input.setText("");showChat(peer); } catch(Throwable e){ onTransportError(String.valueOf(e.getCause()==null?e.getMessage():e.getCause().getMessage())); }});
    }
}