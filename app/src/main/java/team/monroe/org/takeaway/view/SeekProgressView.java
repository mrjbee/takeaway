package team.monroe.org.takeaway.view;

import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import org.monroe.team.android.box.utils.DisplayUtils;

import team.monroe.org.takeaway.R;

public class SeekProgressView extends View{

    private Paint mValuePaint;
    private Paint mBackgroundPaint;
    private float mProgress = 0.1f;
    private float mPublicProgress = 0.1f;
    private ObjectAnimator mProgressAnimator;
    private SeekListener mSeekListener = NO_OP_SEEK_LISTENER;

    private final static SeekListener NO_OP_SEEK_LISTENER = new SeekListener() {
        @Override
        public void onSeekStart(SeekProgressView seekProgressView, float progress) {}
        @Override
        public void onSeekStop(SeekProgressView seekProgressView, float progress) {}
        @Override
        public void onSeek(SeekProgressView seekProgressView, float progress) {}
    };

    public SeekProgressView(Context context) {
        super(context);
        init();
    }


    public SeekProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SeekProgressView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SeekProgressView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        mValuePaint = new Paint();
        mValuePaint.setStrokeWidth(DisplayUtils.dpToPx(3f, getResources()));
        mValuePaint.setAntiAlias(true);
        mValuePaint.setColor(getResources().getColor(R.color.text_highlight));

        mBackgroundPaint = new Paint();
        mBackgroundPaint.setStrokeWidth(DisplayUtils.dpToPx(3f, getResources()));
        mBackgroundPaint.setAntiAlias(true);
        mBackgroundPaint.setColor(getResources().getColor(R.color.gray));
        mBackgroundPaint.setAlpha(10);
    }

    public SeekListener getSeekListener() {
        if (mSeekListener == NO_OP_SEEK_LISTENER) return null;
        return mSeekListener;
    }

    public void setSeekListener(SeekListener seekListener) {
        this.mSeekListener = seekListener;
        if (mSeekListener == null){
            mSeekListener = NO_OP_SEEK_LISTENER;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float progress = calculateProgress(event);
        setProgress(progress, AnimationSpeed.NO_ANIMATION);
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
              mSeekListener.onSeekStart(this, progress);
              break;
            case MotionEvent.ACTION_MOVE:
              mSeekListener.onSeek(this, progress);
              break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
              mSeekListener.onSeekStop(this, progress);
              break;
        }
        return true;
    }

    private float calculateProgress(MotionEvent event) {
        return event.getX()/(float)getWidth();
    }


    @Override
    protected void onDraw(Canvas canvas) {
        float linePosition = getHeight()/2;
        canvas.drawLine(0,linePosition,getWidth(),linePosition, mBackgroundPaint);
        float valueWidth = getWidth() * mProgress;
        canvas.drawLine(0,linePosition,valueWidth,linePosition, mValuePaint);
    }

    public float getProgress() {
        return mPublicProgress;
    }

    public void setProgress(float progress, AnimationSpeed speed) {
        if (mPublicProgress == progress) return;
        this.mPublicProgress = progress;

        cancelAnimator();

        if (speed == AnimationSpeed.NO_ANIMATION){
            setInternalProgress(progress);
            return;
        }

        mProgressAnimator = ObjectAnimator.ofFloat(this, "internalProgress", mProgress, progress);
        float diff = Math.abs(mProgress - progress);
        long duration = (long) (diff * 1000 * (speed == AnimationSpeed.NORMAL ? 1 : 2));
        mProgressAnimator.setDuration(Math.max(500, duration));
        mProgressAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        mProgressAnimator.start();
    }

    private void cancelAnimator() {
        if (mProgressAnimator != null){
            mProgressAnimator.cancel();
            mProgressAnimator = null;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        cancelAnimator();
        super.onDetachedFromWindow();
    }

    @Deprecated
    public float getInternalProgress() {
        return mProgress;
    }

    @Deprecated
    public void setInternalProgress(float progress) {
        this.mProgress = progress;
        this.invalidate();
    }

    public static enum AnimationSpeed {
        NO_ANIMATION, NORMAL, SLOW
    }

    public interface SeekListener {
        void onSeekStart(SeekProgressView seekProgressView, float progress);
        void onSeekStop(SeekProgressView seekProgressView, float progress);
        void onSeek(SeekProgressView seekProgressView, float progress);
    }



}
