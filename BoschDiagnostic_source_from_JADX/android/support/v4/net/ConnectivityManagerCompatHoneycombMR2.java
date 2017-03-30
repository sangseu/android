package android.support.v4.net;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.media.TransportMediator;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.internal.widget.ActionBarView;
import com.eScooterDiagTool.C0102R;

class ConnectivityManagerCompatHoneycombMR2 {
    ConnectivityManagerCompatHoneycombMR2() {
    }

    public static boolean isActiveNetworkMetered(ConnectivityManager cm) {
        NetworkInfo info = cm.getActiveNetworkInfo();
        if (info == null) {
            return true;
        }
        switch (info.getType()) {
            case ActionBarView.DISPLAY_DEFAULT /*0*/:
            case CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER /*2*/:
            case FragmentManagerImpl.ANIM_STYLE_CLOSE_ENTER /*3*/:
            case TransportMediator.FLAG_KEY_MEDIA_PLAY /*4*/:
            case FragmentManagerImpl.ANIM_STYLE_FADE_ENTER /*5*/:
            case FragmentManagerImpl.ANIM_STYLE_FADE_EXIT /*6*/:
                return true;
            case CursorAdapter.FLAG_AUTO_REQUERY /*1*/:
            case C0102R.styleable.Spinner_spinnerMode /*7*/:
            case C0102R.styleable.Spinner_disableChildrenWhenDisabled /*9*/:
                return false;
            default:
                return true;
        }
    }
}
