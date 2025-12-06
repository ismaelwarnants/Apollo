package org.nuclearfog.apollo.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A View that other Views can use to create a touch-interceptor layer above
 * their other sub-views. This layer can be enabled and disabled; when enabled,
 * clicks are intercepted and passed to a listener. Also supports an alpha layer
 * to dim the content underneath. By default, the alpha layer is the same View
 * as the touch-interceptor layer. However, for some use-cases, you want a few
 * Views to not be dimmed, but still have touches intercepted (for example,
 * {@link CarouselTab}'s label appears above the alpha layer). In this case, you
 * can specify the View to use as the alpha layer via setAlphaLayer(); in this
 * case you are responsible for managing the z-order of the alpha-layer with
 * respect to your other sub-views. Typically, you would not use this class
 * directly, but rather use another class that uses it.
 */
public class AlphaTouchInterceptorOverlay extends FrameLayout implements OnClickListener {

	private View mInterceptorLayer;
	private View mAlphaLayer = this;

	@Nullable
	private OnOverlayClickListener listener;
	private float mAlpha = 0.0f;

	/**
	 *
	 */
	public AlphaTouchInterceptorOverlay(@NonNull Context context) {
		this(context, null);
	}

	/**
	 *
	 */
	public AlphaTouchInterceptorOverlay(@NonNull Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		mInterceptorLayer = new View(context);
		addView(mInterceptorLayer);

		mInterceptorLayer.setBackgroundColor(0);
		mInterceptorLayer.setOnClickListener(this);
	}


	@Override
	public void onClick(View v) {
		if (listener != null) {
			listener.onOverlayClick();
		}
	}

	/**
	 * Set the View that the overlay will use as its alpha-layer. If none is set
	 * it will use itself. Only necessary to set this if some child views need
	 * to appear above the alpha-layer but below the touch-interceptor.
	 */
	public void setAlphaLayer(@NonNull View alphaLayer) {
		mAlphaLayer = alphaLayer;
		setAlphaLayerValue(mAlpha);
	}

	/**
	 * Sets the alpha value on the alpha layer.
	 *
	 * @param alpha transparency value between 0.0 and 1.0
	 */
	public void setAlphaLayerValue(float alpha) {
		mAlpha = Math.max(0f, Math.min(1f, alpha));
		mAlphaLayer.setBackgroundColor(Math.round(alpha * 255) << 24);
	}

	/**
	 * Delegate to interceptor-layer.
	 */
	public void setOverlayClickable(boolean clickable) {
		mInterceptorLayer.setClickable(clickable);
	}

	/**
	 * sets an overlay click listener
	 */
	public void setOnOverlayClickListener(@Nullable OnOverlayClickListener listener) {
		this.listener = listener;
	}

	/**
	 * click listener for the alpha overlay
	 */
	public interface OnOverlayClickListener {

		/**
		 * called if the overlay view was clicked
		 */
		void onOverlayClick();
	}
}