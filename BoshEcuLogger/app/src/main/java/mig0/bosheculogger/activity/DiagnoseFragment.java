package mig0.bosheculogger.activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView.LayoutParams;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import mig0.bosheculogger.R;
import mig0.bosheculogger.utils.ConfigManager;
import mig0.bosheculogger.utils.ConfigManager.Strings;
import mig0.bosheculogger.utils.Fault;
import mig0.bosheculogger.utils.FaultGroup;
import java.util.ArrayList;
import java.util.Iterator;

@SuppressLint({"NewApi"})
public class DiagnoseFragment extends BaseFragment {
    private static final String LOG_TAG = "DiagnoseFragment.java";
    TextView mEmptyDataView;
    ExpandableListView mExpandlistView;
    int mGroupSize = 0;
    private ExpandableListAdapter mListAdapter;


    class ExpandableListAdapter extends BaseExpandableListAdapter {
        private Context mContext;
        private ArrayList<FaultGroup> mFaultList;

        public ExpandableListAdapter(Context context) {
            this.mContext = context;
            updateData();
        }

        public ArrayList<FaultGroup> updateData() {
            ArrayList<FaultGroup> filterList = new ArrayList<FaultGroup>();
            Iterator it = ConfigManager.getInstance(this.mContext).getFaultList().iterator();
            while (it.hasNext()) {
                FaultGroup group = (FaultGroup) it.next();
                ArrayList<Fault> faults = group.childFaults;
                int faultCount = 0;
                ArrayList<Fault> needToAdd = new ArrayList<Fault>();
                Iterator it2 = faults.iterator();
                while (it2.hasNext()) {
                    Fault f = (Fault) it2.next();
                    if (f.getBooleanValue()) {
                        faultCount++;
                        needToAdd.add(f);
                    }
                }
                if (faultCount > 0) {
                    FaultGroup filterGroup = new FaultGroup();
                    filterGroup.en = group.en;
                    filterGroup.zh = group.zh;
                    filterGroup.childFaults.addAll(needToAdd);
                    filterList.add(filterGroup);
                }
            }
            this.mFaultList = filterList;
            return this.mFaultList;
        }

        TextView getTextViewChild() {
            LayoutParams lp = new LayoutParams(-1, -2);
            TextView textView = new TextView(DiagnoseFragment.this.getActivity());
            textView.setLayoutParams(lp);
            textView.setGravity(17);
            textView.setPadding(30, 20, 90, 20);
            textView.setTextSize(22.0f);
            textView.setTextColor(Color.parseColor("#0081c7"));
            return textView;
        }

        public Object getChild(int groupPosition, int childPosition) {
            return ((FaultGroup) this.mFaultList.get(groupPosition)).childFaults.get(childPosition);
        }

        public long getChildId(int groupPosition, int childPosition) {
            return (long) childPosition;
        }

        public int getGroupType(int groupPosition) {
            return super.getGroupType(groupPosition);
        }

        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            LinearLayout ll = new LinearLayout(DiagnoseFragment.this.getActivity());
            ll.setOrientation(LinearLayout.HORIZONTAL);
            TextView textView = getTextViewChild();
            Fault f = (Fault) getChild(groupPosition, childPosition);
            f.setLanguage(DiagnoseFragment.this.mCurrentLanguage);
            textView.setText(f.getName());
            ll.addView(textView);
            Log.i(DiagnoseFragment.LOG_TAG, "in getChildView name:" + f.getName() + " value:" + f.getValue());
            return ll;
        }

        public int getChildrenCount(int groupPosition) {
            return ((FaultGroup) this.mFaultList.get(groupPosition)).childFaults.size();
        }

        public Object getGroup(int groupPosition) {
            return this.mFaultList.get(groupPosition);
        }

        public int getGroupCount() {
            return this.mFaultList.size();
        }

        public long getGroupId(int groupPosition) {
            return (long) groupPosition;
        }

        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            LinearLayout linear = (LinearLayout) LayoutInflater.from(DiagnoseFragment.this.getActivity()).inflate(R.layout.diagnoseitem, parent, false);
            TextView textView1 = (TextView) linear.findViewById(R.id.textViewdiag);
            FaultGroup group = (FaultGroup) this.mFaultList.get(groupPosition);
            group.lan = DiagnoseFragment.this.mCurrentLanguage;
            textView1.setText(group.getName());
            textView1.setPadding(20, 20, 0, 20);
            textView1.setTextSize(25.0f);
            ImageView imageView = (ImageView) linear.findViewById(R.id.imageView1);
            imageView.setPadding(20, 40, 20, 20);
            if (isExpanded) {
                imageView.setImageResource(R.drawable.arrow_down);
            } else {
                imageView.setImageResource(R.drawable.arrow_right);
            }
            return linear;
        }

        public boolean hasStableIds() {
            return true;
        }

        public boolean isChildSelectable(int arg0, int arg1) {
            return true;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.diagnosefragment, container, false);
        this.mExpandlistView = (ExpandableListView) view.findViewById(R.id.expandableListView1);
        this.mEmptyDataView = (TextView) view.findViewById(R.id.data_empty_text);
        this.mListAdapter = new ExpandableListAdapter(getActivity());
        this.mExpandlistView.setAdapter(this.mListAdapter);
        this.mExpandlistView.setOnChildClickListener(new OnChildClickListener() {
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                String diagMsg = ((Fault) DiagnoseFragment.this.mListAdapter.getChild(groupPosition, childPosition)).getPrompt();
                Log.i(DiagnoseFragment.LOG_TAG, "diagMsg:" + diagMsg);
                String exitCancel = ConfigManager.getInstance(DiagnoseFragment.this.getActivity()).getString(DiagnoseFragment.this.mCurrentLanguage, Strings.BT_OK);
                View faultDialog = LayoutInflater.from(DiagnoseFragment.this.getActivity()).inflate(R.layout.fault_dialog, null);
                Button faultBtn = (Button) faultDialog.findViewById(R.id.fault_btn);
                ((TextView) faultDialog.findViewById(R.id.fault_text)).setText(diagMsg);
                faultBtn.setText(exitCancel);
                Builder builder = new Builder(DiagnoseFragment.this.getActivity());
                builder.setView(faultDialog);
                final AlertDialog dialog = builder.create();
                dialog.setCanceledOnTouchOutside(false);
                dialog.show();
                faultBtn.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        if (dialog != null) {
                            dialog.dismiss();
                        }
                    }
                });
                return false;
            }
        });
        return view;
    }

    public void updateView() {
        if (this.mListAdapter != null) {
            this.mListAdapter.updateData();
            this.mListAdapter.notifyDataSetChanged();
            setupNoDataView();
        }
    }

    private void setupNoDataView() {
        this.mEmptyDataView.setText(ConfigManager.getInstance(getActivity()).getString(this.mCurrentLanguage, Strings.NO_FAULT));
        if (this.mListAdapter.isEmpty()) {
            this.mEmptyDataView.setVisibility(View.VISIBLE);
        } else {
            this.mEmptyDataView.setVisibility(View.INVISIBLE);
        }
    }
}
