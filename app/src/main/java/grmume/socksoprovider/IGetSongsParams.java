package grmume.socksoprovider;

/**
 * Created by greg on 15.10.16.
 */

public interface IGetSongsParams {
    ISocksoLibraryProvider getLibProvider();

    int getOffset();

    int getLimit();

}
