package com.example.planetview

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.text.TextPaint
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.OverScroller
import androidx.annotation.ColorInt
import androidx.core.math.MathUtils
import androidx.core.view.ViewCompat
import com.blankj.utilcode.util.ColorUtils
import com.blankj.utilcode.util.ImageUtils
import com.blankj.utilcode.util.ScreenUtils
import com.blankj.utilcode.util.SizeUtils
import kotlin.math.abs
import kotlin.math.min
import kotlin.random.Random

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2022/4/15
 */
class PlanetView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val pathMatrix: Matrix = Matrix()
    private val path: Path = Path()
    private val pathMeasure = PathMeasure()
    private val pathMeasurePos = FloatArray(2)
    private val pathMeasureTan = FloatArray(2)
    private val camera: Camera = Camera()

    var centerXOffset: Float = -SizeUtils.dp2px(10f).toFloat()
        set(value) {
            field = value
            centerPositionUpdated = false
            updateCenterPositionIfNeed()
            invalidateIfNeed()
        }

    var centerYOffset: Float = -SizeUtils.dp2px(50f).toFloat()
        set(value) {
            field = value
            centerPositionUpdated = false
            updateCenterPositionIfNeed()
            invalidateIfNeed()
        }

    private var centerX: Float = 0f
    private var centerY: Float = 0f

    private val planetTrailPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val satellitePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    var satelliteTextSize: Float = SizeUtils.dp2px(10f).toFloat()
        set(value) {
            field = value
            satellitePaint.textSize = value
            invalidateIfNeed()
        }
    var satelliteTextColor: Int = Color.WHITE
        set(value) {
            field = value
            satellitePaint.color = value
            invalidateIfNeed()
        }

    private val fontMetrics: Paint.FontMetrics = Paint.FontMetrics()
    private val textPaint: TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)

    var headerImageSize: Int = SizeUtils.dp2px(48f)
    var headerTextSize: Float = SizeUtils.dp2px(12f).toFloat()
    var headerTextColor: Int = Color.WHITE
    var headerSpacing: Int = SizeUtils.dp2px(0f)

    var rotateX: Float = 45f
        set(value) {
            field = value
            invalidateIfNeed()
        }

    var rotateY: Float = -25f
        set(value) {
            field = value
            invalidateIfNeed()
        }

    var locationZ: Float = -55f
        set(value) {
            field = value
            invalidateIfNeed()
        }

    @ColorInt
    var planetTrailPathColor: Int = Color.WHITE
        set(value) {
            field = value
            invalidateIfNeed()
        }

    var planetTrailPathWidth: Float = SizeUtils.dp2px(0.1f).toFloat()
        set(value) {
            field = value
            invalidateIfNeed()
        }

    private var centerPositionUpdated: Boolean = false
    private var planetTrailsUpdated: Boolean = false
    private var planetTrails: List<PlanetTrail>? = null

    var planetTrailPathMinRadius: Float = SizeUtils.dp2px(150f).toFloat()
        set(value) {
            field = value
            planetTrailsUpdated = false
            updatePlanetTrailsIfNeed()
            invalidateIfNeed()
        }

    var planetTrailPathSpacing: Float = SizeUtils.dp2px(50f).toFloat()
        set(value) {
            field = value
            planetTrailsUpdated = false
            updatePlanetTrailsIfNeed()
            invalidateIfNeed()
        }

    var planetTrailCount: Int = 8
        set(value) {
            field = value
            planetTrailsUpdated = false
            updatePlanetTrailsIfNeed()
            invalidateIfNeed()
        }

    private var headerPositionUpdated: Boolean = false
    private val headerImagePosition: PointF = PointF()
    private val headerTextPosition: PointF = PointF()

    var headerImage: Bitmap? = null
        set(value) {
            field = value
            headerPositionUpdated = false
            updateHeaderPositionIfNeed()
            invalidateIfNeed()
        }

    var headerText: String? = null
        set(value) {
            field = value
            headerPositionUpdated = false
            updateHeaderPositionIfNeed()
            invalidateIfNeed()
        }

    private var satelliteAnimatedValue: Float = 0f
    private var satelliteAnimator: ValueAnimator? = null

    private val gestureDetector: GestureDetector

    private var downMotionX: Float = 0f
    private var downMotionY: Float = 0f
    private var dragDirection: Path.Direction? = null
    private var dragAxis: Int = ViewCompat.SCROLL_AXIS_NONE
    private var dragRate: Float = -1f
    private var dragDelta: Float = -1f
    private var lastScrollX: Int = 0
    private var lastScrollY: Int = 0

    private val scroll: OverScroller = OverScroller(context)

    private val onGestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(event: MotionEvent): Boolean {
            downMotionX = event.rawX
            downMotionY = event.rawY
            dragDirection = null
            dragAxis = ViewCompat.SCROLL_AXIS_NONE
            dragRate = -1f
            dragDelta = -1f
            if (!scroll.isFinished) {
                scroll.abortAnimation()
            }
            return super.onDown(event)
        }

        override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            rotatePlanetTrails(distanceX, distanceY)
            rotateSatellites(e2, distanceX, distanceY)
            invalidate()
            return true
        }

        override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            doFling(velocityX, velocityY)
            return true
        }
    }

    init {
        minimumWidth = SizeUtils.dp2px(300f)
        minimumHeight = SizeUtils.dp2px(300f)

        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_header)
        headerImage = ImageUtils.scale(bitmap, headerImageSize, headerImageSize, true)
        headerText = "PlanetView"

        gestureDetector = GestureDetector(context, onGestureListener)
        gestureDetector.setIsLongpressEnabled(false)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        if (changed || !centerPositionUpdated) {
            updateCenterPosition()
        }
        if (changed || !planetTrailsUpdated) {
            updatePlanetTrails()
        }
        if (changed && !headerPositionUpdated) {
            updateHeaderPosition()
        }
    }

    private fun updateCenterPositionIfNeed() {
        if (isLaidOut && !centerPositionUpdated) {
            updateCenterPosition()
        }

        headerPositionUpdated = false
        updateHeaderPositionIfNeed()

        planetTrailsUpdated = false
        updatePlanetTrailsIfNeed()
    }

    private fun updateCenterPosition() {
        centerPositionUpdated = true
        centerX = width * 0.5f + centerXOffset
        centerY = height * 0.5f + centerYOffset
    }

    private fun updatePlanetTrailsIfNeed() {
        if (isLaidOut && !planetTrailsUpdated) {
            updatePlanetTrails()
        }
    }

    private fun updatePlanetTrails() {
        planetTrailsUpdated = true
        val list = mutableListOf<PlanetTrail>()
        repeat(planetTrailCount) {
            val radius = planetTrailPathMinRadius + ((it - 1) * planetTrailPathSpacing)
            val path = Path().apply {
                addCircle(centerX, centerY, radius, Path.Direction.CCW)
            }
            val direction = if (it % 2 == 0) Path.Direction.CW else Path.Direction.CCW
            val duration = 30 * 1000L + 5 * 1000L * it
            val satellites = createSatellites(direction, duration)
            val planetTrail = PlanetTrail(
                radius,
                path,
                satellites,
                direction,
                duration,
            )
            list.add(planetTrail)
        }
        planetTrails = list
    }

    private fun createSatellites(direction: Path.Direction, duration: Long): List<Satellite> {
        val satelliteCount = 2 + Random.nextInt(5)
        val list = mutableListOf<Satellite>()
        repeat(satelliteCount) {
            val radius = Satellite.MIN_RADIUS + Random.nextInt(Satellite.MAX_RADIUS).toFloat()
            val position = (1f / satelliteCount) * it
            val satellite = Satellite(
                ColorUtils.getRandomColor(false),
                radius,
                "用户: $it",
                position,
                direction,
                duration,
            )
            list.add(satellite)
        }
        return list
    }

    private fun updateSatellites(delta: Float, direction: Path.Direction?) {
        planetTrails?.forEach { planetTrail ->
            planetTrail.satellites?.forEach { satellite ->
                val newPosition = satellite.calculateNewPosition(delta, direction)
                satellite.position = newPosition
            }
        }
        invalidate()
    }

    private fun updateHeaderPositionIfNeed() {
        if (isLaidOut && !headerPositionUpdated) {
            updateHeaderPosition()
        }
    }

    private fun updateHeaderPosition() {
        headerPositionUpdated = true
        updateHeaderImagePosition()
        updateHeaderTextPosition()
    }

    override fun onDraw(canvas: Canvas) {
        drawCenter(canvas)
        drawPlanetTrails(canvas)
    }

    private fun drawPlanetTrails(canvas: Canvas) {
        pathMatrix.reset()
        canvas.save()
        //保存 camera 状态
        camera.save()
        //旋转 camera 的三维空间 X
        camera.rotateX(rotateX)
        //旋转 camera 的三维空间 Y
        camera.rotateY(rotateY)
        //设置 camera 的 Z 轴位置, 防止产生糊脸的效果
        camera.setLocation(0f, 0f, locationZ)
        Log.d("tag", "rotateX:$rotateX, rotateY:$rotateY, locationZ:$locationZ")
        camera.getMatrix(pathMatrix)
        //回复 camera 的状态
        camera.restore()

        //将旋转点设置为中心
        pathMatrix.preTranslate(-centerX, -centerY)
        pathMatrix.postTranslate(centerX, centerY)

        planetTrailPaint.style = Paint.Style.STROKE
        planetTrailPaint.color = planetTrailPathColor
        planetTrailPaint.strokeWidth = planetTrailPathWidth
        planetTrails?.forEach { planetTrail ->
            val path = path.apply {
                reset()
                set(planetTrail.path)
                transform(pathMatrix)
            }
            canvas.drawPath(path, planetTrailPaint)
            drawSatellites(canvas, path, planetTrail.satellites)
        }
        canvas.restore()
    }

    private fun drawSatellites(canvas: Canvas, path: Path, satellites: List<Satellite>?) {
        satellitePaint.style = Paint.Style.FILL
        val textHeight = textPaint.getHeight(fontMetrics)
        val textBottom = textPaint.getBottom(fontMetrics)
        satellites?.forEach { satellite ->
            val scale = satellite.scale
            //绘制卫星点
            satellitePaint.color = satellite.color
            pathMeasure.setPath(path, false)
            val distance = pathMeasure.length * satellite.position
            pathMeasure.getPosTan(distance, pathMeasurePos, pathMeasureTan)
            val x = pathMeasurePos[0]
            val y = pathMeasurePos[1]
            canvas.drawCircle(x, y, satellite.radius * scale, satellitePaint)
            //绘制卫星名字
            if (satellite.radius > Satellite.MIN_RADIUS * 2) {
                satellitePaint.color = satelliteTextColor
                satellitePaint.textSize = satelliteTextSize * scale
                val textWidth = textPaint.measureText(satellite.name)
                val textX = x - textWidth * scale * 0.5f
                val textY = y + (satellite.radius * scale) + (textHeight * scale) - textBottom
                canvas.drawText(satellite.name, textX, textY, satellitePaint)
            }
        }
    }

    private fun drawCenter(canvas: Canvas) {
        val bitmap = this.headerImage
        if (bitmap != null) {
            canvas.drawBitmap(bitmap, headerImagePosition.x, headerImagePosition.y, planetTrailPaint)
        }
        val text = this.headerText
        if (!text.isNullOrEmpty()) {
            textPaint.color = headerTextColor
            canvas.drawText(text, headerTextPosition.x, headerTextPosition.y, textPaint)
        }
    }

    private fun updateHeaderImagePosition() {
        val bitmap = this.headerImage
        val text = this.headerText
        if (bitmap != null) {
            if (text.isNullOrEmpty()) {
                //bitmap 居中
                val x = centerX - bitmap.width * 0.5f
                val y = centerY - bitmap.height * 0.5f
                headerImagePosition.set(x, y)
            } else {
                //bitmap 和文字一起居中
                textPaint.textSize = headerTextSize
                val textHeight = textPaint.getHeight(fontMetrics)
                val x = centerX - bitmap.width * 0.5f
                val y = centerY - (bitmap.height + textHeight) * 0.5f
                headerImagePosition.set(x, y)
            }
        } else {
            headerImagePosition.set(0f, 0f)
        }
    }

    private fun updateHeaderTextPosition() {
        val bitmap = this.headerImage
        val text = this.headerText
        if (text.isNullOrEmpty()) {
            headerTextPosition.set(0f, 0f)
        } else {
            textPaint.textSize = headerTextSize
            val textWidth = textPaint.measureText(text)
            val textHeight = textPaint.getHeight(fontMetrics)
            val bottom = textPaint.getBottom(fontMetrics)
            if (bitmap != null) {
                //bitmap 和文字一起居中
                val x = centerX - textWidth * 0.5f
                val y = (centerY + (bitmap.height + textHeight) * 0.5f) - bottom
                headerTextPosition.set(x, y + headerSpacing)
            } else {
                //文字居中
                val x = centerX - textWidth * 0.5f
                val y = centerY + textHeight * 0.5f - bottom
                headerTextPosition.set(x, y)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                pause()
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                resume()
            }
        }
        return true
    }

    private fun rotatePlanetTrails(dx: Float, dy: Float) {
        val minRotateX = 40f
        val maxRotateX = 60f
        val yPercent = (dy / (ScreenUtils.getScreenHeight() * 0.5f))
        val offsetY = yPercent * (maxRotateX - minRotateX)
        rotateX = MathUtils.clamp(rotateX + offsetY, minRotateX, maxRotateX)
        val minRotateY = 20f
        val maxRotateY = 40f
        val xPercent = (dx / (ScreenUtils.getScreenWidth() * 0.5f))
        val offsetX = xPercent * (maxRotateY - minRotateY)
        rotateY = -MathUtils.clamp(abs(rotateY) + offsetX, minRotateY, maxRotateY)
    }

    private fun rotateSatellites(event: MotionEvent, dx: Float, dy: Float) {
        val currentX = event.x
        val currentY = event.y
        val maxDistance = min(width, height).toFloat()
        //滑动时的选装方向
        var direction: Path.Direction? = null
        //旋转增量, 增量为 1f 时即旋转一周
        var delta: Float = -1f
        val rateConstant = 0.3f
        when {
            abs(dx) >= abs(dy) && currentY <= height * 0.5f -> {
                //水平滑动, 上半区域
                direction = if (dx > 0) {
                    Path.Direction.CW
                } else {
                    Path.Direction.CCW
                }
                //滑动时的倍率, 用于保证越靠近中心越难滑动
                val rate = (height * 0.5f - currentY) / (height * rateConstant)
                //旋转增量, 增量为 1f 时即旋转一周
                delta = (abs(dx) / maxDistance) * rate
                dragAxis = ViewCompat.SCROLL_AXIS_HORIZONTAL
                dragRate = rate
            }
            abs(dx) >= abs(dy) && currentY > height * 0.5f -> {
                //水平滑动, 下半区域
                direction = if (dx < 0) {
                    Path.Direction.CW
                } else {
                    Path.Direction.CCW
                }
                //滑动时的倍率, 用于保证越靠近中心越难滑动
                val rate = (currentY - height * 0.5f) / (height * rateConstant)
                //旋转增量, 增量为 1f 时即旋转一周
                delta = (abs(dx) / maxDistance) * rate
                dragAxis = ViewCompat.SCROLL_AXIS_HORIZONTAL
                dragRate = rate
            }
            abs(dx) < abs(dy) && currentX <= width * 0.5f -> {
                //纵向滑动, 左半区域
                direction = if (dy < 0) {
                    Path.Direction.CW
                } else {
                    Path.Direction.CCW
                }
                //滑动时的倍率, 用于保证越靠近中心越难滑动
                val rate = (width * 0.5f - currentX) / (width * rateConstant)
                //旋转增量, 增量为 1f 时即旋转一周
                delta = (abs(dy) / maxDistance) * rate
                dragAxis = ViewCompat.SCROLL_AXIS_VERTICAL
                dragRate = rate
            }
            abs(dx) < abs(dy) && currentX > width * 0.5f -> {
                //纵向滑动, 右半区域
                direction = if (dy > 0) {
                    Path.Direction.CW
                } else {
                    Path.Direction.CCW
                }
                //滑动时的倍率, 用于保证越靠近中心越难滑动
                val rate = (currentX - width * 0.5f) / (width * rateConstant)
                //旋转增量, 增量为 1f 时即旋转一周
                delta = (abs(dy) / maxDistance) * rate
                dragAxis = ViewCompat.SCROLL_AXIS_VERTICAL
                dragRate = rate
            }
        }
        if (direction != null && delta != -1f) {
            //更新拖拽时的方向
            dragDirection = direction
            dragDelta = delta
            //这里将增量放大 20 倍, 确保手动旋转时更快
            updateSatellites(delta * 20f, direction)
        }
    }

    private val flingRunnable: Runnable = object : Runnable {
        override fun run() {
            val direction = this@PlanetView.dragDirection
            val dragAxis = this@PlanetView.dragAxis
            val dragRate = this@PlanetView.dragRate
            val dragDelta = this@PlanetView.dragDelta
            if (direction == null || dragAxis == ViewCompat.SCROLL_AXIS_NONE || dragRate == -1f || dragDelta == -1f) {
                scroll.abortAnimation()
                resume()
                return
            }
            if (scroll.computeScrollOffset()) {
                val maxDistance = min(width, height).toFloat()
                val deltaScroll: Int
                if (dragAxis == ViewCompat.SCROLL_AXIS_HORIZONTAL) {
                    val currentX = scroll.currX
                    deltaScroll = currentX - lastScrollX
                    lastScrollX = currentX
                } else {
                    val currentY = scroll.currY
                    deltaScroll = currentY - lastScrollY
                    lastScrollY = currentY
                }
                val delta = (abs(deltaScroll) / maxDistance) * dragRate
                //这里将增量放大 20 倍, 确保手动旋转时更快
                updateSatellites(delta * 20f, direction)
                //继续 fling
                ViewCompat.postOnAnimation(this@PlanetView, this)
            } else {
                resume()
            }
        }
    }

    private fun doFling(velocityX: Float, velocityY: Float) {
        if (dragDirection != null && dragAxis != ViewCompat.SCROLL_AXIS_NONE && dragRate != -1f) {
            pause()
            lastScrollX = 0
            lastScrollY = 0
            if (dragAxis == ViewCompat.SCROLL_AXIS_HORIZONTAL) {
                scroll.fling(
                    0, 0,
                    velocityX.toInt(), 0,
                    -Int.MAX_VALUE, Int.MAX_VALUE,
                    0, 0
                )
            } else {
                scroll.fling(
                    0, 0,
                    0, velocityY.toInt(),
                    0, 0,
                    -Int.MAX_VALUE, Int.MAX_VALUE
                )
            }
            ViewCompat.postOnAnimation(this, flingRunnable)
        }
    }

    fun start() {
        satelliteAnimator?.resume() ?: kotlin.run {
            val animator = ValueAnimator.ofFloat(0f, 1f)
            animator.duration = 1000L
            animator.interpolator = LinearInterpolator()
            animator.repeatMode = ValueAnimator.REVERSE
            animator.repeatCount = ValueAnimator.INFINITE
            animator.addUpdateListener {
                val animatedValue = it.animatedValue as Float
                val delta = abs(animatedValue - satelliteAnimatedValue)
                updateSatellites(delta, null)
                satelliteAnimatedValue = animatedValue
            }
            animator.start()
            satelliteAnimator = animator
        }
    }

    fun resume() {
        satelliteAnimator?.resume()
    }

    fun pause() {
        satelliteAnimator?.pause()
    }

    fun stop() {
        satelliteAnimator?.apply {
            cancel()
            removeAllUpdateListeners()
            satelliteAnimator = null
        }
    }

    private fun invalidateIfNeed() {
        if (isLaidOut) {
            invalidate()
        }
    }

    private fun TextPaint.getHeight(metrics: Paint.FontMetrics): Float {
        this.getFontMetrics(metrics)
        return metrics.bottom - metrics.top
    }

    private fun TextPaint.getBottom(metrics: Paint.FontMetrics): Float {
        this.getFontMetrics(metrics)
        return metrics.bottom
    }

    data class PlanetTrail(
        val radius: Float,
        val path: Path,
        /**
         * 轨道上的卫星
         */
        var satellites: List<Satellite>?,
        /**
         * 旋转方向
         */
        var direction: Path.Direction,
        /**
         * 转一圈的时长, 单位毫秒 ms
         */
        var duration: Long,
    )

    data class Satellite(
        @ColorInt
        var color: Int,
        var radius: Float,
        var name: String,
        /**
         * 当前所在轨道的位置, 取值范围: 0f~1f
         */
        var position: Float,
        /**
         * 旋转方向
         */
        var direction: Path.Direction,
        /**
         * 转一圈的时长, 单位毫秒 ms
         */
        var duration: Long,
    ) {
        val scale: Float
            get() {
                return if (position > MIN_SCALE_POSITION && position <= MAX_SCALE_POSITION) {
                    val changed = position - MIN_SCALE_POSITION
                    MIN_SCALE + (1f - MIN_SCALE) * changed * 2f
                } else {
                    val changed = if (position > MAX_SCALE_POSITION && position <= 1f) {
                        position - MAX_SCALE_POSITION
                    } else {
                        1f - MAX_SCALE_POSITION + position
                    }
                    1f - (1f - MIN_SCALE) * changed * 2f
                }
            }

        /**
         * delta 变化率, 1s 从 0.0 到 1.0
         */
        fun calculateNewPosition(delta: Float, direction: Path.Direction?): Float {
            val changed = delta / (this.duration / 1000L)
            when (direction ?: this.direction) {
                Path.Direction.CW -> {
                    var newPosition = this.position + changed
                    if (newPosition > 1f) {
                        newPosition -= 1f
                    }
                    return newPosition
                }
                Path.Direction.CCW -> {
                    var newPosition = this.position - changed
                    if (newPosition < 0f) {
                        newPosition += 1f
                    }
                    return newPosition
                }
            }
        }

        companion object {
            val MIN_RADIUS: Int = SizeUtils.dp2px(2f)
            val MAX_RADIUS: Int = SizeUtils.dp2px(12f)

            //最小缩放比例
            const val MIN_SCALE: Float = 0.6f

            //最小缩放比例时的位置
            const val MIN_SCALE_POSITION = 0.3f

            //最小缩放比例时的位置
            const val MAX_SCALE_POSITION = 0.8f
        }
    }

}