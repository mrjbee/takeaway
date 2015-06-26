package team.monroe.org.takeaway.view;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import team.monroe.org.takeaway.R;

public class WarningViewPresenter {

    private final View mRootView;
    private final TextView mCaptionText;
    private final TextView mDescriptionText;
    private final ImageView mImageView;

    public WarningViewPresenter(View panelView, View.OnClickListener refreshListener) {
        this.mRootView = panelView;
        mCaptionText = (TextView) mRootView.findViewById(R.id.text_error_caption);
        mDescriptionText = (TextView) mRootView.findViewById(R.id.text_error_description);
        mImageView = (ImageView) mRootView.findViewById(R.id.image_error);
        mRootView.findViewById(R.id.action_refresh).setOnClickListener(refreshListener);
    }

    public void hide() {
        mRootView.setVisibility(View.GONE);
    }

    public void show() {
        mRootView.setVisibility(View.VISIBLE);
    }

    public void updateDetails(WarningType error, String description, String extraDescription) {
        mDescriptionText
                .setText(extraDescription == null ? description: description+" ["+extraDescription+"] ");
        if (error == WarningType.ERROR){
            mCaptionText.setText("Uppps, something goes wrong !");
            mImageView.setImageResource(R.drawable.android_error_big);
        }else {
            mCaptionText.setText("Sorry, but no music here !");
            mImageView.setImageResource(R.drawable.android_music_queue_big);
        }
    }


    public static enum WarningType{
        ERROR, NO_ITEMS
    }
}
