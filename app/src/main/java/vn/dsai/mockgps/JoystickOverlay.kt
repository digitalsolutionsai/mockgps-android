package vn.dsai.mockgps

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt

/** Floating joystick drawn over other apps; drag the top bar to move it. */
class JoystickOverlay(
    private val ctx: Context,
    private val prefs: Prefs,
    private val onMove: (Double, Double) -> Unit,
    private val onClose: () -> Unit,
) {
    private val wm = ctx.getSystemService(WindowManager::class.java)
    private val density = ctx.resources.displayMetrics.density
    private var root: View? = null

    private val speeds = listOf(
        5f to R.string.route_travel_foot,
        10f to R.string.joystick_run,
        18f to R.string.route_travel_bike,
        40f to R.string.route_travel_motorbike,
        60f to R.string.route_travel_car,
    )

    private fun dp(v: Int) = (v * density).roundToInt()

    private fun pill(color: Int) = GradientDrawable().apply {
        cornerRadius = dp(14).toFloat()
        setColor(color)
    }

    private fun speedLabel(): String {
        val kmh = prefs.joystickKmh
        val name = ctx.getString(speeds.firstOrNull { it.first == kmh }?.second ?: R.string.joystick_speed)
        return ctx.getString(R.string.joystick_speed_label, name, kmh.roundToInt())
    }

    @SuppressLint("ClickableViewAccessibility")
    fun show() {
        if (root != null) return
        val metrics = ctx.resources.displayMetrics
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = prefs.joystickX.takeIf { it >= 0 } ?: dp(16)
            y = prefs.joystickY.takeIf { it >= 0 } ?: (metrics.heightPixels * 0.32).toInt()
        }

        fun label(text: String, bg: Int? = null) = TextView(ctx).apply {
            this.text = text
            setTextColor(Color.WHITE)
            textSize = 12f
            setPadding(dp(8), dp(4), dp(8), dp(4))
            bg?.let { background = pill(it) }
        }

        val bar = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = pill(0xDD202124.toInt())
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }
        val speed = label(speedLabel(), 0x40FFFFFF).apply {
            setOnClickListener {
                val i = speeds.indexOfFirst { it.first == prefs.joystickKmh }
                prefs.joystickKmh = speeds[(i + 1) % speeds.size].first
                text = speedLabel()
            }
        }
        bar.addView(label("⠿"))
        bar.addView(speed)
        bar.addView(label("✕").apply { setOnClickListener { onClose() } })

        val stick = JoystickView(ctx, onMove).apply {
            layoutParams = LinearLayout.LayoutParams(dp(150), dp(150)).apply { topMargin = dp(6) }
        }
        val column = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            addView(bar)
            addView(stick)
        }

        bar.setOnTouchListener(object : View.OnTouchListener {
            var startX = 0f
            var startY = 0f
            var origX = 0
            var origY = 0

            override fun onTouch(v: View, e: MotionEvent): Boolean {
                when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = e.rawX; startY = e.rawY; origX = params.x; origY = params.y
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = origX + (e.rawX - startX).toInt()
                        params.y = origY + (e.rawY - startY).toInt()
                        wm.updateViewLayout(column, params)
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        prefs.joystickX = params.x
                        prefs.joystickY = params.y
                    }
                }
                return true
            }
        })

        wm.addView(column, params)
        root = column
    }

    fun hide() {
        root?.let { runCatching { wm.removeView(it) } }
        root = null
    }
}

@SuppressLint("ViewConstructor")
private class JoystickView(ctx: Context, private val onMove: (Double, Double) -> Unit) : View(ctx) {
    private val density = ctx.resources.displayMetrics.density
    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x88202124.toInt() }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xCCFFFFFF.toInt(); style = Paint.Style.STROKE; strokeWidth = 2 * density
    }
    private val knobPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF1A73E8.toInt() }
    private val northPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFEA4335.toInt() }
    private val northPath = Path()
    private var kx = 0f
    private var ky = 0f

    private val radius get() = min(width, height) / 2f - 2 * density
    private val travel get() = radius * 0.62f

    override fun onDraw(c: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val r = radius
        c.drawCircle(cx, cy, r, basePaint)
        c.drawCircle(cx, cy, r, ringPaint)
        // Small red triangle marks north, which is always "up" on the stick.
        val t = 7 * density
        northPath.reset()
        northPath.moveTo(cx, cy - r + 3 * density)
        northPath.lineTo(cx - t, cy - r + 3 * density + t * 1.4f)
        northPath.lineTo(cx + t, cy - r + 3 * density + t * 1.4f)
        northPath.close()
        c.drawPath(northPath, northPaint)
        c.drawCircle(cx + kx, cy + ky, r * 0.36f, knobPaint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                var dx = e.x - width / 2f
                var dy = e.y - height / 2f
                val d = hypot(dx, dy)
                val max = travel
                if (d > max) { dx = dx / d * max; dy = dy / d * max }
                kx = dx; ky = dy
                invalidate()
                onMove((dx / max).toDouble(), (-dy / max).toDouble())
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                kx = 0f; ky = 0f
                invalidate()
                onMove(0.0, 0.0)
            }
        }
        return true
    }
}
