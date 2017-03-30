package android.support.v7.app;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle.Delegate;
import android.support.v4.app.NavUtils;
import android.support.v4.media.TransportMediator;
import android.support.v7.appcompat.C0048R;
import android.support.v7.internal.view.SupportMenuInflater;
import android.support.v7.view.ActionMode;
import android.support.v7.view.ActionMode.Callback;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

abstract class ActionBarActivityDelegate {
    static final String METADATA_UI_OPTIONS = "android.support.UI_OPTIONS";
    private static final String TAG = "ActionBarActivityDelegate";
    static final String UIOPTION_SPLIT_ACTION_BAR_WHEN_NARROW = "splitActionBarWhenNarrow";
    private ActionBar mActionBar;
    final ActionBarActivity mActivity;
    private boolean mEnableDefaultActionBarUp;
    boolean mHasActionBar;
    private MenuInflater mMenuInflater;
    boolean mOverlayActionBar;

    abstract void addContentView(View view, LayoutParams layoutParams);

    abstract ActionBar createSupportActionBar();

    abstract Delegate getDrawerToggleDelegate();

    abstract boolean onBackPressed();

    abstract void onConfigurationChanged(Configuration configuration);

    abstract void onContentChanged();

    abstract boolean onCreatePanelMenu(int i, Menu menu);

    abstract View onCreatePanelView(int i);

    abstract boolean onMenuItemSelected(int i, MenuItem menuItem);

    abstract void onPostResume();

    abstract boolean onPreparePanel(int i, View view, Menu menu);

    abstract void onStop();

    abstract void onTitleChanged(CharSequence charSequence);

    abstract void setContentView(int i);

    abstract void setContentView(View view);

    abstract void setContentView(View view, LayoutParams layoutParams);

    abstract void setSupportProgress(int i);

    abstract void setSupportProgressBarIndeterminate(boolean z);

    abstract void setSupportProgressBarIndeterminateVisibility(boolean z);

    abstract void setSupportProgressBarVisibility(boolean z);

    abstract ActionMode startSupportActionMode(Callback callback);

    abstract void supportInvalidateOptionsMenu();

    abstract boolean supportRequestWindowFeature(int i);

    static ActionBarActivityDelegate createDelegate(ActionBarActivity activity) {
        int version = VERSION.SDK_INT;
        if (version >= 16) {
            return new ActionBarActivityDelegateJB(activity);
        }
        if (version >= 14) {
            return new ActionBarActivityDelegateICS(activity);
        }
        if (version >= 11) {
            return new ActionBarActivityDelegateHC(activity);
        }
        return new ActionBarActivityDelegateBase(activity);
    }

    ActionBarActivityDelegate(ActionBarActivity activity) {
        this.mActivity = activity;
    }

    final ActionBar getSupportActionBar() {
        if (!this.mHasActionBar && !this.mOverlayActionBar) {
            this.mActionBar = null;
        } else if (this.mActionBar == null) {
            this.mActionBar = createSupportActionBar();
            if (this.mEnableDefaultActionBarUp) {
                this.mActionBar.setDisplayHomeAsUpEnabled(true);
            }
        }
        return this.mActionBar;
    }

    MenuInflater getMenuInflater() {
        if (this.mMenuInflater == null) {
            ActionBar ab = getSupportActionBar();
            if (ab != null) {
                this.mMenuInflater = new SupportMenuInflater(ab.getThemedContext());
            } else {
                this.mMenuInflater = new SupportMenuInflater(this.mActivity);
            }
        }
        return this.mMenuInflater;
    }

    void onCreate(Bundle savedInstanceState) {
        TypedArray a = this.mActivity.obtainStyledAttributes(C0048R.styleable.ActionBarWindow);
        if (a.hasValue(0)) {
            this.mHasActionBar = a.getBoolean(0, false);
            this.mOverlayActionBar = a.getBoolean(1, false);
            a.recycle();
            if (NavUtils.getParentActivityName(this.mActivity) == null) {
                return;
            }
            if (this.mActionBar == null) {
                this.mEnableDefaultActionBarUp = true;
                return;
            } else {
                this.mActionBar.setDisplayHomeAsUpEnabled(true);
                return;
            }
        }
        a.recycle();
        throw new IllegalStateException("You need to use a Theme.AppCompat theme (or descendant) with this activity.");
    }

    boolean onPrepareOptionsPanel(View view, Menu menu) {
        if (VERSION.SDK_INT < 16) {
            return this.mActivity.onPrepareOptionsMenu(menu);
        }
        return this.mActivity.superOnPrepareOptionsPanel(view, menu);
    }

    protected final String getUiOptionsFromMetadata() {
        try {
            ActivityInfo info = this.mActivity.getPackageManager().getActivityInfo(this.mActivity.getComponentName(), TransportMediator.FLAG_KEY_MEDIA_NEXT);
            if (info.metaData != null) {
                return info.metaData.getString(METADATA_UI_OPTIONS);
            }
            return null;
        } catch (NameNotFoundException e) {
            Log.e(TAG, "getUiOptionsFromMetadata: Activity '" + this.mActivity.getClass().getSimpleName() + "' not in manifest");
            return null;
        }
    }

    protected final Context getActionBarThemedContext() {
        Context context = this.mActivity;
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            return ab.getThemedContext();
        }
        return context;
    }
}
