package com.tlcn.mvpapplication.mvp.Main.fragment.presenter;

import android.support.annotation.NonNull;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.tlcn.mvpapplication.app.App;
import com.tlcn.mvpapplication.base.BasePresenter;
import com.tlcn.mvpapplication.mvp.Main.fragment.IHomeFragmentView;

/**
 * Created by ducthinh on 12/09/2017.
 */

public class HomeFragmentPresenter extends BasePresenter implements IHomeFragmentPresenter {

    private GoogleApiClient mGoogleApiClient;

    public GoogleApiClient getGoogleApiClient() {
        return mGoogleApiClient;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (App.getGoogleApiHelper().isConnected()) {
            mGoogleApiClient = App.getGoogleApiHelper().getGoogleApiClient();
        }
    }

    public void attachView(IHomeFragmentView view) {
        super.attachView(view);
    }

    public IHomeFragmentView getView() {
        return (IHomeFragmentView) getIView();
    }

    @Override
    public void getDetailPlace(String idPlace) {
        PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                .getPlaceById(mGoogleApiClient, idPlace);
        placeResult.setResultCallback(new ResultCallback<PlaceBuffer>() {
            @Override
            public void onResult(@NonNull PlaceBuffer places) {
                if (places.getCount() == 1) {
                    getView().getDetailPlaceSuccess(places);
                } else {
                    getView().onFail("Can't get detail place!");
                }
            }
        });
    }
}
