package com.bosch.diag.activity;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.bosch.diag.utils.ConfigManager;
import com.bosch.diag.utils.InformationInterface;
import com.eScooterDiagTool.C0102R;
import java.util.ArrayList;

public class BasicFragment extends BaseFragment {
    private static final String LOG_TAG = "BasicFragment.java";
    private BasicAdapter mAdapter;
    private ListView mBasicList;

    private class BasicAdapter extends BaseAdapter {
        private ArrayList<InformationInterface> mBasicListData;
        private ConfigManager mConfigManager;
        private LayoutInflater mInflater;

        public BasicAdapter(Context context) {
            this.mConfigManager = ConfigManager.getInstance(context);
            this.mBasicListData = this.mConfigManager.getBasicInformationList();
            this.mInflater = LayoutInflater.from(context);
        }

        public void update() {
            this.mBasicListData = this.mConfigManager.getBasicInformationList();
        }

        public int getCount() {
            return this.mBasicListData.size();
        }

        public Object getItem(int position) {
            return this.mBasicListData.get(position);
        }

        public long getItemId(int position) {
            return 0;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = this.mInflater.inflate(C0102R.layout.basic_item, parent, false);
            }
            TextView itemText = (TextView) convertView.findViewById(C0102R.id.item_text);
            TextView itemValue = (TextView) convertView.findViewById(C0102R.id.item_value);
            InformationInterface info = (InformationInterface) this.mBasicListData.get(position);
            info.setLanguage(BasicFragment.this.mCurrentLanguage);
            String name = info.getName();
            itemText.setText(name);
            String value = info.getValue();
            itemValue.setText(value);
            Log.i(BasicFragment.LOG_TAG, "in getView name:" + name + "  value:" + value);
            return convertView;
        }

        public boolean isEnabled(int position) {
            return false;
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(C0102R.layout.basicfragment, container, false);
        initView(view);
        return view;
    }

    private void initView(View view) {
        this.mBasicList = (ListView) view.findViewById(C0102R.id.basic_list);
        this.mAdapter = new BasicAdapter(getActivity());
        this.mBasicList.setAdapter(this.mAdapter);
    }

    public void updateView() {
        if (this.mAdapter != null) {
            this.mAdapter.update();
            this.mAdapter.notifyDataSetChanged();
        }
    }
}
