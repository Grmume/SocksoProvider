package grmume.socksoprovider;

import com.fastbootmobile.encore.providers.AudioSocket;

/**
 * Created by greg on 16.10.16.
 */

public interface ICachedPlayer {

    int getSongOfflineStatus(String ref);

    void playSong(String ref, AudioSocket socket);

    void prefetchSong(String ref);

}
