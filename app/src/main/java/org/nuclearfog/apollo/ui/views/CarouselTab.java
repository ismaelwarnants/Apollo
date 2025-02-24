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
import android.net.Uri;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.cache.ImageFetcher;

/**
 * a custom view representing a tab. Used by {@link ProfileTabCarousel}
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public class CarouselTab extends FrameLayoutWithOverlay implements OnClickListener {

	private ImageView mPhoto;
	private ImageView mAlbumArt;
	private TextView mLabelView;
	private View mColorstrip;

	private ImageFetcher mFetcher;

	@Nullable
	private OnPhotoClickedListener listener;

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
		View view = LayoutInflater.from(context).inflate(R.layout.tab_profile, this, false);
		View mAlphaLayer = view.findViewById(R.id.profile_tab_alpha_overlay);
		mColorstrip = view.findViewById(R.id.profile_tab_colorstrip);
		mPhoto = view.findViewById(R.id.profile_tab_photo);
		mAlbumArt = view.findViewById(R.id.profile_tab_album_art);
		mLabelView = view.findViewById(R.id.profile_tab_label);
		mFetcher = new ImageFetcher(context);
		// add child views
		addView(view);
		// Set the alpha layer
		setAlphaLayer(mAlphaLayer);

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
			if (listener != null) {
				listener.onPhotoClicked();
			}
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
	 * load album art from cache
	 *
	 * @param id     album ID
	 * @param artist artist name of the album
	 * @param album  album name
	 */
	public void setAlbumImage(long id, String artist, String album) {
		if (!TextUtils.isEmpty(album) && !TextUtils.isEmpty(artist)) {
			mFetcher.loadAlbumImage(artist, album, id, mAlbumArt, mPhoto);
			mAlbumArt.setVisibility(View.VISIBLE);
		} else {
			setDefault();
		}
	}

	/**
	 * load artist image from cache
	 *
	 * @param artist artist name
	 */
	public void setArtistImage(String artist) {
		if (!TextUtils.isEmpty(artist)) {
			mFetcher.loadArtistImage(artist, mPhoto);
		} else {
			setDefault();
		}
	}

	/**
	 * load genre image from cache
	 *
	 * @param genre genre name
	 */
	public void setGenreImage(String genre) {
		if (!TextUtils.isEmpty(genre)) {
			mFetcher.loadGenreImage(genre, mPhoto);
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
		mPhoto.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.header_temp));
	}

	/**
	 * sets click listener for the first photo
	 */
	public void setOnPhotoClickedListener(OnPhotoClickedListener listener) {
		this.listener = listener;
	}

	/**
	 * @param label The string to set as the label.
	 */
	public void setLabel(String label) {
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

		void onPhotoClicked();
	}
}