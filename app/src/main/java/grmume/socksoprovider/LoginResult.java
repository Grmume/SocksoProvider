package grmume.socksoprovider;

import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by greg on 15.10.16.
 */

public class LoginResult {

    public LoginResult()
    {
        this.success = false;
        this.cookies = null;
    }

    public LoginResult(boolean succes, List<HttpCookie> cookies)
    {
        this.success = success;
        this.cookies = cookies;
    }

    private boolean success;

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setCookies(List<HttpCookie> cookies) {
        this.cookies = cookies;
    }

    public List<HttpCookie> getCookies() {

        return cookies;
    }

    public boolean isSuccess() {
        return success;
    }

    private List<HttpCookie> cookies;

}
