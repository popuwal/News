package com.android.popuwal.news.activity;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.popuwal.news.adapter.NewsAdapter;
import com.android.popuwal.news.R;
import com.android.popuwal.news.adapter.TitleAdatper;
import com.android.popuwal.news.data.NewsEntry;
import com.android.popuwal.news.data.TitleEntry;
import com.android.popuwal.news.listener.TitleAdapterListener;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private RecyclerView mRecyclerView;
    private RecyclerView mRecyclerViewTitle;
    private MyHandler myHandler ;
    private NewsAdapter newsAdapter ;
    private TitleAdatper titleAdatper ;
    private SwipeRefreshLayout swipeRefreshLayout ;
    private static int NEWS = 1;
    private static int TITLE = 2;
    private List<String> title = new ArrayList<>();
    private final Map<String,Integer> stringIntegerMap = new HashMap<>();

    /**
     * keep to handle sth later.
     */
    private static class MyHandler extends Handler{
        WeakReference<MainActivity> activityWeakReference;

        MyHandler(MainActivity activity){
            activityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1:
                    String deleverType = msg.getData().getString("TYPE");
                    if (activityWeakReference.get().mRecyclerView != null){
                        String tt = activityWeakReference.get().stringIntegerMap.get("CURRENT") == null ?
                                activityWeakReference.get().title.get(0).toString() :
                                activityWeakReference.get().title.get(activityWeakReference.get().stringIntegerMap.get("CURRENT")).toString();
                        Log.e("POPUWAL","To avoid onclick item ssync error data:"+tt+" deleverType "+deleverType);
                        if (tt.equals(deleverType)) {
                            activityWeakReference.get().newsAdapter.AddHeaderItem((List<NewsEntry.ResultBeanX.ResultBean.ListBean>) (msg.obj), tt);
                            activityWeakReference.get().swipeRefreshLayout.setRefreshing(false);
                        }
                    }
                    break;
                case 2:
                    if (activityWeakReference.get().mRecyclerView != null){
                        activityWeakReference.get().title = (List<String>) (msg.obj);
                        activityWeakReference.get().titleAdatper.addData(activityWeakReference.get().title);
                        String tt = activityWeakReference.get().stringIntegerMap.get("CURRENT") == null ?
                                activityWeakReference.get().title.get(0).toString() :
                                activityWeakReference.get().title.get(activityWeakReference.get().stringIntegerMap.get("CURRENT")).toString();
                        String data = activityWeakReference.get().getSharedPreferences(activityWeakReference.get().getPackageName(), Context.MODE_PRIVATE).getString(tt,"nodata");
                        Log.e("POPUWAL111","type is: "+tt+"  cache data "+data+" title: "+activityWeakReference.get().title);
                        //FIX 第一次打开的时候没有缓存的时候更新数据
                        if (data.equals("nodata")){
                            if (activityWeakReference.get().title != null) {
                                activityWeakReference.get().initNewsRequest(tt);
                                activityWeakReference.get().swipeRefreshLayout.setRefreshing(true);
                            }
                        }
                    }
                    break;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e("POPUWAL1","onCreate ");
        setContentView(R.layout.activity_main);
        myHandler = new MyHandler(this);
        List<NewsEntry.ResultBeanX.ResultBean.ListBean> headDatas = new ArrayList<>();
        String data = this.getSharedPreferences(this.getPackageName(), Context.MODE_PRIVATE).getString("头条","nodata");
        if (!data.equals("nodata")){
            Gson gson = new Gson();
            List<NewsEntry.ResultBeanX.ResultBean.ListBean> newsEntry = gson.fromJson(data, new TypeToken<List<NewsEntry.ResultBeanX.ResultBean.ListBean>>(){}.getType());
            headDatas.addAll(0,newsEntry);
        }
        newsAdapter = new NewsAdapter(getApplicationContext(),headDatas);
        //横向title start
        List<String> titleData = new ArrayList<>();
        titleAdatper = new TitleAdatper(getApplicationContext(),titleData);
        String tttt = this.getSharedPreferences(this.getPackageName(), Context.MODE_PRIVATE).getString("title","nodata");
        Log.e("POPUWAL","tttt:1"+tttt);
        if (!tttt.equals("nodata")){
            Gson gson = new Gson();
            List<String> newsEntry = gson.fromJson(tttt, new TypeToken<List<String>>(){}.getType());
            List<String> titleDataCache = new ArrayList<>();
            titleDataCache.addAll(0,newsEntry);
            titleAdatper.addData(titleDataCache);
            title = titleData;
            String tt = stringIntegerMap.get("CURRENT")==null?title.get(0).toString():title.get(stringIntegerMap.get("CURRENT")).toString();
            if (data.equals("nodata")){
                initNewsRequest(tt);
            }
        }else{
            initTitleRequest();
        }

        mRecyclerViewTitle = findViewById(R.id.id_recyclerview_title);
        LinearLayoutManager layoutManagerTitle = new LinearLayoutManager(this);
        layoutManagerTitle.setOrientation(LinearLayoutManager.HORIZONTAL);
        mRecyclerViewTitle.setLayoutManager(layoutManagerTitle);
        //添加Android自带的分割线
        //mRecyclerViewTitle.addItemDecoration(new DividerItemDecoration(this,DividerItemDecoration.HORIZONTAL));
        //mRecyclerViewTitle.setAdapter(titleAdatper);
        titleAdatper.setAdapterListener( new TitleAdapterListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                newsAdapter.deleteAllList();
                stringIntegerMap.put("CURRENT",position);
                List<NewsEntry.ResultBeanX.ResultBean.ListBean> headDatas = new ArrayList<>();
                Log.e("POPUWAL111","title is: "+title+" stringIntegerMap "+stringIntegerMap.get("CURRENT"));
                String dataType = getApplicationContext().getSharedPreferences(getApplicationContext().getPackageName(), Context.MODE_PRIVATE).getString(title.get(stringIntegerMap.get("CURRENT")==null?0:stringIntegerMap.get("CURRENT")).toString(),"nodata");
                if (!dataType.equals("nodata")){
                    Gson gson = new Gson();
                    List<NewsEntry.ResultBeanX.ResultBean.ListBean> newsEntry = gson.fromJson(dataType, new TypeToken<List<NewsEntry.ResultBeanX.ResultBean.ListBean>>(){}.getType());
                    headDatas.addAll(0,newsEntry);
                    newsAdapter.AddHeaderItem(headDatas,title.get(stringIntegerMap.get("CURRENT")==null?0:stringIntegerMap.get("CURRENT")).toString());
                }
                // Not update once user click this item
                //swipeRefreshLayout.setRefreshing(true);
                //initNewsRequest(title.get(position).toString());
            }
        });
        mRecyclerViewTitle.setAdapter(titleAdatper);
        //横向title end

        swipeRefreshLayout = findViewById(R.id.swipeRefreshview);
        swipeRefreshLayout.setColorSchemeColors(Color.RED,Color.BLUE,Color.GREEN);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        String tt = stringIntegerMap.get("CURRENT")==null?title.get(0).toString():title.get(stringIntegerMap.get("CURRENT")).toString();
                        initNewsRequest(tt);
                        //修改逻辑，第一次打开的时候不更新界面，暂时
                        /*
                        List<NewsEntry.ResultBeanX.ResultBean.ListBean> headDatas = new ArrayList<>();
                        NewsEntry.ResultBeanX.ResultBean.ListBean listBean = new NewsEntry.ResultBeanX.ResultBean.ListBean();
                        listBean.setCategory("test");
                        listBean.setContent("News for all of the world");
                        listBean.setSrc("CMCC");
                        listBean.setTime("19911124");
                        listBean.setTitle("震惊！");
                        for (int i = 20; i <30 ; i++) {
                            headDatas.add(listBean);
                        }
                        newsAdapter.AddHeaderItem(headDatas);
                        //刷新完成
                        swipeRefreshLayout.setRefreshing(false);
                        Toast.makeText(MainActivity.this, "更新了 "+headDatas.size()+" 条目数据", Toast.LENGTH_SHORT).show();
                        */
                    }

                }, 1000);

            }
        });
        mRecyclerView = findViewById(R.id.id_recyclerview);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(layoutManager);
        //添加Android自带的分割线
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this,DividerItemDecoration.VERTICAL));
        mRecyclerView.setAdapter(newsAdapter);

        if(mRecyclerView.getRecycledViewPool()!=null){
            mRecyclerView.getRecycledViewPool().setMaxRecycledViews(0, 25);
        }
        //FIX 第一次打开的时候暂时不更新数据
        //initNewsRequest();
    }

    private void initTitleRequest() {
        Log.e("POPUWAL1","initTitleRequest ");
        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
        // https://wx.jdcloud.com/market/datas/31/11073
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET,
                "https://way.jd.com/jisuapi/channel?appkey=5141f67e8ed04b4f173d63e2efb58825",
                null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                String ss = response.toString();
                //Log.e("POPUWAL1","onResponse "+ss);
                Gson gson = new Gson();
                TitleEntry titleEntry = gson.fromJson(ss, new TypeToken<TitleEntry>(){}.getType());
                List<String> list = titleEntry.getResult().getResult();
                //Log.e("POPUWAL1","onResponse "+list.size());
                Message message = myHandler.obtainMessage();
                //message.obj = testVolleyJsons.getData(); //for all
                message.what = TITLE;
                message.obj =list;
                myHandler.sendMessage(message);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("POPUWAL1","onErrorResponse "+error.toString());
                Toast.makeText(getApplicationContext(), "error.toString()", Toast.LENGTH_SHORT).show();
            }
        });
        // Fix for onErrorResponse com.android.volley.TimeoutError
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(3000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(jsonObjectRequest);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e("POPUWAL1","onResume ");
    }

    private void initNewsRequest(final String title) {
        Log.e("POPUWAL1","initNewsRequest ");
        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
        // https://wx.jdcloud.com/market/datas/31/11073
        String uri = "https://way.jd.com/jisuapi/get?channel="+title+"&num=20&start=0&appkey=5141f67e8ed04b4f173d63e2efb58825";
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET,
                uri,
                null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                String ss = response.toString();
                //Log.e("POPUWAL1","onResponse "+ss);
                Gson gson = new Gson();
                NewsEntry newsEntry = gson.fromJson(ss, new TypeToken<NewsEntry>(){}.getType());
                List<NewsEntry.ResultBeanX.ResultBean.ListBean> list = newsEntry.getResult().getResult().getList();
                //Log.e("POPUWAL1","onResponse "+list.size());
                Message message = myHandler.obtainMessage();
                //message.obj = testVolleyJsons.getData(); //for all
                message.what = NEWS;
                Bundle bundle = new Bundle() ;
                bundle.putString("TYPE",title);
                message.setData(bundle);
                message.obj =list;
                myHandler.sendMessage(message);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("POPUWAL1","onErrorResponse "+error.toString());
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(getApplicationContext(), "连接超时，请重新刷新!", Toast.LENGTH_SHORT).show();
            }
        });
        // Fix for onErrorResponse com.android.volley.TimeoutError
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(3000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(jsonObjectRequest);
    }
}
