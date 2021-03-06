package com.tlcn.mvpapplication.mvp.main.fragment.Home.presenter;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.tlcn.mvpapplication.R;
import com.tlcn.mvpapplication.api.network.ApiCallback;
import com.tlcn.mvpapplication.api.network.ApiServices;
import com.tlcn.mvpapplication.api.network.BaseResponse;
import com.tlcn.mvpapplication.api.network.RestCallback;
import com.tlcn.mvpapplication.api.network.RestError;
import com.tlcn.mvpapplication.api.request.LocationByDistanceRequest;
import com.tlcn.mvpapplication.api.request.user.LoginRequest;
import com.tlcn.mvpapplication.api.response.GetDirectionResponse;
import com.tlcn.mvpapplication.api.response.LocationsResponse;
import com.tlcn.mvpapplication.api.response.LoginResponse;
import com.tlcn.mvpapplication.app.App;
import com.tlcn.mvpapplication.app.AppManager;
import com.tlcn.mvpapplication.base.BasePresenter;
import com.tlcn.mvpapplication.caches.storage.MapStorage;
import com.tlcn.mvpapplication.interactor.event_bus.type.ObjectEvent;
import com.tlcn.mvpapplication.model.Locations;
import com.tlcn.mvpapplication.model.PolylineInfo;
import com.tlcn.mvpapplication.model.direction.Route;
import com.tlcn.mvpapplication.mvp.main.fragment.Home.view.IHomeView;
import com.tlcn.mvpapplication.utils.KeyUtils;
import com.tlcn.mvpapplication.utils.MapUtils;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import static com.tlcn.mvpapplication.utils.KeyUtils.checkLevel;

public class HomePresenter extends BasePresenter implements IHomePresenter {
    private int boundRadiusLoad = 300;
    private LatLng lngStart, lngEnd, currentLocation;
    private GetDirectionResponse directionResponse;
    private List<Route> routes;
    private List<Locations> listPlace = new ArrayList<>();
    private List<Locations> allLocation = new ArrayList<>();
    private GoogleApiClient mGoogleApiClient;
    private CameraPosition mCameraPosition;
    private boolean continuousShowDialog = true;
    private boolean stateUI;

    public void setContinuousShowDialog(boolean continuousShowDialog) {
        this.continuousShowDialog = continuousShowDialog;
    }

    public void setCameraPosition(CameraPosition cameraPosition) {
        this.mCameraPosition = cameraPosition;
    }

    public boolean isStateUI() {
        return stateUI;
    }

    public CameraPosition getCameraPosition() {
        return mCameraPosition;
    }

    public List<Route> getRoutes() {
        return routes;
    }

    public Route getRouteSelected() {
        Route item = new Route();
        for (Route route : routes) {
            if (route.isSelected()) {
                item = route;
                break;
            }
        }
        return item;
    }

    public GoogleApiClient getGoogleApiClient() {
        if (mGoogleApiClient == null)
            mGoogleApiClient = App.getGoogleApiHelper().getGoogleApiClient();
        return mGoogleApiClient;
    }

    public void setLngStart(LatLng lngStart) {
        this.lngStart = lngStart;
    }

    public void setLngEnd(LatLng lngEnd) {
        this.lngEnd = lngEnd;
    }


    public int getBoundRadiusLoad() {
        return boundRadiusLoad;
    }

    public void setBoundRadiusLoad(int boundRadiusLoad) {
        this.boundRadiusLoad = boundRadiusLoad;
    }

    public List<Locations> getListPlace() {
        if (listPlace == null)
            listPlace = new ArrayList<>();
        return listPlace;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (App.getGoogleApiHelper().isConnected()) {
            mGoogleApiClient = App.getGoogleApiHelper().getGoogleApiClient();
        }
        mCameraPosition = MapStorage.getInstance().getCameraPosition();
        stateUI = MapStorage.getInstance().getStateUI();
        routes = MapStorage.getInstance().getDirection();
        if (!getEventManager().isRegister(this))
            getEventManager().register(this);
    }

    public void attachView(IHomeView view) {
        super.attachView(view);
    }

    public IHomeView getView() {
        return (IHomeView) getIView();
    }

    @Override
    public void getDetailPlace(String idPlace) {
        PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                .getPlaceById(mGoogleApiClient, idPlace);
        placeResult.setResultCallback(new ResultCallback<PlaceBuffer>() {
            @Override
            public void onResult(@NonNull PlaceBuffer places) {
                if (!isViewAttached())
                    return;
                if (places.getCount() == 1) {
                    getView().getDetailPlaceSuccess(places);
                    places.release();
                } else {
                    getView().onFail(App.getContext().getString(R.string.cant_get_detail_place));
                }
            }
        });
    }

    @Override
    public void getDirectionFromTwoPoint() {
        if (lngEnd == null || lngStart == null) {
            getView().onFail(App.getContext().getString(R.string.null_data));
            return;
        }
        getView().showLoading();
        AppManager.http_api_direction().from(ApiServices.class).getDirection(convertLatLngToString(lngStart),
                convertLatLngToString(lngEnd),
                KeyUtils.KEY_DIRECTION_API, true, "vi").enqueue(new RestCallback<GetDirectionResponse>() {
            @Override
            public void success(GetDirectionResponse res) {
                if (!isViewAttached())
                    return;
                getView().onStartFindDirection();
                if (routes != null) {
                    routes.clear();
                }
                directionResponse = res;
                getTrafficJamLocation();
            }

            @Override
            public void failure(RestError error) {
                if (!isViewAttached())
                    return;
                getView().onFail(error.message);
                getView().hideLoading();
            }
        });
    }

    private void getTrafficJamLocation() {
        getManager().getTrafficJamLocation(new ApiCallback<LocationsResponse>() {
            @Override
            public void success(LocationsResponse res) {
                if (!isViewAttached())
                    return;
                if (res.getData() != null) {
                    allLocation.clear();
                    allLocation.addAll(res.getData());
                }
                new getDirectionTask().execute(directionResponse);
            }

            @Override
            public void failure(RestError error) {
                if (!isViewAttached())
                    return;
                Log.d("error", " find location");
                new getDirectionTask().execute(directionResponse);
            }
        });
    }

    @Override
    public void getDetailLocation(final LatLng latLng) {
        for (Locations item : listPlace) {
            if (item.getLat() == latLng.latitude && item.getLng() == latLng.longitude) {
                getView().getDetailLocationSuccess(item);
                getView().hideLoading();
                return;
            }
        }
        getView().hideLoading();
        getView().onFail(App.getContext().getString(R.string.there_is_not_current_infomation));
    }

    @Override
    public void getInfoPlace(final LatLng latLng) {
        LocationByDistanceRequest request = new LocationByDistanceRequest();
        request.setLatitude(latLng.latitude);
        request.setLongitude(latLng.longitude);
        request.setDistance(boundRadiusLoad);
        currentLocation = latLng;
        getManager().getLocationsByDistance(request, new ApiCallback<LocationsResponse>() {
            @Override
            public void success(LocationsResponse res) {
                if (!isViewAttached())
                    return;
                listPlace.clear();
                listPlace.addAll(res.getData());
                if (listPlace.size() == 0 && boundRadiusLoad < 500 && continuousShowDialog) {
                    getView().showDialogConfirmNewRadius();
                }
                getView().showPlaces();
            }

            @Override
            public void failure(RestError error) {
                if (!isViewAttached())
                    return;
                getView().onFail(error.message);
            }
        });
    }

    @Override
    public void saveCurrentStateMap() {
        if (mCameraPosition != null)
            MapStorage.getInstance().setCameraPosition(mCameraPosition);
        MapStorage.getInstance().setStateUI(stateUI);
        if (routes != null) {
            MapStorage.getInstance().setDirection(routes);
        }
    }

    @Override
    public void login(LoginRequest request) {
        getView().showLoading();
        getManager().login(request, new ApiCallback<LoginResponse>() {
            @Override
            public void success(LoginResponse res) {
                if (!isViewAttached())
                    return;
                App.getUserInfo().saveInfo(res.getData());
                getView().hideLoading();
            }

            @Override
            public void failure(RestError error) {
                if (!isViewAttached())
                    return;
                getView().hideLoading();
                getView().onFail(error.message);
            }
        });
    }

    @Override
    public void logout() {
        getView().showLoading();
        if (App.getUserInfo().getInfo() == null || App.getUserInfo().getInfo().getToken().isEmpty()) {
            getView().onFail(App.getContext().getString(R.string.please_login));
            return;
        }
        getManager().logout(App.getUserInfo().getInfo().getToken(), new ApiCallback<BaseResponse>() {
            @Override
            public void success(BaseResponse res) {
                if (!isViewAttached())
                    return;
                App.getUserInfo().deleteInfo();
                getView().hideLoading();
            }

            @Override
            public void failure(RestError error) {
                if (!isViewAttached())
                    return;
                getView().hideLoading();
                getView().onFail(error.message);
            }
        });
    }

    private String convertLatLngToString(LatLng latLng) {
        return String.valueOf(latLng.latitude) + "," + String.valueOf(latLng.longitude);
    }

    public void setStateUI(boolean stateUI) {
        this.stateUI = stateUI;
    }

    private class getDirectionTask extends AsyncTask<GetDirectionResponse, Void, Void> {

        @Override
        protected Void doInBackground(GetDirectionResponse... params) {
            for (Route route : params[0].getRoutes()) {
                routes.add(new PolylineInfo(route, allLocation).getRoute());
            }
            routes.get(0).setSelected(true);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            getView().getDirectionSuccess();
            getView().hideLoading();
        }
    }

    @Override
    public void onDestroy() {
        if (getEventManager().isRegister(this))
            getEventManager().unRegister(this);
        super.onDestroy();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(ObjectEvent objectEvent) {
        if (objectEvent == null || !isViewAttached())
            return;
        if (objectEvent.getKeyId().equals(KeyUtils.KEY_EVENT_LOCATIONS) && objectEvent.getSocketLocation() != null) {
            Locations pointChange = objectEvent.getSocketLocation();
            Log.d("subscribe home fragment", new Gson().toJson(pointChange));
            if (listPlace.contains(pointChange)) {
                Locations locExisted = listPlace.get(listPlace.indexOf(pointChange));
                if (checkLevel(locExisted.getCurrent_level()) != checkLevel(pointChange.getCurrent_level())) {
                    listPlace.set(listPlace.indexOf(locExisted), pointChange);
                    getView().showPlaces();
                }
            } else {
                LatLng newPoint = new LatLng(pointChange.getLat(), pointChange.getLng());
                if (MapUtils.distanceBetweenTwoPoint(currentLocation, newPoint) < boundRadiusLoad) {
                    listPlace.add(pointChange);
                    getView().showPlaces();
                }
            }
        }
    }
}
