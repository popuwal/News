package com.android.popuwal.news.Utils;

import android.graphics.Bitmap;
import android.util.LruCache;

public class LocalLruCache {
    private static LruCache<String, String> singleLruCache ;
    private LocalLruCache(){
    }

    public static LruCache getInstance() {
        long maxMemory = Runtime.getRuntime().maxMemory();
        int cacheSize = (int) (maxMemory / 8);
        if (singleLruCache == null) {
            singleLruCache =  new LruCache<String,String>(cacheSize);
        }
        return singleLruCache;
    }
}
