package grmume.socksoprovider;

import android.content.res.AssetFileDescriptor;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.fastbootmobile.encore.model.BoundEntity;
import com.fastbootmobile.encore.providers.AudioSocket;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * Created by greg on 18.10.16.
 */

public class CachedPlayer implements ICachedPlayer {

    private static final String TAG = "SocksoPluginPlayer";

    private ISocksoConnectionParams connParams;

    private AudioSocket socket;

    private IFetchSongCallback cb = new IFetchSongCallback() {
        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
        @Override
        public void handleChunk(List<Byte> chunk) {
//            AssetFileDescriptor sampleFD = null;//getResources().openRawResourceFd(R.raw.sample);
//
//            MediaExtractor extractor;
//            MediaCodec codec;
//            ByteBuffer[] codecInputBuffers;
//            ByteBuffer[] codecOutputBuffers;
//
//            extractor = new MediaExtractor();
//            try {
//                extractor.setDataSource(sampleFD.getFileDescriptor(), sampleFD.getStartOffset(), sampleFD.getLength());
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//
//            Log.d(TAG, String.format("TRACKS #: %d", extractor.getTrackCount()));
//            MediaFormat format = extractor.getTrackFormat(0);
//            String mime = format.getString(MediaFormat.KEY_MIME);
//            Log.d(TAG, String.format("MIME TYPE: %s", mime));

            Log.d(TAG, "Received "+chunk.size()+" bytes from server");
        }
    };

    public CachedPlayer(ISocksoConnectionParams params)
    {
        this.connParams = params;
    }

    @Override
    public void setAudioSocket(AudioSocket socket) {
        this.socket = socket;
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
    public void playSong(String ref) {
        Log.d(TAG, "Playing song "+ref);
        FetchSongTask task = new FetchSongTask();
        task.execute(new FetchSongParams(connParams, Reference.idFromRef(ref), cb));
        while(task.getStatus()!= AsyncTask.Status.FINISHED) ;
    }


    @Override
    public void prefetchSong(String ref) {

    }
}
