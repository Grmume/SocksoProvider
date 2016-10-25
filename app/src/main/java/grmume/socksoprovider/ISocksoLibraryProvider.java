package grmume.socksoprovider;

import android.graphics.Bitmap;
import android.os.RemoteException;

import com.fastbootmobile.encore.model.Album;
import com.fastbootmobile.encore.model.Artist;
import com.fastbootmobile.encore.model.Genre;
import com.fastbootmobile.encore.model.Playlist;
import com.fastbootmobile.encore.model.Song;
import com.fastbootmobile.encore.providers.IArtCallback;
import com.fastbootmobile.encore.providers.ProviderIdentifier;

import java.util.List;

/**
 * Created by greg on 15.10.16.
 */

public interface ISocksoLibraryProvider {

    List<Album> getAlbums() throws RemoteException;

    List<Artist> getArtists() throws RemoteException;

    List<Song> getSongs(int offset, int limit) throws RemoteException;

    List<Playlist> getPlaylists() throws RemoteException;

    List<Genre> getGenres() throws RemoteException;

    Artist getArtist(String ref) throws RemoteException;

    Album getAlbum(String ref) throws RemoteException;

    Song getSong(String ref) throws RemoteException;

    Playlist getPlaylist(String ref) throws RemoteException;

    boolean getArtistArt(Artist entity, IArtCallback callback) throws RemoteException;

    boolean getAlbumArt(Album entity, IArtCallback callback) throws RemoteException;

    boolean getPlaylistArt(Playlist entity, IArtCallback callback) throws RemoteException;

    boolean getSongArt(Song entity, IArtCallback callback) throws RemoteException;

    boolean fetchArtistAlbums(String artistRef) throws RemoteException;

    boolean fetchAlbumTracks(String albumRef) throws RemoteException;

    boolean onUserSwapPlaylistItem(int oldPosition, int newPosition, String playlistRef) throws RemoteException;

    boolean deletePlaylist(String playlistRef) throws RemoteException;

    boolean renamePlaylist(String ref, String title) throws RemoteException;

    boolean deleteSongFromPlaylist(int songPosition, String playlistRef) throws RemoteException;

    boolean addSongToPlaylist(String songRef, String playlistRef, ProviderIdentifier providerIdentifier) throws RemoteException;

    String addPlaylist(String playlistName) throws RemoteException;

    void startSearch(String query) throws RemoteException;

    Bitmap getLogo(String ref) throws RemoteException;

    void setPlaylistOfflineMode(String ref, boolean offline) throws RemoteException;

    void setOfflineMode(boolean offline) throws RemoteException;

    void clear();
}
