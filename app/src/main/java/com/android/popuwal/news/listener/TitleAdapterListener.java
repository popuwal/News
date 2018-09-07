package com.android.popuwal.news.listener;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

public class TitleAdapterListener implements android.support.v7.widget.RecyclerView.OnItemTouchListener {
    private OnItemClickListener onItemClickListener ;
    private int mLastDownX;
    private int mLastDownY;
    private boolean isMove;
    private int touchSlop;
    boolean isClick =false;
    public TitleAdapterListener(Context context, OnItemClickListener listener){
        onItemClickListener =listener;
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }
    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
        int x = (int) e.getX();
        int y = (int) e.getY();
        switch (e.getAction()){
            case MotionEvent.ACTION_DOWN:
                mLastDownX = x;
                mLastDownY = y;
                isMove = false;
                isClick = false;
                break;

            case MotionEvent.ACTION_MOVE:
                if(Math.abs(x - mLastDownX)>touchSlop || Math.abs(y - mLastDownY)>touchSlop){
                    isMove = true;
                }
                break;
            case MotionEvent.ACTION_UP:
                if (isMove)break;
                isClick = true;
                break;
        }
        View childView = rv.findChildViewUnder(e.getX(),e.getY());
        Log.e("POPUWALmove","isMove: "+isMove+" isClick: "+isClick);
        if(childView != null && isClick){
            //回调mListener#onItemClick方法
            onItemClickListener.onItemClick(childView,rv.getChildLayoutPosition(childView));
            return true;
        }
        return false;
    }

    @Override
    public void onTouchEvent(RecyclerView rv, MotionEvent e) {

    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

    }
    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

}
