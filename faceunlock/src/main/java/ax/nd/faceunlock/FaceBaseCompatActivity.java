package ax.nd.faceunlock;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class FaceBaseCompatActivity extends AppCompatActivity {
    protected boolean mLaunchedConfirmLock;
    protected byte[] mToken;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        byte[] byteArrayExtra = getIntent().getByteArrayExtra(AppConstants.EXTRA_KEY_CHALLENGE_TOKEN);
        this.mToken = byteArrayExtra;
        if (bundle != null && byteArrayExtra == null) {
            this.mLaunchedConfirmLock = bundle.getBoolean(AppConstants.EXTRA_KEY_LAUNCHED_CONFIRM);
            this.mToken = bundle.getByteArray(AppConstants.EXTRA_KEY_CHALLENGE_TOKEN);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putBoolean(AppConstants.EXTRA_KEY_LAUNCHED_CONFIRM, this.mLaunchedConfirmLock);
        bundle.putByteArray(AppConstants.EXTRA_KEY_CHALLENGE_TOKEN, this.mToken);
    }
}
