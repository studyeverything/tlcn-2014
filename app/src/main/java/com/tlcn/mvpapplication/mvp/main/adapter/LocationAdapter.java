package com.tlcn.mvpapplication.mvp.main.adapter;

import android.content.Context;
import android.support.v7.widget.AppCompatRatingBar;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.tlcn.mvpapplication.R;
import com.tlcn.mvpapplication.model.Locations;
import com.tlcn.mvpapplication.utils.DateUtils;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

public class LocationAdapter extends RecyclerView.Adapter<LocationAdapter.LocationViewHolder> {
    private List<Locations> mList;
    private Context mContext;

    public LocationAdapter(List<Locations> mList, Context mContext, OnItemClick mListener) {
        this.mList = mList;
        this.mContext = mContext;
        this.mListener = mListener;
    }

    @Override
    public LocationViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new LocationViewHolder(LayoutInflater.from(mContext).inflate(R.layout.item_location, parent, false));
    }

    @Override
    public void onBindViewHolder(LocationViewHolder holder, int position) {
        final Locations item = mList.get(position);
        holder.tvTitle.setText(item.getTitle());
        holder.rtbLevel.setRating((float) item.getCurrent_level());
        if (item.getCurrent_level() <= 2) {
            holder.tvLevel.setText(R.string.low);
            holder.tvLevel.setTextColor(mContext.getResources().getColor(R.color.green));
        } else if (item.getCurrent_level() < 4) {
            holder.tvLevel.setText(R.string.medium);
            holder.tvLevel.setTextColor(mContext.getResources().getColor(R.color.yellow));
        } else {
            holder.tvLevel.setText(R.string.high);
            holder.tvLevel.setTextColor(mContext.getResources().getColor(R.color.red));
        }
        holder.tvTime.setText(DateUtils.getTimeAgo(mContext, DateUtils.parseStringToDate(item.getLast_modify())));
        holder.btnDetail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.OnClickDetail(item);
            }
        });
        holder.rlShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.OnClickShare(item);
            }
        });
        holder.tvStopped.setVisibility(item.isStatus() ? View.VISIBLE : View.GONE);
        holder.tvStopped.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onClickStopped(item.getId());
            }
        });
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    class LocationViewHolder extends RecyclerView.ViewHolder {
        @Bind(R.id.tv_title)
        TextView tvTitle;
        @Bind(R.id.rtb_level)
        AppCompatRatingBar rtbLevel;
        @Bind(R.id.tv_level)
        TextView tvLevel;
        @Bind(R.id.tv_time)
        TextView tvTime;
        @Bind(R.id.tv_stopped)
        TextView tvStopped;
        @Bind(R.id.btn_details)
        Button btnDetail;
        @Bind(R.id.rl_share)
        RelativeLayout rlShare;

        LocationViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    private OnItemClick mListener;

    public interface OnItemClick {

        void OnClickShare(Locations item);

        void OnClickDetail(Locations item);

        void onClickStopped(String id);
    }
}
