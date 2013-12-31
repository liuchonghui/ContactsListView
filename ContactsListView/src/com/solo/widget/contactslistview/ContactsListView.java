package com.solo.widget.contactslistview;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.GradientDrawable.Orientation;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.Adapter;
import android.widget.HeaderViewListAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SectionIndexer;

public class ContactsListView extends ListView implements OnScrollListener {
	private boolean mIsFastScrollEnabled = false;
	private boolean mIsSpinnedShadowEnabled = false;
	private IndexScroller mScroller = null;
	private GestureDetector mGestureDetector = null;

	static class PinnedSection {
		public View view;
		public int position;
		public long id;
	}

	// private View transparentView;
	PinnedSection mPinnedSection;
	PinnedSection mRecycleSection;
	private final Rect mTouchRect = new Rect();
	private final PointF mTouchPoint = new PointF();
	private int mTouchSlop;
	private View mTouchTarget;
	private MotionEvent mDownEvent;

	int mTranslateY;
	private GradientDrawable mShadowDrawable;
	private int mSectionsDistanceY;
	private int mShadowHeight;

	public ContactsListView(final Context context, final AttributeSet attrs,
			final int defStyle) {
		super(context, attrs, defStyle);
		commonInitialisation();
	}

	public ContactsListView(final Context context, final AttributeSet attrs) {
		super(context, attrs);
		commonInitialisation();
	}

	public ContactsListView(final Context context) {
		super(context);
		commonInitialisation();
	}

	protected final void commonInitialisation() {
		setOnScrollListener(this);
		setVerticalFadingEdgeEnabled(false);
		setFadingEdgeLength(0);

		initShadow(true);
	}

	public void initShadow(boolean visible) {
		if (visible) {
			if (mShadowDrawable == null) {
				mShadowDrawable = new GradientDrawable(Orientation.TOP_BOTTOM,
						new int[] { Color.parseColor("#ffa0a0a0"),
								Color.parseColor("#50a0a0a0"),
								Color.parseColor("#00a0a0a0") });
				mShadowHeight = (int) (8 * getResources().getDisplayMetrics().density);
			}
		} else {
			if (mShadowDrawable != null) {
				mShadowDrawable = null;
				mShadowHeight = 0;
			}
		}
	}

	public boolean isSpinnedShadowEnabled() {
		return mIsSpinnedShadowEnabled;
	}

	public void setSpinnedShadowEnabled(boolean enabled) {
		mIsSpinnedShadowEnabled = enabled;
	}

	@Override
	public boolean isFastScrollEnabled() {
		return mIsFastScrollEnabled;
	}

	@Override
	public void setFastScrollEnabled(boolean enabled) {
		mIsFastScrollEnabled = enabled;
		if (mIsFastScrollEnabled) {
			if (mScroller == null) {
				mScroller = new IndexScroller(getContext(), this);
			}
		} else {
			if (mScroller != null) {
				mScroller.hide();
				mScroller = null;
			}
		}
	}

	@Override
	public void draw(Canvas canvas) {
		super.draw(canvas);

		if (mScroller != null) {
			mScroller.draw(canvas);
		}
	}

	@Override
	protected void dispatchDraw(Canvas canvas) {
		super.dispatchDraw(canvas);

		if (mPinnedSection != null) {

			// prepare variables
			int pLeft = getListPaddingLeft();
			int pTop = getListPaddingTop();
			View view = mPinnedSection.view;

			// draw child
			canvas.save();

			int clipHeight = view.getHeight()
					+ (mShadowDrawable == null ? 0 : Math.min(mShadowHeight,
							mSectionsDistanceY));
			canvas.clipRect(pLeft, pTop, pLeft + view.getWidth(), pTop
					+ clipHeight);

			canvas.translate(pLeft, pTop + mTranslateY);
			drawChild(canvas, mPinnedSection.view, getDrawingTime());

			if (mShadowDrawable != null && mSectionsDistanceY > 0) {
				mShadowDrawable.setBounds(mPinnedSection.view.getLeft(),
						mPinnedSection.view.getBottom(),
						mPinnedSection.view.getRight(),
						mPinnedSection.view.getBottom() + mShadowHeight);
				mShadowDrawable.draw(canvas);
			}

			canvas.restore();
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		// Intercept ListView's touch event
		if (mScroller != null && mScroller.onTouchEvent(ev))
			return true;

		if (mGestureDetector == null) {
			mGestureDetector = new GestureDetector(getContext(),
					new GestureDetector.SimpleOnGestureListener() {

						@Override
						public boolean onFling(MotionEvent e1, MotionEvent e2,
								float velocityX, float velocityY) {
							// If fling happens, index bar shows
							if (mScroller != null) {
								mScroller.show();
							}
							return super.onFling(e1, e2, velocityX, velocityY);
						}

					});
		}
		mGestureDetector.onTouchEvent(ev);

		return super.onTouchEvent(ev);
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {

		final float x = ev.getX();
		final float y = ev.getY();
		final int action = ev.getAction();

		if (action == MotionEvent.ACTION_DOWN && mTouchTarget == null
				&& mPinnedSection != null
				&& isPinnedViewTouched(mPinnedSection.view, x, y)) { // create
																		// touch
																		// target

			// user touched pinned view
			mTouchTarget = mPinnedSection.view;
			mTouchPoint.x = x;
			mTouchPoint.y = y;

			// copy down event for eventually be used later
			mDownEvent = MotionEvent.obtain(ev);
		}

		if (mTouchTarget != null) {
			if (isPinnedViewTouched(mTouchTarget, x, y)) { // forward event to
															// pinned view
				mTouchTarget.dispatchTouchEvent(ev);
			}

			if (action == MotionEvent.ACTION_UP) { // perform onClick on pinned
													// view
				super.dispatchTouchEvent(ev);
				performPinnedItemClick();
				clearTouchTarget();

			} else if (action == MotionEvent.ACTION_CANCEL) { // cancel
				clearTouchTarget();

			} else if (action == MotionEvent.ACTION_MOVE) {
				if (Math.abs(y - mTouchPoint.y) > mTouchSlop) {

					// cancel sequence on touch target
					MotionEvent event = MotionEvent.obtain(ev);
					event.setAction(MotionEvent.ACTION_CANCEL);
					mTouchTarget.dispatchTouchEvent(event);
					event.recycle();

					// provide correct sequence to super class for further
					// handling
					super.dispatchTouchEvent(mDownEvent);
					super.dispatchTouchEvent(ev);
					clearTouchTarget();

				}
			}

			return true;
		}

		// call super if this was not our pinned view
		return super.dispatchTouchEvent(ev);
	}

	private boolean isPinnedViewTouched(View view, float x, float y) {
		view.getHitRect(mTouchRect);

		// by taping top or bottom padding, the list performs on click on a
		// border item.
		// we don't add top padding here to keep behavior consistent.
		mTouchRect.top += mTranslateY;

		mTouchRect.bottom += mTranslateY + getPaddingTop();
		mTouchRect.left += getPaddingLeft();
		mTouchRect.right -= getPaddingRight();
		return mTouchRect.contains((int) x, (int) y);
	}

	private void clearTouchTarget() {
		mTouchTarget = null;
		if (mDownEvent != null) {
			mDownEvent.recycle();
			mDownEvent = null;
		}
	}

	private boolean performPinnedItemClick() {
		if (mPinnedSection == null)
			return false;

		OnItemClickListener listener = getOnItemClickListener();
		if (listener != null) {
			View view = mPinnedSection.view;
			playSoundEffect(SoundEffectConstants.CLICK);
			if (view != null) {
				view.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED);
			}
			listener.onItemClick(this, view, mPinnedSection.position,
					mPinnedSection.id);
			return true;
		}
		return false;
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		if (mScroller != null) {
			mScroller.onSizeChanged(w, h, oldw, oldh);
		}
	}

	/** Default change observer. */
	private final DataSetObserver mDataSetObserver = new DataSetObserver() {
		@Override
		public void onChanged() {
			recreatePinnedShadow();
		};

		@Override
		public void onInvalidated() {
			recreatePinnedShadow();
		}
	};

	void recreatePinnedShadow() {
		destroyPinnedShadow();
		ListAdapter adapter = getAdapter();
		if (adapter != null && adapter.getCount() > 0) {
			int firstVisiblePosition = getFirstVisiblePosition();
			int sectionPosition = findCurrentSectionPosition(firstVisiblePosition);
			if (sectionPosition == -1)
				return; // no views to pin, exit
			ensureShadowForPosition(sectionPosition, firstVisiblePosition,
					getLastVisiblePosition() - firstVisiblePosition);
		}
	}

	/** Makes sure we have an actual pinned shadow for given position. */
	void ensureShadowForPosition(int sectionPosition, int firstVisibleItem,
			int visibleItemCount) {
		if (visibleItemCount < 2) { // no need for creating shadow at all, we
									// have a single visible item
			destroyPinnedShadow();
			return;
		}

		if (mPinnedSection != null
				&& mPinnedSection.position != sectionPosition) { // invalidate
																	// shadow,
																	// if
																	// required
			destroyPinnedShadow();
		}

		if (mPinnedSection == null) { // create shadow, if empty
			createPinnedShadow(sectionPosition);
		}

		// align shadow according to next section position, if needed
		int nextPosition = sectionPosition + 1;
		if (nextPosition < getCount()) {
			int nextSectionPosition = findFirstVisibleSectionPosition(
					nextPosition, visibleItemCount
							- (nextPosition - firstVisibleItem));
			if (nextSectionPosition > -1) {
				View nextSectionView = getChildAt(nextSectionPosition
						- firstVisibleItem);
				final int bottom = mPinnedSection.view.getBottom()
						+ getPaddingTop();
				mSectionsDistanceY = nextSectionView.getTop() - bottom;
				if (mSectionsDistanceY < 0) {
					// next section overlaps pinned shadow, move it up
					mTranslateY = mSectionsDistanceY;
				} else {
					// next section does not overlap with pinned, stick to top
					mTranslateY = 0;
				}
			} else {
				// no other sections are visible, stick to top
				mTranslateY = 0;
				mSectionsDistanceY = Integer.MAX_VALUE;
			}
		}

	}

	int findFirstVisibleSectionPosition(int firstVisibleItem,
			int visibleItemCount) {
		ListAdapter adapter = getAdapter();
		for (int childIndex = 0; childIndex < visibleItemCount; childIndex++) {
			int position = firstVisibleItem + childIndex;
			if (((ContactsListAdapter) adapter).isSection(position)) {
				return position;
			}
		}
		return -1;
	}

	/** Create shadow wrapper with a pinned view for a view at given position */
	void createPinnedShadow(int position) {

		// try to recycle shadow
		PinnedSection pinnedShadow = mRecycleSection;
		mRecycleSection = null;

		// create new shadow, if needed
		if (pinnedShadow == null)
			pinnedShadow = new PinnedSection();
		// request new view using recycled view, if such
		View pinnedView = getAdapter().getView(position, pinnedShadow.view,
				this);

		// read layout parameters
		LayoutParams layoutParams = (LayoutParams) pinnedView.getLayoutParams();
		if (layoutParams == null) { // create default layout params
			layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT,
					LayoutParams.WRAP_CONTENT);
		}

		int heightMode = MeasureSpec.getMode(layoutParams.height);
		int heightSize = MeasureSpec.getSize(layoutParams.height);

		if (heightMode == MeasureSpec.UNSPECIFIED)
			heightMode = MeasureSpec.EXACTLY;

		int maxHeight = getHeight() - getListPaddingTop()
				- getListPaddingBottom();
		if (heightSize > maxHeight)
			heightSize = maxHeight;

		// measure & layout
		int ws = MeasureSpec.makeMeasureSpec(getWidth() - getListPaddingLeft()
				- getListPaddingRight(), MeasureSpec.EXACTLY);
		int hs = MeasureSpec.makeMeasureSpec(heightSize, heightMode);
		pinnedView.measure(ws, hs);
		pinnedView.layout(0, 0, pinnedView.getMeasuredWidth(),
				pinnedView.getMeasuredHeight());
		mTranslateY = 0;

		// initialize pinned shadow
		pinnedShadow.view = pinnedView;
		pinnedShadow.position = position;
		pinnedShadow.id = getAdapter().getItemId(position);

		// store pinned shadow
		mPinnedSection = pinnedShadow;
	}

	int findCurrentSectionPosition(int fromPosition) {
		ListAdapter adapter = getAdapter();
		for (int position = fromPosition; position >= 0; position--) {
			// int viewType = adapter.getItemViewType(position);
			// if (isItemViewTypePinned(adapter, viewType))
			// return position;
			if (((ContactsListAdapter) adapter).isSection(position)) {
				return position;
			}
		}
		return -1; // no candidate found
	}

	void destroyPinnedShadow() {
		if (mPinnedSection != null) {
			// keep shadow for being recycled later
			mRecycleSection = mPinnedSection;
			mPinnedSection = null;
		}
	}

	@Override
	public void onRestoreInstanceState(Parcelable state) {
		super.onRestoreInstanceState(state);
		post(new Runnable() {
			@Override
			public void run() { // restore pinned view after configuration
								// change
				recreatePinnedShadow();
			}
		});
	}

	@Override
	public void setAdapter(final ListAdapter adapter) {
		if (!(adapter instanceof ContactsListAdapter)) {
			throw new IllegalArgumentException(
					"The adapter needds to be of type "
							+ ContactsListAdapter.class + " and is "
							+ adapter.getClass());
		}

		ListAdapter oldAdapter = getAdapter();
		if (oldAdapter != null)
			oldAdapter.unregisterDataSetObserver(mDataSetObserver);
		if (adapter != null)
			adapter.registerDataSetObserver(mDataSetObserver);

		// destroy pinned shadow, if new adapter is not same as old one
		if (oldAdapter != adapter) {
			destroyPinnedShadow();
		}
		if (mScroller != null) {
			mScroller.setAdapter(adapter);
		}
		super.setAdapter(adapter);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);
		if (mPinnedSection != null) {
			int parentWidth = r - l - getPaddingLeft() - getPaddingRight();
			int shadowWidth = mPinnedSection.view.getWidth();
			if (parentWidth != shadowWidth) {
				recreatePinnedShadow();
			}
		}
	}

	@Override
	public void onScroll(final AbsListView view, final int firstVisibleItem,
			final int visibleItemCount, final int totalItemCount) {
		if (!mIsSpinnedShadowEnabled) {
			return;
		}
		// get expected adapter or fail fast
		ListAdapter adapter = getAdapter();
		if (adapter == null || visibleItemCount == 0) {
			return; // nothing to do
		}

		if (adapter instanceof HeaderViewListAdapter) {
            adapter = ((HeaderViewListAdapter)adapter).getWrappedAdapter();
		}
		final boolean isFirstVisibleItemSection = ((ContactsListAdapter) adapter)
				.isSection(firstVisibleItem);

		if (isFirstVisibleItemSection) {
            View sectionView = getChildAt(0);
            if (sectionView.getTop() == getPaddingTop()) { // view sticks to the top, no need for pinned shadow
                destroyPinnedShadow();
            } else { // section doesn't stick to the top, make sure we have a pinned shadow
            	ensureShadowForPosition(firstVisibleItem, firstVisibleItem, visibleItemCount);
            }

		} else { // section is not at the first visible position
            int sectionPosition = findCurrentSectionPosition(firstVisibleItem);
            if (sectionPosition > -1) { // we have section position
                ensureShadowForPosition(sectionPosition, firstVisibleItem, visibleItemCount);
            } else { // there is no section for the first visible item, destroy shadow
            	destroyPinnedShadow();
            }
		}
	}

	@Override
	public void onScrollStateChanged(final AbsListView view,
			final int scrollState) {
		// do nothing
	}

	/**
	 * play index
	 * 
	 */
	private class IndexScroller {

		private float mIndexbarWidth;
		private float mIndexbarMargin;
		private float mPreviewPadding;
		private float mDensity;
		private float mScaledDensity;
		private float mAlphaRate;
		private int mState = STATE_HIDDEN;
		private int mListViewWidth;
		private int mListViewHeight;
		private int mCurrentSection = -1;
		private boolean mIsIndexing = false;
		private ListView mListView = null;
		private SectionIndexer mIndexer = null;
		private String[] mSections = null;
		private RectF mIndexbarRect;

		private static final int STATE_HIDDEN = 0;
		private static final int STATE_SHOWING = 1;
		private static final int STATE_SHOWN = 2;
		private static final int STATE_HIDING = 3;

		public IndexScroller(Context context, ListView lv) {
			mDensity = context.getResources().getDisplayMetrics().density;
			mScaledDensity = context.getResources().getDisplayMetrics().scaledDensity;
			mListView = lv;
			setAdapter(mListView.getAdapter());

			mIndexbarWidth = 20 * mDensity;
			mIndexbarMargin = 10 * mDensity;
			mPreviewPadding = 5 * mDensity;
		}

		public void draw(Canvas canvas) {
			if (mState == STATE_HIDDEN)
				return;

			// mAlphaRate determines the rate of opacity
			Paint indexbarPaint = new Paint();
			indexbarPaint.setColor(Color.BLACK);
			indexbarPaint.setAlpha((int) (64 * mAlphaRate));
			indexbarPaint.setAntiAlias(true);
			canvas.drawRoundRect(mIndexbarRect, 5 * mDensity, 5 * mDensity,
					indexbarPaint);

			if (mSections != null && mSections.length > 0) {
				// Preview is shown when mCurrentSection is set
				if (mCurrentSection >= 0) {
					Paint previewPaint = new Paint();
					previewPaint.setColor(Color.BLACK);
					previewPaint.setAlpha(96);
					previewPaint.setAntiAlias(true);
					previewPaint.setShadowLayer(3, 0, 0,
							Color.argb(64, 0, 0, 0));

					Paint previewTextPaint = new Paint();
					previewTextPaint.setColor(Color.WHITE);
					previewTextPaint.setAntiAlias(true);
					previewTextPaint.setTextSize(50 * mScaledDensity);

					float previewTextWidth = previewTextPaint
							.measureText(mSections[mCurrentSection]);
					float previewSize = 2 * mPreviewPadding
							+ previewTextPaint.descent()
							- previewTextPaint.ascent();
					RectF previewRect = new RectF(
							(mListViewWidth - previewSize) / 2,
							(mListViewHeight - previewSize) / 2,
							(mListViewWidth - previewSize) / 2 + previewSize,
							(mListViewHeight - previewSize) / 2 + previewSize);

					canvas.drawRoundRect(previewRect, 5 * mDensity,
							5 * mDensity, previewPaint);
					canvas.drawText(mSections[mCurrentSection],
							previewRect.left + (previewSize - previewTextWidth)
									/ 2 - 1, previewRect.top + mPreviewPadding
									- previewTextPaint.ascent() + 1,
							previewTextPaint);
				}

				Paint indexPaint = new Paint();
				indexPaint.setColor(Color.WHITE);
				indexPaint.setAlpha((int) (255 * mAlphaRate));
				indexPaint.setAntiAlias(true);
				indexPaint.setTextSize(12 * mScaledDensity);

				float sectionHeight = (mIndexbarRect.height() - 2 * mIndexbarMargin)
						/ mSections.length;
				float paddingTop = (sectionHeight - (indexPaint.descent() - indexPaint
						.ascent())) / 2;
				for (int i = 0; i < mSections.length; i++) {
					float paddingLeft = (mIndexbarWidth - indexPaint
							.measureText(mSections[i])) / 2;
					canvas.drawText(mSections[i], mIndexbarRect.left
							+ paddingLeft,
							mIndexbarRect.top + mIndexbarMargin + sectionHeight
									* i + paddingTop - indexPaint.ascent(),
							indexPaint);
				}
			}
		}

		public boolean onTouchEvent(MotionEvent ev) {
			switch (ev.getAction()) {
			case MotionEvent.ACTION_DOWN:
				// If down event occurs inside index bar region, start indexing
				if (mState != STATE_HIDDEN && contains(ev.getX(), ev.getY())) {
					setState(STATE_SHOWN);

					// It demonstrates that the motion event started from index
					// bar
					mIsIndexing = true;
					// Determine which section the point is in, and move the
					// list to
					// that section
					mCurrentSection = getSectionByPoint(ev.getY());
					mListView.setSelection(mIndexer
							.getPositionForSection(mCurrentSection));
					return true;
				}
				break;
			case MotionEvent.ACTION_MOVE:
				if (mIsIndexing) {
					// If this event moves inside index bar
					if (contains(ev.getX(), ev.getY())) {
						// Determine which section the point is in, and move the
						// list to that section
						mCurrentSection = getSectionByPoint(ev.getY());
						mListView.setSelection(mIndexer
								.getPositionForSection(mCurrentSection));
					}
					return true;
				}
				break;
			case MotionEvent.ACTION_UP:
				if (mIsIndexing) {
					mIsIndexing = false;
					mCurrentSection = -1;
				}
				if (mState == STATE_SHOWN)
					setState(STATE_HIDING);
				break;
			}
			return false;
		}

		public void onSizeChanged(int w, int h, int oldw, int oldh) {
			mListViewWidth = w;
			mListViewHeight = h;
			mIndexbarRect = new RectF(w - mIndexbarMargin - mIndexbarWidth,
					mIndexbarMargin, w - mIndexbarMargin, h - mIndexbarMargin);
		}

		public void show() {
			if (mState == STATE_HIDDEN)
				setState(STATE_SHOWING);
			else if (mState == STATE_HIDING)
				setState(STATE_HIDING);
		}

		public void hide() {
			if (mState == STATE_SHOWN)
				setState(STATE_HIDING);
		}

		public void setAdapter(Adapter adapter) {
			if (adapter instanceof SectionIndexer) {
				mIndexer = (SectionIndexer) adapter;
				mSections = (String[]) mIndexer.getSections();
			}
		}

		private void setState(int state) {
			if (state < STATE_HIDDEN || state > STATE_HIDING)
				return;

			mState = state;
			switch (mState) {
			case STATE_HIDDEN:
				// Cancel any fade effect
				mHandler.removeMessages(0);
				break;
			case STATE_SHOWING:
				// Start to fade in
				mAlphaRate = 0;
				fade(0);
				break;
			case STATE_SHOWN:
				// Cancel any fade effect
				mHandler.removeMessages(0);
				break;
			case STATE_HIDING:
				// Start to fade out after three seconds
				mAlphaRate = 1;
				fade(3000);
				break;
			}
		}

		private boolean contains(float x, float y) {
			// Determine if the point is in index bar region, which includes the
			// right margin of the bar
			return (x >= mIndexbarRect.left && y >= mIndexbarRect.top && y <= mIndexbarRect.top
					+ mIndexbarRect.height());
		}

		private int getSectionByPoint(float y) {
			if (mSections == null || mSections.length == 0)
				return 0;
			if (y < mIndexbarRect.top + mIndexbarMargin)
				return 0;
			if (y >= mIndexbarRect.top + mIndexbarRect.height()
					- mIndexbarMargin)
				return mSections.length - 1;
			return (int) ((y - mIndexbarRect.top - mIndexbarMargin) / ((mIndexbarRect
					.height() - 2 * mIndexbarMargin) / mSections.length));
		}

		private void fade(long delay) {
			mHandler.removeMessages(0);
			mHandler.sendEmptyMessageAtTime(0, SystemClock.uptimeMillis()
					+ delay);
		}

		private Handler mHandler = new Handler() {

			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);

				switch (mState) {
				case STATE_SHOWING:
					// Fade in effect
					mAlphaRate += (1 - mAlphaRate) * 0.2;
					if (mAlphaRate > 0.9) {
						mAlphaRate = 1;
						setState(STATE_SHOWN);
					}

					mListView.invalidate();
					fade(10);
					break;
				case STATE_SHOWN:
					// If no action, hide automatically
					setState(STATE_HIDING);
					break;
				case STATE_HIDING:
					// Fade out effect
					mAlphaRate -= mAlphaRate * 0.2;
					if (mAlphaRate < 0.1) {
						mAlphaRate = 0;
						setState(STATE_HIDDEN);
					}

					mListView.invalidate();
					fade(10);
					break;
				}
			}

		};
	}
}
