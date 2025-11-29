package org.nuclearfog.apollo.ui.views.dragdrop;

import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;

import org.nuclearfog.apollo.ui.views.ProfileTabCarousel;

/**
 *
 */
public class VerticalScrollController implements OnScrollListener {

	/**
	 * Used to determine the off set to scroll the header
	 */
	private ScrollableHeader mHeader;
	private ProfileTabCarousel mTabCarousel;

	private int mPageIndex;

	/**
	 *
	 */
	public VerticalScrollController(ScrollableHeader header, ProfileTabCarousel carousel, int pageIndex) {
		mHeader = header;
		mTabCarousel = carousel;
		mPageIndex = pageIndex;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		if (mTabCarousel == null || mTabCarousel.isTabCarouselIsAnimating()) {
			return;
		}
		if (view.getChildAt(firstVisibleItem) == null) {
			return;
		}
		if (firstVisibleItem != 0) {
			mTabCarousel.moveToYCoordinate(mPageIndex, -mTabCarousel.getAllowedVerticalScrollLength());
			return;
		}
		float y = view.getChildAt(firstVisibleItem).getY();
		float amtToScroll = Math.max(y, -mTabCarousel.getAllowedVerticalScrollLength());
		mTabCarousel.moveToYCoordinate(mPageIndex, amtToScroll);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		if (mHeader != null) {
			mHeader.onScrollStateChanged(scrollState);
		}
	}

	/**
	 * Defines the header to be scrolled.
	 */
	public interface ScrollableHeader {

		/**
		 * Used the pause the disk cache while scrolling
		 */
		void onScrollStateChanged(int scrollState);
	}
}