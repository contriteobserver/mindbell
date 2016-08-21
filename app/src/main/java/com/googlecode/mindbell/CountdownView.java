/*******************************************************************************
 * MindBell - Aims to give you a support for staying mindful in a busy life -
 *            for remembering what really counts
 *
 *     Copyright (C) 2014-2016 Uwe Damken
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
 *******************************************************************************/
package com.googlecode.mindbell;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.PowerManager;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import com.googlecode.mindbell.accessors.AndroidContextAccessor;

import java.util.Timer;
import java.util.TimerTask;

/**
 * TODO: document your custom view class.
 */
public class CountdownView extends View {
    public static final long ONE_SECOND = 1000;
    public static final long ONE_MINUTE = 60 * ONE_SECOND;

    private static long nowTimeMillis;
    private static long startingTimeMillis;
    private static long endingTimeMillis;

    private long meditationSeconds;
    private long elapsedMeditationSeconds;
    private long rampUpSeconds;
    private long elapsedRampUpSeconds;
    private String mExampleString; // TODO: use a default from R.string...
    private int mExampleColor = Color.RED; // TODO: use a default from R.color...
    private float mExampleDimension = 0; // TODO: use a default from R.dimen...
    private Drawable mExampleDrawable;

    private TextPaint mTextPaint;
    private float mTextWidth;
    private float mTextHeight;

    private Timer displayUpdateTimer;

    private PowerManager.WakeLock wakeLock;

    public CountdownView(Context context) {
        super(context);
        init(null, 0);
    }

    public CountdownView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public CountdownView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.CountdownView, defStyle, 0);

        mExampleString = a.getString(
                R.styleable.CountdownView_exampleString);
        mExampleColor = a.getColor(
                R.styleable.CountdownView_exampleColor,
                mExampleColor);
        // Use getDimensionPixelSize or getDimensionPixelOffset when dealing with
        // values that should fall on pixel boundaries.
        mExampleDimension = a.getDimension(
                R.styleable.CountdownView_exampleDimension,
                mExampleDimension);

        if (a.hasValue(R.styleable.CountdownView_exampleDrawable)) {
            mExampleDrawable = a.getDrawable(
                    R.styleable.CountdownView_exampleDrawable);
            mExampleDrawable.setCallback(this);
        }

        a.recycle();

        // Set up a default TextPaint object
        mTextPaint = new TextPaint();
        mTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextAlign(Paint.Align.LEFT);

    }

    /**
     * Starts a meditation by resetting times and starting timers.
     */
    public void startMeditation(long nowTimeMillis, long startingTimeMillis, long endingTimeMillis) {

        this.nowTimeMillis = nowTimeMillis;
        this.startingTimeMillis = startingTimeMillis;
        this.endingTimeMillis = endingTimeMillis;

        startDisplayUpdateTimer();
    }

    public void startDisplayUpdateTimer() {

        // FIXME dkn Nur wenn der Bildschirm anbleiben soll
//        PowerManager pm = (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
//        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
//        wakeLock.acquire();
//        MindBell.logDebug("Meditation started, wake lock acquired");

        final AndroidContextAccessor contextAccessor = AndroidContextAccessor.getInstance(getContext());
        displayUpdateTimer = new Timer();

        displayUpdateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                postInvalidate(); // => onDraw()
            }
        }, ONE_SECOND, ONE_SECOND);
    }

    /**
     * Updates the string to be displayed with the remaining time.
     */
    private void updateExampleString(long seconds, long elapsedSeconds, boolean showMinutes) {
        StringBuilder sb = new StringBuilder();
        if (showMinutes) {
            long remainingBeganMinutes = (long) Math.ceil((seconds - elapsedSeconds) / 60d);
            sb.append(remainingBeganMinutes);
        } else {
            long remainingCompleteMinutes = (seconds - elapsedSeconds) / 60;
            if (remainingCompleteMinutes  > 0) {
                sb.append(remainingCompleteMinutes );
            }
            sb.append(":");
            sb.append(seconds - elapsedSeconds - remainingCompleteMinutes * 60);
        }
        setExampleString(sb.toString());
    }

    /**
     * Stops a meditation by stopping timers and releasing wake lock
     */
    public void stopDisplayUpdateTimer() {
        if (displayUpdateTimer != null) {
            displayUpdateTimer.cancel();
            displayUpdateTimer = null;
        }
        // FIXME dkn Nur wenn der Bildschirm anbleiben soll
//        if (wakeLock != null) {
//            wakeLock.release();
//            wakeLock = null;
//            MindBell.logDebug("Meditation stopped, wake lock released");
//        } else {
//            MindBell.logDebug("Meditation stopped, no wake lock to release");
//        }
    }

    private void invalidateTextPaintAndMeasurements() {
        mTextPaint.setTextSize(mExampleDimension);
        mTextPaint.setColor(mExampleColor);
        mTextWidth = mTextPaint.measureText(mExampleString);

        Paint.FontMetrics fontMetrics = mTextPaint.getFontMetrics();
        mTextHeight = fontMetrics.bottom;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        boolean rampUp;
        long currentTimeMillis = Math.min(System.currentTimeMillis(), endingTimeMillis);
        if (currentTimeMillis < startingTimeMillis) {
            rampUp = true;
            // FIXME dkn Vielleicht gleich remaining ausrechnen?
            meditationSeconds = (startingTimeMillis - nowTimeMillis) / ONE_SECOND;
            elapsedMeditationSeconds = (currentTimeMillis - nowTimeMillis) / ONE_SECOND;
            updateExampleString(meditationSeconds, elapsedMeditationSeconds, false);
        } else  {
            rampUp = false;
            // FIXME dkn Vielleicht gleich remaining ausrechnen?
            meditationSeconds = (endingTimeMillis - startingTimeMillis) / ONE_SECOND;
            elapsedMeditationSeconds = (currentTimeMillis - startingTimeMillis) / ONE_SECOND;
            updateExampleString(meditationSeconds, elapsedMeditationSeconds, true);
        }

        // TODO: consider storing these as member variables to reduce
        // allocations per draw cycle.
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();

        int contentWidth = getWidth() - paddingLeft - paddingRight;
        int contentHeight = getHeight() - paddingTop - paddingBottom;

        if (contentHeight == contentWidth) {
            // nothing to correct for a square canvas
        } else if (contentHeight > contentWidth) {
            int difference = contentHeight - contentWidth;
            paddingTop += difference / 2;
            paddingBottom += difference - difference / 2;
            contentHeight = getHeight() - paddingTop - paddingBottom;
        } else {
            int difference = contentWidth - contentHeight;
            paddingLeft += difference / 2;
            paddingRight += difference - difference / 2;
            contentWidth = getWidth() - paddingLeft - paddingRight;
        }

        int paddedLeft = getLeft() + paddingLeft;
        int paddedTop = getTop() + paddingTop;
        int paddedRight = getRight() - paddingRight;
        int paddedBottom = getBottom() - paddingBottom;

        Paint paint = new Paint();
        paint.setColor(Color.WHITE);

        // Always draw a vertical line just in case elapsedMeditationSeconds doesn't make a sector with more than zero degrees
        int centerX = paddedLeft + contentWidth / 2;
        int centerY = paddedTop + contentHeight / 2;
        canvas.drawLine(centerX, centerY, centerX, paddedTop, paint);
        // Draw sector that represents the elapsed seconds versus the total number of seconds
        if (!rampUp && elapsedMeditationSeconds > 0) {
            RectF oval = new RectF(paddedLeft, paddedTop, paddedRight, paddedBottom);
            canvas.drawArc(oval, -90, elapsedMeditationSeconds * 360 / (float) meditationSeconds, true, paint);
        }

        // Draw the text on top of the circle
        canvas.drawText(mExampleString,
                paddingLeft + (contentWidth - mTextWidth) / 2,
                paddingTop + (contentHeight + mTextHeight) / 2,
                mTextPaint);

    }

    /**
     * Gets the example string attribute value.
     *
     * @return The example string attribute value.
     */
    public String getExampleString() {
        return mExampleString;
    }

    /**
     * Sets the view's example string attribute value. In the example view, this string
     * is the text to draw.
     *
     * @param exampleString The example string attribute value to use.
     */
    public void setExampleString(String exampleString) {
        mExampleString = exampleString;
        invalidateTextPaintAndMeasurements();
    }

    /**
     * Gets the example color attribute value.
     *
     * @return The example color attribute value.
     */
    public int getExampleColor() {
        return mExampleColor;
    }

    /**
     * Sets the view's example color attribute value. In the example view, this color
     * is the font color.
     *
     * @param exampleColor The example color attribute value to use.
     */
    public void setExampleColor(int exampleColor) {
        mExampleColor = exampleColor;
        invalidateTextPaintAndMeasurements();
    }

    /**
     * Gets the example dimension attribute value.
     *
     * @return The example dimension attribute value.
     */
    public float getExampleDimension() {
        return mExampleDimension;
    }

    /**
     * Sets the view's example dimension attribute value. In the example view, this dimension
     * is the font size.
     *
     * @param exampleDimension The example dimension attribute value to use.
     */
    public void setExampleDimension(float exampleDimension) {
        mExampleDimension = exampleDimension;
        invalidateTextPaintAndMeasurements();
    }

    /**
     * Gets the example drawable attribute value.
     *
     * @return The example drawable attribute value.
     */
    public Drawable getExampleDrawable() {
        return mExampleDrawable;
    }

    /**
     * Sets the view's example drawable attribute value. In the example view, this drawable is
     * drawn above the text.
     *
     * @param exampleDrawable The example drawable attribute value to use.
     */
    public void setExampleDrawable(Drawable exampleDrawable) {
        mExampleDrawable = exampleDrawable;
    }

}
