package com.bosch.diag.activity;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import java.util.Locale;

public class BaseActivity extends ActionBarActivity {
    protected String LOG_TAG;
    protected String mCurrentLanguage;

    public BaseActivity() {
        this.LOG_TAG = getClass().getSimpleName();
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getLanguageSettings();
    }

    private void getLanguageSettings() {
        this.mCurrentLanguage = PreferenceManager.getDefaultSharedPreferences(this).getString("language", Locale.getDefault().getLanguage());
    }
}
