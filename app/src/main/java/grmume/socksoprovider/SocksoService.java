package grmume.socksoprovider;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.util.Log;

import com.fastbootmobile.encore.model.Album;
import com.fastbootmobile.encore.model.Artist;
import com.fastbootmobile.encore.model.Genre;
import com.fastbootmobile.encore.model.Playlist;
import com.fastbootmobile.encore.model.Song;
import com.fastbootmobile.encore.providers.AudioClientSocket;
import com.fastbootmobile.encore.providers.Constants;
import com.fastbootmobile.encore.providers.IArtCallback;
import com.fastbootmobile.encore.providers.IMusicProvider;
import com.fastbootmobile.encore.providers.IProviderCallback;
import com.fastbootmobile.encore.providers.ProviderIdentifier;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownServiceException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SocksoService extends Service {

    private static final String TAG = "SocksoPluginService";

    public static final String referencePrefix = "sockso:";

    private static final String loginUrl = "/user/login";

    private List<Album> albums;

    private List<Artist> artists;

    private List<Song> songs;


    private AudioClientSocket socket;

    private LoginResult loginResult = null;

    boolean isAuthenticated = false;

    private ProviderIdentifier mIdentifier;

    private final List<IProviderCallback> mCallbacks;

    private long loginTimestamp;

    private ISocksoConnectionParams connParams = new ISocksoConnectionParams() {
        @Override
        public List<HttpCookie> getCookies() {
            if(loginResult == null) return null;
            return loginResult.getCookies();
        }

        @Override
        public String getServerAddress() {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(SocksoService.this);
            return pref.getString("serverAddress", null);
        }

        @Override
        public String getServerPort() {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(SocksoService.this);
            return pref.getString("serverPort", null);
        }

        @Override
        public String getAccountName() {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(SocksoService.this);
            return pref.getString("accountName", null);
        }

        @Override
        public String getAccountPassword() {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(SocksoService.this);
            return pref.getString("accountPassword", null);
        }

        @Override
        public long getTimestampSeconds() {
            return loginTimestamp;
        }
    };

    private final CachedPlayer player = new CachedPlayer(connParams);

    private final CachedSocksoLibraryProvider libProvider = new CachedSocksoLibraryProvider(connParams, player);


    /**
     * Default constructor
     */
    public SocksoService() {
        mCallbacks = new ArrayList<>();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        synchronized (mCallbacks) {
            mCallbacks.clear();
        }
    }

    @Override
    public void onDestroy() {
        // Clear up the remaining callbacks
        synchronized (mCallbacks) {
            mCallbacks.clear();
        }

        // And destroy the service
        super.onDestroy();
    }

    public ProviderIdentifier getIdentifier()
    {
        return mIdentifier;
    }

    private class LoginTask extends AsyncTask<String, Void, LoginResult>
    {

        @Override
        protected LoginResult doInBackground(String... params) {

            LoginResult res = new LoginResult();
            String serverName = params[0];
            String serverPort = params[1];
            String accountName = params[2];
            String accountPassword = params[3];

            // try to connect
            try {
                String postData = "todo=login&name="+URLEncoder.encode(accountName, "UTF-8")+"&pass="+URLEncoder.encode(accountPassword, "UTF-8");
                byte[] postDataBytes = postData.getBytes("UTF-8");
                Log.d(TAG, "Connecting to url:"+"http://"+serverName+":"+serverPort+loginUrl);
                URL login = new URL("http://"+serverName+":"+serverPort+loginUrl);

                HttpURLConnection serverConn = (HttpURLConnection)login.openConnection();
                Log.d(TAG, "Connection opened.");
                serverConn.setRequestMethod("POST");
                serverConn.setInstanceFollowRedirects(false);
                serverConn.setDoOutput(true);
                serverConn.setDoInput(true);

                Log.d(TAG, "Sending logindata:"+postData);

                try {
                    OutputStream out = new BufferedOutputStream(serverConn.getOutputStream());
                    out.write(postDataBytes);
                    out.close();
                }catch(Exception e)
                {
                    Log.d(TAG, "Could not send logindata:"+e.toString()+" Message:"+e.getMessage());
                    res.setSuccess(false);
                }

                Log.d(TAG, "Logindata sent. postData:"+postData);

                // Wait for response
                int response = serverConn.getResponseCode();

                Log.d(TAG, "HttpResponse received. Responsecode "+Integer.toString(response));

                //Check if the session cookie was received
                Map<String, List<String>> headerFields = serverConn.getHeaderFields();
                for(String headerField:headerFields.keySet())
                {
                    Log.d(TAG, "HttpResponse received. Headerfield: "+headerField);
                }
                List<String> cookiesHeader = headerFields.get("Set-Cookie");

                if (cookiesHeader != null) {
                    res.setSuccess(true);
                    res.setCookies(new ArrayList<HttpCookie>());
                    loginTimestamp = System.currentTimeMillis()/1000;

                    for (String cookie : cookiesHeader) {
                        HttpCookie c = HttpCookie.parse(cookie).get(0);
                        Log.d(TAG, "Adding cookie "+c.getName() + ":" + c.getValue());
                        res.getCookies().add(c);
                    }
                    serverConn.disconnect();

                    return res;
                }

                serverConn.disconnect();

            } catch (MalformedURLException e) {
                Log.d(TAG, "URL is malformed.");
                res.setSuccess(false);
                return res;
            } catch (IOException e) {
                Log.d(TAG, "IOException:"+e.getMessage());
                res.setSuccess(false);
                return res;
            }

            return res;
        }

        @Override
        protected void onPostExecute(LoginResult Result)
        {
            loginResult = Result;
        }

        private String readInputStreamToString(HttpURLConnection connection) {
            String result = null;
            StringBuffer sb = new StringBuffer();
            InputStream is = null;

            try {
                is = new BufferedInputStream(connection.getInputStream());
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String inputLine;
                while ((inputLine = br.readLine()) != null) {
                    sb.append(inputLine);
                }
                result = sb.toString();
            }
            catch (Exception e) {
                Log.i(TAG, "Error reading InputStream");
                result = null;
            }
            finally {
                if (is != null) {
                    try {
                        is.close();
                    }
                    catch (IOException e) {
                        Log.i(TAG, "Error closing InputStream");
                    }
                }
            }

            return result;
        }
    }

    private class GetAlbumsTask extends AsyncTask<CachedSocksoLibraryProvider, Void, List<Album>> {

        @Override
        protected List<Album> doInBackground(CachedSocksoLibraryProvider... params) {
            try {
                return params[0].getAlbums();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            return null;
        }

        protected void onPostExecute(List<Album> Result)
        {
            albums = Result;
        }
    }

    private class GetArtistsTask extends AsyncTask<CachedSocksoLibraryProvider, Void, List<Artist>> {
        @Override
        protected List<Artist> doInBackground(CachedSocksoLibraryProvider... params) {
            try {
                return params[0].getArtists();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            return null;
        }

        protected void onPostExecute(List<Artist> Result)
        {
            artists = Result;
        }
    }

    private class GetSongsTask extends AsyncTask<IGetSongsParams, Void, List<Song>> {

        @Override
        protected List<Song> doInBackground(IGetSongsParams... params) {
            try {
                return params[0].getLibProvider().getSongs(params[0].getOffset(), params[0].getLimit());
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            return null;
        }

        protected void onPostExecute(List<Song> Result)
        {
            songs = Result;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private IMusicProvider.Stub mBinder = new IMusicProvider.Stub()
    {

        @Override
        public int getVersion() throws RemoteException {
            return Constants.API_VERSION;
        }

        @Override
        public void setIdentifier(ProviderIdentifier identifier) throws RemoteException {
            Log.d(TAG, "Identifier set to "+identifier);
            mIdentifier = identifier;
            libProvider.setIdentifier(identifier);
        }

        @Override
        public void registerCallback(IProviderCallback cb) throws RemoteException {
            if (cb == null) {
                Log.e(TAG, "Trying to register null callback!");
                throw new IllegalArgumentException("Trying to register null callback!");
            }

            boolean contains = false;
            try {
                final int cbId = cb.getIdentifier();

                synchronized (mCallbacks) {
                    for (IProviderCallback callback : mCallbacks) {
                        if (callback.getIdentifier() == cbId) {
                            contains = true;
                            break;
                        }
                    }
                }

                if (!contains) {
                    mCallbacks.add(cb);
                }
            } catch (Exception e) {
                Log.e(TAG, "Cannot add callback, object is dead");
            }
        }

        @Override
        public void unregisterCallback(IProviderCallback cb) throws RemoteException {
            synchronized (mCallbacks) {
                try {
                    final int cbId = cb.getIdentifier();
                    for (IProviderCallback callback : mCallbacks) {
                        if (callback.getIdentifier() == cbId) {
                            Log.e(TAG, "Found callback with identifier " + cbId);
                            mCallbacks.remove(callback);
                            return;
                        }
                    }
                } catch (RemoteException e) {
                    Log.e(TAG, "Remote exception while getting identifier", e);
                }

                Log.e(TAG, "Can't find callback for unregistering!");
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public boolean isSetup() throws RemoteException {
            Log.d(TAG, "isSetup called.");
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(SocksoService.this);
            if(pref.contains("serverAddress") &&
                    pref.contains("serverPort") &&
                    pref.contains("accountName") &&
                    pref.contains("accountPassword"))
            {
                Log.d(TAG, "Preferences are set.");
                return true;
            }else
            {
                Log.d(TAG, "Preferences are NOT set.");
                return false;
            }
        }

        @Override
        public boolean login() throws RemoteException {
            Log.d(TAG, "Login called.");

            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(SocksoService.this);
            new LoginTask().execute(pref.getString("serverAddress",null),pref.getString("serverPort",null),pref.getString("accountName",null),pref.getString("accountPassword",null));

            while(loginResult == null);

            Log.d(TAG, "Login successful:"+loginResult.isSuccess());

            if(loginResult.isSuccess()) isAuthenticated = true;

            return loginResult.isSuccess();

        }

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public boolean isAuthenticated() throws RemoteException {
                Log.d(TAG, "isAuthenticated called");
                if(connParams != null) {
                    Log.d(TAG, "connParams is set");
                    if(connParams.getCookies() == null) return false;
                    Log.d(TAG, "cookies is set");
                    if(connParams.getCookies().size() == 0) return false;
                    Log.d(TAG, "cookies size is greater than 0");

                    // Check if the last login is more than half an hour in the past
                    if(((System.currentTimeMillis()/1000L) - connParams.getTimestampSeconds()) > 1800)
                    {
                        // Login again, return false
                        return false;
                    }
                    else {
                        Log.d(TAG, "Comparing settings to connParams");
                        Log.d(TAG, "connParams.serverAddress="+connParams.getServerAddress());
                        Log.d(TAG, "connParams.getServerPort="+connParams.getServerPort());
                        Log.d(TAG, "connParams.getAccountName="+connParams.getAccountName());
                        Log.d(TAG, "connParams.getAccountPassword="+connParams.getAccountPassword());
                        // Compare the connection parameters to the one in the preferences
                        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(SocksoService.this);
                        if (connParams.getServerAddress().equalsIgnoreCase(pref.getString("serverAddress", null)) &&
                                connParams.getServerPort().equalsIgnoreCase(pref.getString("serverPort", null)) &&
                                connParams.getAccountName().equalsIgnoreCase(pref.getString("accountName", null)) &&
                                connParams.getAccountPassword().equalsIgnoreCase(pref.getString("accountPassword", null))) {
                            // Settings match
                            return true;
                        } else {
                            return false;
                        }
                    }
                }else {
                    Log.d(TAG, "connParams is NOT set");
                    return false;
                }
        }

        @Override
        public boolean isInfinite() throws RemoteException {
            return false;
        }

        @Override
        public List<Album> getAlbums() throws RemoteException {
            Log.d(TAG, "Querying albums");
            return libProvider.getAlbums();
        }

        @Override
        public List<Artist> getArtists() throws RemoteException {
            Log.d(TAG, "Querying artists");
            return libProvider.getArtists();
        }

        @Override
        public List<Song> getSongs(int offset, int limit) throws RemoteException {
            Log.d(TAG, "Querying songs");
            return libProvider.getSongs(offset, limit);
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
        public Song getSong(String ref) throws RemoteException {
            Log.d(TAG, "Querying song "+ref);
            return libProvider.getSong(ref);
        }

        @Override
        public Artist getArtist(String ref) throws RemoteException {
            Log.d(TAG, "Querying song "+ref);
            return libProvider.getArtist(ref);
        }

        @Override
        public Album getAlbum(String ref) throws RemoteException {
            Log.d(TAG, "Querying album "+ref);
            return libProvider.getAlbum(ref);
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
        public void setAudioSocketName(String socketName) throws RemoteException {
            socket = new AudioClientSocket();
            try {
                socket.connect(socketName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public long getPrefetchDelay() throws RemoteException {
            return 0;
        }

        @Override
        public void prefetchSong(String ref) throws RemoteException {

        }

        @Override
        public boolean playSong(String ref) throws RemoteException {
            return false;
        }

        @Override
        public void pause() throws RemoteException {

        }

        @Override
        public void resume() throws RemoteException {

        }

        @Override
        public void seek(long timeMs) throws RemoteException {

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
        public List<String> getSupportedRosettaPrefix() throws RemoteException {
            return null;
        }

        @Override
        public void setPlaylistOfflineMode(String ref, boolean offline) throws RemoteException {

        }

        @Override
        public void setOfflineMode(boolean offline) throws RemoteException {

        }


    };
}
