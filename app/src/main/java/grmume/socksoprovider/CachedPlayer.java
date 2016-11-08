package grmume.socksoprovider;

import android.media.MediaFormat;
import android.os.AsyncTask;
import android.util.Log;

import com.fastbootmobile.encore.model.BoundEntity;
import com.fastbootmobile.encore.providers.AudioClientSocket;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

/**
 * Created by greg on 18.10.16.
 */

public class CachedPlayer implements ICachedPlayer {

    private static final String TAG = "SocksoPluginPlayer";

    // 3 seconds buffer
    private final int bufferSize = 44000*20;

    // Play if 1 second is buffered
    private final int bufferThreshold = 1000;

    private ByteBuffer pcmBuffer;

    private ByteBuffer currentEncodedBuffer;

    private ISocksoConnectionParams connParams;

    private AudioClientSocket socket;

    private IPlayerCallback playerCallback;

    private boolean bufferingFinished;

    private int counter;

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    private IFetchSongCallback cb = new IFetchSongCallback() {
        @Override
        public void handleEncodedChunk(byte[] chunk) {

            currentEncodedBuffer.put(chunk);
        }

        @Override
        public void handlePcmChunk(byte[] chunk) {
            boolean justStarted = false;
            pcmBuffer.put(chunk);

            if(!bufferingFinished)
            {
                Log.d(TAG, "Buffering. Buffer position="+pcmBuffer.position());
                if(pcmBuffer.position() >= bufferThreshold)
                {
                    Log.d(TAG, "Starting playback");
                    bufferingFinished = true;
                    justStarted = true;
                }
            }

            if(bufferingFinished)
            {

                pcmBuffer.flip();
                byte[] pcm = new byte[pcmBuffer.remaining()];
                try {
                    pcmBuffer.get(pcm);
                }catch(BufferUnderflowException e)
                {
                    Log.e(TAG,"Buffer underflow");
                }
                pcmBuffer.clear();

                try {
                    socket.writeAudioData(pcm,0,pcm.length);
                    if(justStarted)
                    {
                        playerCallback.onPlaybackStarted();
                    }
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                }
                if (counter < 10) {
                    Log.d(TAG, "Sent " + pcm.length + " bytes to socket.");
                    counter++;
                }
            }
        }

        @Override
        public void setMediaFormat(MediaFormat format) {
            try {
                socket.writeFormatData(format.getInteger(MediaFormat.KEY_CHANNEL_COUNT),format.getInteger(MediaFormat.KEY_SAMPLE_RATE));
            } catch (IOException e) {
                Log.e(TAG,"IOException:"+e.getMessage());
            }
        }
    };

    public static String byteToHex(Byte b) {
        int v = b.byteValue() & 0xFF;
        return Character.toString(hexArray[v >>> 4]) + Character.toString(hexArray[v & 0x0F]);
    }

    public CachedPlayer(ISocksoConnectionParams params, IPlayerCallback playerCallback)
    {
        this.connParams = params;
        this.playerCallback = playerCallback;
    }

    @Override
    public void setAudioSocket(AudioClientSocket socket) {
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

        counter = 0;

        pcmBuffer = ByteBuffer.allocateDirect(bufferSize);

        // 15 Minutes at 320kbit/s:
        currentEncodedBuffer = ByteBuffer.allocateDirect(15*60*320*256);

        bufferingFinished = false;


        Log.d(TAG, "Playing song "+ref);
        FetchSongTaskExtractor task = new FetchSongTaskExtractor();
        task.execute(new FetchSongParams(connParams, Reference.idFromRef(ref), cb));
        //while(task.getStatus()!= AsyncTask.Status.FINISHED) ;



    }


    @Override
    public void prefetchSong(String ref) {

    }
}
