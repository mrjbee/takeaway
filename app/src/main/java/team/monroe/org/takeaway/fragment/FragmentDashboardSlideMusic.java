package team.monroe.org.takeaway.fragment;

import android.os.Bundle;
import android.view.View;

import org.monroe.team.android.box.app.ActivitySupport;
import org.monroe.team.android.box.data.Data;
import org.monroe.team.corebox.utils.Lists;

import java.util.ArrayList;
import java.util.List;

import team.monroe.org.takeaway.R;
import team.monroe.org.takeaway.presentations.Folder;
import team.monroe.org.takeaway.presentations.FolderContent;

public class FragmentDashboardSlideMusic extends FragmentDashboardSlide {

    private View mLoadingPanel;
    private View mNoItemsPanel;
    private View mItemsPanel;
    private List<Folder> mFolderStack;
    private Data<FolderContent> mFolderData;
    private Data.DataChangeObserver<FolderContent> mDataObserver;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_dashboard_slide_music;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null){
            mFolderStack = (List<Folder>) savedInstanceState.getSerializable("browse_folder");
        }
        if (mFolderStack == null){
            mFolderStack = new ArrayList<>();
            mFolderStack.add(Folder.FOLDER_ROOT);
        }

        mLoadingPanel = view(R.id.panel_loading);
        mNoItemsPanel = view(R.id.panel_no_items);
        mItemsPanel = view(R.id.panel_items);

        mLoadingPanel.setVisibility(View.GONE);
        mNoItemsPanel.setVisibility(View.GONE);
        mItemsPanel.setVisibility(View.GONE);
    }

    @Override
    public void onStart() {
        super.onStart();
        Folder folder = getTopFolder();
        setup_folder(folder);
    }

    @Override
    public void onStop() {
        super.onStop();
        deinit_folder_data();
    }

    private void deinit_folder_data() {
        if (mFolderData != null) {
            mFolderData.removeDataChangeObserver(mDataObserver);
            mFolderData = null;
        }
    }

    private void setup_folder(final Folder folder) {
        deinit_folder_data();
        mLoadingPanel.setVisibility(View.VISIBLE);

        mNoItemsPanel.setVisibility(View.GONE);
        mNoItemsPanel.setVisibility(View.GONE);

        mFolderData = application().data_range_folder.getOrCreate(folder);
        mDataObserver = new Data.DataChangeObserver<FolderContent>() {
            @Override
            public void onDataInvalid() {
                fetch_folder(folder);
            }

            @Override
            public void onData(FolderContent folderContent) {
                onFolderData(folderContent);
            }
        };
        mFolderData.addDataChangeObserver(mDataObserver);
        fetch_folder(folder);
    }

    private void fetch_folder(Folder folder) {
        mFolderData.fetch(true, activity().observe_data(new ActivitySupport.OnValue<FolderContent>() {
            @Override
            public void action(FolderContent folderContent) {
                onFolderData(folderContent);
            }
        }));
    }

    private void onFolderData(FolderContent folderContent) {

       if (folderContent.folder != getTopFolder()) return;

       mLoadingPanel.setVisibility(View.GONE);
       if (folderContent.subFolders.isEmpty()){
           mNoItemsPanel.setVisibility(View.VISIBLE);
       }else {
           mItemsPanel.setVisibility(View.VISIBLE);

       }
    }

    private Folder getTopFolder() {
        return Lists.getLast(mFolderStack);
    }
}
