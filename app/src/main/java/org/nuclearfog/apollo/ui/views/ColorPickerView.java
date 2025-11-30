package org.nuclearfog.apollo.ui.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Displays a color picker to the user and allow them to select a color.
 *
 * @author Daniel Nilsson
 */
public class ColorPickerView extends View {

	/**
	 * The width in pixels of the border surrounding all color panels.
	 */
	private static final float BORDER_WIDTH_PX = 1;
	private static final int PANEL_SAT_VAL = 0;
	private static final int PANEL_HUE = 1;
	private static final int SLIDER_COLOR = 0xff1c1c1c;

	/**
	 * The width in dp of the hue panel.
	 */
	private float hue_panel_width = 30f;

	/**
	 * The distance in dp between the different color panels.
	 */
	private float panel_spacing = 10f;

	/**
	 * The radius in dp of the color palette tracker circle.
	 */
	private float PaletteCircleTrackerRadius = 5f;

	/**
	 * The dp which the tracker of the hue panel will extend outside of
	 * its bounds.
	 */
	private float rectangleTrackerOffset = 2f;

	/**
	 * To remember which panel that has the "focus" when processing har
	 * button data.
	 */
	private int mLastTouchedPanel = PANEL_SAT_VAL;
	/**
	 * Offset from the edge we must have or else the finger tracker will get
	 * clipped when it is drawn outside of the view.
	 */
	private float mDrawingOffset;

	@Nullable
	private OnColorChangedListener mListener;
	@Nullable
	private RectF mDrawingRect, mSatValRect, mHueRect;
	@Nullable
	private Shader mValShader, mHueShader;
	@Nullable
	private Point mStartTouchPoint;

	private Paint mSatValPaint = new Paint();
	private Paint mSatValTrackerPaint = new Paint();
	private Paint mHuePaint = new Paint();
	private Paint mHueTrackerPaint = new Paint();
	private Paint mBorderPaint = new Paint();

	private float[] hsv = {360f, 0f, 0f};
	private int mBorderColor = 0xff6E6E6E;
	private float mDensity;

	/**
	 *
	 */
	public ColorPickerView(@NonNull Context context) {
		this(context, null);
	}

	/**
	 *
	 */
	public ColorPickerView(@NonNull Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	/**
	 *
	 */
	public ColorPickerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// disable hardware acceleration
		setLayerType(View.LAYER_TYPE_SOFTWARE, null);
		// init
		mDensity = getContext().getResources().getDisplayMetrics().density;
		PaletteCircleTrackerRadius *= mDensity;
		rectangleTrackerOffset *= mDensity;
		hue_panel_width *= mDensity;
		panel_spacing = panel_spacing * mDensity;
		mDrawingOffset = calculateRequiredOffset();
		// init paint tools
		mSatValTrackerPaint.setStyle(Style.STROKE);
		mSatValTrackerPaint.setStrokeWidth(2f * mDensity);
		mSatValTrackerPaint.setAntiAlias(true);
		mHueTrackerPaint.setColor(SLIDER_COLOR);
		mHueTrackerPaint.setStyle(Style.STROKE);
		mHueTrackerPaint.setStrokeWidth(2f * mDensity);
		mHueTrackerPaint.setAntiAlias(true);
		// Needed for receiving track ball motion events.
		setFocusable(true);
		setFocusableInTouchMode(true);
	}


	@Override
	protected void onDraw(@NonNull Canvas canvas) {
		if (mDrawingRect != null && mDrawingRect.width() > 0 && mDrawingRect.height() > 0) {
			drawSatValPanel(canvas);
			drawHuePanel(canvas);
		}
	}


	@Override
	public boolean onTrackballEvent(MotionEvent event) {
		float x = event.getX();
		float y = event.getY();
		boolean update = false;
		if (event.getAction() == MotionEvent.ACTION_MOVE) {
			switch (mLastTouchedPanel) {
				case PANEL_SAT_VAL:
					float sat = hsv[1] + x / 50f;
					float val = hsv[2] - y / 50f;
					if (sat < 0f) {
						sat = 0f;
					} else if (sat > 1f) {
						sat = 1f;
					}
					if (val < 0f) {
						val = 0f;
					} else if (val > 1f) {
						val = 1f;
					}
					hsv[1] = sat;
					hsv[2] = val;
					update = true;
					break;

				case PANEL_HUE:
					float hue = hsv[0] - y * 10f;
					if (hue < 0f) {
						hue = 0f;
					} else if (hue > 360f) {
						hue = 360f;
					}
					hsv[0] = hue;
					update = true;
					break;
			}
		}
		if (update) {
			if (mListener != null) {
				mListener.onColorChanged(getColor());
			}
			invalidate();
			return true;
		}
		return super.onTrackballEvent(event);
	}


	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		boolean update = false;

		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				mStartTouchPoint = new Point(Math.round(event.getX()), Math.round(event.getY()));
				update = moveTrackersIfNeeded(event);
				break;

			case MotionEvent.ACTION_MOVE:
				update = moveTrackersIfNeeded(event);
				break;

			case MotionEvent.ACTION_UP:
				mStartTouchPoint = null;
				update = moveTrackersIfNeeded(event);
				break;
		}
		if (update) {
			if (mListener != null) {
				mListener.onColorChanged(getColor());
			}
			invalidate();
			return true;
		}
		return super.onTouchEvent(event);
	}


	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int width;
		int height;
		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		int widthAllowed = MeasureSpec.getSize(widthMeasureSpec);
		int heightAllowed = MeasureSpec.getSize(heightMeasureSpec);
		widthAllowed = chooseWidth(widthMode, widthAllowed);
		heightAllowed = chooseHeight(heightMode, heightAllowed);
		height = Math.round(widthAllowed - panel_spacing - hue_panel_width);
		// If calculated height (based on the width) is more than the
		// allowed height.
		if (height > heightAllowed) {
			height = heightAllowed;
			width = Math.round(height + panel_spacing + hue_panel_width);
		} else {
			width = widthAllowed;
		}
		setMeasuredDimension(width, height);
	}


	@Override
	protected void onSizeChanged(int w, int h, int oldW, int oldH) {
		super.onSizeChanged(w, h, oldW, oldH);
		mDrawingRect = new RectF();
		mDrawingRect.left = mDrawingOffset + getPaddingLeft();
		mDrawingRect.right = w - mDrawingOffset - getPaddingRight();
		mDrawingRect.top = mDrawingOffset + getPaddingTop();
		mDrawingRect.bottom = h - mDrawingOffset - getPaddingBottom();
		// setup val rect
		float panelSide = mDrawingRect.height() - BORDER_WIDTH_PX * 2;
		float left = mDrawingRect.left + BORDER_WIDTH_PX;
		float top = mDrawingRect.top + BORDER_WIDTH_PX;
		float bottom = top + panelSide;
		float right = left + panelSide;
		mSatValRect = new RectF(left, top, right, bottom);
		// setup hue rect
		left = mDrawingRect.right - hue_panel_width + BORDER_WIDTH_PX;
		top = mDrawingRect.top + BORDER_WIDTH_PX;
		bottom = mDrawingRect.bottom - BORDER_WIDTH_PX;
		right = mDrawingRect.right - BORDER_WIDTH_PX;
		mHueRect = new RectF(left, top, right, bottom);
	}

	/**
	 * Set a OnColorChangedListener to get notified when the color selected by
	 * the user has changed.
	 */
	public void setOnColorChangedListener(OnColorChangedListener listener) {
		mListener = listener;
	}

	/**
	 * Get the current color this view is showing.
	 *
	 * @return the current color.
	 */
	public int getColor() {
		return Color.HSVToColor(0xff, hsv);
	}

	/**
	 * Set the color the view should show.
	 *
	 * @param color The color that should be selected.
	 */
	public void setColor(int color) {
		int red = Color.red(color);
		int blue = Color.blue(color);
		int green = Color.green(color);
		Color.RGBToHSV(red, green, blue, hsv);
		invalidate();
	}

	/**
	 * set the color resource
	 */
	public void setColorResource(@ColorRes int colorRes) {
		int color = getResources().getColor(colorRes);
		setColor(color);
	}


	private float calculateRequiredOffset() {
		float offset = Math.max(PaletteCircleTrackerRadius, rectangleTrackerOffset);
		offset = Math.max(offset, BORDER_WIDTH_PX * mDensity);
		return offset * 1.5f;
	}


	private int[] buildHueColorArray() {
		int[] hue = new int[361];
		int count = 0;
		for (int i = hue.length - 1; i >= 0; i--, count++) {
			float[] color = new float[]{i, 1f, 1f};
			hue[count] = Color.HSVToColor(color);
		}
		return hue;
	}


	private void drawSatValPanel(Canvas canvas) {
		mBorderPaint.setColor(mBorderColor);
		if (mSatValRect != null && mDrawingRect != null) {
			canvas.drawRect(mDrawingRect.left, mDrawingRect.top, mSatValRect.right + BORDER_WIDTH_PX, mSatValRect.bottom + BORDER_WIDTH_PX, mBorderPaint);
			if (mValShader == null) {
				mValShader = new LinearGradient(mSatValRect.left, mSatValRect.top, mSatValRect.left, mSatValRect.bottom, 0xffffffff, 0xff000000, TileMode.CLAMP);
			}
			int rgb = Color.HSVToColor(new float[]{hsv[0], 1f, 1f});
			Shader mSatShader = new LinearGradient(mSatValRect.left, mSatValRect.top, mSatValRect.right, mSatValRect.top, 0xffffffff, rgb, TileMode.CLAMP);
			ComposeShader mShader = new ComposeShader(mValShader, mSatShader, PorterDuff.Mode.MULTIPLY);
			mSatValPaint.setShader(mShader);

			canvas.drawRect(mSatValRect, mSatValPaint);
			Point p = satValToPoint(hsv[1], hsv[2]);

			mSatValTrackerPaint.setColor(0xff000000);
			canvas.drawCircle(p.x, p.y, PaletteCircleTrackerRadius - mDensity, mSatValTrackerPaint);
			mSatValTrackerPaint.setColor(0xffdddddd);
			canvas.drawCircle(p.x, p.y, PaletteCircleTrackerRadius, mSatValTrackerPaint);
		}
	}


	private void drawHuePanel(Canvas canvas) {
		mBorderPaint.setColor(mBorderColor);
		if (mHueRect != null) {
			canvas.drawRect(mHueRect.left - BORDER_WIDTH_PX, mHueRect.top - BORDER_WIDTH_PX, mHueRect.right + BORDER_WIDTH_PX, mHueRect.bottom + BORDER_WIDTH_PX, mBorderPaint);
			if (mHueShader == null) {
				mHueShader = new LinearGradient(mHueRect.left, mHueRect.top, mHueRect.left, mHueRect.bottom, buildHueColorArray(), null, TileMode.CLAMP);
				mHuePaint.setShader(mHueShader);
			}
			canvas.drawRect(mHueRect, mHuePaint);

			float rectHeight = 4 * mDensity / 2;
			Point p = hueToPoint(hsv[0]);
			RectF r = new RectF();
			r.left = mHueRect.left - rectangleTrackerOffset;
			r.right = mHueRect.right + rectangleTrackerOffset;
			r.top = p.y - rectHeight;
			r.bottom = p.y + rectHeight;
			canvas.drawRoundRect(r, 2, 2, mHueTrackerPaint);
		}
	}


	private Point hueToPoint(float hue) {
		Point point = new Point();
		if (mHueRect != null) {
			float height = mHueRect.height();
			point.y = Math.round(height - hue * height / 360f + mHueRect.top);
			point.x = Math.round(mHueRect.left);
		}
		return point;
	}


	private Point satValToPoint(float sat, float val) {
		Point point = new Point();
		if (mSatValRect != null) {
			point.x = Math.round(sat * mSatValRect.width() + mSatValRect.left);
			point.y = Math.round((1f - val) * mSatValRect.height() + mSatValRect.top);
		}
		return point;
	}


	private float[] pointToSatVal(float x, float y) {
		float[] result = new float[2];
		if (mSatValRect != null) {
			float width = mSatValRect.width();
			float height = mSatValRect.height();
			if (x < mSatValRect.left) {
				x = 0f;
			} else if (x > mSatValRect.right) {
				x = width;
			} else {
				x = x - mSatValRect.left;
			}
			if (y < mSatValRect.top) {
				y = 0f;
			} else if (y > mSatValRect.bottom) {
				y = height;
			} else {
				y = y - mSatValRect.top;
			}
			if (width != 0) {
				result[0] = 1.f / width * x;
			}
			if (height != 0) {
				result[1] = 1.f - 1.f / height * y;
			}
		}
		return result;
	}


	private float pointToHue(float y) {
		if (mHueRect != null) {
			float height = mHueRect.height();
			if (y < mHueRect.top) {
				y = 0f;
			} else if (y > mHueRect.bottom) {
				y = height;
			} else {
				y = y - mHueRect.top;
			}
			if (height != 0) {
				return 360f - y * 360f / height;
			}
		}
		return 0f;
	}


	private boolean moveTrackersIfNeeded(MotionEvent event) {
		if (mStartTouchPoint == null || mHueRect == null || mSatValRect == null) {
			return false;
		}
		boolean update = false;
		int startX = mStartTouchPoint.x;
		int startY = mStartTouchPoint.y;

		if (mHueRect.contains(startX, startY)) {
			mLastTouchedPanel = PANEL_HUE;
			hsv[0] = pointToHue(event.getY());
			update = true;
		} else if (mSatValRect.contains(startX, startY)) {
			mLastTouchedPanel = PANEL_SAT_VAL;
			float[] result = pointToSatVal(event.getX(), event.getY());
			hsv[1] = result[0];
			hsv[2] = result[1];
			update = true;
		}
		return update;
	}


	private int chooseWidth(int mode, int size) {
		if (mode == MeasureSpec.AT_MOST || mode == MeasureSpec.EXACTLY) {
			return size;
		} else {
			return getPreferredWidth();
		}
	}


	private int chooseHeight(int mode, int size) {
		if (mode == MeasureSpec.AT_MOST || mode == MeasureSpec.EXACTLY) {
			return size;
		} else {
			return Math.round(200 * mDensity);
		}
	}


	private int getPreferredWidth() {
		int width = Math.round(200 * mDensity);
		return Math.round(width + hue_panel_width + panel_spacing);

	}

	/**
	 *
	 */
	public interface OnColorChangedListener {

		void onColorChanged(int color);
	}
}