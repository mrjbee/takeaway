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

import java.util.ArrayList;
import java.util.List;

import team.monroe.org.takeaway.R;
import team.monroe.org.takeaway.fragment.contract.ContractBackButton;
import team.monroe.org.takeaway.presentations.FilePointer;
import team.monroe.org.takeaway.presentations.Folder;
import team.monroe.org.takeaway.presentations.FolderContent;
import team.monroe.org.takeaway.presentations.Source;

public class FragmentDashboardSlideMusic extends FragmentDashboardSlide  implements ContractBackButton{

    private View mLoadingPanel;
    private View mNoItemsPanel;
    private View mItemsPanel;
    private View mSourcePanel;

    private ArrayList<FilePointer> mFileStack;
    private Data<List<FilePointer>> mFolderData;
    private Data.DataChangeObserver<List<FilePointer>> mDataObserver;

    private ListView mFileList;
    private GenericListViewAdapter<FilePointer, GetViewImplementation.ViewHolder<FilePointer>> mFolderAdapter;
    private Data.DataChangeObserver<List<Source>> mSourcesDataListener;
    private List<Source> mSources;
    private ListView mSourcesList;
    private GenericListViewAdapter<Source, GetViewImplementation.ViewHolder<Source>> mSourceListAdapter;
    private View mHeaderFilesView;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_dashboard_slide_music;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null){
            mFileStack = (ArrayList<FilePointer>) savedInstanceState.getSerializable("browse_folder");
        }

        if (mFileStack == null){
            mFileStack = new ArrayList<>();
        }

        mHeaderFilesView = activity().getLayoutInflater().inflate(R.layout.panel_header_files, null);

        mLoadingPanel = view(R.id.panel_loading);
        mNoItemsPanel = view(R.id.panel_no_items);
        mItemsPanel = view(R.id.panel_items);
        mSourcePanel = view(R.id.panel_sources);

        visibility_all(View.GONE);

        mFileList = view_list(R.id.list_items);
        mFileList.addHeaderView(mHeaderFilesView,null,false);
        mFileList.addFooterView(activity().getLayoutInflater().inflate(R.layout.panel_bottom_files,null),null,false);

        mSourcesList = view_list(R.id.list_sources);

        mFolderAdapter = new GenericListViewAdapter<FilePointer, GetViewImplementation.ViewHolder<FilePointer>>(activity(), new GetViewImplementation.ViewHolderFactory<GetViewImplementation.ViewHolder<FilePointer>>() {
            @Override
            public GetViewImplementation.ViewHolder<FilePointer> create(final View convertView) {
                return new GetViewImplementation.ViewHolder<FilePointer>() {

                    TextView caption = (TextView) convertView.findViewById(R.id.item_caption);
                    View separator = convertView.findViewById(R.id.separator);

                    @Override
                    public void update(FilePointer filePointer, int position) {
                        separator.setVisibility(position == 0? View.GONE : View.VISIBLE);
                        caption.setText(filePointer.name);
                    }

                    @Override
                    public void cleanup() {

                    }
                };
            }
        }, R.layout.item_file_list);
        mFileList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                FilePointer filePointer = mFolderAdapter.getItem(position - 1);
                mFileStack.add(0, filePointer);
                update_folder();
            }
        });
        mFileList.setAdapter(mFolderAdapter);

        mSourceListAdapter = new GenericListViewAdapter<Source,GetViewImplementation.ViewHolder<Source>>(activity(), new GetViewImplementation.ViewHolderFactory<GetViewImplementation.ViewHolder<Source>>() {
            @Override
            public GetViewImplementation.ViewHolder<Source> create(final View convertView) {
                return new GetViewImplementation.GenericViewHolder<Source>() {
                    TextView caption = (TextView) convertView.findViewById(R.id.item_caption);

                    @Override
                    public void update(Source source, int position) {
                        caption.setText(source.title);
                    }
                };
            }
        }, R.layout.item_debug);
        mSourcesList.setAdapter(mSourceListAdapter);

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("browse_folder",mFileStack);
    }

    private void visibility_all(int visibility) {
        mLoadingPanel.setVisibility(visibility);
        mNoItemsPanel.setVisibility(visibility);
        mItemsPanel.setVisibility(visibility);
        mSourcePanel.setVisibility(visibility);
    }

    @Override
    public void onStart() {
        super.onStart();
        mSourcesDataListener = new Data.DataChangeObserver<List<Source>>() {
            @Override
            public void onDataInvalid() {
                fetch_sources(false);
            }
            @Override
            public void onData(List<Source> sources) {}
        };
        application().data_sources.addDataChangeObserver(mSourcesDataListener);
        fetch_sources(false);
        if(mFileStack.isEmpty()){
            fetch_sources(true);
        }else{
            update_folder();
        }
    }

    private void update_folder() {
        FilePointer filePointer = mFileStack.get(0);
        mFolderData = application().data_range_folder.getOrCreate(filePointer);
        mDataObserver = new Data.DataChangeObserver<List<FilePointer>>() {
            @Override
            public void onDataInvalid() {
                 fetch_folder();
            }

            @Override
            public void onData(List<FilePointer> filePointers) {}
        };
        mFolderData.addDataChangeObserver(mDataObserver);
        fetch_folder();
    }

    private void fetch_folder() {
        show_loading();
        mFolderData.fetch(true, activity().observe_data(new ActivitySupport.OnValue<List<FilePointer>>() {
            @Override
            public void action(List<FilePointer> filePointers) {
                visibility_all(View.GONE);
                if (filePointers.isEmpty()){
                    mNoItemsPanel.setVisibility(View.VISIBLE);
                }else {
                    mFolderAdapter.clear();
                    mFolderAdapter.addAll(filePointers);
                    mFolderAdapter.notifyDataSetChanged();
                    mItemsPanel.setVisibility(View.VISIBLE);
                }
            }
        }) );
    }

    private void show_loading() {
        visibility_all(View.GONE);
        mLoadingPanel.setVisibility(View.VISIBLE);
    }

    private void fetch_sources(final boolean andShow) {
        if (andShow) {
            show_loading();
        }
        application().data_sources.fetch(true, activity().observe_data(new ActivitySupport.OnValue<List<Source>>() {
            @Override
            public void action(List<Source> sources) {
                mSources = sources;
                if (andShow) {
                    visibility_all(View.GONE);
                    if (sources.isEmpty()){
                        mNoItemsPanel.setVisibility(View.VISIBLE);
                    }else {
                        if (sources.size()==1){
                            FilePointer filePointer = sources.get(0).asFilePointer();
                            mFileStack.add(0,filePointer);
                            update_folder();
                        }else {
                            mSourceListAdapter.clear();
                            mSourceListAdapter.addAll(sources);
                            mSourceListAdapter.notifyDataSetChanged();
                            mItemsPanel.setVisibility(View.VISIBLE);
                        }
                    }
                }
            }
        }));
    }

    @Override
    public void onStop() {
        super.onStop();
        deinit_folder_data();
        application().data_sources.removeDataChangeObserver(mSourcesDataListener);
    }

    private void deinit_folder_data() {
        if (mFolderData != null) {
            mFolderData.removeDataChangeObserver(mDataObserver);
            mFolderData = null;
        }
    }





    @Override
    public boolean onBackPressed() {
        switch (mFileStack.size()){
            case 0: return false;
            case 1: {
                if (mSources.size() == 1){
                    return false;
                } else {
                    fetch_sources(true);
                    return true;
                }
            }
            default:
                mFileStack.remove(0);
                update_folder();
                return true;
        }
    }
}
