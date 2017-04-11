package mig0.bosheculogger.activity;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;

import java.util.Locale;

// API level 24 changed ActionBarActivity to AppCompatActivity
public class BaseActivity extends AppCompatActivity {
    protected String LOG_TAG = getClass().getSimpleName();
    protected String mCurrentLanguage;

    public static int isAnimation = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getLanguageSettings();
    }

    private void getLanguageSettings() {
        mCurrentLanguage = PreferenceManager.getDefaultSharedPreferences(this).getString("language", Locale.getDefault().getLanguage());
    }
}
