package ax.nd.faceunlock.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.SurfaceView;

import ax.nd.faceunlock.R;

public class CircleSurfaceView extends SurfaceView {
    private float m_progress = 0.0f;

    public CircleSurfaceView(Context context) {
        super(context);
    }

    public CircleSurfaceView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public CircleSurfaceView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    public void setPercent(float f) {
        if (f >= 0.0f && f <= 100.0f) {
            m_progress = f;
        }
    }

    @Override
    public void draw(Canvas canvas) {
        float measuredWidth = (float) (getMeasuredWidth() / 2);
        float measuredHeight = (float) (getMeasuredHeight() / 2);
        float min = Math.min(measuredWidth, measuredHeight);
        RectF rectF = new RectF(measuredWidth - min, measuredHeight - min, measuredWidth + min, measuredHeight + min);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(getContext().getColor(R.color.enroll_progress_bar));
        canvas.drawArc(rectF, 270.0f, 360.0f, true, paint);
        paint.setColor(getContext().getColor(R.color.theme_color));
        canvas.drawArc(rectF, 270.0f, m_progress * 3.6f, true, paint);
        Path path = new Path();
        path.addCircle(measuredWidth, measuredHeight, min * 0.95f, Path.Direction.CCW);
        canvas.clipPath(path);
        super.draw(canvas);
        invalidate();
    }
}
