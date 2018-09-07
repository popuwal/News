package com.android.popuwal.news.Utils;

import android.graphics.Bitmap;
import android.util.LruCache;

public class SingleLruCache {
    private static LruCache<String, Bitmap> singleLruCache ;
    private SingleLruCache(){
    }

    public static LruCache getInstance() {
        long maxMemory = Runtime.getRuntime().maxMemory();
        int cacheSize = (int) (maxMemory / 8);
        if (singleLruCache == null) {
            singleLruCache =  new LruCache<String,Bitmap>(cacheSize);
        }
        return singleLruCache;
    }
}
