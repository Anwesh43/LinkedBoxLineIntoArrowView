package com.anwesh.uiprojects.boxlineintoarrowview

/**
 * Created by anweshmishra on 06/06/19.
 */

import android.view.View
import android.view.MotionEvent
import android.app.Activity
import android.graphics.Paint
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.content.Context

val nodes : Int = 5
val lines : Int = 2
val scGap : Float = 0.05f
val scDiv : Double = 0.51
val strokeFactor : Int = 90
val sizeFactor : Float = 2.9f
val foreColor : Int = Color.parseColor("#4527A0")
val backColor : Int = Color.parseColor("#BDBDBD")
val lineFactor : Float = 2.5f
val rWFactor : Float = 2f
val rHFactor : Float = 2f
val delay : Long = 20

fun Int.inverse() : Float = 1f / this
fun Float.scaleFactor() : Float = Math.floor(this / scDiv).toFloat()
fun Float.maxScale(i : Int, n : Int) : Float = Math.max(0f, this - i * n.inverse())
fun Float.divideScale(i : Int, n : Int) : Float = Math.min(n.inverse(), maxScale(i, n)) * n
fun Float.mirrorValue(a : Int, b : Int) : Float {
    val k : Float = scaleFactor()
    return (1 - k) * a.inverse() + k * b.inverse()
}
fun Float.updateValue(dir : Float, a : Int, b : Int) = mirrorValue(a, b) * dir * scGap

fun Canvas.drawLineIntoArrow(i : Int, size : Float, sc1 : Float, sc2 : Float, paint : Paint) {
    val sc1i : Float = sc1.divideScale(i, lines)
    val sc2i : Float = sc2.divideScale(i, lines)
    val lineSize : Float = size / lineFactor
    val sf : Float = 1f - 2 * i
    for (j in 0..(lines - 1)) {
        save()
        translate((rWFactor * size - lineSize) * (1 - sc1i) * sf, 0f)
        rotate(45f * (1 - 2 *j) * sc2i.divideScale(j, lines))
        drawLine(0f, 0f, lineSize * sf, 0f, paint)
        restore()
    }
}

fun Canvas.drawLIANode(i : Int, scale : Float, paint : Paint) {
    val w : Float = width.toFloat()
    val h : Float = height.toFloat()
    val gap : Float = h / (nodes + 1)
    val size : Float = gap / sizeFactor
    val sc1 : Float = scale.divideScale(0, 2)
    val sc2 : Float = scale.divideScale(1, 2)
    paint.color = foreColor
    paint.strokeWidth = Math.min(w, h) / strokeFactor
    paint.strokeCap = Paint.Cap.ROUND
    paint.style = Paint.Style.STROKE
    save()
    translate(w / 2, gap * (i + 1))
    drawRect(RectF(-rWFactor * size, -size / rHFactor, rWFactor * size, size / rHFactor), paint)
    for (j in 0..(lines - 1)) {
        drawLineIntoArrow(j, size, sc1, sc2, paint)
    }
    restore()
}

class BoxLineIntoArrowView(ctx : Context) : View(ctx) {

    private val paint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val renderer : Renderer = Renderer(this)

    override fun onDraw(canvas : Canvas) {
        renderer.render(canvas, paint)
    }

    override fun onTouchEvent(event : MotionEvent) : Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                renderer.handleTap()
            }
        }
        return true
    }

    data class State(var scale : Float = 0f, var dir : Float = 0f, var prevScale : Float = 0f) {

        fun update(cb : (Float) -> Unit) {
            scale += scale.updateValue(dir, lines, lines * lines)
            if (Math.abs(scale - prevScale) > 1) {
                scale = prevScale + dir
                dir = 0f
                prevScale = scale
                cb(prevScale)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            if (dir == 0f) {
                dir = 1f - 2 * prevScale
                cb()
            }
        }
    }

    data class Animator(var view : View, var animated : Boolean = false) {

        fun animate(cb : () -> Unit) {
            if (animated) {
                cb()
                try {
                    Thread.sleep(delay)
                    view.invalidate()
                } catch(ex : Exception) {

                }
            }
        }

        fun start() {
            if (!animated) {
                animated = true
                view.postInvalidate()
            }
        }

        fun stop() {
            if (animated) {
                animated = false
            }
        }
    }

    data class LIANode(var i : Int, val state : State = State()) {

        private var next : LIANode? = null
        private var prev : LIANode? = null

        init {
            addNeighbor()
        }

        fun addNeighbor() {
            if (i < nodes - 1) {
                next = LIANode(i + 1)
                next?.prev = this
            }
        }

        fun draw(canvas : Canvas, paint : Paint) {
            canvas.drawLIANode(i, state.scale, paint)
            next?.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            state.update {
                cb(i, it)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            state.startUpdating(cb)
        }

        fun getNext(dir : Int, cb : () -> Unit) : LIANode {
            var curr : LIANode? = prev
            if (dir == 1) {
                curr = next
            }
            if (curr != null) {
                return curr
            }
            cb()
            return this
        }
    }

    data class BoxLineIntoArrow(var i : Int) {

        private val root : LIANode = LIANode(0)
        private var curr : LIANode = root
        private var dir : Int = 1

        fun draw(canvas : Canvas, paint : Paint) {
            root.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            curr.update {i, scl ->
                curr = curr.getNext(dir) {
                    dir *= -1
                }
                cb(i, scl)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            curr.startUpdating(cb)
        }
    }

    data class Renderer(var view : BoxLineIntoArrowView) {

        private val animator : Animator = Animator(view)
        private val bla : BoxLineIntoArrow = BoxLineIntoArrow(0)

        fun render(canvas : Canvas, paint : Paint) {
            bla.draw(canvas, paint)
            animator.animate {
                bla.update {i, scl ->
                    animator.stop()
                }
            }
        }

        fun handleTap() {
            bla.startUpdating {
                animator.start()
            }
        }
    }

    companion object {

        fun create(activity : Activity) : BoxLineIntoArrowView {
            val view : BoxLineIntoArrowView = BoxLineIntoArrowView(activity)
            activity.setContentView(view)
            return view
        }
    }
}