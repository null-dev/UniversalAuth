package ax.nd.faceunlock;

import android.os.Bundle;

public class FaceUpgradeFinish extends FaceBaseActivity {
    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.face_upgrade_finish);
        findViewById(R.id.singleButton).setOnClickListener(view -> {
            setResult(-1, null);
            finish();
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        setResult(-1, null);
    }
}
