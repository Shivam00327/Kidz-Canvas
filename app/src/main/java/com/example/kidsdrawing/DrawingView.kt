package com.example.kidsdrawing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

import android.util.AttributeSet
import android.view.View

import android.graphics.Path
import android.util.TypedValue
import android.view.MotionEvent

class DrawingView(context: Context,attrs:AttributeSet): View(context,attrs) {
    private var mDrawPath: CustomPath? = null
    private var mCanvasBitmap: Bitmap? = null

    private var mDrawPaint: Paint? = null
    private var mCanvasPaint: Paint? = null

    private var mBrushSize: Float = 0.toFloat()
    private var color = Color.BLACK


    private var canvas: Canvas? = null

    private val mPaths = ArrayList<CustomPath>()

    private val mUndoPaths = ArrayList<CustomPath>()

    init {
        setUpDrawing()
    }
    private fun setUpDrawing() {
        mDrawPaint = Paint()
        mDrawPath = CustomPath(color, mBrushSize)

        mDrawPaint?.color = color

        mDrawPaint?.style = Paint.Style.STROKE
        mDrawPaint?.strokeJoin = Paint.Join.ROUND
        mDrawPaint?.strokeCap = Paint.Cap.ROUND

        mCanvasPaint = Paint(Paint.DITHER_FLAG) // Paint flag that enables dithering when blitting.

    }

    override fun onSizeChanged(w: Int, h: Int, wprev: Int, hprev: Int) {
        super.onSizeChanged(w, h, wprev, hprev)
        mCanvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        canvas = Canvas(mCanvasBitmap!!)
    }


    /**
     * This method is called when a stroke is drawn on the canvas
     * as a part of the painting.
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        mCanvasBitmap?.let {
            canvas.drawBitmap(it, 0f,   0f, mCanvasPaint)
        }


        for (p in mPaths) {
            mDrawPaint?.strokeWidth = p.brushThickness
            mDrawPaint?.color = p.color
            canvas.drawPath(p, mDrawPaint!!)
        }

        if (!mDrawPath!!.isEmpty) {
            mDrawPaint!!.strokeWidth = mDrawPath!!.brushThickness
            mDrawPaint!!.color = mDrawPath!!.color
            canvas.drawPath(mDrawPath!!, mDrawPaint!!)
        }
    }

    /**
     * This method acts as an event listener when a touch
     * event is detected on the device.
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val touchX = event.x
        val touchY = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mDrawPath!!.color = color
                mDrawPath!!.brushThickness = mBrushSize

                mDrawPath!!.reset()
                mDrawPath!!.moveTo(
                    touchX,
                    touchY
                )
            }

            MotionEvent.ACTION_MOVE -> {
                mDrawPath!!.lineTo(
                    touchX,
                    touchY
                )
            }

            MotionEvent.ACTION_UP -> {

                mPaths.add(mDrawPath!!) //Add when to stroke is drawn to canvas and added in the path arraylist

                mDrawPath = CustomPath(color, mBrushSize)
            }
            else -> return false
        }

        invalidate()
        return true
    }


    fun setSizeForBrush(newSize: Float) {
        mBrushSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, newSize,
            resources.displayMetrics
        )
        mDrawPaint!!.strokeWidth = mBrushSize
    }


    fun setColor(newColor: String) {
        color = Color.parseColor(newColor)
        mDrawPaint!!.color = color
    }


    fun onClickUndo() {
        if (mPaths.size > 0) {

            mUndoPaths.add(mPaths.removeAt(mPaths.size - 1))
            invalidate() // Invalidate the whole view. If the view is visible
        }
    }
    fun OnClickRedo(){
        if(mUndoPaths.size>0){
            mPaths.add(mUndoPaths.removeAt(mUndoPaths.size-1))
            invalidate()
        }
    }
    internal inner class CustomPath(var color:Int,var brushThickness:Float):Path()
}

