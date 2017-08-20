package com.zchu.reader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import com.zchu.reader.anim.CoverPageAnim;
import com.zchu.reader.anim.HorizonPageAnim;
import com.zchu.reader.anim.NonePageAnim;
import com.zchu.reader.anim.PageAnimation;
import com.zchu.reader.anim.ScrollPageAnim;
import com.zchu.reader.anim.SimulationPageAnim;
import com.zchu.reader.anim.SlidePageAnim;


/**
 * Created by Administrator on 2016/8/29 0029.
 * 原作者的GitHub Project Path:(https://github.com/PeachBlossom/treader)
 * 绘制页面显示内容的类
 */
public class PageView extends View {

    public final static int PAGE_MODE_SIMULATION = 0;
    public final static int PAGE_MODE_COVER = 1;
    public final static int PAGE_MODE_SLIDE = 2;
    public final static int PAGE_MODE_NONE = 3;
    //滚动效果
    public final static int PAGE_MODE_SCROLL = 4;

    private final static String TAG = "BookPageWidget";

    private int mViewWidth = 0; // 当前View的宽
    private int mViewHeight = 0; // 当前View的高

    private int moveX = 0;
    private int moveY = 0;
    //初始化参数
    private int mBgColor = 0xFFCEC29C;
    private int mPageMode = PAGE_MODE_SIMULATION;

    //是否允许点击
    private boolean canTouch = true;
    //判断是否初始化完成
    private boolean isPrepare = false;
    //唤醒菜单的区域
    private RectF mCenterRect = null;

    //动画类
    private PageAnimation mPageAnim;
    //动画监听类
    private PageAnimation.OnPageChangeListener mPageAnimListener = new PageAnimation.OnPageChangeListener() {
        @Override
        public boolean hasPrev() {
            return PageView.this.hasPrev();
        }

        @Override
        public boolean hasNext() {
            return PageView.this.hasNext();
        }

        @Override
        public void pageCancel() {
            if (mTouchListener != null) {
                mTouchListener.cancel();
            }
            mPageLoader.pageCancel();
        }
    };

    //点击监听
    private TouchListener mTouchListener;

    private OnPageChangeListener mPageChangeListener;

    //内容加载器
    private PageLoader mPageLoader;

    private int mStartSection = -1;

    public PageView(Context context) {
        this(context, null);
    }

    public PageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mViewWidth = w;
        mViewHeight = h;
        //重置图片的大小,由于w,h可能比原始的Bitmap更大，所以如果使用Bitmap.setWidth/Height()是会报错的。
        //所以最终还是创建Bitmap的方式。这种方式比较消耗性能，暂时没有找到更好的方法。

        setPageMode(mPageMode);
        //重置页面加载器的页面
        if (mPageLoader == null) {
            mPageLoader = new PageLoader(this);
            mPageLoader.setOnPageChangeListener(mPageChangeListener);
        }
        mPageLoader.setDisplaySize(w, h);
        if (mPageLoader.getAdapter() != null && mStartSection != -1) {
            mPageLoader.openChapter(mStartSection);
        }
        //初始化完成
        isPrepare = true;
    }

    //设置翻页的模式
    @Deprecated
    public void setPageMode(int pageMode) {
        mPageMode = pageMode;
        //视图未初始化的时候，禁止调用
        if (mViewWidth == 0 || mViewHeight == 0) return;

        switch (pageMode) {
            case PAGE_MODE_SIMULATION:
                mPageAnim = new SimulationPageAnim(mViewWidth, mViewHeight, this, mPageAnimListener);
                break;
            case PAGE_MODE_COVER:
                mPageAnim = new CoverPageAnim(mViewWidth, mViewHeight, this, mPageAnimListener);
                break;
            case PAGE_MODE_SLIDE:
                mPageAnim = new SlidePageAnim(mViewWidth, mViewHeight, this, mPageAnimListener);
                break;
            case PAGE_MODE_NONE:
                mPageAnim = new NonePageAnim(mViewWidth, mViewHeight, this, mPageAnimListener);
                break;
            case PAGE_MODE_SCROLL:
                mPageAnim = new ScrollPageAnim(
                        mViewWidth, mViewHeight, 0,
                        (int) TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP,
                                PageLoader.DEFAULT_MARGIN_HEIGHT,
                                getResources().getDisplayMetrics()),
                        this, mPageAnimListener);
                break;
            default:
                mPageAnim = new SimulationPageAnim(mViewWidth, mViewHeight, this, mPageAnimListener);
        }
    }

    public void setPageAnim(PageAnimation pageAnim) {

    }

    public Bitmap getNextPage() {
        if (mPageAnim == null) return null;
        return mPageAnim.getNextBitmap();
    }

    public Bitmap getBgBitmap() {
        if (mPageAnim == null) return null;
        return mPageAnim.getBgBitmap();
    }


    public boolean autoPrevPage() {
        //滚动暂时不支持自动翻页
        if (mPageAnim instanceof ScrollPageAnim) {
            return false;
        } else {
            startPageAnim(PageAnimation.Direction.PRE);
            return true;
        }
    }

    public boolean autoNextPage() {
        if (mPageAnim instanceof ScrollPageAnim) {
            return false;
        } else {
            startPageAnim(PageAnimation.Direction.NEXT);
            return true;
        }
    }

    private void startPageAnim(PageAnimation.Direction direction) {
        if (mTouchListener == null) return;
        //是否正在执行动画
        abortAnimation();
        if (direction == PageAnimation.Direction.NEXT) {
            int x = mViewWidth;
            int y = mViewHeight;
            //设置点击点
            mPageAnim.setTouchPoint(x, y);
            //初始化动画
            mPageAnim.setStartPoint(x, y);
            //设置方向
            Boolean hasNext = hasNext();

            mPageAnim.setDirection(direction);
            if (!hasNext) {
                return;
            }
        } else {
            int x = 0;
            int y = mViewHeight;
            //初始化动画
            mPageAnim.setStartPoint(x, y);
            //设置点击点
            mPageAnim.setTouchPoint(x, y);
            mPageAnim.setDirection(direction);
            //设置方向方向
            Boolean hashPrev = hasPrev();
            if (!hashPrev) {
                return;
            }
        }
        mPageAnim.startAnim();
        this.postInvalidate();
    }

    public void setBgColor(int color) {
        mBgColor = color;
    }

    public void setTouchable(boolean touchable) {
        canTouch = touchable;
    }

    @Override
    protected void onDraw(Canvas canvas) {

        //绘制背景
        canvas.drawColor(mBgColor);

        //绘制动画
        mPageAnim.draw(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        if (!canTouch && event.getAction() != MotionEvent.ACTION_DOWN) return true;

        int x = (int) event.getX();
        int y = (int) event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                moveX = 0;
                moveY = 0;
                mPageAnim.onTouchEvent(event);
                break;
            case MotionEvent.ACTION_MOVE:
                moveX = x;
                moveY = y;
                mPageAnim.onTouchEvent(event);
                break;
            case MotionEvent.ACTION_UP:

                if (moveX == 0 && moveY == 0) {

                    //设置中间区域范围
                    if (mCenterRect == null) {
                        mCenterRect = new RectF(mViewWidth / 5, mViewHeight / 3,
                                mViewWidth * 4 / 5, mViewHeight * 2 / 3);
                    }

                    //是否点击了中间
                    if (mCenterRect.contains(x, y)) {
                        if (mTouchListener != null) {
                            mTouchListener.center();
                        }
                        return true;
                    }
                }
                mPageAnim.onTouchEvent(event);
                break;
            default:
                mPageAnim.onTouchEvent(event);
                break;
        }
        return true;
    }

    //判断是否下一页存在
    private boolean hasNext() {
        return mPageLoader.next();
    }

    //判断是否存在上一页
    private boolean hasPrev() {
        return mPageLoader.prev();
    }

    @Override
    public void computeScroll() {
        //进行滑动
        mPageAnim.scrollAnim();
        super.computeScroll();
    }

    //如果滑动状态没有停止就取消状态，重新设置Anim的触碰点
    public void abortAnimation() {
        mPageAnim.abortAnim();
    }

    public boolean isPrepare() {
        return isPrepare;
    }

    public boolean isRunning() {
        return mPageAnim.isRunning();
    }

    public void setTouchListener(TouchListener mTouchListener) {
        this.mTouchListener = mTouchListener;
    }

    public void drawNextPage() {
        if (mPageAnim instanceof HorizonPageAnim) {
            ((HorizonPageAnim) mPageAnim).changePage();
        }
        mPageLoader.onDraw(getNextPage(), false);
    }

    /**
     * 刷新当前页(主要是为了ScrollAnimation)
     */
    public void refreshPage() {
        if (mPageAnim instanceof ScrollPageAnim) {
            ((ScrollPageAnim) mPageAnim).refreshBitmap();
        }
        drawCurPage(false);
    }

    //refreshPage和drawCurPage容易造成歧义,后面需要修改

    /**
     * 绘制当前页。
     *
     * @param isUpdate
     */
    public void drawCurPage(boolean isUpdate) {
        mPageLoader.onDraw(getNextPage(), isUpdate);
    }


    public void setAdapter(PageLoaderAdapter adapter) {
        mPageLoader.setAdapter(adapter);
    }

    public void openSection(int section) {
        mStartSection = section;
        if (isPrepare) {
            mPageLoader.openChapter(section);
        }
    }

    public void setOnPageChangeListener(OnPageChangeListener listener) {
        this.mPageChangeListener = listener;
        if (mPageLoader != null) {
            mPageLoader.setOnPageChangeListener(listener);
        }

    }

    public interface TouchListener {
        void center();

        void cancel();
    }

    public void setTextSize(int sizePx) {
        mPageLoader.setTextSize(sizePx);
    }

    public int getTextSize() {
        return mPageLoader.getTextSize();
    }

    public void setTextColor(int color) {
        mPageLoader.setTextColor(color);
    }

    public int getTextColor() {
        return mPageLoader.getTextColor();
    }

    public void setPageBackground(int color) {
        mPageLoader.setPageBackground(color);
    }

    public int getPageBackground() {
        return mPageLoader.getPageBackground();
    }

    public int getPageMode() {
        return mPageMode;
    }

    public void setPageAnimMode(int mode){
        mPageLoader.setPageMode(mode);
    }

}
