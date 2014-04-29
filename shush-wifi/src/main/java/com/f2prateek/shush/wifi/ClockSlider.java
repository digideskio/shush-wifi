/*
 * Copyright (C) 2010 Jesse Wilson
 * Copyright 2014 Prateek Srivastava (@f2prateek)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.f2prateek.shush.wifi;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * A slider around a circle, to select a time between now and 12 hours from now.
 */
final class ClockSlider extends View {
  private static final int INSETS = 6;
  private static final int MINUTES_PER_HALF_DAY = 720;

  private int width;
  private int height;
  private int centerX;
  private int centerY;
  private int diameter;
  private final RectF outerCircle = new RectF();
  private final RectF innerCircle = new RectF();
  private final RectF buttonCircle = new RectF();
  private final Path path = new Path();

  private Paint lightGrey = new Paint();
  private Paint sliderPaint = new Paint();
  private Paint white = new Paint();
  private Paint duration = new Paint();
  private Paint durationUnits = new Paint();
  private Paint unshushTime = new Paint();
  private Paint percentPaint = new Paint();
  private Paint buttonCirclePaint = new Paint();

  private Calendar start = new GregorianCalendar();
  private int startAngle = 0;
  private Calendar end = new GregorianCalendar();

  /** minutes to shush. */
  private int minutes = 0;
  private boolean upPushed;
  private boolean downPushed;

  public ClockSlider(Context context, AttributeSet attrs) {
    super(context, attrs);

    lightGrey.setColor(Color.rgb(115, 115, 115));
    lightGrey.setAntiAlias(true);
    sliderPaint.setColor(Color.rgb(255, 0, 165));
    sliderPaint.setAntiAlias(true);
    white.setColor(Color.WHITE);
    white.setAntiAlias(true);
    duration.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
    duration.setSubpixelText(true);
    duration.setAntiAlias(true);
    duration.setColor(Color.WHITE);
    duration.setTextAlign(Paint.Align.CENTER);
    durationUnits = new Paint(duration);
    durationUnits.setTypeface(Typeface.SANS_SERIF);
    unshushTime = new Paint(duration);
    unshushTime.setColor(lightGrey.getColor());
    percentPaint = new Paint(duration);
    percentPaint.setTextAlign(Paint.Align.LEFT);
    percentPaint.setColor(lightGrey.getColor());
    buttonCirclePaint.setColor(Color.argb(102, 115, 115, 115));
    buttonCirclePaint.setAntiAlias(true);
  }

  public void setColor(int color) {
    sliderPaint.setColor(color);
  }

  @Override protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);

    if (getWidth() != width || getHeight() != height) {
      width = getWidth();
      height = getHeight();
      centerX = width / 2;
      centerY = height / 2;

      diameter = Math.min(width, height) - (2 * INSETS);
      int thickness = diameter / 15;

      int left = (width - diameter) / 2;
      int top = (height - diameter) / 2;
      int bottom = top + diameter;
      int right = left + diameter;
      outerCircle.set(left, top, right, bottom);

      int innerDiameter = diameter - thickness * 2;
      innerCircle.set(left + thickness, top + thickness, left + thickness + innerDiameter,
          top + thickness + innerDiameter);

      int offset = thickness * 2;
      int buttonDiameter = diameter - offset * 2;
      buttonCircle.set(left + offset, top + offset, left + offset + buttonDiameter,
          top + offset + buttonDiameter);

      duration.setTextSize(diameter * 0.32f);
      durationUnits.setTextSize(diameter * 0.10f);
      unshushTime.setTextSize(diameter * 0.13f);
      percentPaint.setTextSize(diameter * 0.08f);
    }

    drawClock(canvas);
    drawClockTextAndButtons(canvas);
  }

  public Date getStart() {
    return start.getTime();
  }

  public void setStart(Date now) {
    start.setTime(now);
    int minuteOfHalfDay = start.get(Calendar.HOUR_OF_DAY) * 60 + start.get(Calendar.MINUTE);
    if (minuteOfHalfDay > MINUTES_PER_HALF_DAY) {
      minuteOfHalfDay -= MINUTES_PER_HALF_DAY;
    }
    int angle = minuteOfHalfDay / 2; // 720 minutes per half-day -> 360 degrees per circle
    angle += 270; // clocks start at 12:00, but our angles start at 3:00
    startAngle = angle % 360;
    postInvalidate();
  }

  public int getMinutes() {
    return minutes;
  }

  public void setMinutes(int minutes) {
    if (minutes == this.minutes) {
      return; // avoid unnecessary repaints
    }
    this.minutes = minutes;
    end.setTimeInMillis(start.getTimeInMillis() + (this.minutes * 60 * 1000L));
    postInvalidate();
  }

  public Date getEnd() {
    return end.getTime();
  }

  /**
   * Draw a circle and an arc of the selected duration from start thru end.
   */
  private void drawClock(Canvas canvas) {
    int sweepDegrees = (minutes / 2) - 1;

    // the colored "filled" part of the circle
    drawArc(canvas, startAngle, sweepDegrees, sliderPaint);

    // the white selected part of the circle
    drawArc(canvas, startAngle + sweepDegrees, 2, white);

    // the grey empty part of the circle
    drawArc(canvas, startAngle + sweepDegrees + 2, 360 - sweepDegrees - 2, lightGrey);
  }

  private void drawArc(Canvas canvas, int startAngle, int sweepDegrees, Paint paint) {
    if (sweepDegrees <= 0) {
      return;
    }

    path.reset();
    path.arcTo(outerCircle, startAngle, sweepDegrees);
    path.arcTo(innerCircle, startAngle + sweepDegrees, -sweepDegrees);
    path.close();
    canvas.drawPath(path, paint);
  }

  /**
   * Draws a vertical slice of a right triangle whose slope goes from left to
   * right in bound. Left and right are lower and upper fractions of the
   * drawn area of bound.
   */
  private void drawTriangleSlice(Canvas canvas, RectF bound, float from, float to, Paint paint) {

        /*
         * Fill the middle slice of the triangle bounded by 'bound':
         *
         *        /|
         *       / |
         *      /| |
         *     / | |
         *    /| | |
         *   /_|_|_|
         */

    float left = bound.left + (from * bound.width());
    float right = bound.left + (to * bound.width());
    float levelTwo = bound.bottom - (from * bound.height());
    float levelThree = bound.bottom - (to * bound.height());

    path.reset();
    path.moveTo(left, bound.bottom);
    path.lineTo(left, levelTwo);
    path.lineTo(right, levelThree);
    path.lineTo(right, bound.bottom);
    path.close();
    canvas.drawPath(path, paint);
  }

  /**
   * Write labels in the middle of the circle like so:
   *
   * 2 1/2
   * hours
   * 10:15 PM
   */
  private void drawClockTextAndButtons(Canvas canvas) {
    // up/down button backgrounds
    if (upPushed) {
      canvas.drawArc(buttonCircle, 270, 180, true, buttonCirclePaint);
    }
    if (downPushed) {
      canvas.drawArc(buttonCircle, 90, 180, true, buttonCirclePaint);
    }

    String durationText;
    int durationUnitsId;
    long timeInMillis = end.getTimeInMillis();
    String onAtText = DateFormat.getTimeFormat(getContext()).format(timeInMillis);
    if (minutes < 60) {
      durationText = Integer.toString(minutes);
      durationUnitsId = R.string.minutes;
    } else if (minutes == 60) {
      durationText = "1";
      durationUnitsId = R.string.hour;
    } else if (minutes % 60 == 0) {
      durationText = Integer.toString(minutes / 60);
      durationUnitsId = R.string.hours;
    } else if (minutes % 60 == 15) {
      durationText = minutes / 60 + "\u00BC"; // 1/4
      durationUnitsId = R.string.hours;
    } else if (minutes % 60 == 30) {
      durationText = minutes / 60 + "\u00BD"; // 1/2
      durationUnitsId = R.string.hours;
    } else if (minutes % 60 == 45) {
      durationText = minutes / 60 + "\u00BE"; // 3/4
      durationUnitsId = R.string.hours;
    } else {
      throw new AssertionError();
    }
    String durationUnitsText = getResources().getString(durationUnitsId);
    canvas.drawText(durationText, centerX, centerY - (diameter * 0.08f), duration);
    canvas.drawText(durationUnitsText, centerX, centerY + (diameter * 0.06f), durationUnits);
    canvas.drawText(onAtText, centerX, centerY + (diameter * 0.25f), unshushTime);

    // up/down buttons
    Paint downPaint = downPushed ? white : lightGrey;
    canvas.drawRect(centerX - diameter * 0.32f, centerY - diameter * 0.01f,
        centerX - diameter * 0.22f, centerY + diameter * 0.01f, downPaint);
    Paint upPaint = upPushed ? white : lightGrey;
    canvas.drawRect(centerX + diameter * 0.22f, centerY - diameter * 0.01f,
        centerX + diameter * 0.32f, centerY + diameter * 0.01f, upPaint);
    canvas.drawRect(centerX + diameter * 0.26f, centerY - diameter * 0.05f,
        centerX + diameter * 0.28f, centerY + diameter * 0.05f, upPaint);
  }

  /**
   * Accept a touches near the circle's edge, translate it to an angle, and
   * update the sweep angle.
   */
  @Override public boolean onTouchEvent(MotionEvent event) {
    if (outerCircle == null) {
      return true; // ignore all events until the canvas is drawn
    }

    int touchX = (int) event.getX();
    int touchY = (int) event.getY();

    upPushed = false;
    downPushed = false;
    int distanceFromCenterX = centerX - touchX;
    int distanceFromCenterY = centerY - touchY;
    int distanceFromCenterSquared =
        distanceFromCenterX * distanceFromCenterX + distanceFromCenterY * distanceFromCenterY;
    float maxSlider = (diameter * 1.3f) / 2;
    float maxUpDown = (diameter * 0.8f) / 2;

    // handle increment/decrement
    if (distanceFromCenterSquared < (maxUpDown * maxUpDown)) {
      boolean up = touchX > centerX;

      if (event.getAction() == MotionEvent.ACTION_DOWN
          || event.getAction() == MotionEvent.ACTION_MOVE) {
        if (up) {
          upPushed = true;
        } else {
          downPushed = true;
        }
        postInvalidate();
        return true;
      }

      int angle = up ? (15 + minutes) : (705 + minutes);
      if (angle > 720) {
        angle -= 720;
      }
      setMinutes(angle);
      return true;

      // if it's on the slider, handle that
    } else if (distanceFromCenterSquared < (maxSlider * maxSlider)) {
      int angle = pointToAngle(touchX, touchY);
            /*
             * Convert the angle into a sweep angle. The sweep angle is a positive
             * angle between the start angle and the touched angle.
             */
      angle = 360 + angle - startAngle;
      int angleX2 = angle * 2;
      angleX2 = roundToNearest15(angleX2);
      if (angleX2 > 720) {
        angleX2 = angleX2 - 720; // avoid mod because we prefer 720 over 0
      }
      setMinutes(angleX2);
      return true;
    } else {
      return false;
    }
  }

  /**
   * Returns a volume fraction that's in permitted bounds. We don't let the
   * volume go too low (what would be the point!) or above 100%.
   */
  private float toVolume(float rawFraction) {
    if (rawFraction < 0.1) {
      return 0.1f;
    }
    if (rawFraction > 1.0) {
      return 1.0f;
    }
    return rawFraction;
  }

  @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    int width = MeasureSpec.getSize(widthMeasureSpec);
    int height = MeasureSpec.getSize(heightMeasureSpec);
    int maxHeight = (int) Math.min(height, width * 0.7);
    setMeasuredDimension(width, maxHeight);
  }

  /**
   * Returns the number of degrees (0-359) for the given point, such that
   * 3pm is 0 and 9pm is 180.
   */
  private int pointToAngle(int x, int y) {

        /* Get the angle from a triangle by dividing opposite by adjacent
         * and taking the atan. This code is careful not to divide by 0.
         *
         *
         *      adj | opp
         *          |
         * opp +180 | +270 adj
         * _________|_________
         *          |
         * adj  +90 | +0   opp
         *          |
         *      opp | adj
         *
         */

    if (x >= centerX && y < centerY) {
      double opp = x - centerX;
      double adj = centerY - y;
      return 270 + (int) Math.toDegrees(Math.atan(opp / adj));
    } else if (x > centerX && y >= centerY) {
      double opp = y - centerY;
      double adj = x - centerX;
      return (int) Math.toDegrees(Math.atan(opp / adj));
    } else if (x <= centerX && y > centerY) {
      double opp = centerX - x;
      double adj = y - centerY;
      return 90 + (int) Math.toDegrees(Math.atan(opp / adj));
    } else if (x < centerX && y <= centerY) {
      double opp = centerY - y;
      double adj = centerX - x;
      return 180 + (int) Math.toDegrees(Math.atan(opp / adj));
    }

    throw new IllegalArgumentException();
  }

  /**
   * Rounds the angle to the nearest 7.5 degrees, which equals 15 minutes on
   * a clock. Not strictly necessary, but it discourages fat-fingered users
   * from being frustrated when trying to select a fine-grained period.
   */
  private int roundToNearest15(int angleX2) {
    return ((angleX2 + 8) / 15) * 15;
  }
}
