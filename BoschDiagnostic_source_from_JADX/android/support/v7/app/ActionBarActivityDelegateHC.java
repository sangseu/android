package android.support.v7.app;

import android.os.Bundle;

class ActionBarActivityDelegateHC extends ActionBarActivityDelegateBase {
    ActionBarActivityDelegateHC(ActionBarActivity activity) {
        super(activity);
    }

    void onCreate(Bundle savedInstanceState) {
        this.mActivity.getWindow().requestFeature(10);
        super.onCreate(savedInstanceState);
    }

    public ActionBar createSupportActionBar() {
        ensureSubDecor();
        return new ActionBarImplHC(this.mActivity, this.mActivity);
    }
}
