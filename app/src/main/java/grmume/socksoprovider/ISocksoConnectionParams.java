package grmume.socksoprovider;

import java.net.HttpCookie;
import java.util.List;

/**
 * Created by greg on 14.10.16.
 */

public interface ISocksoConnectionParams{

    List<HttpCookie> getCookies();

    String getServerAddress();

    String getServerPort();

    String getAccountName();

    String getAccountPassword();

}
