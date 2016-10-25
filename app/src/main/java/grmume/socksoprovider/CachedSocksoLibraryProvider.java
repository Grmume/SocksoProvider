package grmume.socksoprovider;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import com.fastbootmobile.encore.model.Album;
import com.fastbootmobile.encore.model.Artist;
import com.fastbootmobile.encore.model.Genre;
import com.fastbootmobile.encore.model.Playlist;
import com.fastbootmobile.encore.model.Song;
import com.fastbootmobile.encore.providers.IArtCallback;
import com.fastbootmobile.encore.providers.ProviderIdentifier;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by greg on 15.10.16.
 */

public class CachedSocksoLibraryProvider implements ISocksoLibraryProvider {

    private static final String TAG = "SocksoPluginService";

    protected class GetSongsParams implements IApiRequestParams
    {
        private int limit;

        private int offset;

        @Override
        public ISocksoConnectionParams getConnectionParams() {
            return connParams;
        }

        @Override
        public String getUrl() {
            return "tracks";
        }

        public void setLimit(int value)
        {
            limit = value;
        }

        public void setOffset(int value) { offset = value; }
        @Override
        public int getLimit() {
            return limit;
        }

        @Override
        public int getOffset() {
            return offset;
        }

        @Override
        public IApiRequestCallback getRequestCallback() {
            return getSongsCallback;
        }
    };

    protected class GetArtistParams implements IApiRequestParams
    {
        private final String id;

        public GetArtistParams(String id)
        {
            this.id = id;
        }

        @Override
        public ISocksoConnectionParams getConnectionParams() {
            return connParams;
        }

        @Override
        public String getUrl() {
            return "artists/"+id;
        }
        @Override
        public int getLimit() {
            return -1;
        }

        @Override
        public int getOffset() {
            return 0;
        }

        @Override
        public IApiRequestCallback getRequestCallback() {
            return artistsCallback;
        }
    };

    protected class GetAlbumParams implements IApiRequestParams
    {
        private final String id;

        public GetAlbumParams(String id)
        {
            this.id = id;
        }

        @Override
        public ISocksoConnectionParams getConnectionParams() {
            return connParams;
        }

        @Override
        public String getUrl() {
            return "albums/"+id;
        }
        @Override
        public int getLimit() {
            return -1;
        }

        @Override
        public int getOffset() {
            return 0;
        }

        @Override
        public IApiRequestCallback getRequestCallback() {
            return albumsCallback;
        }
    };

    protected class GetSongParams implements IApiRequestParams
    {
        private final String id;

        public GetSongParams(String id)
        {
            this.id = id;
        }

        @Override
        public ISocksoConnectionParams getConnectionParams() {
            return connParams;
        }

        @Override
        public String getUrl() {
            return "tracks/"+id;
        }
        @Override
        public int getLimit() {
            return -1;
        }

        @Override
        public int getOffset() {
            return 0;
        }

        @Override
        public IApiRequestCallback getRequestCallback() {
            return getSongsCallback;
        }
    };

    private final ICachedPlayer player;

    private final ISocksoConnectionParams connParams;

    private final HashMap<String, Artist> artists = new HashMap<>();

    private final HashMap<String, Album> albums = new HashMap<>();

    private final HashMap<String, Song> songs = new HashMap<>();

    private final HashMap<Integer, String> songIndices = new HashMap<>();

    private final GetSongsParams getSongsParams = new GetSongsParams();


    private final HashMap<String, Playlist> playlists = new HashMap<>();

    private ProviderIdentifier identifier;

    private final IApiRequestParams.IApiRequestCallback albumsCallback = new IApiRequestParams.IApiRequestCallback() {
        @Override
        public void handleApiResponse(String jsonString) {
            try {
                Object json = new JSONTokener(jsonString).nextValue();
                if (json instanceof JSONObject) {
                    JSONObject album = (JSONObject)json;
                    Album alb = new Album(SocksoService.referencePrefix+"album:"+Integer.toString(album.getInt("id")));
                    alb.setName(album.getString("name"));

                    //TODO get album songs

                    alb.setProvider(identifier);
                    Log.d(TAG, "Album:"+album.getString("name"));
                    albums.put(alb.getRef(), alb);
                }else if(json instanceof JSONArray) {
                    JSONArray array = (JSONArray)json;
                    for(int i=0;i<array.length();++i)
                    {
                        JSONObject album = array.getJSONObject(i);
                        Album alb = new Album(SocksoService.referencePrefix+"album:"+Integer.toString(album.getInt("id")));
                        alb.setName(album.getString("name"));

                        //TODO get album songs

                        alb.setProvider(identifier);
                        Log.d(TAG, "Album:"+album.getString("name"));
                        albums.put(alb.getRef(), alb);
                    }
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    private final IApiRequestParams.IApiRequestCallback artistsCallback = new IApiRequestParams.IApiRequestCallback() {
        @Override
        public void handleApiResponse(String jsonString) {
            try {
                Object json = new JSONTokener(jsonString).nextValue();
                if (json instanceof JSONObject) {
                    JSONObject artist = (JSONObject)json;
                    Artist art = new Artist(SocksoService.referencePrefix + "artist:" + Integer.toString(artist.getInt("id")));
                    art.setName(artist.getString("name"));
                    art.setProvider(identifier);
                    Log.d(TAG, "Artist:" + artist.getString("name"));
                    artists.put(art.getRef(), art);
                }
                else if (json instanceof JSONArray) {
                    JSONArray array = new JSONArray(jsonString);
                    for (int i = 0; i < array.length(); ++i) {
                        JSONObject artist = array.getJSONObject(i);
                        Artist art = new Artist(SocksoService.referencePrefix + "artist:" + Integer.toString(artist.getInt("id")));
                        art.setName(artist.getString("name"));
                        art.setProvider(identifier);
                        Log.d(TAG, "Artist:" + artist.getString("name"));
                        artists.put(art.getRef(), art);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };



    private final IApiRequestParams.IApiRequestCallback getSongsCallback = new IApiRequestParams.IApiRequestCallback() {
        @Override
        public void handleApiResponse(String jsonString) {
            try {
                Object json = new JSONTokener(jsonString).nextValue();
                if (json instanceof JSONObject) {
                    JSONObject song = (JSONObject)json;
                    Song s = new Song(SocksoService.referencePrefix+"song:"+Integer.toString(song.getInt("id")));
                    s.setTitle(song.getString("name"));
                    s.setArtist(SocksoService.referencePrefix+"artist:"+Integer.toString(song.getJSONObject("artist").getInt("id")));
                    s.setArtist(SocksoService.referencePrefix+"album:"+Integer.toString(song.getJSONObject("album").getInt("id")));
                    s.setProvider(identifier);
                    s.setOfflineStatus(player.getSongOfflineStatus(s.getRef()));
                    s.setAvailable(true);
                    s.setIsLoaded(true);
                    songs.put(s.getRef(), s);
                    songIndices.put(song.getInt("id"), s.getRef());
                }else if(json instanceof JSONArray)
                {
                    JSONArray array = (JSONArray)json;
                    for(int i=0;i<array.length();++i)
                    {
                        JSONObject song = array.getJSONObject(i);
                        Song s = new Song(SocksoService.referencePrefix+"song:"+Integer.toString(song.getInt("id")));
                        s.setTitle(song.getString("name"));
                        s.setArtist(SocksoService.referencePrefix+"artist:"+Integer.toString(song.getJSONObject("artist").getInt("id")));
                        s.setArtist(SocksoService.referencePrefix+"album:"+Integer.toString(song.getJSONObject("album").getInt("id")));
                        s.setProvider(identifier);
                        s.setOfflineStatus(player.getSongOfflineStatus(s.getRef()));
                        s.setAvailable(true);
                        s.setIsLoaded(true);
                        Log.d(TAG, "Song:"+song.getString("name"));
                        songs.put(s.getRef(), s);
                        songIndices.put(song.getInt("id"), s.getRef());
                    }
                }

            } catch (JSONException e) {
                Log.d(TAG,"JSONException:"+e.getMessage());
            }
        }
    };

    public CachedSocksoLibraryProvider(ISocksoConnectionParams params, ProviderIdentifier identifier, ICachedPlayer player)
    {
        this.player = player;
        connParams = params;
        this.identifier = identifier;
    }

    @Override
    public List<Album> getAlbums() throws RemoteException {
        IApiRequestParams params = new IApiRequestParams() {
            @Override
            public ISocksoConnectionParams getConnectionParams() {
                return connParams;
            }

            @Override
            public String getUrl() {
                return "albums";
            }

            @Override
            public int getLimit() {
                return -1;
            }

            @Override
            public int getOffset() {
                return 0;
            }

            @Override
            public IApiRequestCallback getRequestCallback() {
                return albumsCallback;
            }
        };

        ApiRequestTask task = new ApiRequestTask();
        task.execute(params);

        while(task.getStatus() != AsyncTask.Status.FINISHED);

        return new ArrayList<Album>(albums.values());
    }

    @Override
    public List<Artist> getArtists() throws RemoteException {
        Log.d(TAG, "Getting artists");
        IApiRequestParams params = new IApiRequestParams() {
            @Override
            public ISocksoConnectionParams getConnectionParams() {
                return connParams;
            }

            @Override
            public String getUrl() {
                return "artists";
            }

            @Override
            public int getLimit() {
                return -1;
            }

            @Override
            public int getOffset() {
                return 0;
            }

            @Override
            public IApiRequestCallback getRequestCallback() {
                return albumsCallback;
            }
        };

        ApiRequestTask task = new ApiRequestTask();
        task.execute(params);

        while(task.getStatus() != AsyncTask.Status.FINISHED);

        return new ArrayList<Artist>(artists.values());
    }

    @Override
    public List<Song> getSongs(int offset, int limit) throws RemoteException {
        GetSongsParams params = new GetSongsParams();
        params.setLimit(limit);
        params.setOffset(offset);

        ApiRequestTask task = new ApiRequestTask();
        task.execute(params);

        while(task.getStatus() != AsyncTask.Status.FINISHED);

        ArrayList<Song> result = new ArrayList<>();
        for(int i = offset;i<offset+limit;++i)
        {
            result.add(songs.get(songIndices.get(i)));
        }
        return result;
    }

    @Override
    public List<Playlist> getPlaylists() throws RemoteException {
        return null;
    }

    @Override
    public List<Genre> getGenres() throws RemoteException {
        return null;
    }

    @Override
    public Artist getArtist(String ref) throws RemoteException {

        if(!artists.containsKey(ref))
        {
            // Get artist id
            String split[] = ref.split(":");
            if(split.length == 3) {
                String id = split[2];
                GetArtistParams params = new GetArtistParams(id);
                ApiRequestTask task = new ApiRequestTask();
                task.execute(params);
                while(task.getStatus() != AsyncTask.Status.FINISHED);
            }
        }
        return artists.get(ref);
    }

    @Override
    public Album getAlbum(String ref) throws RemoteException {
        if(!albums.containsKey(ref))
        {
            // Get album id
            String split[] = ref.split(":");
            if(split.length == 3) {
                String id = split[2];
                GetAlbumParams params = new GetAlbumParams(id);
                ApiRequestTask task = new ApiRequestTask();
                task.execute(params);
                while(task.getStatus() != AsyncTask.Status.FINISHED);
            }
        }
        return albums.get(ref);
    }

    @Override
    public Song getSong(String ref) throws RemoteException {
        if(!songs.containsKey(ref))
        {
            // Get album id
            String split[] = ref.split(":");
            if(split.length == 3) {
                String id = split[2];
                GetSongParams params = new GetSongParams(id);
                ApiRequestTask task = new ApiRequestTask();
                task.execute(params);
                while(task.getStatus() != AsyncTask.Status.FINISHED);
            }
        }
        return songs.get(ref);
    }


    @Override
    public Playlist getPlaylist(String ref) throws RemoteException {
        return null;
    }

    @Override
    public boolean getArtistArt(Artist entity, IArtCallback callback) throws RemoteException {
        return false;
    }

    @Override
    public boolean getAlbumArt(Album entity, IArtCallback callback) throws RemoteException {
        return false;
    }

    @Override
    public boolean getPlaylistArt(Playlist entity, IArtCallback callback) throws RemoteException {
        return false;
    }

    @Override
    public boolean getSongArt(Song entity, IArtCallback callback) throws RemoteException {
        return false;
    }

    @Override
    public boolean fetchArtistAlbums(String artistRef) throws RemoteException {
        return false;
    }

    @Override
    public boolean fetchAlbumTracks(String albumRef) throws RemoteException {
        return false;
    }

    @Override
    public boolean onUserSwapPlaylistItem(int oldPosition, int newPosition, String playlistRef) throws RemoteException {
        return false;
    }

    @Override
    public boolean deletePlaylist(String playlistRef) throws RemoteException {
        return false;
    }

    @Override
    public boolean renamePlaylist(String ref, String title) throws RemoteException {
        return false;
    }

    @Override
    public boolean deleteSongFromPlaylist(int songPosition, String playlistRef) throws RemoteException {
        return false;
    }

    @Override
    public boolean addSongToPlaylist(String songRef, String playlistRef, ProviderIdentifier providerIdentifier) throws RemoteException {
        return false;
    }

    @Override
    public String addPlaylist(String playlistName) throws RemoteException {
        return null;
    }

    @Override
    public void startSearch(String query) throws RemoteException {

    }

    @Override
    public Bitmap getLogo(String ref) throws RemoteException {
        return null;
    }

    @Override
    public void setPlaylistOfflineMode(String ref, boolean offline) throws RemoteException {

    }

    @Override
    public void setOfflineMode(boolean offline) throws RemoteException {

    }

    @Override
    public void clear() {

    }
}
