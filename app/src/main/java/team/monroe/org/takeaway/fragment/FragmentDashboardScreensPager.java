package team.monroe.org.takeaway.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v4.view.ViewPager;

import team.monroe.org.takeaway.R;
import team.monroe.org.takeaway.fragment.common.FragmentPagerAdapter;
import team.monroe.org.takeaway.fragment.contract.ContractBackButton;

public class FragmentDashboardScreensPager extends FragmentDashboardScreen implements ContractBackButton{

    private ViewPager mViewPager;
    private FragmentPagerAdapter mFragmentPagerAdapter;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_body_main;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewPager = view(R.id.view_pager,ViewPager.class);
        mFragmentPagerAdapter = new FragmentPagerAdapter(activity().getFragmentManager()) {

            @Override
            public Fragment getItem(int position) {
                switch (position){
                    case 0: return new FragmentDashboardMyMusic();
                    case 1: return new FragmentDashboardHome();
                    case 2: return new FragmentDashboardSearch();
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
        int curItem = mViewPager.getCurrentItem();
        if (curItem == 1) return false;
        mViewPager.setCurrentItem(1, true);
        return true;
    }

    public void updateScreen(int screenPosition) {
        mViewPager.setCurrentItem(screenPosition, true);
    }
}
