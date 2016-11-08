package grmume.socksoprovider;

import com.fastbootmobile.encore.providers.AudioClientSocket;
import com.fastbootmobile.encore.providers.AudioSocket;

/**
 * Created by greg on 16.10.16.
 */

public interface ICachedPlayer {

    void setAudioSocket(AudioClientSocket socket);

    int getSongOfflineStatus(String ref);

    void playSong(String ref);

    void prefetchSong(String ref);

}
