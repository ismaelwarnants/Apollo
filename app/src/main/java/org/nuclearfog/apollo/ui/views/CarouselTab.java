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
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.cache.ImageFetcher;
import org.nuclearfog.apollo.cache.ImageFetcher.ImageType;
import org.nuclearfog.apollo.utils.MusicUtils;

/**
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public class CarouselTab extends FrameLayoutWithOverlay {

	private ImageView mPhoto;
	private ImageView mAlbumArt;
	private TextView mLabelView;
	private View mColorstrip;

	private ImageFetcher mFetcher;

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
	 * Used to set the artist image in the artist profile.
	 *
	 * @param artist The name of the artist in the profile the user is viewing.
	 */
	public void setPhoto(ImageType type, String artist, String album) {
		switch (type) {
			case ARTIST:
				if (!TextUtils.isEmpty(artist)) {
					mFetcher.loadArtistImage(artist, mPhoto);
				} else {
					setDefault();
				}
				break;

			case ALBUM:
				if (!TextUtils.isEmpty(album)) {
					mFetcher.loadAlbumImage(artist, album, MusicUtils.getIdForAlbum(getContext(), album, artist), mAlbumArt, mPhoto);
					mAlbumArt.setVisibility(View.VISIBLE);
				} else {
					setDefault();
				}
				break;

			case PLAYLIST:
			case GENRE:/*
				if (!TextUtils.isEmpty(profileName)) {
					Bitmap image = mFetcher.getCachedBitmap(type, profileName);
					if (image != null) {
						mPhoto.setImageBitmap(image);
					} else {
						setDefault();
					}
				} else {
					setDefault();
				}*/
				break;

		}
	}

	/**
	 *
	 */
	public void setDefault() {
		mPhoto.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.header_temp));
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
	 * @return The {@link ImageView} used to set the header photo.
	 */
	public ImageView getPhoto() {
		return mPhoto;
	}

	/**
	 * @return The {@link ImageView} used to set the album art .
	 */
	public ImageView getAlbumArt() {
		return mAlbumArt;
	}
}