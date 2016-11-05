package grmume.socksoprovider;

import android.annotation.TargetApi;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Created by greg on 18.10.16.
 */

public class CachedPlayer implements ICachedPlayer {

    private static final String TAG = "SocksoPluginPlayer";

    private String currentMimeType;

    private String currentFilename;

    private int currentContentLength;

    private final List<Byte> currentBuffer = new ArrayList<>();

    private MediaFormat currentFormat;

    private MediaCodec currentCodec;

    private final List<Short> currentPcmBuffer = new ArrayList<>();

    private long currentUs;

    private long currentLength;

    private ISocksoConnectionParams connParams;

    private AudioSocket socket;

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String byteToHex(Byte b) {
        int v = b.byteValue() & 0xFF;
        return Character.toString(hexArray[v >>> 4]) + Character.toString(hexArray[v & 0x0F]);
    }

    private IFetchSongCallback cb = new IFetchSongCallback() {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void handleChunk(List<Byte> chunk) {

            Log.d(TAG, "Received "+chunk.size()+" bytes from server");

            if(currentFormat == null)
            {
                Log.d(TAG, "CurrentFormat is null. Setting mimeType to "+currentMimeType);
                // This is the first chunk.
                // TODO Extract the currentFormat using MediaExtractor
                currentFormat = MediaFormat.createAudioFormat(currentMimeType, 320, 2);
                try {
                    currentCodec = MediaCodec.createDecoderByType(currentMimeType);
                    socket.writeFormatData(2, 320);
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                }
                currentCodec.configure(currentFormat, null /* surface */, null /* crypto */, 0 /* flags */);
            }

            ByteBuffer codecInputBuffer;
            ByteBuffer codecOutputBuffer;
            Log.d(TAG, "Starting codec");
            currentCodec.start();

            int inputBufIndex = currentCodec.dequeueInputBuffer(-1);
            codecInputBuffer = currentCodec.getInputBuffer(inputBufIndex);

            if (codecInputBuffer != null) {
                Log.d(TAG, "Got inputbuffer");

                String bytesInHex = "";

                for (Byte b : chunk) {
                    codecInputBuffer.put(b);
                    bytesInHex+=byteToHex(b);
                }

                Log.d(TAG, "========== BYTES =========");
                Log.d(TAG, bytesInHex);
                Log.d(TAG, "========== BYTES =========");

                Log.d(TAG, "Inputbuffer:"+codecInputBuffer.toString());

                currentCodec.queueInputBuffer(inputBufIndex,
                        0, //offset
                        chunk.size(),
                        currentUs,
                        (currentLength == currentContentLength) ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);
                Log.d(TAG, "queued input buffer");

                // 320kbit!! (and seconds to microseconds)(1024/8 0=> 128)
                currentUs += (chunk.size() * 1000000) / (320 * 128);

                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                int outputBufIndex = 0;
                Log.d(TAG, "Created bufferInfo");
                try {
                    outputBufIndex = currentCodec.dequeueOutputBuffer(bufferInfo, -1);

                }catch(MediaCodec.CodecException e)
                {
                    Log.e(TAG, "CodecException:"+e.getDiagnosticInfo());
                }catch(IllegalStateException e)
                {
                    Log.e(TAG, "IllegalStateException: "+e.getMessage());
                }

                Log.d(TAG, "outputBufIndex is "+outputBufIndex);

                codecOutputBuffer = currentCodec.getOutputBuffer(outputBufIndex);

                Log.d(TAG, "Got outputBuffer. bufferIndex is "+outputBufIndex);

                final byte[] pcmChunk = new byte[bufferInfo.size];
                codecOutputBuffer.get(pcmChunk); // Read the buffer all at once
                codecOutputBuffer.clear();

                currentCodec.releaseOutputBuffer(outputBufIndex, false /* render */);

                try {
                    Log.d(TAG, "Writing to socket");
                    socket.writeAudioData(pcmChunk, 0, pcmChunk.length);
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                }

            }

            currentCodec.stop();
            currentCodec.release();

        }

        @Override
        public void handleStartOfStream(String mimeDatatype, String filename, int length) {
            Log.d(TAG, "Received start of stream.");
            currentUs = 0;
            currentLength = 0;
            currentMimeType = mimeDatatype;
            currentFilename = filename;
            currentContentLength = length;
            currentFormat = null;
            currentCodec = null;
            currentBuffer.clear();
            currentPcmBuffer.clear();
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
