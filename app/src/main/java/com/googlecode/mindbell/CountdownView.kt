/*
 * MindBell - Aims to give you a support for staying mindful in a busy life -
 *            for remembering what really counts
 *
 *     Copyright (C) 2014-2017 Uwe Damken
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.googlecode.mindbell

import android.content.Context
import android.graphics.*
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import com.googlecode.mindbell.accessors.PrefsAccessor
import java.util.*

/**
 * Regularly show current state of meditation by updating a time slice drawing regularly.
 */
class CountdownView : View {

    // Bound of the text to be displayed
    internal var textBounds = Rect()

    private var rampUpStartingTimeMillis: Long = 0

    private var meditationStartingTimeMillis: Long = 0

    // Style information about the background
    private val backgroundPaint = Paint()

    // Style information about the time slice to be drawn
    private val timeSlicePaint = Paint()

    private var meditationEndingTimeMillis: Long = 0

    // Style information about the countdown string to be displayed during ramp-up
    private var textPaintRampUp: TextPaint? = null

    // Style information about the countdown string to be displayed during meditation
    private var textPaintMeditation: TextPaint? = null

    // Style information about the countdown string to be displayed after meditation
    private var textPaintBeyond: TextPaint? = null

    // Time to update the display with a time slice drawing regularly
    private var displayUpdateTimer: Timer? = null

    // Dimensions of the bowl circle to be drawn
    private var bowlDimensions: RectF? = null

    // Dimensions of the gap circle to be drawn "between" bowl and time slice
    private var gapDimensions: RectF? = null

    // Dimensions of the time slice to be drawn
    private var timeSliceDimensions: RectF? = null

    // Dimesions ot the rectangle to be drawn to crop the of the bowl circle
    private var cropDimensions: RectF? = null

    // X part of the center
    private var centerX: Int = 0

    // Y part of the center
    private var centerY: Int = 0

    constructor(context: Context) : super(context) {
        init(null, 0)
    }

    private fun init(attrs: AttributeSet?, defStyle: Int) {

        // Load application and system attributes
        val app = context.obtainStyledAttributes(attrs, R.styleable.CountdownView, defStyle, 0)
        val sys = context.obtainStyledAttributes(attrs, intArrayOf(android.R.attr.background))

        // Set up the colors for painting the time slice from background color of the view and the given color of the bell
        backgroundPaint.color = sys.getColor(0, Color.BLUE)
        timeSlicePaint.color = app.getColor(R.styleable.CountdownView_bellColor, Color.WHITE)

        // Set up a TextPaint object for writing the remaining time onto the time slice during ramp-up
        textPaintRampUp = TextPaint()
        textPaintRampUp!!.flags = Paint.ANTI_ALIAS_FLAG
        textPaintRampUp!!.color = app.getColor(R.styleable.CountdownView_rampUpColor, Color.RED)
        textPaintRampUp!!.textSize = app.getDimension(R.styleable.CountdownView_rampUpSize, 100f)

        // Set up a TextPaint object for writing the remaining time onto the time slice during meditation
        textPaintMeditation = TextPaint()
        textPaintMeditation!!.flags = Paint.ANTI_ALIAS_FLAG
        textPaintMeditation!!.color = app.getColor(R.styleable.CountdownView_meditationColor, Color.RED)
        textPaintMeditation!!.textSize = app.getDimension(R.styleable.CountdownView_meditationSize, 100f)

        // Set up a TextPaint object for writing the remaining time onto the time slice during meditation
        textPaintBeyond = TextPaint()
        textPaintBeyond!!.flags = Paint.ANTI_ALIAS_FLAG
        textPaintBeyond!!.color = app.getColor(R.styleable.CountdownView_beyondColor, Color.GREEN)
        textPaintBeyond!!.textSize = app.getDimension(R.styleable.CountdownView_beyondSize, 50f)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        init(attrs, defStyle)
    }

    /**
     * Starts displaying state of meditation by resetting and starting timers.
     */
    fun startDisplayUpdateTimer() {

        // Retrieve and store meditation times
        val prefs = PrefsAccessor.getInstance(context)
        this.rampUpStartingTimeMillis = prefs.rampUpStartingTimeMillis
        this.meditationStartingTimeMillis = prefs.meditationStartingTimeMillis
        this.meditationEndingTimeMillis = prefs.meditationEndingTimeMillis

        displayUpdateTimer = Timer()

        displayUpdateTimer!!.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                postInvalidate() // => onDraw()
            }
        }, 0, ONE_SECOND) // draw at once and then every second

        ReminderShowActivity.logDebug("Countdown timers started")
    }

    public override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        // Retrieve view dimensions and calculate dimensions of all shapes to be drawn
        calculateDimensions()
    }

    /**
     * Retrieve view dimensions and calculate dimensions of all shapes to be drawn
     */
    private fun calculateDimensions() {

        // Get view dimensions
        var paddingLeft = paddingLeft
        var paddingTop = paddingTop
        var paddingRight = paddingRight
        var paddingBottom = paddingBottom

        var contentWidth = width - paddingLeft - paddingRight
        var contentHeight = height - paddingTop - paddingBottom

        // Derive inner square dimensions for the rectangular view
        if (contentHeight == contentWidth) {
            // nothing to correct for a square canvas
        } else if (contentHeight > contentWidth) {
            val difference = contentHeight - contentWidth
            paddingTop += difference / 2
            paddingBottom += difference - difference / 2
            contentHeight = height - paddingTop - paddingBottom
        } else {
            val difference = contentWidth - contentHeight
            paddingLeft += difference / 2
            paddingRight += difference - difference / 2
            contentWidth = width - paddingLeft - paddingRight
        }

        // Calculate effective outer bounds of the square to be drawn into
        val paddedLeft = left + paddingLeft
        val paddedTop = top + paddingTop
        val paddedRight = right - paddingRight
        val paddedBottom = bottom - paddingBottom

        // Calculate center ot the square
        centerX = paddedLeft + contentWidth / 2
        centerY = paddedTop + contentHeight / 2

        // Calculate insets for the gap and time slice circle
        val gapInset = Math.min(paddingLeft, paddingTop) * 1.2f
        val timeSliceInset = gapInset * 1.7f

        // Calculate dimensions for the bowl, gap and time slice circle plus the crop rectangle
        bowlDimensions = RectF(paddedLeft.toFloat(), paddedTop.toFloat(), paddedRight.toFloat(), paddedBottom.toFloat())
        gapDimensions = inset(bowlDimensions!!, gapInset)
        timeSliceDimensions = inset(bowlDimensions!!, timeSliceInset)
        cropDimensions = RectF(paddedLeft.toFloat(), paddedTop.toFloat(), paddedRight.toFloat(), centerY - gapInset)
    }

    /**
     * Returns a rectangle based on the given one but inset as specified.
     */
    private fun inset(outer: RectF, inset: Float): RectF {
        return RectF(outer.left + inset, outer.top + inset, outer.right - inset, outer.bottom - inset)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val rampUp: Boolean
        val beyond: Boolean
        val currentTimeMillis = System.currentTimeMillis()
        var meditationSeconds = (meditationEndingTimeMillis - meditationStartingTimeMillis) / ONE_SECOND
        val displaySeconds: Long
        if (currentTimeMillis < meditationStartingTimeMillis) { // ramp up
            rampUp = true
            beyond = false
            meditationSeconds = (meditationStartingTimeMillis - rampUpStartingTimeMillis) / ONE_SECOND
            displaySeconds = (currentTimeMillis - rampUpStartingTimeMillis) / ONE_SECOND
        } else if (currentTimeMillis < meditationEndingTimeMillis) { // meditation
            rampUp = false
            beyond = false
            displaySeconds = (currentTimeMillis - meditationStartingTimeMillis) / ONE_SECOND
        } else if (currentTimeMillis < meditationEndingTimeMillis + meditationSeconds * ONE_SECOND) { // beyond
            rampUp = false
            beyond = true
            displaySeconds = (currentTimeMillis - meditationEndingTimeMillis) / ONE_SECOND
        } else { // meditation time twice over
            stopDisplayUpdateTimer()
            rampUp = false
            beyond = true
            displaySeconds = meditationSeconds
        }

        // Draw bowl by drawing a bowl circle, a gap circle and a rectangle to crop the top
        canvas.drawArc(bowlDimensions!!, -90f, 360f, true, timeSlicePaint)
        canvas.drawArc(gapDimensions!!, -90f, 360f, true, backgroundPaint)
        canvas.drawRect(cropDimensions!!, backgroundPaint)

        // Always draw a vertical line just in case displaySeconds doesn't make a sector with more than zero degrees

        if (rampUp) {
            // do not draw a sector during ramp up
        } else if (!beyond) { // meditation
            if (displaySeconds == 0L) {
                canvas.drawLine(centerX.toFloat(), centerY.toFloat(), centerX.toFloat(), timeSliceDimensions!!.top, timeSlicePaint) // just a vertical line
            } else {
                canvas.drawArc(timeSliceDimensions!!, -90f, displaySeconds * 360 / meditationSeconds.toFloat(), true, timeSlicePaint)
            }
        } else { // beyond
            if (displaySeconds == 0L) {
                canvas.drawArc(timeSliceDimensions!!, -90f, 360f, true, timeSlicePaint)
                canvas.drawLine(centerX.toFloat(), centerY.toFloat(), centerX.toFloat(), timeSliceDimensions!!.top, backgroundPaint) // just a vertical line
            } else {
                canvas.drawArc(timeSliceDimensions!!, -90f, -(meditationSeconds - displaySeconds) * 360 / meditationSeconds.toFloat(),
                        true, timeSlicePaint)
            }
        }

        // Draw the text on top of the circle
        val countdownString = getCountdownString(meditationSeconds, displaySeconds, rampUp, beyond)
        val textPaint = if (rampUp) textPaintRampUp else if (!beyond) textPaintMeditation else textPaintBeyond
        textPaint!!.getTextBounds(countdownString, 0, countdownString.length, textBounds)
        val textX = centerX - textBounds.exactCenterX()
        val textY = centerY - textBounds.exactCenterY()
        canvas.drawText(countdownString, textX, textY, textPaint)
    }

    /**
     * Stops a meditation by stopping timers and releasing wake lock
     */
    fun stopDisplayUpdateTimer() {
        if (displayUpdateTimer != null) {
            displayUpdateTimer!!.cancel()
            displayUpdateTimer = null
        }
        ReminderShowActivity.logDebug("Countdown timers stopped")
    }

    /**
     * Returns the string with the remaining time to be displayed.
     */
    private fun getCountdownString(seconds: Long, displaySeconds: Long, rampUp: Boolean, beyond: Boolean): String {
        val sb = StringBuilder()
        if (rampUp) {
            sb.append(seconds - displaySeconds)
        } else if (!beyond) { // meditation
            sb.append(Math.ceil((seconds - displaySeconds) / 60.0).toInt())
        } else { // beyond
            sb.append("-")
            sb.append(Math.floor(displaySeconds / 60.0).toInt())
            sb.append("-")
        }
        return sb.toString()
    }

    companion object {

        val ONE_SECOND: Long = 1000
    }

}
