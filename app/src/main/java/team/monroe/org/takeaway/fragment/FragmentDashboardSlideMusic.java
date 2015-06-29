package team.monroe.org.takeaway.fragment;

import android.content.Context;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import org.monroe.team.android.box.app.ui.GenericListViewAdapter;
import org.monroe.team.android.box.app.ui.GetViewImplementation;
import static org.monroe.team.android.box.app.ui.animation.apperrance.AppearanceControllerBuilder.*;

import org.monroe.team.android.box.app.ui.animation.apperrance.AppearanceController;
import org.monroe.team.android.box.app.ui.animation.apperrance.SceneDirector;
import org.monroe.team.android.box.data.Data;
import org.monroe.team.android.box.event.Event;
import org.monroe.team.corebox.utils.Closure;

import java.util.ArrayList;
import java.util.List;

import team.monroe.org.takeaway.R;
import team.monroe.org.takeaway.fragment.contract.ContractBackButton;
import team.monroe.org.takeaway.manage.Events;
import team.monroe.org.takeaway.manage.exceptions.ApplicationException;
import team.monroe.org.takeaway.manage.exceptions.FileOperationException;
import team.monroe.org.takeaway.presentations.FilePointer;
import team.monroe.org.takeaway.presentations.Source;
import team.monroe.org.takeaway.view.WarningViewPresenter;

public class FragmentDashboardSlideMusic extends FragmentDashboardSlide  implements ContractBackButton{

    private View mLoadingPanel;
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
    private CheckBox mHeaderFilesOfflineModeCheck;
    private WarningViewPresenter mWarningPresenter;

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
        mHeaderFilesOfflineModeCheck = (CheckBox) mHeaderFilesView.findViewById(R.id.check_offline);
        mHeaderFilesOfflineModeCheck.setChecked(application().isOfflineModeEnabled());
        mHeaderFilesOfflineModeCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                application().offlineMode(isChecked);
            }
        });

        mLoadingPanel = view(R.id.panel_loading);
        mItemsPanel = view(R.id.panel_items);
        mSourcePanel = view(R.id.panel_sources);
        mWarningPresenter = new WarningViewPresenter(view(R.id.panel_error), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mFileStack.isEmpty()){
                    fetch_sources(true);
                }else {
                    fetch_folder();
                }
            }
        });

        view_check(R.id.check_offline_only_error).setChecked(application().isOfflineModeEnabled());
        view_check(R.id.check_offline_only_error).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                application().offlineMode(isChecked);
            }
        });
        visibility_all(View.GONE);

        mFileList = view_list(R.id.list_items);
        mFileList.addHeaderView(mHeaderFilesView,null,false);

        mSourcesList = view_list(R.id.list_sources);

        mFolderAdapter = new GenericListViewAdapter<FilePointer, GetViewImplementation.ViewHolder<FilePointer>>(activity(), new GetViewImplementation.ViewHolderFactory<GetViewImplementation.ViewHolder<FilePointer>>() {
            @Override
            public GetViewImplementation.ViewHolder<FilePointer> create(final View convertView) {
                return new GetViewImplementation.ViewHolder<FilePointer>() {

                    TextView caption = (TextView) convertView.findViewById(R.id.item_caption);
                    TextView description = (TextView) convertView.findViewById(R.id.item_description);
                    View separator = convertView.findViewById(R.id.separator);
                    View background = convertView.findViewById(R.id.panel_content);
                    ImageButton image = (ImageButton) convertView.findViewById(R.id.item_image);
                    AppearanceController mPlayAnimator = animateAppearance(image, scale(1f,0f))
                            .showAnimation(duration_constant(200), interpreter_overshot())
                            .hideAnimation(duration_constant(200), interpreter_accelerate(0.5f))
                            .build();

                    FilePointer mFilePointer;

                    public boolean mMainActionActive;
                    public boolean mMainLongActionActive;

                    @Override
                    public void update(final FilePointer filePointer, int position) {
                        mMainActionActive = true;
                        mMainLongActionActive = true;

                        mPlayAnimator.showWithoutAnimation();
                        mFilePointer = filePointer;
                        int backgroundResource = R.drawable.panel_left_right_shadow;
                        if (position == 0){
                            backgroundResource = R.drawable.panel_round_top;
                        }else if (position == mFolderAdapter.getCount()-1){
                            backgroundResource = R.drawable.panel_round_bottom;
                        }

                        updateIcon(filePointer);

                        if (filePointer.type == FilePointer.Type.FILE){
                            description.setText("Song");
                        }else {
                            description.setText("Music Collection");
                        }
                        image.setFocusable(false);
                        background.setBackgroundResource(backgroundResource);
                        separator.setVisibility(position == 0? View.GONE : View.VISIBLE);
                        caption.setText(filePointer.getNormalizedTitle());
                        image.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (!mMainActionActive) return;
                                mMainActionActive = false;
                                SceneDirector
                                        .scenario()
                                            .hide(mPlayAnimator)
                                            .then()
                                                .action(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        image.setImageResource(R.drawable.android_play_lightgray);
                                                    }
                                                })
                                                    .then()
                                                        .show(mPlayAnimator)
                                        .play();
                                final FilePointer usedFilePointer = filePointer;
                                runLastOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (usedFilePointer != mFilePointer){
                                            return;
                                        }
                                        SceneDirector
                                                .scenario()
                                                .hide(mPlayAnimator)
                                                .then()
                                                .action(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        updateIcon(mFilePointer);
                                                        mMainActionActive = true;
                                                    }
                                                })
                                                .then()
                                                .show(mPlayAnimator)
                                                .play();

                                    }
                                },1000);
                            }
                        });

                        image.setOnLongClickListener(new View.OnLongClickListener() {
                            @Override
                            public boolean onLongClick(View v) {
                                if (!mMainLongActionActive) return true;
                                mMainLongActionActive = false;
                                Vibrator vb = (Vibrator) activity().getSystemService(Context.VIBRATOR_SERVICE);
                                vb.vibrate(100);
                                SceneDirector
                                        .scenario()
                                        .hide(mPlayAnimator)
                                        .then()
                                        .action(new Runnable() {
                                            @Override
                                            public void run() {
                                                image.setImageResource(R.drawable.android_playlist_add);
                                            }
                                        })
                                        .then()
                                        .show(mPlayAnimator)
                                        .play();
                                final FilePointer usedFilePointer = filePointer;
                                runLastOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (usedFilePointer != mFilePointer){
                                            return;
                                        }
                                        SceneDirector
                                                .scenario()
                                                .hide(mPlayAnimator)
                                                .then()
                                                .action(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        updateIcon(mFilePointer);
                                                        mMainLongActionActive = true;
                                                    }
                                                })
                                                .then()
                                                .show(mPlayAnimator)
                                                .play();

                                    }
                                },1000);
                            return true;
                            }

                        });
                    }

                    private void updateIcon(FilePointer filePointer) {
                        if (filePointer.type == FilePointer.Type.FILE){
                            image.setImageResource(R.drawable.android_note_lightgray);
                        }else {
                            image.setImageResource(R.drawable.android_music_lib_lightgray);
                        }
                    }

                    @Override
                    public void cleanup() {
                        image.setOnClickListener(null);
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
        mSourcesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Source source = mSourceListAdapter.getItem(position);
                FilePointer filePointer = source.asFilePointer();
                mFileStack.add(0,filePointer);
                update_folder();
            }
        });

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("browse_folder",mFileStack);
    }

    private void visibility_all(int visibility) {
        mLoadingPanel.setVisibility(visibility);
        mItemsPanel.setVisibility(visibility);
        mSourcePanel.setVisibility(visibility);
        if (visibility != View.GONE){
            throw new IllegalStateException();
        }
        mWarningPresenter.hide();
    }

    @Override
    public void onStart() {
        super.onStart();
        mSourcesDataListener = new Data.DataChangeObserver<List<Source>>() {
            @Override
            public void onDataInvalid() {
                if(mFileStack.isEmpty()){
                    fetch_sources(true);
                }else {
                    fetch_sources(false);
                }
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

        Event.subscribeOnEvent(activity(), this, Events.OFFLINE_MODE_CHANGED, new Closure<Boolean, Void>() {
            @Override
            public Void execute(Boolean arg) {
                view_check(R.id.check_offline_only_error).setChecked(arg);
                mHeaderFilesOfflineModeCheck.setChecked(arg);
                return null;
            }
        });
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
        final  Data<List<FilePointer>> data = mFolderData;
        if (mFolderData == null) return;
        mFolderData.fetch(true, new Data.FetchObserver<List<FilePointer>>() {
            @Override
            public void onFetch(List<FilePointer> filePointers) {
                FilePointer filePointer = mFileStack.get(0);
                updateHeader(filePointer.name);

                visibility_all(View.GONE);
                if (filePointers.isEmpty()){
                    warning_asNoItems("Try to switch offline mode");
                }else {
                    mFolderAdapter.clear();
                    mFolderAdapter.addAll(filePointers);
                    mFolderAdapter.notifyDataSetChanged();
                    mItemsPanel.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(Data.FetchError fetchError) {
                handle_fetchError(fetchError, data);
            }
        });
    }

    private void handle_fetchError(Data.FetchError fetchError, Data<List<FilePointer>> data) {
        if (data != null && mFolderData != data) return;
        if (fetchError instanceof Data.ExceptionFetchError){
            Throwable exception = ((Data.ExceptionFetchError) fetchError).cause;
            FileOperationException foe = ApplicationException.get(exception, FileOperationException.class);
            if (foe != null){
                warning_asError(foe.errorCode.toHumanString(getResources()), foe.extraDescription);
                return;
            }
        }
        warning_asError("Completely unexpected error", fetchError.message());
    }

    private void warning_asError(String description, String extraDescription) {
        visibility_all(View.GONE);
        mWarningPresenter.updateDetails(WarningViewPresenter.WarningType.ERROR, description, extraDescription);
        mWarningPresenter.show();
    }

    private void warning_asNoItems(String description) {
        visibility_all(View.GONE);
        mWarningPresenter.updateDetails(WarningViewPresenter.WarningType.NO_ITEMS, description, null);
        mWarningPresenter.show();
    }

    private void updateHeader(String title) {
        if (title == null){
            requestSecondaryHeader(null);
        }else{
            View view = activity().getLayoutInflater().inflate(R.layout.panel_folder_secondary_header,null);
            view.findViewById(R.id.action_back).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity().onBackPressed();
                }
            });
            ((TextView)view.findViewById(R.id.item_caption)).setText(title);
            requestSecondaryHeader(view);
        }
    }



    private void show_loading() {
        visibility_all(View.GONE);
        mLoadingPanel.setVisibility(View.VISIBLE);
    }

    private void fetch_sources(final boolean andShow) {
        if (andShow) {
            show_loading();
        }

        application().data_sources.fetch(true, new Data.FetchObserver<List<Source>>() {
            @Override
            public void onFetch(List<Source> sources) {
                mSources = sources;
                if (andShow) {
                    visibility_all(View.GONE);
                    if (sources.isEmpty()){
                        warning_asNoItems("Try to switch offline mode");
                    }else {
                        if (sources.size()==1){
                            FilePointer filePointer = sources.get(0).asFilePointer();
                            mFileStack.add(0,filePointer);
                            update_folder();
                        }else {
                            mSourceListAdapter.clear();
                            mSourceListAdapter.addAll(sources);
                            mSourceListAdapter.notifyDataSetChanged();
                            mSourcePanel.setVisibility(View.VISIBLE);
                        }
                    }
                }
            }

            @Override
            public void onError(Data.FetchError fetchError) {
                if (andShow){
                    handle_fetchError(fetchError , null);
                }
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        deinit_folder_data();
        application().data_sources.removeDataChangeObserver(mSourcesDataListener);
        Event.unSubscribeFromEvents(activity(), this);
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
