package ru.netology.statsview.ui

import android.animation.*
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
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
        private var TOTAL_SUM = 0F
    }

    private var textSize = AndroidUtils.dp(context, 20).toFloat()
    private var lineWidth = AndroidUtils.dp(context, 5)
    private var colors = emptyList<Int>()
    private var emptyColor: Int = 0xFFE6E8EA.toInt()
    private var colorFirst = 0xFF000000.toInt()
    private var animationType: Int = 0

    init {
        context.withStyledAttributes(attributeSet, R.styleable.StatsView) {
            textSize = getDimension(R.styleable.StatsView_textSize, textSize)
            lineWidth = getDimension(R.styleable.StatsView_lineWidth, lineWidth.toFloat()).toInt()
            animationType = getInt(R.styleable.StatsView_animationType, 0)
            TOTAL_SUM = getFloat(R.styleable.StatsView_totalSum, 0F)
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

    private var progress = 0F
    private var valueAnimator: ValueAnimator? = null

    private var renderingComplete = false
    private val listObjectsAnimator = mutableListOf<Animator>()

    private var startAngle = -90F
    private var percentSum = 0F
    private var currentColor = 0
    private var finishAnimation = false
    private var idx = 0

    var data: List<Float> = emptyList()
        set(value) {
            field = value
            update()
        }

    private fun update() {
        valueAnimator?.let {
            it.removeAllListeners()
            it.cancel()
        }
        progress = 0F
        val dataCount = if (TOTAL_SUM == 0F) 1F else data.sum() / TOTAL_SUM
        var currentStartAngle = -90F
        if (animationType == 1) {
            currentColor = colors.getOrElse(0) { generateRandomColor() }
            for (i in data.indices) {
                val objectAnimator = ObjectAnimator.ofFloat(progress, dataCount / data.size).apply {
                    addUpdateListener {
                        startAngle = currentStartAngle
                        progress = it.animatedValue as Float
                        idx = i
                        invalidate()
                    }
                    duration = 1500
                    interpolator = LinearInterpolator()
                }
                    .also {
                        it.addListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator?) {
                                currentStartAngle += progress * 360F

                                if (i < data.size - 1) {
                                    currentColor = colors.getOrElse(i + 1) { generateRandomColor() }
                                    percentSum += progress
                                }
                            }
                        })
                    }
                listObjectsAnimator.add(objectAnimator)
            }

            val finishObjectAnimator = ObjectAnimator.ofFloat(0F, dataCount).apply {
                addUpdateListener {
                    finishAnimation = true
                    startAngle = -90F
                    progress = it.animatedValue as Float
                    percentSum = progress
                    invalidate()
                }
                startDelay = 100
                duration = 500
                interpolator = LinearInterpolator()
            }

            listObjectsAnimator.add(finishObjectAnimator)

            AnimatorSet().apply {
                playSequentially(listObjectsAnimator)
            }.start()
            return
        }

        valueAnimator = ValueAnimator.ofFloat(0F, 1F).apply {
            addUpdateListener { anim ->
                progress = anim.animatedValue as Float
                invalidate()
            }
            duration = 3000
            interpolator = LinearInterpolator()
        }
            .also {
                it.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator?) {
                        renderingComplete = true
                    }
                })
                it.start()
            }
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

        canvas.drawCircle(center.x, center.y, radius, paint.apply { color = emptyColor })
        val dataSum = if (TOTAL_SUM == 0F) data.sum() else TOTAL_SUM
        var percent: Float

        if (animationType == 0) {
            percentSum = 0F
            data.forEachIndexed { index, datum ->
                if (datum == 0F) return@forEachIndexed
                val coefficient = getCoefficient(dataSum, datum)
                percent = 100 / coefficient
                percentSum += percent
                val angle =
                    (if (TOTAL_SUM != 0F) 100 / getCoefficient(
                        data.sum(),
                        datum
                    ) else percent) * 360F
                paint.color = colors.getOrElse(index) { generateRandomColor() }
                if (index == 0) colorFirst = paint.color
                canvas.drawArc(
                    oval,
                    startAngle + progress * 360F,
                    angle * progress,
                    false,
                    paint
                )

                startAngle += angle
            }
        } else {
            val angle = progress * 360F
            paint.color = currentColor
            if (idx == 0) colorFirst = paint.color
            canvas.drawArc(
                oval,
                startAngle,
                angle,
                false,
                paint
            )
        }

        canvas.drawText(
            "%.2f%%".format((if (animationType == 0 || finishAnimation) percentSum else percentSum + progress) * 100),
            center.x,
            center.y + textPaint.textSize / 4,
            textPaint
        )
        if (renderingComplete)
            canvas.drawPoint(center.x, center.y - radius, paintPoint.apply
            { color = colorFirst })
    }

    private fun getCoefficient(dataSum: Float, datum: Float): Float = dataSum / datum * 100

    private fun generateRandomColor() = Random.nextInt(0xFF000000.toInt(), 0xFFFFFFFF.toInt())
}