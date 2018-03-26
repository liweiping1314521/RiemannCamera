package com.riemann.camera;

import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.riemann.camera.data.HorizontalData;

import java.util.List;

public class FilterRecyclerAdapter extends RecyclerView.Adapter<FilterRecyclerAdapter.ViewHolder>{

    private List<HorizontalData> values;
    public FilterRecyclerAdapter(List<HorizontalData> values){
        this.values = values;
    }

    @Override
    public int getItemCount() {
        return values.size();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.camera_effect_list_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        HorizontalData data = values.get(position);
        holder.mRel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mOnItemClickLitener.onItemClick(holder.mRel, position);
            }
        });
        holder.mTextView.setText(data.getEffectName());
        holder.mTextView.setBackgroundColor(Color.parseColor(data.getTopFilter()));
        holder.mFilterImage.setImageDrawable(data.getFilterDrawable());
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position, List<Object> payloads) {
        super.onBindViewHolder(holder, position, payloads);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public RelativeLayout mRel;
        public TextView mTextView;
        public ImageView mFilterImage;
        public ImageView mNewFilterImage;
        public ImageView mCoverImage;

        public ViewHolder(View convertView) {
            super(convertView);
            mRel = (RelativeLayout)convertView.findViewById(R.id.rel_item);
            mTextView = (TextView) convertView.findViewById(R.id.tv_name);
            mFilterImage = (ImageView) convertView.findViewById(R.id.iv_filter);
            mNewFilterImage = (ImageView) convertView.findViewById(R.id.iv_new_filter);
            mCoverImage = (ImageView) convertView.findViewById(R.id.iv_cover);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    private OnItemClickListener mOnItemClickLitener;

    public void setOnItemClickLitener(OnItemClickListener mOnItemClickLitener) {
        this.mOnItemClickLitener = mOnItemClickLitener;
    }
}
