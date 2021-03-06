package com.example.csci571andriodstocks;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;

import io.github.luizgrp.sectionedrecyclerviewadapter.Section;
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionParameters;

public class PortfolioSection extends Section implements ItemMoveCallback.ItemTouchHelperContract{
    private final String title;
    private final List<Company> list;
    private final PortfolioSection.ClickListener clickListener;
    private String netWorth;
    private final Context ctx;

    public PortfolioSection(@NonNull final String title, @NonNull final List<Company> list, String netWorth,
                     Context ctx, @NonNull final PortfolioSection.ClickListener clickListener) {
        // call constructor with layout resources for this Section header and items
        super(SectionParameters.builder()
                .itemResourceId(R.layout.item_company)
                .headerResourceId(R.layout.portfolio_sec_header)
                .build());

        this.title = title;
        this.list = list;
        this.clickListener = clickListener;
        this.netWorth = netWorth;
        this.ctx = ctx;
    }

    public void setNetWorth(String netWorth){
        this.netWorth = netWorth;
    }


    @Override
    public int getContentItemsTotal() {
        return list.size(); // number of items of this section
    }

    public List<Company> getData() {
        return list;
    }

    @Override
    public RecyclerView.ViewHolder getItemViewHolder(View view) {
        // return a custom instance of ViewHolder for the items of this section
        return new CompanyItemViewHolder(view);
    }

    @Override
    public void onBindItemViewHolder(RecyclerView.ViewHolder holder, int position) {
        final CompanyItemViewHolder itemHolder = (CompanyItemViewHolder) holder;

        final Company company = list.get(position);

        itemHolder.ticker.setText(company.ticker);
        itemHolder.imgItem.setImageResource(company.arrow);
        itemHolder.change.setText(String.valueOf(company.change));
        itemHolder.change.setTextColor(company.changeColor);
        itemHolder.shares_or_name.setText(company.shares + " shares");
        itemHolder.last.setText(company.last);

        itemHolder.rootView.setOnClickListener(v ->
                clickListener.onItemRootViewClicked(company, itemHolder.getAdapterPosition())
        );

        itemHolder.btnGoTo.setOnClickListener(v ->
                clickListener.onItemRootViewClicked(company, itemHolder.getAdapterPosition())
        );
    }


    @Override
    public RecyclerView.ViewHolder getHeaderViewHolder(View view) {

        return new CompanyHeaderViewHolder(view);
    }

    @Override
    public void onBindHeaderViewHolder(final RecyclerView.ViewHolder holder) {
        final CompanyHeaderViewHolder headerHolder = (CompanyHeaderViewHolder) holder;

        headerHolder.tvNetWorth.setText(netWorth);
        headerHolder.tvTitle.setText(title);
    }

    @Override
    public void onRowMoved(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(list, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(list, i, i - 1);
            }
        }
    }

    @Override
    public void onRowSelected(CompanyItemViewHolder myViewHolder) {
        myViewHolder.rootView.setBackgroundColor(Color.GRAY);
    }

    @Override
    public void onRowClear(CompanyItemViewHolder myViewHolder) {
        myViewHolder.rootView.setBackgroundColor(ResourcesCompat.getColor(ctx.getResources(), R.color.grey, null));
    }


    interface ClickListener {

        void onItemRootViewClicked(Company company, final int itemAdapterPosition);
    }
}
