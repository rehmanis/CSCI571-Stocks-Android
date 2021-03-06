package com.example.csci571andriodstocks;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.List;

public class CustomNewsAdapter extends RecyclerView.Adapter<CustomNewsAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(News newsItem, int position);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(News newsItem, int position);
    }


    private final List<News> newsList;
    private final OnItemClickListener clickListener;
    private final OnItemLongClickListener longClickListener;

    private final Context ctx;

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvNewsSrc;
        private final ImageView ivNewsImg;
        private final TextView tvLastUpdated;
        private final TextView tvNewsTitle;
        final View rootView;

        public ViewHolder(View view) {
            super(view);
            // Define click listener for the ViewHolder's View
            rootView = view;
            tvNewsSrc = (TextView) view.findViewById(R.id.tv_news_src);
            ivNewsImg = (ImageView) view.findViewById(R.id.iv_news_img);
            tvNewsTitle = (TextView) view.findViewById(R.id.tv_news_title);
            tvLastUpdated = (TextView) view.findViewById(R.id.tv_news_last_updated);
        }

        public TextView getTvNewsSrc() {
            return tvNewsSrc;
        }

        public TextView getTvLastUpdated() {
            return tvLastUpdated;
        }

        public TextView getTvNewsTitle() {
            return tvNewsTitle;
        }

        public ImageView getIvNewsImg() {
            return ivNewsImg;
        }
    }

    /**
     * Initialize the newsList of the Adapter.
     *
     * @param newsList List<News> containing the data to populate views to be used
     * by RecyclerView.
     */
    public CustomNewsAdapter(List<News> newsList, Context ctx, OnItemClickListener clickListener,
                             OnItemLongClickListener longClickListener) {

        this.newsList = newsList;
        this.ctx = ctx;
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    @Override
    public int getItemViewType(int position) {
        // Just as an example, return 0 or 2 depending on position
        // Note that unlike in ListView adapters, types don't have to be contiguous
        if (position == 0){
            return 0;
        }

        return 1;
    }


    // Create new views (invoked by the layout manager)
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        // Create a new view, which defines the UI of the list item

        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.news_item, viewGroup, false);

        if (viewType == 0){
            view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.first_news, viewGroup, false);
        }


        return new ViewHolder(view);
    }


    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {

        final News news = newsList.get(position);

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        viewHolder.getTvLastUpdated().setText(news.lastUpdated);
        viewHolder.getTvNewsTitle().setText(news.title);
        viewHolder.getTvNewsSrc().setText(news.src);

        ImageView ivNewsImg = (ImageView)  viewHolder.getIvNewsImg();

        if (viewHolder.getItemViewType() != 0){
            Picasso.with(ctx).load(news.img).resize(400, 400).into(ivNewsImg);
        }else{
            Picasso.with(ctx).load(news.img).resize(1500, 0).into(ivNewsImg);
        }

        viewHolder.rootView.setOnClickListener(v ->
                clickListener.onItemClick(news, viewHolder.getAdapterPosition()));

        viewHolder.rootView.setOnLongClickListener(v -> {

            longClickListener.onItemLongClick(news, viewHolder.getAdapterPosition());
            return true;
        });


    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return newsList.size();
    }

}
