package com.bosch.diag.activity;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.bosch.diag.utils.ConfigManager;
import com.bosch.diag.utils.ConfigManager.Strings;
import com.bosch.diag.utils.StringUtil;
import com.eScooterDiagTool.C0102R;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class HelpActivity extends BaseActivity {
    private Context mContext;
    private ListView mlistView;

    /* renamed from: com.bosch.diag.activity.HelpActivity.1 */
    class C00811 implements OnItemClickListener {
        C00811() {
        }

        public void onItemClick(AdapterView<?> adapterView, View arg1, int position, long arg3) {
            File file;
            Intent intent;
            if (position == 2) {
                intent = new Intent(HelpActivity.this, SearchActivity.class);
                intent.putExtra(SearchActivity.FLAG_NEED_BACK, true);
                HelpActivity.this.startActivityForResult(intent, 0);
            } else if (position == 3) {
                String pdfName = "FAQ.pdf";
                try {
                    InputStream is = HelpActivity.this.getResources().getAssets().open(pdfName);
                    FileOutputStream outputStream = HelpActivity.this.mContext.openFileOutput(pdfName, 1);
                    byte[] temp = new byte[AccessibilityNodeInfoCompat.ACTION_NEXT_HTML_ELEMENT];
                    while (true) {
                        int i = is.read(temp);
                        if (i <= 0) {
                            break;
                        }
                        outputStream.write(temp, 0, i);
                    }
                    outputStream.close();
                    is.close();
                    File fileTemp = new File("/data/data/com.eScooterDiagTool/files/" + pdfName);
                    try {
                        if (fileTemp.exists()) {
                            Uri path = Uri.fromFile(fileTemp);
                            intent = new Intent("android.intent.action.VIEW");
                            intent.setDataAndType(path, "application/pdf");
                            intent.setFlags(67108864);
                            HelpActivity.this.startActivity(intent);
                        }
                    } catch (ActivityNotFoundException e) {
                        file = fileTemp;
                        HelpActivity.this.showToast(Strings.PDF_NO_APP);
                    } catch (IOException e2) {
                        file = fileTemp;
                        HelpActivity.this.showToast(Strings.FAQ_NOT_FOUND);
                    }
                } catch (ActivityNotFoundException e3) {
                } catch (IOException e4) {
                }
            }
        }
    }

    private class MyAdapter extends BaseAdapter {
        String[] mItems;

        public MyAdapter(String[] items) {
            this.mItems = items;
        }

        public int getCount() {
            return this.mItems.length;
        }

        public Object getItem(int position) {
            return Integer.valueOf(position);
        }

        public long getItemId(int position) {
            return (long) position;
        }

        public View getView(int position, View contentView, ViewGroup viewGroup) {
            TextView mTextView = new TextView(HelpActivity.this.getApplicationContext());
            mTextView.setText(this.mItems[position]);
            mTextView.setTextSize(18.0f);
            mTextView.setTextColor(Color.parseColor("#004986"));
            mTextView.setPadding(20, 30, 0, 30);
            return mTextView;
        }

        public boolean isEnabled(int position) {
            if (position == 0 || position == 1) {
                return false;
            }
            return super.isEnabled(position);
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView((int) C0102R.layout.help_activity);
        this.mContext = this;
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        CharSequence title = ConfigManager.getInstance(this).getString(this.mCurrentLanguage, Strings.HELP);
        setTitle(title);
        actionBar.setTitle(title);
        if (VERSION.SDK_INT <= 13) {
            getSupportActionBar().setIcon((int) C0102R.drawable.logo1);
        }
        this.mlistView = (ListView) findViewById(C0102R.id.listView1);
        this.mlistView.setAdapter(new MyAdapter(StringUtil.getHelpItems(this.mCurrentLanguage)));
        this.mlistView.setOnItemClickListener(new C00811());
    }

    private void showToast(String messageId) {
        Toast.makeText(this, ConfigManager.getInstance(this).getString(this.mCurrentLanguage, messageId), 0).show();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    public Intent getSupportParentActivityIntent() {
        finish();
        return super.getSupportParentActivityIntent();
    }

    public boolean onSupportNavigateUp() {
        return super.onSupportNavigateUp();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == -1) {
            Log.d(this.LOG_TAG, "help get result ok");
            finish();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
