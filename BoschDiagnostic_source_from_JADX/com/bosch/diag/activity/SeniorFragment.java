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

public class SeniorFragment extends BaseFragment {
    private static final String LOG_TAG = "SeniorFragment.java";
    private AdvanceAdapter mAdapter;
    private ListView mAdvanceList;

    private class AdvanceAdapter extends BaseAdapter {
        private ArrayList<InformationInterface> mAdvanceListData;
        private ConfigManager mConfigManager;
        private LayoutInflater mInflater;

        public AdvanceAdapter(Context context) {
            this.mConfigManager = ConfigManager.getInstance(context);
            this.mAdvanceListData = this.mConfigManager.getAdvancedInformationList();
            this.mInflater = LayoutInflater.from(context);
        }

        public void update() {
            this.mAdvanceListData = this.mConfigManager.getAdvancedInformationList();
        }

        public int getCount() {
            return this.mAdvanceListData.size();
        }

        public Object getItem(int position) {
            return this.mAdvanceListData.get(position);
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
            InformationInterface info = (InformationInterface) this.mAdvanceListData.get(position);
            info.setLanguage(SeniorFragment.this.mCurrentLanguage);
            String name = info.getName();
            itemText.setText(name);
            String value = info.getValue();
            itemValue.setText(value);
            Log.i(SeniorFragment.LOG_TAG, "in getView name:" + name + "  value:" + value);
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
        View view = inflater.inflate(C0102R.layout.seniorfragment, container, false);
        initView(view);
        return view;
    }

    private void initView(View view) {
        this.mAdvanceList = (ListView) view.findViewById(C0102R.id.advance_list);
        this.mAdapter = new AdvanceAdapter(getActivity());
        this.mAdvanceList.setAdapter(this.mAdapter);
    }

    public void updateView() {
        if (this.mAdapter != null) {
            this.mAdapter.update();
            this.mAdapter.notifyDataSetChanged();
        }
    }
}
