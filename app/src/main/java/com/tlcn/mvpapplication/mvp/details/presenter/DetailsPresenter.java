package com.tlcn.mvpapplication.mvp.details.presenter;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;
import com.tlcn.mvpapplication.R;
import com.tlcn.mvpapplication.api.network.ApiCallback;
import com.tlcn.mvpapplication.api.network.RestError;
import com.tlcn.mvpapplication.api.request.BaseListRequest;
import com.tlcn.mvpapplication.api.request.action.ActionRequest;
import com.tlcn.mvpapplication.api.request.save.SaveRequest;
import com.tlcn.mvpapplication.api.response.DetailLocationResponse;
import com.tlcn.mvpapplication.api.response.DetailNewsResponse;
import com.tlcn.mvpapplication.api.response.NewsResponse;
import com.tlcn.mvpapplication.app.App;
import com.tlcn.mvpapplication.base.BasePresenter;
import com.tlcn.mvpapplication.interactor.event_bus.type.Empty;
import com.tlcn.mvpapplication.interactor.event_bus.type.ObjectEvent;
import com.tlcn.mvpapplication.model.Locations;
import com.tlcn.mvpapplication.model.Post;
import com.tlcn.mvpapplication.mvp.details.view.IDetailsView;
import com.tlcn.mvpapplication.utils.KeyUtils;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

public class DetailsPresenter extends BasePresenter implements IDetailsPresenter {
    private List<Post> mListPost;
    private SaveRequest save;
    private String idLocation;
    private Locations locations;

    public Locations getLocations() {
        return locations;
    }

    public void setIdLocation(String idLocation) {
        this.idLocation = idLocation;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        if (!getEventManager().isRegister(this))
            getEventManager().register(this);
    }

    public void attachView(IDetailsView view) {
        super.attachView(view);
    }

    public IDetailsView getView() {
        return (IDetailsView) super.getIView();
    }

    @Override
    public void getSaveState() {
        if (locations != null) {
            getView().getSaveStateSuccess(locations.isIs_save());
        } else {
            getView().getSaveStateSuccess(false);
        }
    }

    @Override
    public void getListPostFromSV(BaseListRequest request) {
        getView().showLoading();
        String token = "";
        if (App.getUserInfo().getInfo() != null && !App.getUserInfo().getInfo().getToken().isEmpty()) {
            token = App.getUserInfo().getInfo().getToken();
        }
        getManager().getListNews(token, idLocation, new ApiCallback<NewsResponse>() {
            @Override
            public void success(NewsResponse res) {
                if (!isViewAttached())
                    return;
                getView().hideLoading();
                getView().getListNewsSuccess(res.getData(), res.getMetaData());
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
    public void saveLocations() {
        getView().showLoading();
        if (App.getUserInfo().getInfo() == null || App.getUserInfo().getInfo().getToken().isEmpty()) {
            getView().hideLoading();
            getView().onFail(App.getContext().getString(R.string.please_login));
            return;
        }
        save = new SaveRequest();
        save.setToken(App.getUserInfo().getInfo().getToken());
        save.setLocation_id(locations.getId());
        getManager().saveLocation(save, new ApiCallback<DetailLocationResponse>() {
            @Override
            public void success(DetailLocationResponse res) {
                if (!isViewAttached())
                    return;
                locations = res.getData();
                getView().hideLoading();
                getView().saveLocationSuccess();
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
    public void actionDislike(String idPost) {
        if (App.getUserInfo().getInfo() == null || App.getUserInfo().getInfo().getToken().isEmpty()) {
            getView().onFail(App.getContext().getString(R.string.please_login));
            return;
        }
        getView().showLoading();
        ActionRequest actionRequest = new ActionRequest();
        actionRequest.setToken(App.getUserInfo().getInfo().getToken());
        actionRequest.setNews_id(idPost);
        getManager().dislikeNews(actionRequest, new ApiCallback<DetailNewsResponse>() {
            @Override
            public void success(DetailNewsResponse res) {
                if (!isViewAttached())
                    return;
                getView().onActionSuccess(res.getData());
                getView().hideLoading();
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

    @Override
    public void actionLike(String idPost) {
        if (App.getUserInfo().getInfo() == null || App.getUserInfo().getInfo().getToken().isEmpty()) {
            getView().onFail(App.getContext().getString(R.string.please_login));
            return;
        }
        getView().showLoading();
        ActionRequest actionRequest = new ActionRequest();
        actionRequest.setToken(App.getUserInfo().getInfo().getToken());
        actionRequest.setNews_id(idPost);
        getManager().likeNews(actionRequest, new ApiCallback<DetailNewsResponse>() {
            @Override
            public void success(DetailNewsResponse res) {
                if (!isViewAttached())
                    return;
                getView().onActionSuccess(res.getData());
                getView().hideLoading();
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

    @Override
    public void getInfoLocation() {
        getView().showLoading();
        String token = "";
        if (App.getUserInfo().getInfo() != null && !App.getUserInfo().getInfo().getToken().isEmpty()) {
            token = App.getUserInfo().getInfo().getToken();
        }
        getManager().getDetailLocation(token, idLocation, new ApiCallback<DetailLocationResponse>() {
            @Override
            public void success(DetailLocationResponse res) {
                if (!isViewAttached())
                    return;
                getView().hideLoading();
                locations = res.getData();
                BaseListRequest request = new BaseListRequest();
                request.setPage(1);
                request.setLimit(15);
                getListPostFromSV(request);
                getView().getPostSuccess();
                getView().getSaveStateSuccess(locations.isIs_save());
            }

            @Override
            public void failure(RestError error) {
                if (!isViewAttached())
                    return;
                getView().onFail(error.message);
            }
        });
//        mReference.child(KeyUtils.LOCATIONS).child(idLocation).addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(DataSnapshot dataSnapshot) {
//                if (dataSnapshot != null) {
//                    locations = dataSnapshot.getValue(Locations.class);
//
//                } else {
//                    getView().onFail("Error!!!");
//                }
//            }
//
//            @Override
//            public void onCancelled(DatabaseError databaseError) {
//                getView().onFail(databaseError.getMessage());
//            }
//        });
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
        if (objectEvent.getKeyId().equals(KeyUtils.KEY_EVENT_NEWS) && objectEvent.getSocketPost() != null) {
            if (idLocation != null && idLocation.equals(objectEvent.getSocketPost().getIdLocation())) {
                Log.d("subscribe new success", new Gson().toJson(objectEvent.getSocketPost()));
                getView().notifyNew(objectEvent.getSocketPost());
            }
        }
    }
}
