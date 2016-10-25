package grmume.socksoprovider;

import com.fastbootmobile.encore.model.BoundEntity;
import com.fastbootmobile.encore.providers.AudioSocket;

/**
 * Created by greg on 18.10.16.
 */

public class CachedPlayer implements ICachedPlayer {

    private ISocksoConnectionParams connParams;

    public CachedPlayer(ISocksoConnectionParams params)
    {
        this.connParams = params;
    }

    @Override
    public int getSongOfflineStatus(String ref) {
        /**
         * The entity has not been marked for offline usage
         */
        //OFFLINE_STATUS_NO = 0;

        /**
         * The entity has been marked for offline usage, but the download is pending
         */
        //OFFLINE_STATUS_PENDING = 1;

        /**
         * The entity has been marked for offline usage, but it is currently downloading
         */
        //OFFLINE_STATUS_DOWNLOADING = 2;

        /**
         * The entity has been marked for offline usage, but an error occurred during download
         */
        //OFFLINE_STATUS_ERROR = 3;

        /**
         * The entity is marked for offline usage and is available
         */
        //OFFLINE_STATUS_READY = 4;

        return BoundEntity.OFFLINE_STATUS_NO;
    }

    @Override
    public void playSong(String ref, AudioSocket socket) {

    }

    @Override
    public void prefetchSong(String ref) {

    }
}
