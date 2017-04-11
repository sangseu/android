package mig0.bosheculogger.activity;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutCompat;
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

import mig0.bosheculogger.R;
import mig0.bosheculogger.utils.ConfigManager;
import mig0.bosheculogger.utils.ConfigManager.Strings;
import mig0.bosheculogger.utils.StringUtil;


public class HelpActivity extends BaseActivity {
    private Context mContext;

    private class MyAdapter extends BaseAdapter {
        String[] mItems;

        private MyAdapter(String[] items) {
            mItems = items;
        }

        public int getCount() {
            return mItems.length;
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return (long) position;
        }

        public View getView(int position, View contentView, ViewGroup viewGroup) {
            TextView mTextView = new TextView(HelpActivity.this.getApplicationContext());
            mTextView.setText(mItems[position]);
            mTextView.setTextSize(18.0f);
            mTextView.setTextColor(Color.parseColor("#004986"));
            mTextView.setPadding(20, 30, 0, 30);
            return mTextView;
        }

        @Override
        public boolean isEnabled(int position) {
            if (position == 0 || position == 1) {
                return false;
            }
            return super.isEnabled(position);
        }
    }

    private void showHelpDialog() {
        ConfigManager cManager = ConfigManager.getInstance(mContext);
        String help_title = cManager.getString(mCurrentLanguage, Strings.HELP_TITLE);
        String help_mess = cManager.getString(mCurrentLanguage, Strings.HELP_MESS);
        String btn_ok = cManager.getString(mCurrentLanguage, Strings.UNPAIR_POSBTN);
        String btn_cal = cManager.getString(mCurrentLanguage, Strings.UNPAIR_NAVBTN);
        AlertDialog.Builder builder1 = new AlertDialog.Builder(mContext);
        builder1.setTitle(help_title);
        builder1.setMessage(help_mess);
        builder1.setCancelable(true);
        builder1.setPositiveButton(
                btn_ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        builder1.setNegativeButton(
                btn_cal,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        AlertDialog alert11 = builder1.create();
        alert11.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(this.LOG_TAG, "onCreate");
        setContentView((int) R.layout.help_activity);
        mContext = this;
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        CharSequence title = ConfigManager.getInstance(this).getString(mCurrentLanguage, Strings.HELP);
        setTitle(title);
        actionBar.setTitle(title);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setIcon(R.drawable.logo_trans);
            getSupportActionBar().setDisplayUseLogoEnabled(true);
        }

        ListView mlistView;
        mlistView = (ListView) findViewById(R.id.listView1);
        mlistView.setAdapter(new MyAdapter(StringUtil.getHelpItems(mCurrentLanguage)));
        mlistView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View arg1, int position, long arg3) {
                Intent intent;
                /* Bluetooth setting */
                if (position == 2) {
                    intent = new Intent(HelpActivity.this, SearchActivity.class);
                    intent.putExtra(SearchActivity.FLAG_NEED_BACK, true);
                    HelpActivity.this.startActivityForResult(intent, 0);
                } else if (position == 3) {
                    /* FAQ -> Help */
                    showHelpDialog();
                }
            }
        });
    }

    private void showToast(String messageId) {
        Toast.makeText(this, ConfigManager.getInstance(this).getString(this.mCurrentLanguage, messageId), Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public Intent getSupportParentActivityIntent() {
        finish();
        return super.getSupportParentActivityIntent();
    }

    @Override
    public boolean onSupportNavigateUp() {
        return super.onSupportNavigateUp();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == -1) {
            Log.d(this.LOG_TAG, "help get result ok");
            finish();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}