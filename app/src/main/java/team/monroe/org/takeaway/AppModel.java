package team.monroe.org.takeaway;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import org.monroe.team.android.box.app.AndroidModel;
import org.monroe.team.android.box.db.DAOFactory;
import org.monroe.team.android.box.db.DAOSupport;
import org.monroe.team.android.box.db.DBHelper;
import org.monroe.team.android.box.db.TextDataBase;
import org.monroe.team.android.box.db.TransactionManager;
import org.monroe.team.android.box.services.HttpManager;
import org.monroe.team.android.box.services.NetworkManager;
import org.monroe.team.corebox.services.ServiceRegistry;
import org.monroe.team.corebox.utils.Closure;

import java.util.ArrayList;
import java.util.List;

import team.monroe.org.takeaway.db.Dao;
import team.monroe.org.takeaway.db.TakeAwaySchema;
import team.monroe.org.takeaway.manage.CloudConfigurationManager;
import team.monroe.org.takeaway.manage.CloudConnectionManager;
import team.monroe.org.takeaway.manage.CloudManager;
import team.monroe.org.takeaway.manage.CloudMetadataProvider;
import team.monroe.org.takeaway.manage.DownloadManager;
import team.monroe.org.takeaway.manage.StorageProvider;
import team.monroe.org.takeaway.manage.PlaylistStorage;
import team.monroe.org.takeaway.manage.impl.KodiCloudProvider;
import team.monroe.org.takeaway.manage.impl.LocalStorageProvider;
import team.monroe.org.takeaway.manage.Player;
import team.monroe.org.takeaway.manage.impl.KodiCloudManager;
import team.monroe.org.takeaway.presentations.SongFile;


public class AppModel extends AndroidModel {

    public AppModel(String appName, Context context) {
        super(appName, context);
    }
    public List<DownloadObserver> downloadObservers;

    @Override
    protected void constructor(String appName, Context context, ServiceRegistry serviceRegistry) {
        super.constructor(appName, context, serviceRegistry);


        PlaylistStorage playlistStorage = new PlaylistStorage();
        TextDataBase textDataBase = new TextDataBase(context, appName, 1, playlistStorage);
        serviceRegistry.registrate(PlaylistStorage.class, playlistStorage);

        downloadObservers = new ArrayList<>();
        serviceRegistry.registrate(DownloadManager.class, new DownloadManager(context, new Closure<SongFile, Void>() {
            @Override
            public Void execute(final SongFile arg) {
                getResponseHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        for (DownloadObserver downloadObserver : downloadObservers) {
                                downloadObserver.onSongFileDownloadDone(arg);
                        }
                    }
                });
                return null;
            }
        }));

        final TakeAwaySchema schema = new TakeAwaySchema();
        DBHelper helper = new DBHelper(context, schema);
        TransactionManager transactionManager = new TransactionManager(helper, new DAOFactory() {
            @Override
            public DAOSupport createInstanceFor(SQLiteDatabase database) {
                return new Dao(database, schema);
            }
        });

        serviceRegistry.registrate(TransactionManager.class, transactionManager);
        serviceRegistry.registrate(LocalStorageProvider.class, new LocalStorageProvider());
        serviceRegistry.registrate(Player.class, new Player(context, this));
        serviceRegistry.registrate(NetworkManager.class, new NetworkManager(context));
        CloudConnectionManager cloudConnectionManager = new CloudConnectionManager(this);
        serviceRegistry.registrate(CloudConnectionManager.class, cloudConnectionManager);
        CloudConfigurationManager configurationManager = new CloudConfigurationManager(context);
        serviceRegistry.registrate(CloudConfigurationManager.class, configurationManager);
        CloudManager cloudManager = new KodiCloudManager(new HttpManager());
        KodiCloudProvider fileProvider = new KodiCloudProvider(cloudManager, cloudConnectionManager, configurationManager);
        serviceRegistry.registrate(CloudManager.class, cloudManager);
        serviceRegistry.registrate(StorageProvider.class, fileProvider);
        serviceRegistry.registrate(CloudMetadataProvider.class, fileProvider);
    }

    public interface DownloadObserver {
        void onSongFileDownloadDone(SongFile songFile);
    }

}
