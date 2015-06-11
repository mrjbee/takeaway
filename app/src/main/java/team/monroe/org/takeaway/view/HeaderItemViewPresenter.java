package team.monroe.org.takeaway.view;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.monroe.team.android.box.app.ui.animation.apperrance.AppearanceController;
import org.monroe.team.android.box.utils.DisplayUtils;

import static org.monroe.team.android.box.app.ui.animation.apperrance.AppearanceControllerBuilder.*;

import team.monroe.org.takeaway.R;

public interface HeaderItemViewPresenter {

    public void select(boolean animate);
    public void deselect(boolean animate);
    public void onClick(View.OnClickListener listener);

    public static class RootItemViewPresenter implements HeaderItemViewPresenter{

        private final View rootView;
        private final View mainPanel;
        private final AppearanceController ac_main_panel;

        public RootItemViewPresenter(View rootView) {
            this.rootView = rootView;
            mainPanel = rootView.findViewById(R.id.panel_main);
            ac_main_panel = animateAppearance(mainPanel, alpha(1f, 0f))
                    .showAnimation(duration_constant(200),interpreter_accelerate(0.5f))
                    .hideAnimation(duration_constant(200), interpreter_accelerate(1f))
                    .hideAndGone()
                    .build();
        }

        @Override
        public void select(boolean animate) {
            if (animate){
                ac_main_panel.show();
            }else {
                ac_main_panel.showWithoutAnimation();
            }
        }

        @Override
        public void deselect(boolean animate) {
            if (animate){
                ac_main_panel.hide();
            }else {
                ac_main_panel.hideWithoutAnimation();
            }
        }

        @Override
        public void onClick(View.OnClickListener listener) {
            rootView.setOnClickListener(listener);
        }
    }

    public static class DefaultItemViewPresenter implements HeaderItemViewPresenter{

        private final View rootView;
        private final ImageView imageView;
        private final ImageView selectedImageView;
        private final TextView captionView;
        private final AppearanceController ac_caption;
        private final AppearanceController ac_image;

        public DefaultItemViewPresenter(View rootView, Context context) {
            this.rootView = rootView;
            imageView = (ImageView) rootView.findViewById(R.id.image);
            selectedImageView = (ImageView) rootView.findViewById(R.id.image_selected);
            captionView = (TextView) rootView.findViewById(R.id.caption);
            ac_caption = animateAppearance(captionView, scale(1f, 0f))
                    .showAnimation(duration_constant(200),interpreter_overshot())
                    .hideAnimation(duration_constant(200), interpreter_accelerate(1f))
                    .hideAndGone()
                    .build();
            ac_image = combine(
                    animateAppearance(rootView.findViewById(R.id.panel_images),
                            heightSlide(
                                    (int)DisplayUtils.dpToPx(18, context.getResources()),
                                    (int)DisplayUtils.dpToPx(25, context.getResources())))
                        .showAnimation(duration_constant(200), interpreter_overshot())
                        .hideAnimation(duration_constant(200), interpreter_accelerate(1f)),

                    animateAppearance(selectedImageView, alpha(1f, 0f))
                        .showAnimation(duration_constant(200), interpreter_overshot())
                        .hideAnimation(duration_constant(200), interpreter_accelerate(1f))
                        .hideAndGone()
                    );
        }

        public void setup(String caption, int resourceId, int selectedResourceId){
            imageView.setImageResource(resourceId);
            selectedImageView.setImageResource(selectedResourceId);
            captionView.setText(caption);
        }

        @Override
        public void select(boolean animate) {
            if (animate){
                ac_caption.show();
                ac_image.show();
            }else {
                ac_caption.showWithoutAnimation();
                ac_image.showWithoutAnimation();
            }
        }

        @Override
        public void deselect(boolean animate) {
            if (animate){
                ac_caption.hide();
                ac_image.hide();
            }else {
                ac_caption.hideWithoutAnimation();
                ac_image.hideWithoutAnimation();
            }
        }

        @Override
        public void onClick(View.OnClickListener listener) {
            rootView.setOnClickListener(listener);
        }
    }

}
