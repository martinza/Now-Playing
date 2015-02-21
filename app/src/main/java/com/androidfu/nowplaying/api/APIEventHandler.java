package com.androidfu.nowplaying.api;

import android.content.Context;

import com.androidfu.nowplaying.R;
import com.androidfu.nowplaying.events.APIErrorEvent;
import com.androidfu.nowplaying.events.APIOkEvent;
import com.androidfu.nowplaying.events.application.GetApplicationSettingsEvent;
import com.androidfu.nowplaying.events.movies.GetMoviesEvent;
import com.androidfu.nowplaying.localcache.AppSettingsLocalStorageHandler;
import com.androidfu.nowplaying.model.application.ApplicationSettings;
import com.androidfu.nowplaying.model.movies.Movies;
import com.androidfu.nowplaying.util.EventBus;
import com.squareup.otto.Subscribe;

import org.apache.http.HttpException;

import java.sql.SQLException;

import hugo.weaving.DebugLog;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by billmote on 9/7/14.
 */
@DebugLog
public class APIEventHandler {
    //private APIRequests mAPIRequests;
    private final AppSettingsLocalStorageHandler mApplicationSettingsLocalStorageHandler;
    private final Context mContext;

    public APIEventHandler(Context context) {
        mApplicationSettingsLocalStorageHandler = new AppSettingsLocalStorageHandler(context);
        //mAPIRequests = APIBuilder.createApiInstance(context);
        mContext = context;
    }

    /**
     * APPLICATION SETTINGS
     */
    @Subscribe
    public void getApplicationSettings(final GetApplicationSettingsEvent event) {
        APIBuilder.createApiInstance(mContext, mContext.getString(R.string.application_settings_url)).getApplicationSettings(new APIHandler<ApplicationSettings>(event.getCallNumber()) {
            @Override
            public void success(ApplicationSettings applicationSettings, Response response) {
                try {
                    APIEventHandler.this.mApplicationSettingsLocalStorageHandler.saveCurrentApplicationSettings(applicationSettings);
                } catch (SQLException e1) {
                    e1.printStackTrace();
                }
                EventBus.post(new APIOkEvent(event.getCallNumber()));
            }
        });
    }

    /**
     * QUOTE OF THE DAY
     */
    @Subscribe
    public void getMovies(final GetMoviesEvent event) {
        /**
         * rotten_tomatoes_api_key is stored in a strings resource file named secret.xml and
         * located in res/values, but secret.xml has been added to .gitignore so my key doesn't
         * end up in source control.
         *
         * simply create your own res/values/secret.xml file with your own Rotten Tomatoes API
         * key and the app will run as expected.
         */
        APIBuilder.createApiInstance(mContext, mContext.getString(R.string.movies_url)).getMovies(mContext.getString(R.string.rotten_tomatoes_api_key /* Look in res/values/secret.xml */), event.getPageNumber(), event.getPageLimit(), new APIHandler<Movies>(event.getCallNumber()) {
            @Override
            public void success(Movies movies, Response response) {
                if (response.getBody().length() > 0) {
                    EventBus.post(movies);
                } else {
                    EventBus.post(new APIErrorEvent(RetrofitError.unexpectedError(mContext.getString(R.string.movies_url), new HttpException("Empty Body")), event.getCallNumber()));
                }
            }
        });
    }
}