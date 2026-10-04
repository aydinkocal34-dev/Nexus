package com.nexus.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.TextView;

public final class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        TextView root = new TextView(this);
        root.setText("NEXUS\n\nUygulama başarıyla açıldı.");
        root.setTextColor(Color.WHITE);
        root.setTextSize(24f);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.rgb(4, 13, 25));

        setContentView(root);
    }
}