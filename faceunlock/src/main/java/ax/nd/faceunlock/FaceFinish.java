package ax.nd.faceunlock;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;

import ax.nd.faceunlock.util.Settings;
import ax.nd.faceunlock.util.Util;

public class FaceFinish extends Activity {
    private static final String TAG = FaceFinish.class.getSimpleName();

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.face_finish);
        final boolean enrollSuccess = getIntent().getBooleanExtra(SetupFaceIntroActivity.EXTRA_ENROLL_SUCCESS, true);
        Button buttonDone = findViewById(R.id.btn_done);
        buttonDone.setOnClickListener(view -> {
            setResult(enrollSuccess ? -1 : 0, null);
            finish();
        });
        Button buttonNext = findViewById(R.id.btn_next);
        buttonNext.setOnClickListener(view -> startFaceUpgrageFinishActivity());
        /*if (Util.isByPassLockScreenAvailable(this)){
            @SuppressLint("UseSwitchCompatOrMaterialCode") Switch bypassLockscreenSwitch = findViewById(R.id.bypassLockscreenSwitch);
            bypassLockscreenSwitch.setChecked(Settings.isByPassLockScreenEnabled(this));
            bypassLockscreenSwitch.setOnCheckedChangeListener((compoundButton, enabled) -> Settings.setByPassLockScreenEnabled(FaceFinish.this, enabled));
        }else{
            findViewById(R.id.ll_bypass_lock_screen).setVisibility(View.GONE);
            findViewById(R.id.face_settings_tip).setVisibility(View.GONE);
        }*/
    }

    @Override
    protected void onActivityResult(int i, int i2, Intent intent) {
        super.onActivityResult(i, i2, intent);
        if (i == 1) {
            boolean enrollSuccess = getIntent().getBooleanExtra(SetupFaceIntroActivity.EXTRA_ENROLL_SUCCESS, true);
            setResult(enrollSuccess ? -1 : 0, null);
            if (Util.DEBUG) {
                Log.i(TAG, "result: " + enrollSuccess);
            }
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        setResult(-1);
        finish();
    }

    private void startFaceUpgrageFinishActivity() {
        startActivityForResult(new Intent(this, FaceUpgradeFinish.class), 1);
    }
}
