package com.ssafy.payclient.ui.chat

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

class RouletteWheelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val segmentColors = intArrayOf(
        Color.parseColor("#FF6B6B"),
        Color.parseColor("#4D96FF"),
        Color.parseColor("#6BCB77"),
        Color.parseColor("#FFD93D"),
        Color.parseColor("#A66CFF"),
        Color.parseColor("#FF9F1C")
    )
    private val segmentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }
    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val pointerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#222222")
        style = Paint.Style.FILL
    }
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 30f
        setFakeBoldText(true)
    }

    private val wheelRect = RectF()
    private val pointerPath = Path()
    private var candidateNames: List<String> = emptyList()

    var wheelRotation: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    fun setCandidates(names: List<String>) {
        candidateNames = names.filter { it.isNotBlank() }
        invalidate()
    }

    fun computeTargetRotation(winnerIndex: Int, rounds: Int = 7): Float {
        val size = candidateNames.size.takeIf { it > 0 } ?: return wheelRotation
        val sweep = 360f / size
        val safeIndex = winnerIndex.coerceIn(0, size - 1)
        val desiredRotation = positiveModulo(-((safeIndex + 0.5f) * sweep))
        val currentRotation = positiveModulo(wheelRotation)
        val delta = positiveModulo(desiredRotation - currentRotation)
        return wheelRotation + rounds * 360f + delta
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val size = min(width, height).toFloat()
        if (size <= 0f) return

        val radius = size * 0.43f
        val centerX = width / 2f
        val centerY = height / 2f + size * 0.05f
        val labels = candidateNames.ifEmpty { listOf("READY") }
        val sweep = 360f / labels.size

        wheelRect.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius)

        labels.forEachIndexed { index, label ->
            val startAngle = -90f + wheelRotation + sweep * index
            segmentPaint.color = segmentColors[index % segmentColors.size]
            canvas.drawArc(wheelRect, startAngle, sweep, true, segmentPaint)
            canvas.drawArc(wheelRect, startAngle, sweep, true, borderPaint)

            drawSegmentText(canvas, label, centerX, centerY, radius, startAngle + sweep / 2f)
        }

        canvas.drawCircle(centerX, centerY, radius * 0.16f, centerPaint)
        drawPointer(canvas, centerX, centerY - radius)
    }

    private fun drawSegmentText(
        canvas: Canvas,
        rawLabel: String,
        centerX: Float,
        centerY: Float,
        radius: Float,
        angle: Float
    ) {
        val label = rawLabel.take(8)
        canvas.save()
        canvas.rotate(angle, centerX, centerY)
        canvas.drawText(label, centerX + radius * 0.58f, centerY + textPaint.textSize / 3f, textPaint)
        canvas.restore()
    }

    private fun drawPointer(canvas: Canvas, centerX: Float, topY: Float) {
        pointerPath.reset()
        pointerPath.moveTo(centerX, topY + 18f)
        pointerPath.lineTo(centerX - 18f, topY - 20f)
        pointerPath.lineTo(centerX + 18f, topY - 20f)
        pointerPath.close()
        canvas.drawPath(pointerPath, pointerPaint)
    }

    private fun positiveModulo(value: Float): Float {
        val result = value % 360f
        return if (result < 0f) result + 360f else result
    }
}
