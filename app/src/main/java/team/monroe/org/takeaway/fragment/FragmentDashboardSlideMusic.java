package team.monroe.org.takeaway.fragment;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import org.monroe.team.android.box.app.ActivitySupport;
import org.monroe.team.android.box.app.ui.GenericListViewAdapter;
import org.monroe.team.android.box.app.ui.GetViewImplementation;
import org.monroe.team.android.box.data.Data;
import org.monroe.team.corebox.utils.Lists;

import java.util.ArrayList;
import java.util.List;

import team.monroe.org.takeaway.R;
import team.monroe.org.takeaway.fragment.contract.ContractBackButton;
import team.monroe.org.takeaway.presentations.Folder;
import team.monroe.org.takeaway.presentations.FolderContent;

public class FragmentDashboardSlideMusic extends FragmentDashboardSlide  implements ContractBackButton{

    private View mLoadingPanel;
    private View mNoItemsPanel;
    private View mItemsPanel;
    private List<Folder> mFolderStack;
    private Data<FolderContent> mFolderData;
    private Data.DataChangeObserver<FolderContent> mDataObserver;
    private ListView mFileList;
    private GenericListViewAdapter<Folder, GetViewImplementation.ViewHolder<Folder>> mFolderAdapter;

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

        mFileList = view_list(R.id.list_items);
        mFolderAdapter = new GenericListViewAdapter<Folder, GetViewImplementation.ViewHolder<Folder>>(activity(), new GetViewImplementation.ViewHolderFactory<GetViewImplementation.ViewHolder<Folder>>() {
            @Override
            public GetViewImplementation.ViewHolder<Folder> create(final View convertView) {
                return new GetViewImplementation.ViewHolder<Folder>() {

                    TextView caption = (TextView) convertView.findViewById(R.id.item_caption);

                    @Override
                    public void update(Folder folder, int position) {
                        caption.setText(folder.title);
                    }

                    @Override
                    public void cleanup() {

                    }
                };
            }
        }, R.layout.item_debug);
        mFileList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Folder folder = mFolderAdapter.getItem(position);
                mFolderStack.add(folder);
                setup_folder(folder);
            }
        });
        mFileList.setAdapter(mFolderAdapter);
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
        mItemsPanel.setVisibility(View.GONE);

        mFolderData = application().data_range_folder.getOrCreate(folder);
        mDataObserver = new Data.DataChangeObserver<FolderContent>() {
            @Override
            public void onDataInvalid() {
                fetch_folder();
            }

            @Override
            public void onData(FolderContent folderContent) {

            }
        };
        mFolderData.addDataChangeObserver(mDataObserver);
        fetch_folder();
    }

    private void fetch_folder() {
        mFolderData.fetch(true, activity().observe_data(new ActivitySupport.OnValue<FolderContent>() {
            @Override
            public void action(FolderContent folderContent) {
                onFolderData(folderContent);
            }
        }));
    }

    private void onFolderData(FolderContent folderContent) {

       if (folderContent.folder != getTopFolder()) throw new IllegalStateException("Something bad");

       mLoadingPanel.setVisibility(View.GONE);
       if (folderContent.subFolders.isEmpty()){
           mNoItemsPanel.setVisibility(View.VISIBLE);
       }else {
           mItemsPanel.setVisibility(View.VISIBLE);
           mFolderAdapter.clear();
           mFolderAdapter.addAll(folderContent.subFolders);
           mFolderAdapter.notifyDataSetChanged();
       }
    }

    private Folder getTopFolder() {
        return Lists.getLast(mFolderStack);
    }

    @Override
    public boolean onBackPressed() {
        if (mFolderStack.size() > 1){
            mFolderStack.remove(mFolderStack.size() - 1);
            setup_folder(getTopFolder());
            return true;
        }
        return false;
    }
}
