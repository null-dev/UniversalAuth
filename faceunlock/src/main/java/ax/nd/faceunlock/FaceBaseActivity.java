package ax.nd.faceunlock;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class FaceBaseActivity extends Activity {
    protected boolean mLaunchedConfirmLock;
    protected byte[] mToken;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        mToken = getIntent().getByteArrayExtra(AppConstants.EXTRA_KEY_CHALLENGE_TOKEN);
        if (bundle != null && mToken == null) {
            mLaunchedConfirmLock = bundle.getBoolean(AppConstants.EXTRA_KEY_LAUNCHED_CONFIRM);
            mToken = bundle.getByteArray(AppConstants.EXTRA_KEY_CHALLENGE_TOKEN);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putBoolean(AppConstants.EXTRA_KEY_LAUNCHED_CONFIRM, mLaunchedConfirmLock);
        bundle.putByteArray(AppConstants.EXTRA_KEY_CHALLENGE_TOKEN, mToken);
    }

    protected void parseIntent(Intent intent) {
        intent.putExtra(AppConstants.EXTRA_KEY_CHALLENGE_TOKEN, mToken);
    }
}
