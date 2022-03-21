package ax.nd.faceunlock.widget;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.view.Surface;
import android.view.TextureView;

public class VideoListener implements TextureView.SurfaceTextureListener {
    private final Context mContext;
    private final boolean mLooping;
    private final TextureView mTextureView;
    private final int mVideoId;
    private MediaPlayer mMediaPlayer;
    private Surface mSurface = null;
    private final MediaPlayer.OnPreparedListener mListener = mediaPlayer -> {
        if (VideoListener.this.mSurface != null) {
            mediaPlayer.setSurface(VideoListener.this.mSurface);
            VideoListener.this.mTextureView.setFocusable(false);
            VideoListener.this.mTextureView.setFocusableInTouchMode(false);
            mediaPlayer.setLooping(VideoListener.this.mLooping);
            mediaPlayer.start();
        }
    };

    public VideoListener(Context context, TextureView textureView, int i, boolean z) {
        mContext = context;
        textureView.setSurfaceTextureListener(this);
        mTextureView = textureView;
        mVideoId = i;
        mLooping = z;
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i2) {
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i2) {
        mSurface = new Surface(surfaceTexture);
        setupMediaPlayer();
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        Surface surface = mSurface;
        if (surface != null) {
            surface.release();
        }
        mSurface = null;
        stopVideo();
        return true;
    }

    private void fixAspectRatio() {
        int i;
        TextureView textureView = mTextureView;
        if (textureView != null && mMediaPlayer != null) {
            int width = textureView.getWidth();
            int height = mTextureView.getHeight();
            double videoHeight = ((double) mMediaPlayer.getVideoHeight()) / ((double) mMediaPlayer.getVideoWidth());
            int i2 = (int) (((double) width) * videoHeight);
            if (height > i2) {
                i = width;
            } else {
                i = (int) (((double) height) / videoHeight);
                i2 = height;
            }
            Matrix matrix = new Matrix();
            mTextureView.getTransform(matrix);
            matrix.setScale(((float) i) / ((float) width), ((float) i2) / ((float) height));
            matrix.postTranslate((float) ((width - i) / 2), (float) (height - i2));
            mTextureView.setTransform(matrix);
        }
    }

    private void setupMediaPlayer() {
        mMediaPlayer = MediaPlayer.create(mContext, mVideoId);
        fixAspectRatio();
        mMediaPlayer.setOnPreparedListener(mListener);
    }

    public void startVideo() {
        if (mMediaPlayer == null && mSurface != null) {
            setupMediaPlayer();
        }
    }

    public void stopVideo() {
        MediaPlayer mediaPlayer = mMediaPlayer;
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
            }
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }
}
