package ax.nd.faceunlock;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class FaceTryAgain extends FaceBaseActivity {

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.face_try_again);
        Button buttonTry = findViewById(R.id.face_try);
        findViewById(R.id.face_try).setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setClass(FaceTryAgain.this, FaceEnrollActivity.class);
            parseIntent(intent);
            startActivity(intent);
            finish();
        });
        if (mToken == null) {
            buttonTry.setVisibility(View.INVISIBLE);
        }
        Button buttonCancel = findViewById(R.id.face_cancel);
        buttonCancel.setOnClickListener(view -> finish());
    }

    @Override
    public void onPause() {
        super.onPause();
        finish();
    }
}
