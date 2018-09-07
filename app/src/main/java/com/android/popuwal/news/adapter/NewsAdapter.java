package com.android.popuwal.news.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.popuwal.news.R;
import com.android.popuwal.news.Utils.LocalLruCache;
import com.android.popuwal.news.Utils.SingleLruCache;
import com.android.popuwal.news.activity.WebActivity;
import com.android.popuwal.news.data.NewsEntry;
import com.google.gson.Gson;
import com.jakewharton.disklrucache.DiskLruCache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.NewsViewHolder> {
    private Context mContext;
    private List<NewsEntry.ResultBeanX.ResultBean.ListBean> mList;
    private static LruCache<String, Bitmap> stringBitmapLruCache;
    private static LruCache<String, String> localLruCache ;
    private static DiskLruCache diskLruCache ;
    public NewsAdapter(Context context, List<NewsEntry.ResultBeanX.ResultBean.ListBean> list) {
        this.mContext = context;
        this.mList = list;
        stringBitmapLruCache = SingleLruCache.getInstance();
        localLruCache = LocalLruCache.getInstance();
        long maxMemory = Runtime.getRuntime().maxMemory();
        int cacheSize = (int) (maxMemory / 8);
        try {
            if (diskLruCache==null ||diskLruCache.isClosed())
            diskLruCache=DiskLruCache.open(new File(mContext.getFilesDir().getPath()+"CacheFile"),1,1,cacheSize);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void AddHeaderItem(List list, String type) {
        List finallist = checkData(mList,list);
        if (mList.size() == 0 && mContext.getSharedPreferences(mContext.getPackageName(),Context.MODE_PRIVATE).getString(type,"nodata").toString().equals("nodata")) {
            Toast.makeText(mContext, "更新了 " + finallist.size() + " 条目数据", Toast.LENGTH_SHORT).show();
        }else if (mList.size() != 0 ){
            Toast.makeText(mContext, "更新了 " + finallist.size() + " 条目数据", Toast.LENGTH_SHORT).show();
        }
        mList.addAll(0,finallist);
        notifyDataSetChanged();
        //存储最新的20条数据，采用SharePreferences实现
        if (finallist.size()>0)
        cacheLatestData(mList,type);
    }

    private List<NewsEntry.ResultBeanX.ResultBean.ListBean> checkData(List<NewsEntry.ResultBeanX.ResultBean.ListBean> mList, List<NewsEntry.ResultBeanX.ResultBean.ListBean> list) {
        if (mList == null || mList.size() == 0) {
            Log.d("POPUWAL","data is null no need to cache");
            return list;
        }
        List<NewsEntry.ResultBeanX.ResultBean.ListBean> tmplist;
        if(mList.size()>=20){
            tmplist = mList.subList(0,19);
        }else {
            tmplist = mList;
        }
        List<NewsEntry.ResultBeanX.ResultBean.ListBean> testList = new ArrayList<>();
        for (int i=0; i<list.size(); i++){
            boolean is = false;
            for (int j = 0;j<tmplist.size();j++) {
                String listTitle = list.get(i).getTitle();
                String mListTitle = tmplist.get(j).getTitle();
                if (listTitle !=null && mListTitle!= null
                        &&listTitle.equals(mListTitle) ){
                    is = true;
                }
            }
            if (!is)
            testList.add(list.get(i));
        }
        return testList;
    }

    private void cacheLatestData(List<NewsEntry.ResultBeanX.ResultBean.ListBean> mList, String type) {
        if (mList == null || mList.size() == 0) {
            Log.d("POPUWAL","data is null no need to cache");
            return;
        }
        List<NewsEntry.ResultBeanX.ResultBean.ListBean> list;
        if(mList.size()>=20){
            list = mList.subList(0,19);
        }else {
            list = mList;
        }
        Gson gson = new Gson();
        String data = gson.toJson(list);
        mContext.getSharedPreferences(mContext.getPackageName(),Context.MODE_PRIVATE).edit().putString(type,data).apply();
    }

    @NonNull
    @Override
    public NewsAdapter.NewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        return new NewsViewHolder(LayoutInflater.from(mContext).inflate(R.layout.index_news, null, false));
    }

    @Override
    public void onBindViewHolder(@NonNull NewsAdapter.NewsViewHolder holder,final int position) {
        final NewsEntry.ResultBeanX.ResultBean.ListBean listBean = (NewsEntry.ResultBeanX.ResultBean.ListBean) mList.get(position);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("POPUWAL","onItemClicked POPUWALWebActivity: "+listBean.getContent());
                Intent intent = new Intent(mContext, WebActivity.class);
                intent.putExtra("uri",mList.get(position).getUrl());
                mContext.startActivity(intent);
            }
        });
            Log.e("POPUWAL1", "pic link is: " + listBean.getPic() + " position: " + position + " imgview tag: " + holder.imageView.getTag());
            if (listBean.getPic() != null && listBean.getPic().length() > 0) {
                Bitmap b = stringBitmapLruCache.get(listBean.getPic());
                if (b == null) {
                    holder.imageView.setImageDrawable(mContext.getDrawable(R.drawable.ic_launcher_background));
                } else {
                    holder.imageView.setImageBitmap(b);
                }
                holder.imageView.setTag(listBean.getPic());

                Log.e("POPUWAL1", "pic get tag: " + holder.imageView.getTag() + " position " + position);
                new MyAsyncTask(new WeakReference<Context>(mContext), holder, listBean.getPic()).execute();
            } else {
                holder.imageView.setVisibility(View.GONE);
            }
            holder.textViewTitle.setText(listBean.getTitle());
            Log.e("POPUWAL1", "textViewTitle " + listBean.getTitle() + " position " + position);
            holder.getTextViewContent.setText(Html.fromHtml(listBean.getContent()));
            holder.fromView.setText("来源：" + listBean.getSrc());
            holder.timeView.setText(listBean.getTime());
    }

    //FIX 翻滚之后图片丢失的问题
    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    public void deleteAllList() {
        mList = new ArrayList();
        notifyDataSetChanged();
    }

    class NewsViewHolder extends RecyclerView.ViewHolder{
        ImageView imageView ;
        TextView textViewTitle ;
        TextView getTextViewContent;
        TextView fromView;
        TextView timeView;
        private NewsViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            textViewTitle = itemView.findViewById(R.id.title);
            getTextViewContent = itemView.findViewById(R.id.content);
            fromView = itemView.findViewById(R.id.from);
            timeView = itemView.findViewById(R.id.time);
        }
    }

    private static class MyAsyncTask extends AsyncTask<String,Void,Bitmap> {
        WeakReference<Context> weakReference;
        NewsViewHolder mHolder;
        String uri;
        MyAsyncTask(WeakReference<Context> context, NewsViewHolder holder, String s){
            this.weakReference = context;
            this.mHolder = holder;
            this.uri = s;
        }

        @Override
        protected Bitmap doInBackground(String... bitmaps) {
            try {
                if (uri != null) {
                    Log.e("POPUWAL1", "doInBackground to download img link is: " + uri);
                    Bitmap b = stringBitmapLruCache.get(uri);
                    String urlLocal = localLruCache.get(uri);

                    if (b == null && urlLocal ==null&& diskLruCache.get(hashKeyForDisk(uri))== null) {
                        URL url = new URL(uri);
                        Log.e("POPUWAL1", "doInBackground no lrucache ");
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setDoInput(true);
                        connection.connect();
                        InputStream inputStream = connection.getInputStream();
                        // Disk cache start
                        b = getCompressBitmap(uri,inputStream);
                        if (b!= null && uri != null) {
                            String map = uri.substring(uri.lastIndexOf("/")+1);
                            saveBitmap(map,b);
                            Log.e("POPUWAL1","save bitmap: "+weakReference.get().getFilesDir().getAbsolutePath()+map);
                            localLruCache.put(uri,weakReference.get().getFilesDir().getPath()+map);
                            stringBitmapLruCache.put(uri,b);
                        }
                    }else if (diskLruCache.get(hashKeyForDisk(uri))!=null){
                        Log.e("POPUWAL","cache from the disk!!!");
                        b = getCompressBitmap(uri,diskLruCache.get(hashKeyForDisk(uri)).getInputStream(0));
                    }
                    return b;
                } else {
                    return null;
                }
            } catch (IOException e){
                e.printStackTrace();
            }
            return null;
        }

        private void saveBitmap(String map, Bitmap b) {
            FileOutputStream fos = null;
            try {
                fos = weakReference.get().openFileOutput( map, Context.MODE_PRIVATE);
                b.compress(Bitmap.CompressFormat.PNG, 100, fos);
                Log.e("POPUWAL1","save success");
            } catch (FileNotFoundException e) {
               Log.e("POPUWAL1",e.toString());
            } finally {
                if(fos != null) {
                    try {
                        fos.flush();
                        fos.close();
                    } catch (IOException e) {
                        Log.e("POPUWAL1",e.toString());
                    }
                }
            }
        }

        /**
         * 获取ImageView实际的宽度
         * @return 返回ImageView实际的宽度
         */
        public int realImageViewWith() {
            DisplayMetrics displayMetrics = weakReference.get().getResources().getDisplayMetrics();
            ViewGroup.LayoutParams layoutParams = mHolder.imageView.getLayoutParams();

            //如果ImageView设置了宽度就可以获取实在宽带
            int width = mHolder.imageView.getWidth();
            if (width <= 0) {
                //如果ImageView没有设置宽度，就获取父级容器的宽度
                width = layoutParams.width;
            }
            if (width <= 0) {
                //获取ImageView宽度的最大值
                width = mHolder.imageView.getMaxWidth();
            }
            if (width <= 0) {
                //获取屏幕的宽度
                width = displayMetrics.widthPixels;
            }
            Log.e("ImageView实际的宽度", String.valueOf(width));
            return width;
        }

        /**
         * 根据输入流返回一个压缩的图片
         * @param input 图片的输入流
         * @return 压缩的图片
         */
        public Bitmap getCompressBitmap(String uri,InputStream input) {
            //因为InputStream要使用两次，但是使用一次就无效了，所以需要复制两个
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = input.read(buffer)) > -1 ) {
                    baos.write(buffer, 0, len);
                }
                baos.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }

            //复制新的输入流
            InputStream is = new ByteArrayInputStream(baos.toByteArray());
            InputStream is2 = new ByteArrayInputStream(baos.toByteArray());
            InputStream is3 = new ByteArrayInputStream(baos.toByteArray());// for disk cache

            DiskLruCache.Editor edit = null;
            BufferedOutputStream out = null;
            BufferedInputStream in = null;
            try {
                if (diskLruCache.get(hashKeyForDisk(uri))==null) {
                    try {
                        edit = diskLruCache.edit(hashKeyForDisk(uri));
                        if (edit != null) {
                            OutputStream outputStream = edit.newOutputStream(0);
                            in = new BufferedInputStream(is3);
                            out = new BufferedOutputStream(outputStream);
                            int b;
                            while ((b = in.read()) != -1) {
                                out.write(b);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            if (out != null) {
                                out.close();
                            }
                            if (in != null) {
                                in.close();
                            }
                            if (edit != null)
                                edit.commit();
                        } catch (final IOException e) {
                            Log.e("POPUWAL", "file cache failed!");
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            //只是获取网络图片的大小，并没有真正获取图片
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, options);
            //获取图片并进行压缩
            options.inSampleSize = getInSampleSize(options);
            options.inJustDecodeBounds = false;
            return BitmapFactory.decodeStream(is2, null, options);
        }

        /**
         * A hashing method that changes a string (like a URL) into a hash suitable for using as a
         * disk filename.
         */
        public static String hashKeyForDisk(String key) {
            String cacheKey;
            try {
                final MessageDigest mDigest = MessageDigest.getInstance("MD5");
                mDigest.update(key.getBytes());
                cacheKey = bytesToHexString(mDigest.digest());
            } catch (NoSuchAlgorithmException e) {
                cacheKey = String.valueOf(key.hashCode());
            }
            return cacheKey;
        }

        private static String bytesToHexString(byte[] bytes) {
            // http://stackoverflow.com/questions/332079
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < bytes.length; i++) {
                String hex = Integer.toHexString(0xFF & bytes[i]);
                if (hex.length() == 1) {
                    sb.append('0');
                }
                sb.append(hex);
            }
            return sb.toString();
        }

        /**
         * 获取ImageView实际的高度
         * @return 返回ImageView实际的高度
         */
        public int realImageViewHeight() {
            DisplayMetrics displayMetrics = weakReference.get().getResources().getDisplayMetrics();
            ViewGroup.LayoutParams layoutParams =mHolder.imageView.getLayoutParams();

            //如果ImageView设置了高度就可以获取实在宽度
            int height =mHolder.imageView.getHeight();
            if (height <= 0) {
                //如果ImageView没有设置高度，就获取父级容器的高度
                height = layoutParams.height;
            }
            if (height <= 0) {
                //获取ImageView高度的最大值
                height = mHolder.imageView.getMaxHeight();
            }
            if (height <= 0) {
                //获取ImageView高度的最大值
                height = displayMetrics.heightPixels;
            }
            Log.e("ImageView实际的高度", String.valueOf(height));
            return height;
        }

        /**
         * 获得需要压缩的比率
         *
         * @param options 需要传入已经BitmapFactory.decodeStream(is, null, options);
         * @return 返回压缩的比率，最小为1
         */
        public int getInSampleSize(BitmapFactory.Options options) {
            int inSampleSize = 1;
            int realWith = realImageViewWith();
            int realHeight = realImageViewHeight();

            int outWidth = options.outWidth;
            Log.e("网络图片实际的宽度", String.valueOf(outWidth));
            int outHeight = options.outHeight;
            Log.e("网络图片实际的高度", String.valueOf(outHeight));

            //获取比率最大的那个
            if (outWidth > realWith || outHeight > realHeight) {
                int withRadio = Math.round(outWidth / realWith);
                int heightRadio = Math.round(outHeight / realHeight);
                inSampleSize = withRadio > heightRadio ? withRadio : heightRadio;
            }
            Log.e("压缩比率", String.valueOf(inSampleSize));
            return inSampleSize;
        }

        @Override
        protected void onPostExecute(Bitmap o) {
            super.onPostExecute(o);
            Log.e("POPUWAL1", "onPostExecute drawable: " + o);
            if (mHolder.imageView.getTag().toString().equals(uri)) {
                if (o != null) {
                    BitmapDrawable bitmapDrawable = new BitmapDrawable(weakReference.get().getResources(),o);
                    mHolder.imageView.setVisibility(View.VISIBLE);
                    mHolder.imageView.setImageDrawable(bitmapDrawable);
                }
            }
        }
    }
}
