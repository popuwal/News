package com.android.popuwal.news.adapter;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.popuwal.news.R;
import com.android.popuwal.news.data.NewsEntry;
import com.android.popuwal.news.listener.TitleAdapterListener;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TitleAdatper extends RecyclerView.Adapter<TitleAdatper.ViewHolder> {
    private static int CLICKED = 1;
    private static int UNCLICKED = 0;

    private Context mContext;
    private List<String> mList ;
    private TitleAdapterListener.OnItemClickListener titleAdapterListener ;
    Map<String,Boolean> stringBooleanMap = new HashMap<>();

    public TitleAdatper(Context context, List<String> list){
        this.mContext = context;
        this.mList = list;
    }
    @NonNull
    @Override
    public TitleAdatper.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(mContext).inflate(R.layout.title_recyclerlayout, parent, false), new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int postion) {
                for (int i=0;i<mList.size();i++){
                    stringBooleanMap.put(mList.get(i).toString(),false);
                }
                stringBooleanMap.put(mList.get(postion).toString(),true);
                notifyDataSetChanged();
                // refer https://blog.csdn.net/qq_39734239/article/details/78521296
                titleAdapterListener.onItemClick(view, postion);
            }
        });
    }

    public void setAdapterListener(TitleAdapterListener.OnItemClickListener listener){
        titleAdapterListener = listener;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.textView.setText(mList.get(position).toString());
        boolean first = true;
        for (int i=0;i<mList.size();i++) {
            if (getItemViewType(i) == 0) {
                continue;
            }
            first = false;
        }
        Log.e("POPUWAL","getItemViewType for position:"+position);
        if (first && position == 0 ) {
            stringBooleanMap.put(mList.get(position).toString(),true);
        }
        //Log.e("POPUWAL","getItemViewType for position:"+position+" getItemViewType(position): "+getItemViewType(position));
        if (getItemViewType(position) == 1){
            holder.itemView.setBackgroundColor(Color.GREEN);
        }else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return stringBooleanMap.get(mList.get(position).toString())?CLICKED:UNCLICKED;
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    public void addData(List<String> obj) {
        Log.e("POPUWAL","tttt:addbefore"+mList);
        mList.addAll(obj);
        Log.e("POPUWAL","tttt:addafter"+mList);
        for (int i=0;i<mList.size();i++){
            stringBooleanMap.put(mList.get(i).toString(),false);
        }
        cacheLatestData(mList,"title");
        notifyDataSetChanged();
    }


    private void cacheLatestData(List<String> mList, String type) {
        if (mList == null || mList.size() == 0) {
            Log.d("POPUWAL","data is null no need to cache");
            return;
        }
        List<String> list;
        if(mList.size()>=14){
            list = mList.subList(0,13);
        }else {
            list = mList;
        }
        Gson gson = new Gson();
        String data = gson.toJson(list);
        mContext.getSharedPreferences(mContext.getPackageName(),Context.MODE_PRIVATE).edit().putString(type,data).apply();
    }
    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView textView ;
        private OnItemClickListener  onClickListener ;
         public ViewHolder(View itemView, OnItemClickListener  listener) {
             super(itemView);
             onClickListener = listener;
             itemView.setOnClickListener(this);
             textView = itemView.findViewById(R.id.title_text);
         }

         @Override
         public void onClick(View v) {
             onClickListener.onItemClick(itemView,getLayoutPosition());
         }
     }
     public interface OnItemClickListener {
        public void onItemClick(View view, int postion);
    }
}
