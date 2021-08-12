// Copyright 2021, Sun Jinbo, All rights reserved.

package com.rock.ui

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.SystemClock
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.animation.AccelerateInterpolator

import com.rock.core.security.Md5Util
import com.rock.logger.Log

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs

/**
 * Banner控件类
 */
class Banner : SurfaceView, SurfaceHolder.Callback, Runnable, DataLoader.Callback {

    interface Callback {
        fun onCardClick(jumpTo: JumpInfo?)
    }

    private enum class State {
        NOT_INIT, // 控件未进行初始化
        INITIALIZED, // 控件完成成员变量的初始化
        LOADING, // 控件正在加载要显示的数据
        LOADED, // 控件已加载要显示的数据
        WAITING, // 控件正在显示数据（静止等待状态）
        SLIPPING, // 控件正在显示数据（滑动状态）
        TOUCHING, // 控件正在显示数据（拖动状态）
        ERROR // 获取线上数据失败
    }

    companion object {
        private const val MAX_FRAME_TIME:Int = (1000.0 / 60.0).toInt()
        private const val MAX_TOUCH_TIME:Int = 500
        private const val DEFAULT_LOOP_INTERVAL:Int = 5000
    }

    private var loadingBitmap: Bitmap? = null
    private var errorBitmap: Bitmap? = null
    private var mode: PorterDuffXfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN) // 混合模式

    private var state: State = State.NOT_INIT // 控件当前状态
    private var cardList: MutableList<Card> = ArrayList()

    private var dstRect = RectF()
    private var srcRect = Rect()

    private var paint: Paint? = null
    private var offset: Int = 0 // 水平偏移

    private var surfaceHolder: SurfaceHolder? = null
    private var drawThread: Thread? = null
    private var surfaceReady = false
    private var drawingActive = false

    private var touchStartX: Float = 0F
    private var touchLastX: Float = 0F
    private var touchTime: Long = 0L
    private var autoLoopStartTime: Long = 0L

    private var animator: ValueAnimator? = null

    private var threadPool: ExecutorService? = null

    private var loader:DataLoader? = null

    /**
     * 设置banner回调方法，用于检测用户点击了哪个卡片.
     */
    var callback: Callback? = null

    /**
     * banner控件主背景色.
     */
    var areaColor: Int = Color.WHITE
        set(value) {
            field = value
            postInvalidate()
        }

    /**
     * banner控件上卡片的背景色.
     */
    var cardColor: Int = Color.LTGRAY
        set(value) {
            field = value
            postInvalidate()
        }

    /**
     * 当前选择显示的dot背景色.
     */
    var selectedDotColor: Int = Color.BLACK
        set(value) {
            field = value
            postInvalidate()
        }

    /**
     * 未选择显示的dot背景色.
     */
    var unselectedDotColor: Int = Color.LTGRAY
        set(value) {
            field = value
            postInvalidate()
        }

    /**
     * 是否支持自动轮播.
     */
    var autoLoop: Boolean = false

    /**
     * 轮播间隔时间是多少.
     */
    var loopInterval: Int = DEFAULT_LOOP_INTERVAL

    /**
     * banner卡片的圆角半径.
     */
    var roundCorner: Float = 0F
        get() = px2dip(field).toFloat()
        set(value) {
            field = dip2px(value).toFloat()
            postInvalidate()
        }

    /**
     * banner卡片网络加载中显示的占位图.
     */
    var loadingImage: Drawable? = null
        set(value) {
            field = value
            loadingBitmap = field?.let { (field as BitmapDrawable).bitmap }
            postInvalidate()
        }

    /**
     * banner卡片网络加载失败时显示的占位图.
     */
    var errorImage: Drawable? = null
        set(value) {
            field = value
            errorBitmap = field?.let { (field as BitmapDrawable).bitmap }
            postInvalidate()
        }

    /**
     * Banner通用SDK的APP KEY.
     */
    var appKey: String = ""

    /**
     * Banner通用SDK的APP SECRET.
     */
    var appSecret: String = ""

    /**
     * 是否是debug模式.
     */
    var debug: Boolean
        get() = Log.debug
        set(value) {
            Log.debug = value
        }

    /**
     * 是否是测试模式.
     */
    var test: Boolean = false
        set(value) {
            field = value
            loader?.test = value
        }

    /**
     * 构造方法.
     */
    constructor(context: Context) : super(context) {
        init(null, 0)
    }

    /**
     * 构造方法.
     */
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs, 0)
    }

    /**
     * 构造方法.
     */
    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init(attrs, defStyle)
    }

    /**
     * 强制重新加载banner上的线上数据.
     */
    fun refreshData() {
        when (state) {
            State.WAITING, State.SLIPPING, State.TOUCHING, State.LOADED, State.ERROR -> {
                // 销毁/停止一些成员变量
                offset = 0
                cardList.clear()
                cancelAnimation()

                // 开始重新获取在线数据
                state = State.LOADING
                threadPool!!.execute(Runnable {
                    loadOnline()
                })
            }
            State.INITIALIZED -> {
                // 开始重新获取在线数据
                state = State.LOADING
                threadPool!!.execute(Runnable {
                    // 先尝试同步加载本地离线数据
                    loadOffline()
                    // 再异步获取线上最新的数据
                    loadOnline()
                })
            }
            else -> {
                Log.d("ignore")
            }
        }
    }

    /**
     * 强制重新加载banner上的线上数据.
     */
    fun size() : Int {
        return cardList.size
    }

    /**
     * 当该控件加入Window中时调用.
     */
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startAnimation()
    }

    /**
     * 当该控件从Window中移除时调用.
     */
    override fun onDetachedFromWindow() {
        cancelAnimation()
        super.onDetachedFromWindow()
    }

    /*
     * 处理父控件拦截move事件导致滑动问题
     */
    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        parent.requestDisallowInterceptTouchEvent(true);
        return super.dispatchTouchEvent(event)
    }

    /**
     * 处理拖拽事件.
     */
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        Log.d("action = " + event!!.action.toString() + ", x = " + event.x + ", y = " + event.y)

        if (!isRunning()) return false

        if (cardList.size < 1) return false
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                state = State.TOUCHING
                touchTime = SystemClock.elapsedRealtime()
                touchStartX = event.x
                touchLastX = touchStartX
            }
            MotionEvent.ACTION_MOVE -> {
                if (cardList.size > 1) {
                    var offsetX = event.x - touchLastX
                    touchLastX = event.x
                    offset += offsetX.toInt()
                }
            }
            MotionEvent.ACTION_UP -> {
                var selectedCardIndex:Int = if (cardList.size == 1) 0 else -1
                val interval: Long = SystemClock.elapsedRealtime() - touchTime

                if (interval > MAX_TOUCH_TIME) {
                    if (cardList.size > 1) {
                        state = State.SLIPPING

                        cardList.forEach {
                            run {
                                var cardOffset = it.getOffset()
                                var range = width / 2
                                if (cardOffset >= -range && cardOffset < range) {
                                    selectedCardIndex = it.cardIndex
                                    animator!!.setIntValues(offset, offset - cardOffset)
                                    animator!!.duration = 222L
                                    animator!!.start()
                                }
                            }
                        }
                    }
                } else {
                    var distance = abs(touchStartX - event.x)
                    Log.d("distance = $distance")
                    if (distance > 8) {
                        if (cardList.size > 1) {
                            if (touchStartX > event.x) {
                                // 向左滑动
                                cardList.forEach {
                                    run {
                                        var cardOffset = it.getOffset()
                                        var range = width / 2
                                        if (cardOffset >= -range && cardOffset < range) {
                                            animator!!.setIntValues(offset, offset - (width + cardOffset))
                                            animator!!.duration = 222L
                                            animator!!.start()
                                        }
                                    }
                                }
                            } else {
                                // 向右滑动
                                cardList.forEach {
                                    run {
                                        var cardOffset = it.getOffset()
                                        var range = width / 2
                                        if (cardOffset >= -range && cardOffset < range) {
                                            animator!!.setIntValues(offset, offset - (cardOffset - width))
                                            animator!!.duration = 222L
                                            animator!!.start()
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        if (cardList.size > 1) {
                            state = State.SLIPPING

                            cardList.forEach {
                                run {
                                    var cardOffset = it.getOffset()
                                    var range = width / 2
                                    if (cardOffset >= -range && cardOffset < range) {
                                        selectedCardIndex = it.cardIndex
                                        animator!!.setIntValues(offset, offset - cardOffset)
                                        animator!!.duration = 222L
                                        animator!!.start()
                                    }
                                }
                            }
                        }

                        if (selectedCardIndex >= 0 && selectedCardIndex < cardList.size) {
                            callback?.onCardClick(cardList[selectedCardIndex].jumpTo)
                        }
                    }
                }
            }
        }

        return true
    }

    /**
     * surface创建后调用.
     */
    override fun surfaceCreated(holder: SurfaceHolder) {
        surfaceHolder = holder

        if (drawThread != null) {
            drawingActive = false
            try {
                drawThread!!.join()
            } catch (e: InterruptedException) {
            }
        }

        surfaceReady = true
        startDrawThread()
    }

    /**
     * surface的状态发生变化后调用.
     */
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        if (width == 0 || height == 0) {
            return
        }

        // resize your UI
    }

    /**
     * surface销毁后调用.
     */
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // Surface is not used anymore - stop the drawing thread

        // Surface is not used anymore - stop the drawing thread
        stopDrawThread()
        // and release the surface
        holder!!.surface.release()

        surfaceHolder = null
        surfaceReady = false
    }

    /**
     * 渲染线程实体逻辑.
     */
    override fun run() {
        try {
            var frameStartTime: Long
            var frameTime: Long

            /*
             * In order to work reliable on Nexus 7, we place ~500ms delay at the start of drawing thread
             */if (Build.BRAND.equals("google", ignoreCase = true)
                && Build.MANUFACTURER.equals("asus", ignoreCase = true)
                && Build.MODEL.equals("Nexus 7", ignoreCase = true)
            ) {
                try {
                    Thread.sleep(500)
                } catch (ignored: InterruptedException) {
                    Log.e(ignored)
                }
            }

            while (drawingActive) {
                if (surfaceHolder == null) {
                    return
                }
                frameStartTime = System.nanoTime()
                val canvas = surfaceHolder!!.lockCanvas()
                if (canvas != null) {
                    try {
                        synchronized(surfaceHolder!!) {
                            tick()
                            render(canvas)
                        }
                    } catch (e: ConcurrentModificationException) {
                        Log.e(e)
                    } finally {
                        surfaceHolder!!.unlockCanvasAndPost(canvas)
                    }
                }

                // calculate the time required to draw the frame in ms
                frameTime = (System.nanoTime() - frameStartTime) / 1000000
                if (frameTime < MAX_FRAME_TIME) {
                    try {
                        Thread.sleep(MAX_FRAME_TIME - frameTime)
                    } catch (e: InterruptedException) {
                        Log.e(e) // ignore
                    }
                }
            }
        } catch (e: java.lang.Exception) {
            Log.e(e)
        }
    }

    /**
     * 加载线上数据成功后调用.
     */
    override fun onDataLoaded(list: MutableList<CardInfo>) {
        if (list!!.size == 0) { // 没有什么数据，将控件隐藏
            // 清除原数据
            offset = 0
            cardList.clear()

            post { visibility = View.GONE }
            state = State.LOADED
            return
        } else {
            post { visibility = View.VISIBLE }
        }

        // 判断新加载的数据是否和缓存数据一致，只有不一致的情况下才会被更新
        var needUpdate = false
        if (cardList.size == 0) {
            needUpdate = true
        } else if (cardList.size != list!!.size) {
            needUpdate = true
        } else {
            for (i in cardList.indices) {
                if (!cardList[i].compare(list[i])) {
                    needUpdate = true
                    break
                }
            }
        }

        if (!needUpdate) { // 数据没变不需要更新
            return
        }

        threadPool!!.execute(Runnable {
            try {
                // 清除原数据
                offset = 0
                cardList.clear()

                // 将数据转成card对象
                for (data in list!!.iterator()) {
                    cardList.add(Card(data, cardList.size))
                }
            } catch (exception: java.lang.Exception) {
                Log.e(exception)
            } finally {
                state = State.LOADED

                // 如果数据已加载，则启动控件动画
                startAnimation()
            }
        })
    }

    /**
     * 加载线上数据失败后调用.
     */
    override fun onLoadFailed(error:Int, message: String) {
        if (state == State.LOADING) {
            if (cardList.size == 0) {
                post { visibility = View.GONE }
            }
            state = State.ERROR
        }
    }

    /**
     * 控制逻辑.
     */
    private fun tick() {
        if (isRunning() && cardList.size > 1 && width > 0) {
            if (autoLoop && loopInterval > 0 && state == State.WAITING) {
                var interval = SystemClock.elapsedRealtime() - autoLoopStartTime
                if (interval > loopInterval) {
                    autoLoopStartTime = SystemClock.elapsedRealtime()

                    cardList.forEach {
                        run {
                            var cardOffset = it.getOffset()
                            var range = width / 2
                            if (cardOffset >= -range && cardOffset < range) {
                                state = State.SLIPPING
                                post {
                                    animator!!.setIntValues(offset, offset - width)
                                    animator!!.duration = 333L
                                    animator!!.start()
                                }
                            }
                        }
                    }
                }
            }

            var range = (cardList.size - 1) * width
            if (offset < -range || offset > range) {
                offset %= (cardList.size * width)
            }
        }
    }

    /**
     * 用于绘制控件.
     */
    private fun render(canvas: Canvas) {
        canvas.drawColor(areaColor)

        when (state) {
            State.NOT_INIT, State.INITIALIZED, State.LOADING -> {
                // 若设置加载占位图，则优先显示加载占位图；否则显示纯色背景图案。
                if (loadingImage != null) {
                    var id = canvas.saveLayer(
                        0F,
                        0F,
                        canvas.width.toFloat(),
                        canvas.height.toFloat(),
                        null,
                        Canvas.ALL_SAVE_FLAG
                    )
                    srcRect.set(0, 0, loadingBitmap!!.width, loadingBitmap!!.height)
                    dstRect.set(0F, 0F, canvas.width.toFloat(), canvas.height.toFloat())
                    paint!!.color = Color.BLACK
                    canvas.drawRoundRect(dstRect, roundCorner, roundCorner, paint!!)
                    paint!!.xfermode = mode
                    trimRect(srcRect, loadingBitmap!!.width, loadingBitmap!!.height, canvas.width, canvas.height)
                    loadingBitmap?.let { canvas.drawBitmap(it, srcRect, dstRect, paint!!) }
                    paint!!.xfermode = null
                    canvas.restoreToCount(id)
                } else {
                    dstRect.set(0F, 0F, canvas.width.toFloat(), canvas.height.toFloat())
                    paint!!.color = cardColor
                    canvas.drawRoundRect(dstRect, roundCorner, roundCorner, paint!!)
                }
            }
            State.LOADED, State.WAITING, State.TOUCHING, State.SLIPPING -> {
                var id = canvas.saveLayer(
                    0F,
                    0F,
                    canvas.width.toFloat(),
                    canvas.height.toFloat(),
                    null,
                    Canvas.ALL_SAVE_FLAG
                )
                drawCards(canvas)
                drawDots(canvas)
                canvas.restoreToCount(id)
            }
            State.ERROR -> {
                // 若设置加载占位图，则优先显示加载占位图；否则显示纯色背景图案。
                if (errorBitmap != null) {
                    var id = canvas.saveLayer(
                        0F,
                        0F,
                        canvas.width.toFloat(),
                        canvas.height.toFloat(),
                        null,
                        Canvas.ALL_SAVE_FLAG
                    )
                    srcRect.set(0, 0, errorBitmap!!.width, errorBitmap!!.height)
                    dstRect.set(0F, 0F, canvas.width.toFloat(), canvas.height.toFloat())
                    paint!!.color = Color.BLACK
                    canvas.drawRoundRect(dstRect, roundCorner, roundCorner, paint!!)
                    paint!!.xfermode = mode
                    trimRect(srcRect, errorBitmap!!.width, errorBitmap!!.height, canvas.width, canvas.height)
                    errorBitmap?.let { canvas.drawBitmap(it, srcRect, dstRect, paint!!) }
                    paint!!.xfermode = null
                    canvas.restoreToCount(id)
                } else {
                    dstRect.set(0F, 0F, canvas.width.toFloat(), canvas.height.toFloat())
                    paint!!.color = cardColor
                    canvas.drawRoundRect(dstRect, roundCorner, roundCorner, paint!!)
                }
            }
        }
    }

    /**
     * 用户绘制cards.
     */
    private fun drawCards(canvas: Canvas) {
        if (cardList.size > 0) {
            cardList.forEach { it.drawCard(canvas) }
        } else {
            dstRect.set(0F, 0F, canvas.width.toFloat(), canvas.height.toFloat())
            paint!!.color = cardColor
            canvas.drawRoundRect(dstRect, roundCorner, roundCorner, paint!!)
        }
    }

    /**
     * 用户绘制dots.
     */
    private fun drawDots(canvas: Canvas) {
        // 当cardList中数目大于等于2时，才会绘制dots
        if (cardList.size >= 2) {
            var dotSize = Math.max(width, height).toFloat() / 50F
            var startLeft = if (cardList.size % 2 == 0) {
                ((cardList.size / 2 * 3 - 1) * dotSize).toInt()
            } else{
                ((cardList.size / 2 * 3 ) * dotSize).toInt()
            }
            startLeft = width / 2 - startLeft
            var startTop = height - 2 * dotSize

            for (index in cardList.indices) {
                paint!!.color = if (cardList[index].isSelectedCard(canvas)) selectedDotColor else unselectedDotColor

                var left = startLeft + dotSize * 3 * index
                var top = startTop
                var right = left + dotSize
                var bottom = top + dotSize
                dstRect.set(left, top, right, bottom)
                canvas.drawOval(dstRect, paint!!)
            }
        }
    }

    /**
     * 初始化方法.
     */
    private fun init(attrs: AttributeSet?, defStyle: Int) {
        // Load attributes
        val a = context.obtainStyledAttributes(
            attrs, R.styleable.attrs, defStyle, 0
        )

        areaColor = a.getColor(
            R.styleable.attrs_areaColor,
            Color.WHITE
        )

        cardColor = a.getColor(
            R.styleable.attrs_cardColor,
            Color.LTGRAY
        )

        selectedDotColor = a.getColor(
            R.styleable.attrs_selectedDotColor,
            Color.BLACK
        )

        unselectedDotColor = a.getColor(
            R.styleable.attrs_unselectedDotColor,
            Color.LTGRAY
        )

        autoLoop = a.getBoolean(
            R.styleable.attrs_autoLoop,
            false
        )

        loopInterval = a.getInteger(
            R.styleable.attrs_loopInterval,
            5000
        )

        roundCorner = a.getDimension(
            R.styleable.attrs_roundCorner,
            0F
        )

        loadingImage = a.getDrawable(
            R.styleable.attrs_loadingImage
        )

        errorImage = a.getDrawable(
            R.styleable.attrs_errorImage
        )

        appKey = a.getString(
            R.styleable.attrs_appKey
        ).toString()

        appSecret = a.getString(
            R.styleable.attrs_appSecret
        ).toString()

        Log.debug = a.getBoolean(
            R.styleable.attrs_debug,
            false
        )

        test = a.getBoolean(
            R.styleable.attrs_test,
            false
        )

        var clazzName = a.getString(
            R.styleable.attrs_loader
        ).toString()

        a.recycle()

        // 初始化成员变量
        surfaceHolder = holder
        surfaceHolder!!.setFormat(PixelFormat.TRANSLUCENT)
        surfaceHolder!!.addCallback(this)

        loadingBitmap = loadingImage?.let { (loadingImage as BitmapDrawable).bitmap }
        errorBitmap = errorImage?.let { (errorImage as BitmapDrawable).bitmap }

        threadPool = Executors.newFixedThreadPool(5)

        animator = ValueAnimator()
        animator!!.interpolator = AccelerateInterpolator()
        animator!!.addUpdateListener { offset = it.animatedValue as Int }
        animator!!.addListener(object : AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
                state = State.SLIPPING
            }

            override fun onAnimationEnd(animation: Animator) {
                state = State.WAITING
                autoLoopStartTime = SystemClock.elapsedRealtime()
            }

            override fun onAnimationCancel(animation: Animator) {
                state = State.WAITING
                autoLoopStartTime = SystemClock.elapsedRealtime()
            }

            override fun onAnimationRepeat(animation: Animator) {
                // no need to implementation required
            }
        })

        paint = Paint()
        paint!!.isAntiAlias = true

        loader = initLoader(clazzName, appKey, appSecret, test)

        state = State.INITIALIZED // 完成成员变量的初始化工作，切换到INITIALIZED状态
    }

    /**
     * 使用反射创建数据加载器.
     */
    private fun initLoader(
        clazzName: String,
        appKey: String,
        appSecret: String,
        test: Boolean
    ): DataLoader? {
        if (TextUtils.isEmpty(clazzName)) return null

        var clazz = Class.forName(clazzName)

        return (clazz.newInstance() as DataLoader).apply {
            context = getContext()
            this.appKey = appKey
            this.appSecret = appSecret
            this.test = test
        }
    }

    /**
     * 停止渲染线程.
     */
    private fun stopDrawThread() {
        if (drawThread == null) {
            return
        }
        drawingActive = false
        while (true) {
            try {
                drawThread!!.join(5000)
                break
            } catch (e: Exception) {
            }
        }
        drawThread = null
    }

    /**
     * 启动渲染线程.
     */
    private fun startDrawThread() {
        if (surfaceReady && drawThread == null) {
            drawThread = Thread(this, "Draw thread")
            drawingActive = true
            drawThread!!.start()
        }
    }

    /**
     * 加载banner上显示的数据.
     */
    private fun loadData() {
        state = State.LOADING // 开始加载数据，切换到LOADING状态
        threadPool!!.execute(Runnable {
            // 特意等待50毫秒，以便调用者可能会在主线程设置tags属性
            SystemClock.sleep(50)
            loadOffline() // 先尝试同步加载本地离线数据
            loadOnline() // 再异步获取线上最新的数据
        })
    }

    /**
     * 加载旧的缓存数据.
     */
    private fun loadOffline() {
        // 存在离线缓存数据
        var cardDataList = loader?.loadOfflineData()

        // 将数据转成card对象
        if (cardDataList!!.isNotEmpty()) {
            for (data in cardDataList.iterator()) {
                cardList.add(Card(data, cardList.size))
            }

            state = State.LOADED

            // 如果数据已加载，则启动控件动画
            startAnimation()
        }
    }

    /**
     * 加载线上新的缓存数据.
     */
    private fun loadOnline() {
        loader?.loadOnlineData(this)
    }

    /**
     * 启动绘制banner控件的动画.
     */
    private fun startAnimation() {
        if (state == State.LOADED) {
            state = State.WAITING
            autoLoopStartTime = SystemClock.elapsedRealtime()
        }
    }

    /**
     * 停止绘制banner控件的动画.
     */
    private fun cancelAnimation() {
        if (isRunning()) {
            if (animator!!.isRunning) {
                animator!!.cancel()
            }
            state = State.LOADED
        }
    }

    /**
     * 判断当前控件是否在运行状态.
     */
    private fun isRunning():Boolean {
        return when (state) {
            State.WAITING, State.TOUCHING, State.SLIPPING -> true
            else -> false
        }
    }

    /**
     * 将dip转换成px.
     */
    private fun dip2px(dp: Float): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)
            .toInt()
    }

    /**
     * 将px转换成dip.
     */
    private fun px2dip(pxValue: Float): Int {
        val scale: Float = resources.displayMetrics.density
        return (pxValue / scale + 0.5f).toInt()
    }

    /**
     * 根据目标矩形的宽高，裁剪源矩形的大小，
     * 使其在不拉伸变形的情况下填充目标矩形.
     */
    private fun trimRect(rect: Rect, srcWidth: Int, srcHeight: Int, dstWidth: Int, dstHeight: Int) {
        if (srcWidth == 0 || srcHeight == 0 || dstWidth == 0 || dstHeight == 0) {
            rect.set(0, 0, srcWidth, srcHeight)
            return
        }

        var srcFactor = srcWidth.toFloat() / srcHeight.toFloat()
        var dstFactor = dstWidth.toFloat() / dstHeight.toFloat()
        when {
            srcFactor == dstFactor -> {
                // 不需要裁剪
                rect.set(0, 0, srcWidth, srcHeight)
            }
            srcFactor < dstFactor -> {
                // 裁剪源矩形的上下部分
                var tempHeight = srcWidth.toFloat() * dstHeight.toFloat() / dstWidth.toFloat()
                var trimHeight = ((srcHeight - tempHeight) / 2).toInt()
                rect.set(0, trimHeight, srcWidth, srcHeight - trimHeight)
            }
            srcFactor > dstFactor -> {
                // 裁剪源矩形的左右部分
                var tempWidth = srcHeight.toFloat() * dstWidth.toFloat() / dstHeight.toFloat()
                var trimWidth = ((srcWidth - tempWidth) / 2).toInt()
                rect.set(trimWidth, 0, srcWidth - trimWidth, srcHeight)
            }
        }
    }

    /**
     * Card类
     * 标记为inner的嵌套类能够访问其外部类的成员
     */
    inner class Card {
        private var imageUrl: String? = null // 卡片的背景图片URL


        /**
         * 卡片的背景图片.
         */
        var cardBitmap: Bitmap? = null

        /**
         * 页面内跳转协议.
         */
        var jumpTo: JumpInfo? = null

        /**
         * 卡片在列表中的索引.
         */
        var cardIndex: Int = 0

        /**
         * 构造方法.
         */
        constructor(data: CardInfo?, index: Int) {
            jumpTo = data!!.jumpTo
            cardIndex = index
            imageUrl = data.urlImage!!
            loadBitmap()
        }

        /**
         * 判断当前card是否是被选中的.
         */
        fun isSelectedCard(canvas: Canvas): Boolean {
            var cardOffset = getOffset()
            var range = canvas.width / 2
            if (cardOffset >= -range && cardOffset < range) {
                return true
            }
            return false
        }

        /**
         * 比较数据是否一致.
         */
        fun compare(data: CardInfo?): Boolean {
            // 如果data不存在，返回false
            if (data == null) return false

            // 如果banner图片不一致，返回false
            if (!TextUtils.equals(data.urlImage!!, imageUrl)) return false

            // 比较JumpTo对象是否一致
            return jumpTo?.compareTo(data.jumpTo!!) == 0
        }

        /**
         * 计算当前card的偏移值.
         */
        fun getOffset(): Int {
            // 如果卡片或者只有1个卡片，偏移值为0
            if (cardList.size < 2) return 0

            // 计算当前card的偏移值
            var cardOffset = offset + width * cardIndex;

            // 超出偏移范围，需要修正card的偏移值
            var range = (cardList.size - 1) * width
            when {
                cardOffset < -range ->  cardOffset += (cardList.size * width)
                cardOffset > range -> cardOffset -= (cardList.size * width)
            }

            return cardOffset
        }

        /**
         * 在canvas上绘制卡片.
         */
        fun drawCard(canvas: Canvas) {
            try {
                var cardOffset = getOffset()
                dstRect.set(
                    0F + cardOffset, 0F,
                    canvas.width.toFloat() + cardOffset, canvas.height.toFloat()
                )

                if (cardBitmap != null) {
                    // card位图不为空，尝试渲染图片位图
                    paint!!.color = Color.BLACK
                    canvas.drawRoundRect(dstRect, roundCorner, roundCorner, paint!!)
                    paint!!.xfermode = mode
                    trimRect(srcRect, cardBitmap!!.width, cardBitmap!!.height, canvas.width, canvas.height)
                    canvas.drawBitmap(cardBitmap!!, srcRect, dstRect, paint!!)
                    paint!!.xfermode = null
                } else{
                    // card位图为空，只渲染颜色
                    paint!!.color = cardColor
                    canvas.drawRoundRect(dstRect, roundCorner, roundCorner, paint!!)
                }
            } catch (exception: java.lang.Exception) {
                Log.e(exception)
            }
        }

        /**
         * 根据线上图片的地址获取对应的bitmap位图对象.
         * 先检测本地，如果找到本地缓存图片，则加载该图片文件进行显示；
         * 如果本地不存在，则先显示加载占位图，等下载成功后再显示。
         */
        private fun loadBitmap() {
            if (imageUrl == null) {
                // 没有线上图片url，数据错误，直接显示错误占位图
                cardBitmap = errorBitmap
                return
            } else {
                cardBitmap = loadingBitmap
            }

            var bmpFile = File(context.cacheDir, Md5Util.getMd5(imageUrl!!))
            if (bmpFile.exists()) {
                threadPool!!.execute(Runnable {
                    try {
                        // 存在本地文件，直接加载显示
                        val op = BitmapFactory.Options()
                        op.inPreferredConfig = Bitmap.Config.ARGB_8888
                        cardBitmap = BitmapFactory.decodeFile(bmpFile.absolutePath, op)
                    } catch (exception: java.lang.Exception) {
                        Log.e(exception)
                        // 加载本地图片失败显示错误占位图
                        cardBitmap = errorBitmap
                    }
                })
            } else{
                // 未发现有本地文件，先显示加载占位图
                cardBitmap = loadingBitmap

                // 开始下载图片文件到本地
                downloadFile(imageUrl!!, bmpFile)
            }
        }

        /**
         * 下载线上图片文件到本地.
         */
        private fun downloadFile(imageUrl: String, file: File) {
            Log.d("Card $cardIndex begin to download $imageUrl to ${file.absolutePath}")
            threadPool!!.execute(Runnable {
                try {
                    var url = URL(imageUrl)
                    var httpConnection = url.openConnection()
                    httpConnection.connectTimeout = 5000
                    httpConnection.connect()
                    var inputStream = httpConnection.getInputStream()
                    val outputStream = FileOutputStream(file)
                    val buffer = ByteArray(1024 * 4)
                    val byteStream = ByteArrayOutputStream()
                    while (true) {
                        var length = inputStream.read(buffer)
                        if (length > 0) {
                            byteStream.write(buffer, 0, length)
                            outputStream.write(buffer, 0, length)
                        } else {
                            break
                        }
                    }

                    // 完成位图下载，将加载占位图替换成card图片
                    var byteArray = byteStream.toByteArray()
                    cardBitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                    Log.d("Card $cardIndex is ready!")

                    outputStream.flush()
                    outputStream.close()
                    byteStream.close()
                    inputStream.close()
                } catch (e: java.lang.Exception) {
                    Log.e(e)
                    // 下载图片失败显示错误占位图
                    cardBitmap = errorBitmap
                }
            })
        }
    }
}
