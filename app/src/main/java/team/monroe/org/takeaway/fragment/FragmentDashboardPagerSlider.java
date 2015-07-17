package team.monroe.org.takeaway.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.MotionEvent;
import android.view.View;

import team.monroe.org.takeaway.R;
import team.monroe.org.takeaway.fragment.common.FragmentPagerAdapter;
import team.monroe.org.takeaway.fragment.contract.ContractBackButton;

public class FragmentDashboardPagerSlider extends FragmentDashboardActivity implements ContractBackButton{

    private ViewPager mViewPager;
    private FragmentPagerAdapter mFragmentPagerAdapter;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_dashboard_page_slider;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewPager = view(R.id.view_pager,ViewPager.class);
        mFragmentPagerAdapter = new FragmentPagerAdapter(activity().getFragmentManager()) {

            @Override
            public Fragment getItem(int position) {
                switch (position){
                    case 0: return new FragmentDashboardSlideMusic();
                    case 1: return new FragmentDashboardSlideHome();
                    case 2: return new FragmentDashboardSlideSearch();
                    default:
                        throw new IllegalStateException();
                }
            }

            @Override
            public int getCount() {
                return 3;
            }
        };
        mViewPager.setAdapter(mFragmentPagerAdapter);
        mFragmentPagerAdapter.notifyDataSetChanged();
        if (getArguments().getBoolean("first_run", false)) {
            mViewPager.setCurrentItem(1);
            getArguments().putBoolean("first_run", false);
        }
        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                dashboard().onScreenChanged(position);
                if (getPage(position) ==null){
                    return;
                }
                getPage(position).onSelect();
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mFragmentPagerAdapter != null) {
            mFragmentPagerAdapter = null;
        }
    }

    @Override
    public boolean onBackPressed() {
        FragmentDashboardSlide dashboardSlide = getCurrentSlide();
        if (dashboardSlide instanceof ContractBackButton){
            if (((ContractBackButton) dashboardSlide).onBackPressed()){
                return true;
            }
        }

        if (mViewPager.getCurrentItem() == 1) return false;
        mViewPager.setCurrentItem(1, true);
        return true;
    }

    public void updateScreen(int screenPosition) {
        mViewPager.setCurrentItem(screenPosition, true);
    }

    private FragmentDashboardSlide getPage(int pageIndex) {
        String pageTag = "android:switcher:" + mViewPager.getId() + ":" + pageIndex;
        return (FragmentDashboardSlide) getFragmentManager().findFragmentByTag(pageTag);
    }

    public FragmentDashboardSlide getCurrentSlide() {
        int curItem = mViewPager.getCurrentItem();
        Fragment fragment = getPage(curItem);
        return (FragmentDashboardSlide) fragment;
    }

    public void viewPagerGesture(boolean enabled) {
        mViewPager.requestDisallowInterceptTouchEvent(!enabled);
    }
}
