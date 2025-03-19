/*
 * Copyright (C) 2012 Andrew Neal Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package org.nuclearfog.apollo.ui.views;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.HorizontalScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.ui.activities.ProfileActivity;
import org.nuclearfog.apollo.ui.views.CarouselTab.OnPhotoClickedListener;
import org.nuclearfog.apollo.ui.views.CarouselTab.OnTabClickListener;
import org.nuclearfog.apollo.utils.AnimatorUtils;
import org.nuclearfog.apollo.utils.ImageUtils;

/**
 * A custom {@link HorizontalScrollView} that displays up to two "tabs" in the
 * {@link ProfileActivity}. If the second tab is visible, a fraction of it will
 * overflow slightly onto the screen.
 *
 * @author nuclearfog
 * @see CarouselTab
 */
public class ProfileTabCarousel extends HorizontalScrollView implements OnTouchListener, OnGlobalLayoutListener, AnimatorListener, OnPhotoClickedListener, OnTabClickListener {

	/**
	 * Alpha layer to be set on the layer view
	 */
	private static final float MAX_ALPHA = 0.6f;

	/**
	 * Y coordinate of the tab at the given index was selected
	 */
	private final float[] mYCoordinateArray = new float[2];

	/**
	 * Tab width as defined as a fraction of the screen width
	 */
	private final float tabWidthScreenWidthFraction;

	/**
	 * Tab height as defined as a fraction of the screen width
	 */
	private final float tabHeightScreenWidthFraction;

	/**
	 * Height of the tab label
	 */
	private final int mTabDisplayLabelHeight;

	/**
	 * Height in pixels of the shadow under the tab carousel
	 */
	private final int mTabShadowHeight;

	/**
	 * The last scrolled position
	 */
	private int mLastScrollPosition = 0;

	/**
	 * Allowed horizontal scroll duration
	 */
	private int mAllowedHorizontalScrollLength = Integer.MIN_VALUE;

	/**
	 * Allowed vertical scroll duration
	 */
	private int mAllowedVerticalScrollLength = Integer.MIN_VALUE;

	/**
	 * Current tab index
	 */
	private int mCurrentTab = 0;

	/**
	 * Factor to scale scroll-amount sent to listeners
	 */
	private float mScrollScaleFactor = 1.0f;

	private boolean mScrollToCurrentTab = false;
	private boolean mTabCarouselIsAnimating;
	private boolean mEnableSwipe;

	private CarouselTab mFirstTab, mSecondTab;

	@Nullable
	private OnTabChangeListener mListener;

	/**
	 *
	 */
	public ProfileTabCarousel(@NonNull Context context) {
		this(context, null);
	}

	/**
	 * @param context The {@link Context} to use
	 * @param attrs   The attributes of the XML tag that is inflating the view.
	 */
	public ProfileTabCarousel(@NonNull Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		View view = LayoutInflater.from(context).inflate(R.layout.tab_carousel, this, false);
		mFirstTab = view.findViewById(R.id.profile_tab_carousel_tab_one);
		mSecondTab = view.findViewById(R.id.profile_tab_carousel_tab_two);
		addView(view);
		updateAlphaLayers();

		mTabDisplayLabelHeight = context.getResources().getDimensionPixelSize(R.dimen.profile_photo_shadow_height);
		mTabShadowHeight = context.getResources().getDimensionPixelSize(R.dimen.profile_carousel_label_height);
		tabWidthScreenWidthFraction = context.getResources().getFraction(R.fraction.tab_width_screen_percentage, 1, 1);
		tabHeightScreenWidthFraction = context.getResources().getFraction(R.fraction.tab_height_screen_percentage, 1, 1);
		// disable scroll effects which can cause bugs
		setHorizontalScrollBarEnabled(false);
		setHorizontalFadingEdgeEnabled(false);
		setOverScrollMode(OVER_SCROLL_NEVER);
		setCurrentTab(0);

		mFirstTab.setOnPhotoClickedListener(this);
		mFirstTab.setOnTabClickListener(this);
		mSecondTab.setOnTabClickListener(this);
		setOnTouchListener(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int screenWidth = MeasureSpec.getSize(widthMeasureSpec);
		int tabWidth = Math.round(tabWidthScreenWidthFraction * screenWidth);
		int tabHeight = Math.round(screenWidth * tabHeightScreenWidthFraction) + mTabShadowHeight;
		mAllowedHorizontalScrollLength = tabWidth * mYCoordinateArray.length - screenWidth;
		if (mAllowedHorizontalScrollLength == 0) {
			mScrollScaleFactor = 1.0f;
		} else {
			mScrollScaleFactor = (float) screenWidth / mAllowedHorizontalScrollLength;
		}
		if (getChildCount() > 0) {
			View child = getChildAt(0);
			if (mEnableSwipe) {
				// Add 1 dip of separation between the tabs
				int separatorPixels = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics()) + 0.5f);
				int width = MeasureSpec.makeMeasureSpec(mYCoordinateArray.length * tabWidth + separatorPixels, MeasureSpec.EXACTLY);
				int height = MeasureSpec.makeMeasureSpec(tabHeight, MeasureSpec.EXACTLY);
				child.measure(width, height);
			} else {
				int width = MeasureSpec.makeMeasureSpec(screenWidth, MeasureSpec.EXACTLY);
				int height = MeasureSpec.makeMeasureSpec(tabHeight, MeasureSpec.EXACTLY);
				child.measure(width, height);
			}
		}
		mAllowedVerticalScrollLength = tabHeight - mTabDisplayLabelHeight - mTabShadowHeight;
		setMeasuredDimension(resolveSize(screenWidth, widthMeasureSpec), resolveSize(tabHeight, heightMeasureSpec));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);
		if (mScrollToCurrentTab) {
			mScrollToCurrentTab = false;
			getViewTreeObserver().addOnGlobalLayoutListener(this);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onScrollChanged(int x, int y, int oldX, int oldY) {
		super.onScrollChanged(x, y, oldX, oldY);
		if (mLastScrollPosition != x) {
			int scaledL = (int) (x * mScrollScaleFactor);
			int oldScaledL = (int) (oldX * mScrollScaleFactor);
			if (mListener != null) {
				mListener.onScrollChanged(scaledL, oldScaledL);
			}
			mLastScrollPosition = x;
			updateAlphaLayers();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (mListener != null) {
			switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					mListener.onTouchDown();
					return true;

				case MotionEvent.ACTION_UP:
					mListener.onTouchUp();
					return true;
			}
		}
		return super.onTouchEvent(event);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		boolean mInterceptTouch = super.onInterceptTouchEvent(ev);
		if (mInterceptTouch && mListener != null) {
			mListener.onTouchDown();
		}
		return mInterceptTouch;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onGlobalLayout() {
		/* Layout pass done, unregister for further events */
		getViewTreeObserver().removeOnGlobalLayoutListener(this);
		scrollTo(mCurrentTab == 0 ? 0 : mAllowedHorizontalScrollLength, 0);
		updateAlphaLayers();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onAnimationCancel(@NonNull Animator animation) {
		mTabCarouselIsAnimating = false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onAnimationEnd(@NonNull Animator animation) {
		mTabCarouselIsAnimating = false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onAnimationRepeat(@NonNull Animator animation) {
		mTabCarouselIsAnimating = true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onAnimationStart(@NonNull Animator animation) {
		mTabCarouselIsAnimating = true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onPhotoClick(View v) {
		if (v.getId() == R.id.profile_tab_carousel_tab_one) {
			if (mListener != null) {
				mListener.onArtworkSelected();
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onTabClick(View view) {
		if (mListener != null) {
			if (view.getId() == R.id.profile_tab_carousel_tab_one) {
				mListener.onTabSelected(0);
			} else if (view.getId() == R.id.profile_tab_carousel_tab_two) {
				mListener.onTabSelected(1);
			}
		}
	}

	/**
	 * Reset the carousel to the start position (i.e. because new data will be
	 * loaded in for a different contact).
	 */
	public void reset() {
		scrollTo(0, 0);
		setCurrentTab(0);
		moveToYCoordinate(0, 0);
	}

	/**
	 * Restore the Y position of this view to the last manually requested value.
	 * This can be done after the parent has been re-laid out again, where this
	 * view's position could have been lost if the view laid outside its
	 * parent's bounds.
	 */
	public void restoreYCoordinate(int duration, int tabIndex) {
		float storedYCoordinate = getStoredYCoordinateForTab(tabIndex);
		AnimatorUtils.translate(this, storedYCoordinate, duration, this);
	}

	/**
	 * Request that the view move to the given Y coordinate. Also store the Y
	 * coordinate as the last requested Y coordinate for the given tabIndex.
	 */
	public void moveToYCoordinate(int tabIndex, float y) {
		storeYCoordinate(tabIndex, y);
		restoreYCoordinate(0, tabIndex);
	}

	/**
	 * Returns the number of pixels that this view can be scrolled horizontally.
	 */
	public int getAllowedHorizontalScrollLength() {
		return mAllowedHorizontalScrollLength;
	}

	/**
	 * Returns the number of pixels that this view can be scrolled vertically
	 * while still allowing the tab labels to still show.
	 */
	public int getAllowedVerticalScrollLength() {
		return mAllowedVerticalScrollLength;
	}

	/**
	 * set image of the album art
	 *
	 * @param uri path to the local image file
	 */
	public void setAlbumArt(Uri uri) {
		mFirstTab.setPhoto(uri);
	}

	/**
	 * set image of the album art
	 *
	 * @param bitmap bitmap of the artwork
	 */
	public void setAlbumArt(Bitmap bitmap) {
		mFirstTab.setPhoto(ImageUtils.createTransitionDrawable(getResources(), bitmap));
		updateAlphaLayers();
	}

	/**
	 * @return True if the carousel is currently animating, false otherwise
	 */
	public boolean isTabCarouselIsAnimating() {
		return mTabCarouselIsAnimating;
	}

	/**
	 * Updates the tab selection.
	 */
	public void setCurrentTab(int position) {
		CarouselTab selected, deselected;
		switch (position) {
			case 0:
				selected = mFirstTab;
				deselected = mSecondTab;
				break;

			case 1:
				selected = mSecondTab;
				deselected = mFirstTab;
				break;

			default:
				return;
		}
		selected.setSelected(true);
		selected.showSelectedState();
		selected.setOverlayClickable(false);
		deselected.setSelected(false);
		deselected.showDeselectedState();
		deselected.setOverlayClickable(true);
		mCurrentTab = position;
	}

	/**
	 * Set the given {@link OnTabChangeListener} to handle carousel events.
	 */
	public void setListener(@Nullable OnTabChangeListener listener) {
		mListener = listener;
	}

	/**
	 * get the current tab position
	 */
	public int getCurrentTab() {
		return mCurrentTab;
	}

	/**
	 * Sets the artist image header
	 *
	 * @param artistId MediaStore ID of the artist
	 */
	public void setArtistProfileHeader(long artistId) {
		mFirstTab.setLabel(R.string.page_songs);
		mSecondTab.setLabel(R.string.page_albums);
		mFirstTab.setArtistImage(artistId);
		mEnableSwipe = true;
	}

	/**
	 * Sets the album image header
	 *
	 * @param albumId MediaStore ID of the album
	 */
	public void setAlbumProfileHeader(long albumId) {
		mFirstTab.setLabel(R.string.page_songs);
		mFirstTab.setAlbumImage(albumId);
		mSecondTab.setVisibility(View.GONE);
		mEnableSwipe = false;
	}

	/**
	 * Sets the playlist image header
	 *
	 * @param id MediaStore playlist ID
	 */
	public void setPlaylistProfileHeader(long id) {
		mFirstTab.setDefault();
		mFirstTab.setLabel(R.string.page_songs);
		mFirstTab.setPlaylistImage(id);
		mSecondTab.setVisibility(View.GONE);
		mEnableSwipe = false;
	}

	/**
	 * Sets the genre image header
	 *
	 * @param ids grouped MediaStore genre IDs
	 */
	public void setGenreProfileHeader(long[] ids) {
		mFirstTab.setDefault();
		mFirstTab.setLabel(R.string.page_songs);
		mFirstTab.setGenreImage(ids);
		mSecondTab.setVisibility(View.GONE);
		mEnableSwipe = false;
	}

	/**
	 * Sets the music folder image header
	 *
	 * @param folder folder path
	 */
	public void setFolderProfileHeader(String folder) {
		mFirstTab.setDefault();
		mFirstTab.setLabel(R.string.page_songs);
		mFirstTab.setFolderImage(folder);
		mSecondTab.setVisibility(View.GONE);
		mEnableSwipe = false;
	}

	/**
	 * clear current images
	 */
	public void setDefault() {
		mFirstTab.setDefault();
		mSecondTab.removeImage();
	}

	/**
	 * Store this information as the last requested Y coordinate for the given
	 * tabIndex.
	 */
	private void storeYCoordinate(int tabIndex, float y) {
		mYCoordinateArray[tabIndex] = y;
	}

	/**
	 * Returns the stored Y coordinate of this view the last time the user was
	 * on the selected tab given by tabIndex.
	 */
	private float getStoredYCoordinateForTab(int tabIndex) {
		return mYCoordinateArray[tabIndex];
	}

	/**
	 * Sets the correct alpha layers over the tabs.
	 */
	private void updateAlphaLayers() {
		float alpha = mLastScrollPosition * MAX_ALPHA / mAllowedHorizontalScrollLength;
		mFirstTab.setAlphaLayerValue(alpha);
		mSecondTab.setAlphaLayerValue(MAX_ALPHA - alpha);
	}

	/**
	 * Interface for callbacks invoked when the user interacts with the carousel.
	 */
	public interface OnTabChangeListener {

		void onTouchDown();

		void onTouchUp();

		void onScrollChanged(int l, int oldL);

		void onTabSelected(int position);

		void onArtworkSelected();
	}
}