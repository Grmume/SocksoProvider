package grmume.socksoprovider;

import org.json.JSONObject;

/**
 * Created by greg on 21.10.16.
 */

public interface IApiRequestParams {

    ISocksoConnectionParams getConnectionParams();

    String getUrl();

    int getLimit();

    int getOffset();

    public interface IApiRequestCallback
    {
        void handleApiResponse(String json);
    }

    IApiRequestCallback getRequestCallback();

}
