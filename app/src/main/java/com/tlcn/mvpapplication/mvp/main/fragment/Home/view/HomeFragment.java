package com.tlcn.mvpapplication.mvp.main.fragment.Home.view;

import android.Manifest;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.tlcn.mvpapplication.R;
import com.tlcn.mvpapplication.custom_view.EditTextCustom;
import com.tlcn.mvpapplication.dialog.DialogProgress;
import com.tlcn.mvpapplication.model.direction.Route;
import com.tlcn.mvpapplication.mvp.main.adapter.PlaceSearchAdapter;
import com.tlcn.mvpapplication.mvp.main.fragment.Home.presenter.HomeFragmentPresenter;
import com.tlcn.mvpapplication.service.GPSTracker;
import com.tlcn.mvpapplication.utils.DialogUtils;
import com.tlcn.mvpapplication.utils.Utilities;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;
import io.github.kobakei.materialfabspeeddial.FabSpeedDial;

public class HomeFragment extends Fragment implements OnMapReadyCallback,
        View.OnClickListener,
        FabSpeedDial.OnMenuItemClickListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        PlaceSearchAdapter.OnItemClick,
        GoogleMap.OnCameraIdleListener,
        IHomeFragmentView {

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Bind(R.id.imv_menu)
    ImageView imvMenu;
    @Bind(R.id.fsd_floating)
    FabSpeedDial fsdFloating;
    @Bind(R.id.nv_drawer)
    NavigationView nvDrawer;
    @Bind(R.id.drl_container)
    DrawerLayout drlContainer;
    @Bind(R.id.imv_location)
    CircleImageView imvGetLocation;
    @Bind(R.id.edit_search)
    EditTextCustom editSearch;
    @Bind(R.id.rcv_search)
    RecyclerView rcvSearch;
    @Bind(R.id.rl_location)
    RelativeLayout rlLocation;

    private DialogProgress mProgressDialog;
    private HomeFragmentPresenter mPresenter = new HomeFragmentPresenter();

    private static final LatLngBounds HCM = new LatLngBounds(new LatLng(10.748822, 106.594357), new LatLng(10.902364, 106.839401));
    private Marker currentMarker;

    private List<Marker> originMarkers = new ArrayList<>();
    private List<Marker> destinationMarkers = new ArrayList<>();
    private List<Polyline> polylinePaths = new ArrayList<>();

    private PlaceSearchAdapter mAdapter;
    GoogleMap mGoogleMap;
    GPSTracker gpsTracker;
    boolean isFirst = true;
    SupportMapFragment supportMapFragment;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_home, container, false);
        supportMapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        supportMapFragment.getMapAsync(this);
        ButterKnife.bind(this, v);
        mPresenter.attachView(this);
        mPresenter.onCreate();
        initData();
        initListener();
        return v;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mGoogleMap.setMyLocationEnabled(true);
        mGoogleMap.setOnCameraIdleListener(this);
        if (isFirst) {
            if (gpsTracker.canGetLocation()) {
                mPresenter.setLngStart(new LatLng(gpsTracker.getLatitude(), gpsTracker.getLongitude()));
                CameraUpdate cameraUpdate;
                if (mPresenter.getCameraPosition() == null) {
                    cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(gpsTracker.getLatitude(), gpsTracker.getLongitude()), Utilities.DEFAULT_MAP_ZOOM);
                } else {
                    cameraUpdate = CameraUpdateFactory.newCameraPosition(mPresenter.getCameraPosition());
                }
                mGoogleMap.moveCamera(cameraUpdate);
            }
        }
        mGoogleMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                showDialog();
            }
        });
        mGoogleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                showDialog();
                return false;
            }
        });
    }

    private void showDialog() {
        final Dialog dialog = new Dialog(getContext());
        dialog.setContentView(R.layout.dialog_chiduong);
        LinearLayout lnl_xemthongtin = (LinearLayout) dialog.findViewById(R.id.lnl_xemthongtin);
        LinearLayout lnl_chiduong = (LinearLayout) dialog.findViewById(R.id.lnl_chiduong);
        lnl_xemthongtin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getContext(), "Xem thông tin", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });
        lnl_chiduong.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getContext(), "Chỉ đường", Toast.LENGTH_SHORT).show();
                mPresenter.getDirectionFromTwoPoint();
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    private void initListener() {
        imvMenu.setOnClickListener(this);
        fsdFloating.addOnMenuItemClickListener(this);
        rlLocation.setOnClickListener(this);
        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (TextUtils.isEmpty(s.toString())) {
                    rcvSearch.setVisibility(View.GONE);
                }
                if (!s.toString().equals("") && mPresenter.getGoogleApiClient().isConnected()) {
                    mAdapter.getFilter().filter(s.toString());
                    rcvSearch.setVisibility(View.VISIBLE);
                } else if (!mPresenter.getGoogleApiClient().isConnected()) {
                }

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void initData() {
        gpsTracker = new GPSTracker(getContext());
        mAdapter = new PlaceSearchAdapter(getContext(), mPresenter.getGoogleApiClient(), HCM, new AutocompleteFilter.Builder().setCountry("VN").build(), this);
        rcvSearch.setLayoutManager(new LinearLayoutManager(getContext()));
        rcvSearch.setAdapter(mAdapter);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.imv_menu:
                drlContainer.openDrawer(Gravity.START);
                break;
            case R.id.rl_location: {
                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(gpsTracker.getLatitude(), gpsTracker.getLongitude()), 15f);
                mGoogleMap.animateCamera(cameraUpdate);
            }
        }
    }

    @Override
    public void onMenuItemClick(FloatingActionButton miniFab, @Nullable TextView label, int itemId) {
        switch (itemId) {
            case R.id.item_tinhtrang:
                break;
            case R.id.item_mucdo:
                break;
        }
    }

    @Override
    public void onClickItem(String placeId, String placeDetail) {
        editSearch.setText(placeDetail);
        rcvSearch.setVisibility(View.GONE);
        mPresenter.getDetailPlace(placeId);
    }

    @Override
    public void getDetailPlaceSuccess(PlaceBuffer places) {
        if (null != currentMarker) {
            currentMarker.remove();
        }
        currentMarker = mGoogleMap.addMarker(new MarkerOptions()
                .position(places.get(0).getLatLng())
                .title(places.get(0).getName().toString()));
        currentMarker.showInfoWindow();
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(places.get(0).getLatLng(), mGoogleMap.getCameraPosition().zoom);
        mPresenter.setLngEnd(places.get(0).getLatLng());
        mGoogleMap.animateCamera(cameraUpdate);
    }

    @Override
    public void onFail(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void getDirectionSuccess(List<Route> routes) {
        for (Route route : routes) {
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(route.getLeg().get(0).getStartLocation().getLatLag(), mGoogleMap.getCameraPosition().zoom));

            originMarkers.add(mGoogleMap.addMarker(new MarkerOptions()
                    .title(route.getLeg().get(0).getStartAddress())
                    .position(route.getLeg().get(0).getStartLocation().getLatLag())));
            destinationMarkers.add(mGoogleMap.addMarker(new MarkerOptions()
                    .title(route.getLeg().get(0).getEndAddress())
                    .position(route.getLeg().get(0).getEndLocation().getLatLag())));

            PolylineOptions polylineOptions = new PolylineOptions().
                    geodesic(true).
                    color(Color.BLUE).
                    width(10);

            for (int i = 0; i < route.getPoints().size(); i++)
                polylineOptions.add(route.getPoints().get(i));

            polylinePaths.add(mGoogleMap.addPolyline(polylineOptions));
        }
    }

    @Override
    public void onStartFindDirection() {
        if (originMarkers != null) {
            for (Marker marker : originMarkers) {
                marker.remove();
            }
        }

        if (destinationMarkers != null) {
            for (Marker marker : destinationMarkers) {
                marker.remove();
            }
        }

        if (polylinePaths != null) {
            for (Polyline polyline : polylinePaths) {
                polyline.remove();
            }
        }
    }

    @Override
    public void showLoading() {
        showDialogLoading();
    }

    @Override
    public void hideLoading() {
        dismissDialogLoading();
    }

    protected void showDialogLoading() {
        dismissDialogLoading();
        mProgressDialog = DialogUtils.showProgressDialog(getContext());
    }

    protected void dismissDialogLoading() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    @Override
    public void onCameraIdle() {
        mPresenter.setCameraPosition(mGoogleMap.getCameraPosition());
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.v("Google API Callback", "Connection Done");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.v("Google API Callback", "Connection Suspended");
        Log.v("Code", String.valueOf(i));
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.v("Google API Callback", "Connection Failed");
        Log.v("Error Code", String.valueOf(connectionResult.getErrorCode()));
    }
}
