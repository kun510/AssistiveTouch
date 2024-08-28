package com.android.kun510.assisttouch

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Service
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.PopupWindow
import java.util.Timer
import java.util.TimerTask


class AssistiveTouchService : Service() {

    private var isMoving = false

    private var rawX: Float = 0f
    private var rawY: Float = 0f

    private var mScreenWidth: Int = 0
    private var mScreenHeight: Int = 0
    private var mStatusBarHeight: Int = 0

    private var lastAssistiveTouchViewX: Int = 0
    private var lastAssistiveTouchViewY: Int = 0

    private lateinit var mAssistiveTouchView: View
    private lateinit var mInflateAssistiveTouchView: View
    private lateinit var mWindowManager: WindowManager
    private lateinit var mParams: WindowManager.LayoutParams
    private var mPopupWindow: PopupWindow? = null
    private lateinit var mBulider: AlertDialog.Builder
    private lateinit var mAlertDialog: AlertDialog
    private var mScreenShotView: View? = null

    private lateinit var mTimer: Timer
    private lateinit var mHandler: Handler

    private lateinit var mInflater: LayoutInflater

    override fun onCreate() {
        super.onCreate()
        init()
        calculateForMyPhone()
        createAssistiveTouchView()
        inflateViewListener()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun init() {
        mTimer = Timer()
        mHandler = MyHandler()
        mBulider = AlertDialog.Builder(this@AssistiveTouchService)
        mAlertDialog = mBulider.create()
        mParams = WindowManager.LayoutParams()
        mWindowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        mInflater = LayoutInflater.from(this)
        mAssistiveTouchView = mInflater.inflate(R.layout.assistive_touch_layout, null)
        mInflateAssistiveTouchView = mInflater.inflate(R.layout.assistive_touch_inflate_layout, null)
    }

    private fun calculateForMyPhone() {
        val displayMetrics = SystemsUtils.getScreenSize(this)
        mScreenWidth = displayMetrics.widthPixels
        mScreenHeight = displayMetrics.heightPixels
        mStatusBarHeight = SystemsUtils.getStatusBarHeight(this)

        mInflateAssistiveTouchView.layoutParams = WindowManager.LayoutParams(
            (mScreenWidth * 0.75).toInt(),
            (mScreenWidth * 0.75).toInt()
        )
    }

    fun createAssistiveTouchView() {
        mParams.type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }

        mParams.width = WindowManager.LayoutParams.WRAP_CONTENT
        mParams.height = WindowManager.LayoutParams.WRAP_CONTENT
        mParams.x = mScreenWidth
        mParams.y = 520
        mParams.gravity = Gravity.TOP or Gravity.LEFT
        mParams.format = PixelFormat.RGBA_8888
        mParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        mWindowManager.addView(mAssistiveTouchView, mParams)
        mAssistiveTouchView.setOnTouchListener { _, event ->
            rawX = event.rawX
            rawY = event.rawY
            when (event.action) {
                MotionEvent.ACTION_DOWN -> isMoving = false
                MotionEvent.ACTION_UP -> setAssitiveTouchViewAlign()
                MotionEvent.ACTION_MOVE -> {
                    isMoving = true
                    mParams.x = (rawX - mAssistiveTouchView.measuredWidth / 2).toInt()
                    mParams.y = (rawY - mAssistiveTouchView.measuredHeight / 2 - mStatusBarHeight).toInt()
                    mWindowManager.updateViewLayout(mAssistiveTouchView, mParams)
                }
            }
            isMoving
        }
        mAssistiveTouchView.setOnClickListener {
            mAssistiveTouchView.alpha = 0f
            lastAssistiveTouchViewX = mParams.x
            lastAssistiveTouchViewY = mParams.y
            myAssitiveTouchAnimator(
                mParams.x,
                mScreenWidth / 2 - mAssistiveTouchView.measuredWidth / 2,
                mParams.y,
                mScreenHeight / 2 - mAssistiveTouchView.measuredHeight / 2,
                false
            ).start()

            mPopupWindow = PopupWindow(
                mInflateAssistiveTouchView,
                (mScreenWidth * 0.75).toInt(),
                (mScreenWidth * 0.75).toInt()
            )
            mPopupWindow?.setOnDismissListener {
                myAssitiveTouchAnimator(
                    mParams.x,
                    lastAssistiveTouchViewX,
                    mParams.y,
                    lastAssistiveTouchViewY,
                    true
                ).start()
                mAssistiveTouchView.alpha = 1f
            }
            mPopupWindow?.isFocusable = true
            mPopupWindow?.isTouchable = true
            mPopupWindow?.setBackgroundDrawable(BitmapDrawable())
            mPopupWindow?.showAtLocation(mAssistiveTouchView, Gravity.CENTER, 0, 0)
        }
    }

    private fun inflateViewListener() {
        val shutdown = mInflateAssistiveTouchView.findViewById<ImageView>(R.id.shutdown)
        val star = mInflateAssistiveTouchView.findViewById<ImageView>(R.id.star)
        val screenshot = mInflateAssistiveTouchView.findViewById<ImageView>(R.id.screenshot)
        val home = mInflateAssistiveTouchView.findViewById<ImageView>(R.id.home)

        shutdown.setOnClickListener {
            SystemsUtils.shutDown(this@AssistiveTouchService)
            mTimer.schedule(object : TimerTask() {
                override fun run() {
                    mHandler.sendEmptyMessage(0)
                }
            }, 626)
        }

        home.setOnClickListener {
            SystemsUtils.goHome(this@AssistiveTouchService)
            mTimer.schedule(object : TimerTask() {
                override fun run() {
                    mHandler.sendEmptyMessage(0)
                }
            }, 626)
        }

        screenshot.setOnClickListener {
            mHandler.sendEmptyMessage(0)
            mTimer.schedule(object : TimerTask() {
                override fun run() {
                    val filename = SystemsUtils.takeScreenShot()
                    val msg = mHandler.obtainMessage()
                    msg.what = 1
                    msg.obj = filename
                    mHandler.sendMessage(msg)
                }
            }, 626)
        }
    }

    private fun myAssitiveTouchAnimator(
        fromx: Int,
        tox: Int,
        fromy: Int,
        toy: Int,
        flag: Boolean
    ): ValueAnimator {
        val p1 = PropertyValuesHolder.ofInt("X", fromx, tox)
        val p2 = PropertyValuesHolder.ofInt("Y", fromy, toy)
        val v1 = ValueAnimator.ofPropertyValuesHolder(p1, p2)
        v1.duration = 100L
        v1.interpolator = DecelerateInterpolator()
        v1.addUpdateListener { animation ->
            val x = animation.getAnimatedValue("X") as Int
            val y = animation.getAnimatedValue("Y") as Int
            mParams.x = x
            mParams.y = y
            mWindowManager.updateViewLayout(mAssistiveTouchView, mParams)
        }
        v1.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                if (flag) mAssistiveTouchView.alpha = 0.85f
            }
        })
        return v1
    }

    private fun setAssitiveTouchViewAlign() {
        val mAssistiveTouchViewWidth = mAssistiveTouchView.measuredWidth
        val mAssistiveTouchViewHeight = mAssistiveTouchView.measuredHeight
        val top = mParams.y + mAssistiveTouchViewWidth / 2
        val left = mParams.x + mAssistiveTouchViewHeight / 2
        val right = mScreenWidth - mParams.x - mAssistiveTouchViewWidth / 2
        val bottom = mScreenHeight - mParams.y - mAssistiveTouchViewHeight / 2
        val lor = minOf(left, right)
        val tob = minOf(top, bottom)
        val min = minOf(lor, tob)
        lastAssistiveTouchViewX = mParams.x
        lastAssistiveTouchViewY = mParams.y
        when (min) {
            top -> mParams.y = 0
            left -> mParams.x = 0
            right -> mParams.x = mScreenWidth - mAssistiveTouchViewWidth
            bottom -> mParams.y = mScreenHeight - mAssistiveTouchViewHeight
        }
        myAssitiveTouchAnimator(lastAssistiveTouchViewX, mParams.x, lastAssistiveTouchViewY, mParams.y, false).start()
    }

    @SuppressLint("RtlHardcoded")
    private fun showScreenshot(name: String) {
        val path = "/sdcard/Pictures/$name.png"
        val bitmap = BitmapFactory.decodeFile(path)

        mScreenShotView = mInflater.inflate(R.layout.screen_shot_show, null)
        val imageView = mScreenShotView!!.findViewById<ImageView>(R.id.screenshot)
        imageView.setImageBitmap(bitmap)

        val mScreenshotParams = WindowManager.LayoutParams()
        mScreenshotParams.width = WindowManager.LayoutParams.MATCH_PARENT
        mScreenshotParams.height = WindowManager.LayoutParams.MATCH_PARENT
        mScreenshotParams.x = mScreenWidth
        mScreenshotParams.y = 0
        mScreenshotParams.gravity = Gravity.TOP or Gravity.LEFT
        mScreenshotParams.format = PixelFormat.RGBA_8888
        mScreenshotParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        mWindowManager.addView(mScreenShotView, mScreenshotParams)

        mScreenShotView!!.setOnTouchListener { _, _ ->
            mWindowManager.removeView(mScreenShotView)
            true
        }
    }

    inner class MyHandler : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                0 -> if (mPopupWindow?.isShowing == true) mPopupWindow?.dismiss()
                1 -> {
                    val name = msg.obj as String
                    showScreenshot(name)
                }
            }
            super.handleMessage(msg)
        }
    }
}
