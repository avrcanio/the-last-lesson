package com.qubit.thelastlesson.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.toColorInt
import kotlin.random.Random

class OutsideSchoolSceneView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    interface SceneListener {
        fun onDoorReached()
        fun onStormLineChanged(line: String)
    }

    var sceneListener: SceneListener? = null

    var detectiveProgress: Float = 0.08f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
            maybeNotifyDoorReached()
        }

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#A8C6FF".toColorInt()
        alpha = 125
        strokeWidth = 3f
    }
    private val schoolPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#1E232C".toColorInt()
    }
    private val roofPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#0E1118".toColorInt()
    }
    private val windowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#F1D98A".toColorInt()
        alpha = 85
    }
    private val entrancePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#56372C".toColorInt()
    }
    private val groundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#0A1011".toColorInt()
    }
    private val pathPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#2B3039".toColorInt()
    }
    private val detectivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#D4D6DB".toColorInt()
    }
    private val detectiveCoatPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#786650".toColorInt()
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 42f
    }
    private val flashPaint = Paint()

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
        updateStormState(now, delta)

        drawSky(canvas)
        drawSchool(canvas)
        drawGround(canvas)
        drawRain(canvas)
        drawDetective(canvas)
        drawObjectiveHint(canvas)
        drawLightningFlash(canvas)

        postInvalidateOnAnimation()
    }

    private fun drawSky(canvas: Canvas) {
        backgroundPaint.shader = LinearGradient(
            0f,
            0f,
            0f,
            height.toFloat(),
            intArrayOf("#06080D".toColorInt(), "#151B27".toColorInt(), "#202938".toColorInt()),
            null,
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)
    }

    private fun drawSchool(canvas: Canvas) {
        val schoolTop = height * 0.2f
        val schoolBottom = height * 0.68f
        val leftWing = RectF(width * 0.08f, schoolTop + height * 0.08f, width * 0.34f, schoolBottom)
        val center = RectF(width * 0.28f, schoolTop, width * 0.72f, schoolBottom)
        val rightWing = RectF(width * 0.66f, schoolTop + height * 0.08f, width * 0.92f, schoolBottom)

        canvas.drawRect(leftWing, schoolPaint)
        canvas.drawRect(center, schoolPaint)
        canvas.drawRect(rightWing, schoolPaint)

        val roofPeakY = schoolTop - height * 0.08f
        val roofPoints = floatArrayOf(
            width * 0.25f, schoolTop,
            width * 0.5f, roofPeakY,
            width * 0.75f, schoolTop,
        )
        canvas.drawLines(roofPoints, roofPaint.apply { strokeWidth = 18f })

        val entrance = RectF(width * 0.43f, height * 0.47f, width * 0.57f, schoolBottom)
        canvas.drawRect(entrance, entrancePaint)

        val windowWidth = width * 0.08f
        val windowHeight = height * 0.08f
        for (row in 0..1) {
            for (col in 0..2) {
                val x = width * (0.17f + (col * 0.12f))
                val y = height * (0.34f + (row * 0.14f))
                canvas.drawRect(x, y, x + windowWidth, y + windowHeight, windowPaint)
                val rightX = width * (0.59f + (col * 0.1f))
                canvas.drawRect(rightX, y, rightX + windowWidth, y + windowHeight, windowPaint)
            }
        }
        canvas.drawRect(width * 0.46f, height * 0.29f, width * 0.54f, height * 0.38f, windowPaint)
    }

    private fun drawGround(canvas: Canvas) {
        val groundTop = height * 0.68f
        canvas.drawRect(0f, groundTop, width.toFloat(), height.toFloat(), groundPaint)
        val path = RectF(width * 0.44f, groundTop, width * 0.56f, height.toFloat())
        canvas.drawRect(path, pathPaint)
    }

    private fun drawRain(canvas: Canvas) {
        val spacing = width / 18f
        var x = -spacing
        while (x < width + spacing) {
            var y = -height * 0.2f
            while (y < height.toFloat()) {
                val startX = x + (y / 5f)
                val startY = y + rainOffset
                val endX = startX - 18f
                val endY = startY + 44f
                canvas.drawLine(startX, startY, endX, endY, rainPaint)
                y += 120f
            }
            x += spacing
        }
    }

    private fun drawDetective(canvas: Canvas) {
        val groundTop = height * 0.68f
        val doorX = width * 0.5f
        val startX = width * 0.12f
        val currentX = startX + ((doorX - startX) * detectiveProgress)
        val feetY = groundTop + height * 0.14f
        canvas.drawCircle(currentX, feetY - 92f, 20f, detectivePaint)
        canvas.drawRect(currentX - 22f, feetY - 82f, currentX + 22f, feetY - 16f, detectiveCoatPaint)
        canvas.drawLine(currentX - 8f, feetY - 16f, currentX - 16f, feetY + 30f, detectivePaint.apply { strokeWidth = 8f })
        canvas.drawLine(currentX + 8f, feetY - 16f, currentX + 16f, feetY + 30f, detectivePaint)
        canvas.drawLine(currentX - 18f, feetY - 66f, currentX - 34f, feetY - 34f, detectivePaint)
        canvas.drawLine(currentX + 18f, feetY - 66f, currentX + 34f, feetY - 34f, detectivePaint)
    }

    private fun drawObjectiveHint(canvas: Canvas) {
        textPaint.alpha = 220
        canvas.drawText("Move toward the front door", width * 0.08f, height * 0.92f, textPaint)
    }

    private fun drawLightningFlash(canvas: Canvas) {
        if (flashAlpha <= 0f) return
        flashPaint.color = Color.argb((flashAlpha * 255).toInt(), 235, 241, 255)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), flashPaint)
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
