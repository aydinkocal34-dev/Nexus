package com.nexus.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        );

        TextView view = new TextView(this);
        view.setText("NEXUS\n\nTEST BUILD\n\nSecure messaging foundation");
        view.setTextSize(22);
        view.setPadding(40, 60, 40, 40);

        setContentView(view);
    }
}
