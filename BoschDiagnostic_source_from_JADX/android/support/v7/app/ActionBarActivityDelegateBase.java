package android.support.v7.app;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.v4.app.ActionBarDrawerToggle.Delegate;
import android.support.v4.media.TransportMediator;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.appcompat.C0048R;
import android.support.v7.internal.view.menu.ListMenuPresenter;
import android.support.v7.internal.view.menu.MenuBuilder;
import android.support.v7.internal.view.menu.MenuPresenter.Callback;
import android.support.v7.internal.view.menu.MenuView;
import android.support.v7.internal.view.menu.MenuWrapperFactory;
import android.support.v7.internal.widget.ActionBarContainer;
import android.support.v7.internal.widget.ActionBarContextView;
import android.support.v7.internal.widget.ActionBarView;
import android.support.v7.internal.widget.ProgressBarICS;
import android.support.v7.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import com.eScooterDiagTool.C0102R;

class ActionBarActivityDelegateBase extends ActionBarActivityDelegate implements Callback, MenuBuilder.Callback {
    private static final int[] ACTION_BAR_DRAWABLE_TOGGLE_ATTRS;
    private static final String TAG = "ActionBarActivityDelegateBase";
    private ActionBarView mActionBarView;
    private ActionMode mActionMode;
    private boolean mFeatureIndeterminateProgress;
    private boolean mFeatureProgress;
    private boolean mInvalidateMenuPosted;
    private final Runnable mInvalidateMenuRunnable;
    private ListMenuPresenter mListMenuPresenter;
    private MenuBuilder mMenu;
    private boolean mSubDecorInstalled;
    private CharSequence mTitleToSet;

    /* renamed from: android.support.v7.app.ActionBarActivityDelegateBase.1 */
    class C00471 implements Runnable {
        C00471() {
        }

        public void run() {
            MenuBuilder menu = ActionBarActivityDelegateBase.this.createMenu();
            if (ActionBarActivityDelegateBase.this.mActivity.superOnCreatePanelMenu(0, menu) && ActionBarActivityDelegateBase.this.mActivity.superOnPreparePanel(0, null, menu)) {
                ActionBarActivityDelegateBase.this.setMenu(menu);
            } else {
                ActionBarActivityDelegateBase.this.setMenu(null);
            }
            ActionBarActivityDelegateBase.this.mInvalidateMenuPosted = false;
        }
    }

    private class ActionBarDrawableToggleImpl implements Delegate {
        private ActionBarDrawableToggleImpl() {
        }

        public Drawable getThemeUpIndicator() {
            TypedArray a = ActionBarActivityDelegateBase.this.mActivity.obtainStyledAttributes(ActionBarActivityDelegateBase.ACTION_BAR_DRAWABLE_TOGGLE_ATTRS);
            Drawable result = a.getDrawable(0);
            a.recycle();
            return result;
        }

        public void setActionBarUpIndicator(Drawable upDrawable, int contentDescRes) {
            if (ActionBarActivityDelegateBase.this.mActionBarView != null) {
                ActionBarActivityDelegateBase.this.mActionBarView.setHomeAsUpIndicator(upDrawable);
            }
        }

        public void setActionBarDescription(int contentDescRes) {
        }
    }

    private class ActionModeCallbackWrapper implements ActionMode.Callback {
        private ActionMode.Callback mWrapped;

        public ActionModeCallbackWrapper(ActionMode.Callback wrapped) {
            this.mWrapped = wrapped;
        }

        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            return this.mWrapped.onCreateActionMode(mode, menu);
        }

        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return this.mWrapped.onPrepareActionMode(mode, menu);
        }

        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return this.mWrapped.onActionItemClicked(mode, item);
        }

        public void onDestroyActionMode(ActionMode mode) {
            this.mWrapped.onDestroyActionMode(mode);
            ActionBarActivityDelegateBase.this.mActivity.onSupportActionModeFinished(mode);
            ActionBarActivityDelegateBase.this.mActionMode = null;
        }
    }

    static {
        ACTION_BAR_DRAWABLE_TOGGLE_ATTRS = new int[]{C0048R.attr.homeAsUpIndicator};
    }

    ActionBarActivityDelegateBase(ActionBarActivity activity) {
        super(activity);
        this.mInvalidateMenuRunnable = new C00471();
    }

    public ActionBar createSupportActionBar() {
        ensureSubDecor();
        return new ActionBarImplBase(this.mActivity, this.mActivity);
    }

    public void onConfigurationChanged(Configuration newConfig) {
        if (this.mHasActionBar && this.mSubDecorInstalled) {
            ((ActionBarImplBase) getSupportActionBar()).onConfigurationChanged(newConfig);
        }
    }

    public void onStop() {
        ActionBarImplBase ab = (ActionBarImplBase) getSupportActionBar();
        if (ab != null) {
            ab.setShowHideAnimationEnabled(false);
        }
    }

    public void onPostResume() {
        ActionBarImplBase ab = (ActionBarImplBase) getSupportActionBar();
        if (ab != null) {
            ab.setShowHideAnimationEnabled(true);
        }
    }

    public void setContentView(View v) {
        ensureSubDecor();
        if (this.mHasActionBar) {
            ViewGroup contentParent = (ViewGroup) this.mActivity.findViewById(16908290);
            contentParent.removeAllViews();
            contentParent.addView(v);
        } else {
            this.mActivity.superSetContentView(v);
        }
        this.mActivity.onSupportContentChanged();
    }

    public void setContentView(int resId) {
        ensureSubDecor();
        if (this.mHasActionBar) {
            ViewGroup contentParent = (ViewGroup) this.mActivity.findViewById(16908290);
            contentParent.removeAllViews();
            this.mActivity.getLayoutInflater().inflate(resId, contentParent);
        } else {
            this.mActivity.superSetContentView(resId);
        }
        this.mActivity.onSupportContentChanged();
    }

    public void setContentView(View v, LayoutParams lp) {
        ensureSubDecor();
        if (this.mHasActionBar) {
            ViewGroup contentParent = (ViewGroup) this.mActivity.findViewById(16908290);
            contentParent.removeAllViews();
            contentParent.addView(v, lp);
        } else {
            this.mActivity.superSetContentView(v, lp);
        }
        this.mActivity.onSupportContentChanged();
    }

    public void addContentView(View v, LayoutParams lp) {
        ensureSubDecor();
        if (this.mHasActionBar) {
            ((ViewGroup) this.mActivity.findViewById(16908290)).addView(v, lp);
        } else {
            this.mActivity.superSetContentView(v, lp);
        }
        this.mActivity.onSupportContentChanged();
    }

    public void onContentChanged() {
    }

    final void ensureSubDecor() {
        if (this.mHasActionBar && !this.mSubDecorInstalled) {
            boolean splitActionBar;
            if (this.mOverlayActionBar) {
                this.mActivity.superSetContentView(C0048R.layout.abc_action_bar_decor_overlay);
            } else {
                this.mActivity.superSetContentView(C0048R.layout.abc_action_bar_decor);
            }
            this.mActionBarView = (ActionBarView) this.mActivity.findViewById(C0048R.id.action_bar);
            this.mActionBarView.setWindowCallback(this.mActivity);
            if (this.mFeatureProgress) {
                this.mActionBarView.initProgress();
            }
            if (this.mFeatureIndeterminateProgress) {
                this.mActionBarView.initIndeterminateProgress();
            }
            boolean splitWhenNarrow = "splitActionBarWhenNarrow".equals(getUiOptionsFromMetadata());
            if (splitWhenNarrow) {
                splitActionBar = this.mActivity.getResources().getBoolean(C0048R.bool.abc_split_action_bar_is_narrow);
            } else {
                TypedArray a = this.mActivity.obtainStyledAttributes(C0048R.styleable.ActionBarWindow);
                splitActionBar = a.getBoolean(2, false);
                a.recycle();
            }
            ActionBarContainer splitView = (ActionBarContainer) this.mActivity.findViewById(C0048R.id.split_action_bar);
            if (splitView != null) {
                this.mActionBarView.setSplitView(splitView);
                this.mActionBarView.setSplitActionBar(splitActionBar);
                this.mActionBarView.setSplitWhenNarrow(splitWhenNarrow);
                ActionBarContextView cab = (ActionBarContextView) this.mActivity.findViewById(C0048R.id.action_context_bar);
                cab.setSplitView(splitView);
                cab.setSplitActionBar(splitActionBar);
                cab.setSplitWhenNarrow(splitWhenNarrow);
            }
            this.mActivity.findViewById(16908290).setId(-1);
            this.mActivity.findViewById(C0048R.id.action_bar_activity_content).setId(16908290);
            if (this.mTitleToSet != null) {
                this.mActionBarView.setWindowTitle(this.mTitleToSet);
                this.mTitleToSet = null;
            }
            this.mSubDecorInstalled = true;
            supportInvalidateOptionsMenu();
        }
    }

    public boolean supportRequestWindowFeature(int featureId) {
        switch (featureId) {
            case CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER /*2*/:
                this.mFeatureProgress = true;
                return true;
            case FragmentManagerImpl.ANIM_STYLE_FADE_ENTER /*5*/:
                this.mFeatureIndeterminateProgress = true;
                return true;
            case TransportMediator.FLAG_KEY_MEDIA_PLAY_PAUSE /*8*/:
                this.mHasActionBar = true;
                return true;
            case C0102R.styleable.Spinner_disableChildrenWhenDisabled /*9*/:
                this.mOverlayActionBar = true;
                return true;
            default:
                return this.mActivity.requestWindowFeature(featureId);
        }
    }

    public void onTitleChanged(CharSequence title) {
        if (this.mActionBarView != null) {
            this.mActionBarView.setWindowTitle(title);
        } else {
            this.mTitleToSet = title;
        }
    }

    public View onCreatePanelView(int featureId) {
        if (featureId != 0) {
            return null;
        }
        boolean show = true;
        MenuBuilder menu = this.mMenu;
        if (this.mActionMode == null) {
            if (menu == null) {
                menu = createMenu();
                setMenu(menu);
                menu.stopDispatchingItemsChanged();
                show = this.mActivity.superOnCreatePanelMenu(0, menu);
            }
            if (show) {
                menu.stopDispatchingItemsChanged();
                show = this.mActivity.superOnPreparePanel(0, null, menu);
            }
        }
        if (show) {
            View createdPanelView = (View) getListMenuView(this.mActivity, this);
            menu.startDispatchingItemsChanged();
            return createdPanelView;
        }
        setMenu(null);
        return null;
    }

    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        if (featureId != 0) {
            return this.mActivity.superOnCreatePanelMenu(featureId, menu);
        }
        return false;
    }

    public boolean onPreparePanel(int featureId, View view, Menu menu) {
        if (featureId != 0) {
            return this.mActivity.superOnPreparePanel(featureId, view, menu);
        }
        return false;
    }

    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (featureId == 0) {
            item = MenuWrapperFactory.createMenuItemWrapper(item);
        }
        return this.mActivity.superOnMenuItemSelected(featureId, item);
    }

    public boolean onMenuItemSelected(MenuBuilder menu, MenuItem item) {
        return this.mActivity.onMenuItemSelected(0, item);
    }

    public void onMenuModeChange(MenuBuilder menu) {
        reopenMenu(menu, true);
    }

    public void onCloseMenu(MenuBuilder menu, boolean allMenusAreClosing) {
        this.mActivity.closeOptionsMenu();
    }

    public boolean onOpenSubMenu(MenuBuilder subMenu) {
        return false;
    }

    public ActionMode startSupportActionMode(ActionMode.Callback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("ActionMode callback can not be null.");
        }
        if (this.mActionMode != null) {
            this.mActionMode.finish();
        }
        ActionMode.Callback wrappedCallback = new ActionModeCallbackWrapper(callback);
        ActionBarImplBase ab = (ActionBarImplBase) getSupportActionBar();
        if (ab != null) {
            this.mActionMode = ab.startActionMode(wrappedCallback);
        }
        if (this.mActionMode != null) {
            this.mActivity.onSupportActionModeStarted(this.mActionMode);
        }
        return this.mActionMode;
    }

    public void supportInvalidateOptionsMenu() {
        if (!this.mInvalidateMenuPosted) {
            this.mInvalidateMenuPosted = true;
            this.mActivity.getWindow().getDecorView().post(this.mInvalidateMenuRunnable);
        }
    }

    private MenuBuilder createMenu() {
        MenuBuilder menu = new MenuBuilder(getActionBarThemedContext());
        menu.setCallback(this);
        return menu;
    }

    private void reopenMenu(MenuBuilder menu, boolean toggleMenuMode) {
        if (this.mActionBarView == null || !this.mActionBarView.isOverflowReserved()) {
            menu.close();
        } else if (this.mActionBarView.isOverflowMenuShowing() && toggleMenuMode) {
            this.mActionBarView.hideOverflowMenu();
        } else if (this.mActionBarView.getVisibility() == 0) {
            this.mActionBarView.showOverflowMenu();
        }
    }

    private MenuView getListMenuView(Context context, Callback cb) {
        if (this.mMenu == null) {
            return null;
        }
        if (this.mListMenuPresenter == null) {
            TypedArray a = context.obtainStyledAttributes(C0048R.styleable.Theme);
            int listPresenterTheme = a.getResourceId(4, C0048R.style.Theme_AppCompat_CompactMenu);
            a.recycle();
            this.mListMenuPresenter = new ListMenuPresenter(C0048R.layout.abc_list_menu_item_layout, listPresenterTheme);
            this.mListMenuPresenter.setCallback(cb);
            this.mMenu.addMenuPresenter(this.mListMenuPresenter);
        } else {
            this.mListMenuPresenter.updateMenuView(false);
        }
        return this.mListMenuPresenter.getMenuView(new FrameLayout(context));
    }

    private void setMenu(MenuBuilder menu) {
        if (menu != this.mMenu) {
            if (this.mMenu != null) {
                this.mMenu.removeMenuPresenter(this.mListMenuPresenter);
            }
            this.mMenu = menu;
            if (!(menu == null || this.mListMenuPresenter == null)) {
                menu.addMenuPresenter(this.mListMenuPresenter);
            }
            if (this.mActionBarView != null) {
                this.mActionBarView.setMenu(menu, this);
            }
        }
    }

    public boolean onBackPressed() {
        if (this.mActionMode != null) {
            this.mActionMode.finish();
            return true;
        } else if (this.mActionBarView == null || !this.mActionBarView.hasExpandedActionView()) {
            return false;
        } else {
            this.mActionBarView.collapseActionView();
            return true;
        }
    }

    void setSupportProgressBarVisibility(boolean visible) {
        updateProgressBars(visible ? -1 : -2);
    }

    void setSupportProgressBarIndeterminateVisibility(boolean visible) {
        updateProgressBars(visible ? -1 : -2);
    }

    void setSupportProgressBarIndeterminate(boolean indeterminate) {
        updateProgressBars(indeterminate ? -3 : -4);
    }

    void setSupportProgress(int progress) {
        updateProgressBars(progress + 0);
    }

    Delegate getDrawerToggleDelegate() {
        return new ActionBarDrawableToggleImpl();
    }

    private void updateProgressBars(int value) {
        ProgressBarICS circularProgressBar = getCircularProgressBar();
        ProgressBarICS horizontalProgressBar = getHorizontalProgressBar();
        if (value == -1) {
            if (this.mFeatureProgress) {
                int visibility = (horizontalProgressBar.isIndeterminate() || horizontalProgressBar.getProgress() < 10000) ? 0 : 4;
                horizontalProgressBar.setVisibility(visibility);
            }
            if (this.mFeatureIndeterminateProgress) {
                circularProgressBar.setVisibility(0);
            }
        } else if (value == -2) {
            if (this.mFeatureProgress) {
                horizontalProgressBar.setVisibility(8);
            }
            if (this.mFeatureIndeterminateProgress) {
                circularProgressBar.setVisibility(8);
            }
        } else if (value == -3) {
            horizontalProgressBar.setIndeterminate(true);
        } else if (value == -4) {
            horizontalProgressBar.setIndeterminate(false);
        } else if (value >= 0 && value <= 10000) {
            horizontalProgressBar.setProgress(value + 0);
            if (value < 10000) {
                showProgressBars(horizontalProgressBar, circularProgressBar);
            } else {
                hideProgressBars(horizontalProgressBar, circularProgressBar);
            }
        }
    }

    private void showProgressBars(ProgressBarICS horizontalProgressBar, ProgressBarICS spinnyProgressBar) {
        if (this.mFeatureIndeterminateProgress && spinnyProgressBar.getVisibility() == 4) {
            spinnyProgressBar.setVisibility(0);
        }
        if (this.mFeatureProgress && horizontalProgressBar.getProgress() < 10000) {
            horizontalProgressBar.setVisibility(0);
        }
    }

    private void hideProgressBars(ProgressBarICS horizontalProgressBar, ProgressBarICS spinnyProgressBar) {
        if (this.mFeatureIndeterminateProgress && spinnyProgressBar.getVisibility() == 0) {
            spinnyProgressBar.setVisibility(4);
        }
        if (this.mFeatureProgress && horizontalProgressBar.getVisibility() == 0) {
            horizontalProgressBar.setVisibility(4);
        }
    }

    private ProgressBarICS getCircularProgressBar() {
        ProgressBarICS pb = (ProgressBarICS) this.mActionBarView.findViewById(C0048R.id.progress_circular);
        if (pb != null) {
            pb.setVisibility(4);
        }
        return pb;
    }

    private ProgressBarICS getHorizontalProgressBar() {
        ProgressBarICS pb = (ProgressBarICS) this.mActionBarView.findViewById(C0048R.id.progress_horizontal);
        if (pb != null) {
            pb.setVisibility(4);
        }
        return pb;
    }
}
