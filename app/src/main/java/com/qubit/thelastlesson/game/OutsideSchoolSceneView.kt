package com.qubit.thelastlesson.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Path
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.os.SystemClock
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.toColorInt
import kotlin.math.abs
import kotlin.random.Random

class OutsideSchoolSceneView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {
    companion object {
        private const val TAG = "OutsideSchoolScene"
        private const val PLAYER_SPEED = 0.42f
        private const val RAIN_LAYERS = 3
    }

    interface SceneListener {
        fun onDoorReached()
        fun onStormLineChanged(line: String)
    }

    var sceneListener: SceneListener? = null

    init {
        isClickable = true
        isFocusable = true
    }

    var detectiveProgress: Float = 0.08f
        set(value) {
            field = value.coerceIn(0f, 1f)
            contentDescription = "Detective progress ${(field * 100).toInt()} percent"
            invalidate()
            maybeNotifyDoorReached()
        }

    private var detectiveTargetProgress: Float = detectiveProgress

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cloudPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val moonGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fogPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#A8C6FF".toColorInt()
        alpha = 125
        strokeWidth = 3f
        strokeCap = Paint.Cap.ROUND
    }
    private val schoolBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#1B202A".toColorInt()
    }
    private val schoolTrimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#2A3240".toColorInt()
    }
    private val schoolShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#0B0E14".toColorInt()
    }
    private val roofPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#090C12".toColorInt()
    }
    private val windowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#E7C677".toColorInt()
        alpha = 92
    }
    private val windowFramePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#11161E".toColorInt()
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val entrancePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#4A2B24".toColorInt()
    }
    private val entranceTrimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#7B4B39".toColorInt()
    }
    private val lampGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#F2D8A3".toColorInt()
        alpha = 75
    }
    private val groundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#090E13".toColorInt()
    }
    private val yardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#0E1318".toColorInt()
    }
    private val pathPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#232833".toColorInt()
    }
    private val puddlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#3D5068".toColorInt()
        alpha = 82
    }
    private val fencePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#161C22".toColorInt()
        strokeWidth = 7f
    }
    private val shrubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#11171B".toColorInt()
    }
    private val detectivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#D4D6DB".toColorInt()
    }
    private val detectiveCoatPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#786650".toColorInt()
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#EAE7DE".toColorInt()
        textSize = 40f
    }
    private val flashPaint = Paint()
    private val vignettePaint = Paint()

    private val random = Random(System.currentTimeMillis())
    private var rainOffset = 0f
    private var lastFrameTime = SystemClock.elapsedRealtime()
    private var flashAlpha = 0f
    private var nextFlashAt = SystemClock.elapsedRealtime() + 2500L
    private var stormLineIndex = -1
    private var doorNotified = false
    private val stormLines = listOf(
        "Rain batters the school roof.",
        "Thunder rolls somewhere behind the gym.",
        "A pale flash reveals the front entrance.",
        "The storm muffles every other sound.",
    )

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        lastFrameTime = SystemClock.elapsedRealtime()
        postInvalidateOnAnimation()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val now = SystemClock.elapsedRealtime()
        val delta = (now - lastFrameTime).coerceAtLeast(16L)
        lastFrameTime = now

        rainOffset = (rainOffset + delta * 0.55f) % 90f
        updateMovement(delta)
        updateStormState(now, delta)

        drawSky(canvas)
        drawBackgroundShapes(canvas)
        drawSchool(canvas)
        drawGround(canvas)
        drawFog(canvas)
        drawRain(canvas)
        drawDetective(canvas)
        drawObjectiveHint(canvas)
        drawLightningFlash(canvas)
        drawVignette(canvas)

        postInvalidateOnAnimation()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            performClick()
            if (event.y >= playableGroundTop()) {
                detectiveTargetProgress = progressForTap(event.x)
                Log.d(TAG, "Tap move target=${"%.2f".format(detectiveTargetProgress)}")
                invalidate()
            }
            return true
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun drawSky(canvas: Canvas) {
        backgroundPaint.shader = LinearGradient(
            0f,
            0f,
            0f,
            height.toFloat(),
            intArrayOf("#04060A".toColorInt(), "#0D1320".toColorInt(), "#1B2432".toColorInt()),
            null,
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        moonGlowPaint.shader = LinearGradient(
            width * 0.7f,
            0f,
            width.toFloat(),
            height * 0.35f,
            intArrayOf(Color.argb(55, 191, 212, 245), Color.argb(0, 191, 212, 245)),
            null,
            Shader.TileMode.CLAMP,
        )
        canvas.drawOval(
            width * 0.66f,
            height * 0.04f,
            width * 0.96f,
            height * 0.34f,
            moonGlowPaint,
        )
    }

    private fun drawBackgroundShapes(canvas: Canvas) {
        cloudPaint.color = "#121825".toColorInt()
        cloudPaint.alpha = 200
        canvas.drawOval(width * 0.02f, height * 0.08f, width * 0.42f, height * 0.25f, cloudPaint)
        canvas.drawOval(width * 0.32f, height * 0.05f, width * 0.84f, height * 0.21f, cloudPaint)
        canvas.drawOval(width * 0.55f, height * 0.1f, width * 1.05f, height * 0.26f, cloudPaint)

        val treeLine = Path().apply {
            moveTo(0f, height * 0.58f)
            lineTo(width * 0.07f, height * 0.43f)
            lineTo(width * 0.12f, height * 0.56f)
            lineTo(width * 0.18f, height * 0.38f)
            lineTo(width * 0.22f, height * 0.57f)
            lineTo(width * 0.77f, height * 0.57f)
            lineTo(width * 0.84f, height * 0.4f)
            lineTo(width * 0.89f, height * 0.58f)
            lineTo(width.toFloat(), height * 0.45f)
            lineTo(width.toFloat(), height * 0.69f)
            lineTo(0f, height * 0.69f)
            close()
        }
        canvas.drawPath(treeLine, schoolShadowPaint)
    }

    private fun drawSchool(canvas: Canvas) {
        val schoolTop = height * 0.18f
        val schoolBottom = height * 0.68f
        val leftWing = RectF(width * 0.05f, schoolTop + height * 0.12f, width * 0.31f, schoolBottom)
        val center = RectF(width * 0.24f, schoolTop + height * 0.03f, width * 0.76f, schoolBottom)
        val rightWing = RectF(width * 0.69f, schoolTop + height * 0.12f, width * 0.95f, schoolBottom)

        canvas.drawRect(leftWing, schoolShadowPaint)
        canvas.drawRect(center, schoolShadowPaint)
        canvas.drawRect(rightWing, schoolShadowPaint)

        canvas.drawRect(leftWing.left + 12f, leftWing.top, leftWing.right, leftWing.bottom, schoolBodyPaint)
        canvas.drawRect(center.left, center.top, center.right, center.bottom, schoolBodyPaint)
        canvas.drawRect(rightWing.left, rightWing.top, rightWing.right - 12f, rightWing.bottom, schoolBodyPaint)

        val leftRoof = Path().apply {
            moveTo(leftWing.left, leftWing.top + 18f)
            lineTo(leftWing.centerX(), leftWing.top - height * 0.05f)
            lineTo(leftWing.right, leftWing.top + 18f)
            close()
        }
        val centerRoof = Path().apply {
            moveTo(center.left - width * 0.02f, center.top + 20f)
            lineTo(width * 0.5f, schoolTop - height * 0.09f)
            lineTo(center.right + width * 0.02f, center.top + 20f)
            close()
        }
        val rightRoof = Path().apply {
            moveTo(rightWing.left, rightWing.top + 18f)
            lineTo(rightWing.centerX(), rightWing.top - height * 0.05f)
            lineTo(rightWing.right, rightWing.top + 18f)
            close()
        }
        canvas.drawPath(leftRoof, roofPaint)
        canvas.drawPath(centerRoof, roofPaint)
        canvas.drawPath(rightRoof, roofPaint)

        canvas.drawRect(width * 0.24f, center.top + 20f, width * 0.76f, center.top + 34f, schoolTrimPaint)
        canvas.drawRect(width * 0.08f, leftWing.top + 18f, width * 0.31f, leftWing.top + 28f, schoolTrimPaint)
        canvas.drawRect(width * 0.69f, rightWing.top + 18f, width * 0.92f, rightWing.top + 28f, schoolTrimPaint)

        drawWindows(canvas, leftWing.left + width * 0.05f, leftWing.top + height * 0.08f, 2, 2, width * 0.07f)
        drawWindows(canvas, center.left + width * 0.07f, center.top + height * 0.1f, 3, 2, width * 0.075f)
        drawWindows(canvas, rightWing.left + width * 0.03f, rightWing.top + height * 0.08f, 2, 2, width * 0.07f)
        drawWindows(canvas, width * 0.455f, center.top + height * 0.02f, 1, 1, width * 0.09f)

        val entrance = RectF(width * 0.415f, height * 0.44f, width * 0.585f, schoolBottom)
        val entranceArch = Path().apply {
            moveTo(entrance.left, entrance.top + 30f)
            quadTo(entrance.centerX(), entrance.top - 34f, entrance.right, entrance.top + 30f)
            lineTo(entrance.right, entrance.bottom)
            lineTo(entrance.left, entrance.bottom)
            close()
        }
        canvas.drawPath(entranceArch, entranceTrimPaint)
        canvas.drawRect(
            entrance.left + width * 0.018f,
            entrance.top + 30f,
            entrance.right - width * 0.018f,
            entrance.bottom,
            entrancePaint,
        )
        canvas.drawLine(entrance.centerX(), entrance.top + 34f, entrance.centerX(), entrance.bottom, entranceTrimPaint.apply {
            strokeWidth = 6f
        })
        canvas.drawCircle(entrance.centerX() - width * 0.03f, entrance.centerY() + height * 0.02f, 5f, windowPaint)
        canvas.drawCircle(entrance.centerX() + width * 0.03f, entrance.centerY() + height * 0.02f, 5f, windowPaint)
        canvas.drawCircle(entrance.left - 14f, entrance.top + 18f, 18f, lampGlowPaint)
        canvas.drawCircle(entrance.right + 14f, entrance.top + 18f, 18f, lampGlowPaint)
    }

    private fun drawGround(canvas: Canvas) {
        val groundTop = height * 0.68f
        canvas.drawRect(0f, groundTop, width.toFloat(), height.toFloat(), groundPaint)
        canvas.drawRect(0f, groundTop, width.toFloat(), height * 0.8f, yardPaint)

        val path = Path().apply {
            moveTo(width * 0.435f, groundTop)
            lineTo(width * 0.565f, groundTop)
            lineTo(width * 0.68f, height.toFloat())
            lineTo(width * 0.32f, height.toFloat())
            close()
        }
        canvas.drawPath(path, pathPaint)

        canvas.drawOval(width * 0.13f, height * 0.8f, width * 0.3f, height * 0.86f, puddlePaint)
        canvas.drawOval(width * 0.68f, height * 0.78f, width * 0.9f, height * 0.85f, puddlePaint)
        canvas.drawOval(width * 0.43f, height * 0.88f, width * 0.58f, height * 0.94f, puddlePaint)

        var fenceX = width * 0.04f
        while (fenceX < width * 0.96f) {
            canvas.drawLine(fenceX, groundTop - 8f, fenceX, groundTop + 50f, fencePaint)
            fenceX += width * 0.08f
        }
        canvas.drawLine(width * 0.02f, groundTop + 18f, width * 0.98f, groundTop + 18f, fencePaint)

        canvas.drawOval(width * 0.04f, height * 0.69f, width * 0.18f, height * 0.78f, shrubPaint)
        canvas.drawOval(width * 0.82f, height * 0.7f, width * 0.96f, height * 0.8f, shrubPaint)
    }

    private fun drawFog(canvas: Canvas) {
        fogPaint.shader = LinearGradient(
            0f,
            height * 0.58f,
            0f,
            height.toFloat(),
            intArrayOf(
                Color.argb(0, 214, 225, 237),
                Color.argb(24, 214, 225, 237),
                Color.argb(52, 214, 225, 237),
            ),
            null,
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, height * 0.58f, width.toFloat(), height.toFloat(), fogPaint)
        canvas.drawOval(width * -0.1f, height * 0.69f, width * 0.45f, height * 0.92f, fogPaint)
        canvas.drawOval(width * 0.4f, height * 0.67f, width * 1.02f, height * 0.95f, fogPaint)
    }

    private fun drawRain(canvas: Canvas) {
        for (layer in 0 until RAIN_LAYERS) {
            val density = width / (14f + layer * 3f)
            rainPaint.alpha = 70 + (layer * 35)
            rainPaint.strokeWidth = 2.5f + layer
            var x = -density
            while (x < width + density) {
                var y = -height * 0.25f
                while (y < height.toFloat()) {
                    val drift = (layer + 1) * 12f
                    val startX = x + (y / (6f - layer)) + (rainOffset * (0.2f + layer * 0.12f))
                    val startY = y + rainOffset * (0.9f + layer * 0.25f)
                    val endX = startX - (16f + drift)
                    val endY = startY + (36f + layer * 18f)
                    canvas.drawLine(startX, startY, endX, endY, rainPaint)
                    y += 130f - (layer * 16f)
                }
                x += density
            }
        }
    }

    private fun drawDetective(canvas: Canvas) {
        val groundTop = height * 0.68f
        val currentX = detectiveXForProgress(detectiveProgress)
        val feetY = groundTop + height * 0.14f
        canvas.drawOval(currentX - 28f, feetY + 22f, currentX + 28f, feetY + 36f, puddlePaint)
        canvas.drawCircle(currentX, feetY - 92f, 20f, detectivePaint)
        canvas.drawRect(currentX - 22f, feetY - 82f, currentX + 22f, feetY - 16f, detectiveCoatPaint)
        canvas.drawLine(currentX - 8f, feetY - 16f, currentX - 16f, feetY + 30f, detectivePaint.apply { strokeWidth = 8f })
        canvas.drawLine(currentX + 8f, feetY - 16f, currentX + 16f, feetY + 30f, detectivePaint)
        canvas.drawLine(currentX - 18f, feetY - 66f, currentX - 34f, feetY - 34f, detectivePaint)
        canvas.drawLine(currentX + 18f, feetY - 66f, currentX + 34f, feetY - 34f, detectivePaint)
    }

    private fun drawObjectiveHint(canvas: Canvas) {
        textPaint.alpha = 225
        canvas.drawText("Tap the ground to move", width * 0.08f, height * 0.92f, textPaint)
    }

    private fun drawLightningFlash(canvas: Canvas) {
        if (flashAlpha <= 0f) return
        flashPaint.color = Color.argb((flashAlpha * 255).toInt(), 226, 236, 255)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), flashPaint)
        flashPaint.color = Color.argb((flashAlpha * 120).toInt(), 188, 212, 255)
        canvas.drawRect(width * 0.22f, height * 0.18f, width * 0.78f, height * 0.69f, flashPaint)
    }

    private fun drawVignette(canvas: Canvas) {
        vignettePaint.shader = LinearGradient(
            0f,
            0f,
            0f,
            height.toFloat(),
            intArrayOf(
                Color.argb(35, 0, 0, 0),
                Color.argb(0, 0, 0, 0),
                Color.argb(70, 0, 0, 0),
            ),
            null,
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), vignettePaint)
        vignettePaint.color = Color.argb(55, 0, 0, 0)
        canvas.drawRect(0f, 0f, width * 0.04f, height.toFloat(), vignettePaint)
        canvas.drawRect(width * 0.96f, 0f, width.toFloat(), height.toFloat(), vignettePaint)
    }

    private fun updateStormState(now: Long, delta: Long) {
        flashAlpha = (flashAlpha - delta / 260f).coerceAtLeast(0f)
        if (now >= nextFlashAt) {
            flashAlpha = 0.65f + random.nextFloat() * 0.2f
            nextFlashAt = now + random.nextLong(2400L, 5200L)
            val nextIndex = random.nextInt(stormLines.size)
            if (nextIndex != stormLineIndex) {
                stormLineIndex = nextIndex
                sceneListener?.onStormLineChanged(stormLines[stormLineIndex])
            }
        }
    }

    private fun updateMovement(delta: Long) {
        val distance = detectiveTargetProgress - detectiveProgress
        if (abs(distance) < 0.002f) {
            detectiveProgress = detectiveTargetProgress
            return
        }
        val step = (PLAYER_SPEED * delta / 1000f).coerceAtLeast(0.01f)
        detectiveProgress = when {
            distance > 0f -> (detectiveProgress + step).coerceAtMost(detectiveTargetProgress)
            else -> (detectiveProgress - step).coerceAtLeast(detectiveTargetProgress)
        }
    }

    private fun drawWindows(
        canvas: Canvas,
        startX: Float,
        startY: Float,
        columns: Int,
        rows: Int,
        windowSize: Float,
    ) {
        val xGap = windowSize * 0.42f
        val yGap = windowSize * 0.6f
        for (row in 0 until rows) {
            for (column in 0 until columns) {
                val x = startX + column * (windowSize + xGap)
                val y = startY + row * (windowSize + yGap)
                val rect = RectF(x, y, x + windowSize, y + windowSize * 1.08f)
                canvas.drawRect(rect, windowPaint)
                canvas.drawRect(rect, windowFramePaint)
                canvas.drawLine(rect.centerX(), rect.top, rect.centerX(), rect.bottom, windowFramePaint)
                canvas.drawLine(rect.left, rect.centerY(), rect.right, rect.centerY(), windowFramePaint)
            }
        }
    }

    private fun progressForTap(tapX: Float): Float {
        val clampedX = tapX.coerceIn(playableStartX(), playableEndX())
        return (clampedX - playableStartX()) / (playableEndX() - playableStartX())
    }

    private fun detectiveXForProgress(progress: Float): Float {
        return playableStartX() + ((playableEndX() - playableStartX()) * progress)
    }

    private fun playableGroundTop(): Float = height * 0.68f

    private fun playableStartX(): Float = width * 0.12f

    private fun playableEndX(): Float = width * 0.5f

    private fun maybeNotifyDoorReached() {
        val reached = detectiveProgress >= 0.92f
        if (reached && !doorNotified) {
            doorNotified = true
            sceneListener?.onDoorReached()
        } else if (!reached) {
            doorNotified = false
        }
    }
}
