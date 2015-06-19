package team.monroe.org.takeaway.view;


import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import org.monroe.team.corebox.utils.Closure;

public class SlidingRelativeLayout extends RelativeLayout{
    public SlidingRelativeLayout(Context context) {
        super(context);
    }

    public SlidingRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SlidingRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SlidingRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public float getYFraction() {
        final int width = getHeight();
        if (width != 0) return getY() / getHeight();
        else return getY();
    }


    public void setYFraction(float xFraction) {
        final int width = getHeight();
        float newWidth = (width > 0) ? (xFraction * width) : -9999;
        setY(newWidth);
    }


    public float getXFraction() {
        final int width = getWidth();
        if (width != 0) return getX() / getWidth();
        else return getX();
    }


    public void setXFraction(float xFraction) {
        final int width = getWidth();
        float newWidth = (width > 0) ? (xFraction * width) : -9999;
        setX(newWidth);
    }

    public Closure<Float, Void> xTranslationObserver = new Closure<Float, Void>() {
        @Override
        public Void execute(Float arg) {
            return null;
        }
    };

    @Override
    public void setTranslationX(float translationX) {
        super.setTranslationX(translationX);
        xTranslationObserver.execute(translationX);
    }


}
