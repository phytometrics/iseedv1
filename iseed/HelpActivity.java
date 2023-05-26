package jp.phytometrics.iseed;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class HelpActivity extends AppCompatActivity {

    private Button mButtonGoBack;
    private Button mPrivacyPolicy;
    private WebView helpView;
    private String helpURL = "file:///android_asset/help/manual.html";
    private String ppURL = "file:///android_asset/help/seed_counter_privacy_policy.html";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.help_activity);

        helpView = findViewById(R.id.helpView);
        helpView.loadUrl(helpURL);

        mButtonGoBack = findViewById(R.id.goBackButton);
        mButtonGoBack.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //Intent intent = new Intent(HelpActivity.this, MainActivity.class);
                //startActivity(intent);
                finish();
            }
        });
        mPrivacyPolicy = findViewById(R.id.privacypolicyButton);
        mPrivacyPolicy.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                helpView.loadUrl(ppURL);
            }
        });


    }
}
