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

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.cache.ImageFetcher;
import org.nuclearfog.apollo.ui.views.AlphaTouchInterceptorOverlay.OnOverlayClickListener;
import org.nuclearfog.apollo.utils.ImageUtils;

/**
 * a custom view representing a tab. Used by {@link ProfileTabCarousel}
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public class CarouselTab extends FrameLayout implements OnClickListener, OnOverlayClickListener {

	private ImageView mPhoto;
	private ImageView mAlbumArt;
	private TextView mLabelView;
	private View mColorstrip;
	private AlphaTouchInterceptorOverlay mOverlay;

	private ImageFetcher mFetcher;

	@Nullable
	private OnPhotoClickedListener photoClickListener;
	@Nullable
	private OnTabClickListener tabClickListener;

	/**
	 *
	 */
	public CarouselTab(@NonNull Context context) {
		this(context, null);
	}

	/**
	 * @param context The {@link Context} to use
	 * @param attrs   The attributes of the XML tag that is inflating the view.
	 */
	public CarouselTab(@NonNull Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		mOverlay = new AlphaTouchInterceptorOverlay(context);
		View view = LayoutInflater.from(context).inflate(R.layout.tab_profile, this, false);
		View mAlphaLayer = view.findViewById(R.id.profile_tab_alpha_overlay);
		mColorstrip = view.findViewById(R.id.profile_tab_colorstrip);
		mPhoto = view.findViewById(R.id.profile_tab_photo);
		mAlbumArt = view.findViewById(R.id.profile_tab_album_art);
		mLabelView = view.findViewById(R.id.profile_tab_label);
		mFetcher = new ImageFetcher(context);

		// add child views
		addView(view);
		addView(mOverlay);

		// Set the alpha layer
		mOverlay.setAlphaLayer(mAlphaLayer);

		mOverlay.setOnOverlayClickListener(this);
		mPhoto.setOnClickListener(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setSelected(boolean selected) {
		super.setSelected(selected);
		if (selected) {
			mColorstrip.setVisibility(View.VISIBLE);
		} else {
			mColorstrip.setVisibility(View.INVISIBLE);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.profile_tab_photo) {
			if (photoClickListener != null) {
				photoClickListener.onPhotoClick(this);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onOverlayClick(View v) {
		if (tabClickListener != null) {
			tabClickListener.onTabClick(this);
		}
	}

	/**
	 * sets photo of the first tab
	 *
	 * @param uri local uri of the image file
	 */
	public void setPhoto(Uri uri) {
		mPhoto.setImageURI(uri);
	}

	/**
	 * sets photo of the first tab
	 *
	 * @param bitmap bitmap of the image
	 */
	public void setPhoto(Bitmap bitmap) {
		if (mAlbumArt.getVisibility() != VISIBLE) {
			mPhoto.setImageDrawable(ImageUtils.createTransitionDrawable(getResources(), bitmap));
		} else {
			mAlbumArt.setImageDrawable(ImageUtils.createTransitionDrawable(getResources(), bitmap));
			mPhoto.setImageDrawable(ImageUtils.createBlurredDrawable(getResources(), bitmap));
		}
	}

	/**
	 * load album art from cache
	 *
	 * @param id album ID
	 */
	public void setAlbumImage(long id) {
		if (id != 0L) {
			mFetcher.loadAlbumImage(id, mAlbumArt, mPhoto);
			mAlbumArt.setVisibility(View.VISIBLE);
		} else {
			setDefault();
		}
	}

	/**
	 * load artist image from cache
	 *
	 * @param id artist ID
	 */
	public void setArtistImage(long id) {
		if (id != 0L) {
			mFetcher.loadArtistImage(id, mPhoto);
		} else {
			setDefault();
		}
	}

	/**
	 * load genre profile image from cache
	 *
	 * @param ids genre IDs
	 */
	public void setGenreImage(long[] ids) {
		if (ids.length > 0) {
			mFetcher.loadGenreImage(ids, mPhoto);
		} else {
			setDefault();
		}
	}

	/**
	 * load playlist image from cache
	 *
	 * @param id playlist ID
	 */
	public void setPlaylistImage(long id) {
		if (id != 0) {
			mFetcher.loadPlaylistImage(id, mPhoto);
		} else {
			setDefault();
		}
	}

	/**
	 * load folder image from cache
	 *
	 * @param folder folder path
	 */
	public void setFolderImage(String folder) {
		if (!TextUtils.isEmpty(folder)) {
			mFetcher.loadFolderImage(folder, mPhoto);
		} else {
			setDefault();
		}
	}

	/**
	 * set default photo
	 */
	public void setDefault() {
		mFetcher.setDefaultImage(mPhoto, mAlbumArt);
	}

	/**
	 * clear image
	 */
	public void removeImage() {
		mPhoto.setImageResource(0);
	}

	/**
	 * Delegate to overlay: set the alpha value on the alpha layer.
	 */
	public void setAlphaLayerValue(float alpha) {
		mOverlay.setAlphaLayerValue(alpha);
	}

	/**
	 * Delegate to overlay.
	 */
	public void setOverlayClickable(boolean clickable) {
		mOverlay.setOverlayClickable(clickable);
	}

	/**
	 * sets click listener for the first photo
	 */
	public void setOnPhotoClickedListener(OnPhotoClickedListener listener) {
		photoClickListener = listener;
	}

	/**
	 * sets click listener for a tab
	 */
	public void setOnTabClickListener(OnTabClickListener listener) {
		tabClickListener = listener;
	}

	/**
	 * sets the tab label
	 *
	 * @param res  string resource ID
	 */
	public void setLabel(@StringRes int res) {
		String label = getContext().getString(res);
		mLabelView.setText(label);
	}

	/**
	 * Selects the label view.
	 */
	public void showSelectedState() {
		mLabelView.setSelected(true);
	}

	/**
	 * Deselects the label view.
	 */
	public void showDeselectedState() {
		mLabelView.setSelected(false);
	}

	/**
	 * listener called if the profile photo was clicked
	 */
	public interface OnPhotoClickedListener {

		void onPhotoClick(View v);
	}

	/**
	 * listener called if the tab was clicked
	 */
	public interface OnTabClickListener {

		void onTabClick(View v);
	}
}