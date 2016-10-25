package grmume.socksoprovider;

import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

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

/**
 * Created by greg on 21.10.16.
 */

public class ApiRequestTask extends AsyncTask<IApiRequestParams, Void, String>
{
    private static final String TAG = "SocksoPluginService";

    private IApiRequestParams.IApiRequestCallback cb;

    @Override
    protected String doInBackground(IApiRequestParams... params) {
        try {
            cb = params[0].getRequestCallback();
            ISocksoConnectionParams connParams = params[0].getConnectionParams();
            Log.d(TAG,"Request url:"+"http://" + URLEncoder.encode(connParams.getServerAddress(), "UTF-8") + ":" + URLEncoder.encode(connParams.getServerPort(), "UTF-8") + "/api/" + params[0].getUrl());
            URL url = new URL("http://" + URLEncoder.encode(connParams.getServerAddress(), "UTF-8") + ":" + URLEncoder.encode(connParams.getServerPort(), "UTF-8") + "/api/" + params[0].getUrl());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestProperty("Cookie",
                    TextUtils.join(";", connParams.getCookies()));

            connection.setRequestMethod("GET");
            connection.setRequestProperty("limit", Integer.toString(params[0].getLimit()));
            connection.setRequestProperty("offset", Integer.toString(params[0].getOffset()));

            InputStream in = new BufferedInputStream(connection.getInputStream());
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            StringBuilder sb = new StringBuilder();
            String inputLine = "";
            while ((inputLine = br.readLine()) != null) {
                sb.append(inputLine);
            }
            String jsonString = sb.toString();
            Log.d(TAG, "Received:"+jsonString);
            Log.d(TAG, "Response code is "+connection.getResponseCode());
            if(connection.getResponseCode() >= 200 && connection.getResponseCode() < 300) {
                return jsonString;
            }else {
                return null;
            }

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            Log.d(TAG, "Unsupported encoding"+e.getMessage());
        } catch (MalformedURLException e) {
            e.printStackTrace();
            Log.d(TAG, "MalformedURLException"+e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "IOException:"+e.getMessage());
        }

        return null;
    }

    @Override
    protected void onPostExecute(String result)
    {
        cb.handleApiResponse(result);
    }
}
