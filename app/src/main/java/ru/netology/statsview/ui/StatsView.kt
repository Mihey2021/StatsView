package ru.netology.statsview.ui

import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.view.View
import androidx.annotation.RequiresApi
import androidx.core.content.withStyledAttributes
import ru.netology.statsview.R
import ru.netology.statsview.utils.AndroidUtils
import java.lang.Integer.min
import kotlin.random.Random

class StatsView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0,
) : View(
    context,
    attributeSet,
    defStyleAttr,
    defStyleRes,
) {

    companion object {
        const val TOTAL_SUM = 2000F
    }

    private var textSize = AndroidUtils.dp(context, 20).toFloat()
    private var lineWidth = AndroidUtils.dp(context, 5)
    private var colors = emptyList<Int>()
    private var emptyColor: Int = 0xFFE6E8EA.toInt()
    private var colorFirst = 0xFF000000.toInt()

    init {
        context.withStyledAttributes(attributeSet, R.styleable.StatsView) {
            textSize = getDimension(R.styleable.StatsView_textSize, textSize)
            lineWidth = getDimension(R.styleable.StatsView_lineWidth, lineWidth.toFloat()).toInt()
            emptyColor = getColor(R.styleable.StatsView_emptyColor, emptyColor)
            colorFirst = getColor(R.styleable.StatsView_color1, generateRandomColor())
            colors = listOf(
                colorFirst,
                getColor(R.styleable.StatsView_color2, generateRandomColor()),
                getColor(R.styleable.StatsView_color3, generateRandomColor()),
                getColor(R.styleable.StatsView_color4, generateRandomColor()),
            )
        }
    }

    var data: List<Float> = emptyList()
        set(value) {
            field = value
            invalidate()
        }

    private var radius = 0F
    private var center = PointF()
    private var oval = RectF()
    private val paint = Paint(
        Paint.ANTI_ALIAS_FLAG
    ).apply {
        strokeWidth = lineWidth.toFloat()
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private val paintPoint = Paint(
        Paint.ANTI_ALIAS_FLAG
    ).apply {
        strokeWidth = lineWidth.toFloat()
        style = Paint.Style.FILL
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private val textPaint = Paint(
        Paint.ANTI_ALIAS_FLAG
    ).apply {
        textSize = this@StatsView.textSize
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        radius = min(w, h) / 2F - lineWidth
        center = PointF(w / 2F, h / 2F)
        oval = RectF(
            center.x - radius,
            center.y - radius,
            center.x + radius,
            center.y + radius,
        )
    }

    override fun onDraw(canvas: Canvas) {
        if (data.isEmpty()) return

        var startAngle = -90F

        canvas.drawCircle(center.x, center.y, radius, paint.apply { color = emptyColor })
        val dataSum = if (TOTAL_SUM == 0F) data.sum() else TOTAL_SUM
        var percent: Float
        var percentSum = 0F
        data.forEachIndexed { index, datum ->
            val coefficient = (dataSum / datum) * 100
            percent = 100 / coefficient
            percentSum += percent
            val angle = if (TOTAL_SUM != 0F && TOTAL_SUM >= data.sum()) percent * 360F else 100 / (data.sum() / datum * 100) * 360F
            paint.color = colors.getOrElse(index) { generateRandomColor() }
            if (index == 0) colorFirst = paint.color
            canvas.drawArc(oval, startAngle, angle, false, paint)
            startAngle += angle
        }

        canvas.drawText(
            "%.2f%%".format(percentSum * 100),
            center.x,
            center.y + textPaint.textSize / 4,
            textPaint
        )
        canvas.drawPoint(center.x, center.y - radius, paintPoint.apply { color = colorFirst })

    }

    private fun generateRandomColor() = Random.nextInt(0xFF000000.toInt(), 0xFFFFFFFF.toInt())
}