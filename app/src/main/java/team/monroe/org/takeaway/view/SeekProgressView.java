package team.monroe.org.takeaway.view;

import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import org.monroe.team.android.box.app.ui.animation.apperrance.AppearanceController;
import static org.monroe.team.android.box.app.ui.animation.apperrance.AppearanceControllerBuilder.*;

import org.monroe.team.android.box.app.ui.animation.apperrance.DefaultAppearanceController;
import org.monroe.team.android.box.utils.DisplayUtils;
import org.monroe.team.android.box.utils.Views;

import team.monroe.org.takeaway.R;

public class SeekProgressView extends View{

    private static float FRACTION_PICKER_MIN = 0.25f;
    private static float FRACTION_PICKER_MAX = 1f;

    private Paint mValuePaint;
    private Paint mBackgroundPaint;
    private float mProgress = 0f;
    private float mPublicProgress = 0f;
    private float mPickerMaxRadius;
    private float mPickerFraction = FRACTION_PICKER_MIN;


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
    private AppearanceController ac_picker;

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
        initPickerAnimation();
        mValuePaint = new Paint();
        mValuePaint.setStrokeWidth(DisplayUtils.dpToPx(3f, getResources()));
        mValuePaint.setAntiAlias(true);
        mValuePaint.setColor(Views.color(this, R.color.text_highlight, Color.RED));
        mBackgroundPaint = new Paint();
        mBackgroundPaint.setStrokeWidth(DisplayUtils.dpToPx(2f, getResources()));
        mBackgroundPaint.setAntiAlias(true);
        mBackgroundPaint.setColor(Views.color(this, R.color.gray, Color.GRAY));
        mBackgroundPaint.setAlpha(10);

        mPickerMaxRadius = DisplayUtils.dpToPx(15f, getResources());
        ac_picker.hideWithoutAnimation();
    }

    private void initPickerAnimation() {
        ac_picker = animateAppearance(this, new TypeBuilder<Float>() {
            @Override
            public DefaultAppearanceController.ValueGetter<Float> buildValueGetter() {
                return new DefaultAppearanceController.ValueGetter<Float>() {
                    @Override
                    public Float getShowValue() {
                        return FRACTION_PICKER_MAX;
                    }

                    @Override
                    public Float getHideValue() {
                        return FRACTION_PICKER_MIN;
                    }

                    @Override
                    public Float getCurrentValue(View view) {
                        return mPickerFraction;
                    }
                };
            }

            @Override
            public TypedValueSetter<Float> buildValueSetter() {
                return new TypedValueSetter<Float>(Float.class) {
                    @Override
                    public void setValue(View view, Float value) {
                         mPickerFraction = value;
                         invalidate();
                    }
                };
            }
        })
        .showAnimation(duration_constant(100), interpreter_accelerate_decelerate())
        .hideAnimation(duration_constant(300), interpreter_overshot())
                .build();
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
              ac_picker.show();
              break;
            case MotionEvent.ACTION_MOVE:
              mSeekListener.onSeek(this, progress);
              break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
              mSeekListener.onSeekStop(this, progress);
              ac_picker.hide();
              break;
        }
        return true;
    }

    private float calculateProgress(MotionEvent event) {
        float xPosition = event.getX() - draw_ProgressPlaceStartPosition();
        float answer =  xPosition/(float)getWidth();
        return Math.min(1,Math.max(answer, 0f));
    }


    @Override
    protected void onDraw(Canvas canvas) {
        float linePosition = getHeight()/2;
        canvas.drawLine(
                draw_ProgressPlaceStartPosition(),
                linePosition,
                draw_ProgressPlaceStartPosition() + draw_ProgressPlaceWidth(),
                linePosition,
                mBackgroundPaint);
        float valueWidth = draw_ProgressPlaceWidth() * mProgress;
        canvas.drawLine(
                draw_ProgressPlaceStartPosition(),
                linePosition,
                draw_ProgressPlaceStartPosition() + valueWidth,
                linePosition,
                mValuePaint);
        float pickerRadius = mPickerMaxRadius * mPickerFraction;
        canvas.drawCircle(draw_ProgressPlaceStartPosition() + valueWidth, linePosition, pickerRadius, mValuePaint);
    }

    private float draw_ProgressPlaceStartPosition() {
        return mPickerMaxRadius;
    }

    private float draw_ProgressPlaceWidth() {
        return getWidth() - draw_ProgressPlaceStartPosition() * 2;
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
