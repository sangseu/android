package mig0.bosheculogger.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;

public class BaseFragment extends Fragment implements UpdateViewInterface {
    protected String mCurrentLanguage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            this.mCurrentLanguage = args.getString("lan");
        }
    }

    public void updateLanguage(String lan) {
        this.mCurrentLanguage = lan;
        updateView();
    }

    public void updateView() {
    }
}
