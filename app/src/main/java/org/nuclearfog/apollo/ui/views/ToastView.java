package org.nuclearfog.apollo.ui.views;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import org.nuclearfog.apollo.R;

/**
 * A simple view shown at the top of an activity view containing a text message.
 *
 * @see org.nuclearfog.apollo.utils.ApolloUtils#showToast(Activity, String, boolean, long)
 * @author nuclearfog
 */
public class ToastView extends LinearLayout {

	/**
	 * minimum height of this view
	 */
	private static final float HEIGHT_DP = 48f;
	/**
	 * text size of the message
	 */
	private static final float TEXT_SIZE = 16f;

	private TextView tv;

	/**
	 * {@inheritDoc}
	 */
	public ToastView(Context context) {
		this(context, null);
	}

	/**
	 * {@inheritDoc}
	 */
	public ToastView(Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		float scale = getResources().getDisplayMetrics().density;
		tv = new TextView(context);
		tv.setMaxLines(2);
		tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE);
		tv.setEllipsize(TextUtils.TruncateAt.END);
		tv.setTextColor(Color.BLACK);
		tv.setTypeface(null, Typeface.BOLD);
		setBackgroundColor(getResources().getColor(R.color.confirm));
		setGravity(Gravity.CENTER);
		setMinimumHeight(Math.round(HEIGHT_DP * scale));
		addView(tv);
		setAlert(false);
	}

	/**
	 * set message text to show
	 *
	 * @param text text message
	 */
	public void setText(String text) {
		tv.setText(text);
	}

	/**
	 * set alert type
	 *
	 * @param alert true to show red alert view, false to show yellow info view
	 */
	public void setAlert(boolean alert) {
		if (alert) {
			setBackgroundColor(getResources().getColor(R.color.alert));
		} else {
			setBackgroundColor(getResources().getColor(R.color.confirm));
		}
	}
}