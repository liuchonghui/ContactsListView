package com.solo.widget.contactslistview;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.SectionIndexer;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;


public class ContactsListAdapter extends BaseAdapter implements ListAdapter,
		OnItemClickListener, SectionIndexer {

	private final DataSetObserver dataSetObserver = new DataSetObserver() {
		@Override
		public void onChanged() {
			super.onChanged();
			updateSessionCache();
		}

		@Override
		public void onInvalidated() {
			super.onInvalidated();
			updateSessionCache();
		};
	};

	private final ListAdapter linkedAdapter;
	private final Map<Integer, String> sectionPositions = new LinkedHashMap<Integer, String>();
	private final Map<Integer, Integer> itemPositions = new LinkedHashMap<Integer, Integer>();
	private final Map<View, String> currentViewSections = new HashMap<View, String>();
	private int viewTypeCount;
	protected final LayoutInflater inflater;

	private OnItemClickListener linkedListener;

	public ContactsListAdapter(final LayoutInflater inflater,
			final ListAdapter linkedAdapter) {
		this.linkedAdapter = linkedAdapter;
		this.inflater = inflater;
		linkedAdapter.registerDataSetObserver(dataSetObserver);
		updateSessionCache();
	}

	private boolean isTheSame(final String previousSection,
			final String newSection) {
		if (previousSection == null) {
			return newSection == null;
		} else {
			return previousSection.equals(newSection);
		}
	}

	private synchronized void updateSessionCache() {
		int currentPosition = 0;
		sectionPositions.clear();
		itemPositions.clear();
		viewTypeCount = linkedAdapter.getViewTypeCount() + 1;
		String currentSection = null;
		final int count = linkedAdapter.getCount();
		for (int i = 0; i < count; i++) {
			final String item = linkedAdapter.getItem(i).toString()
					.substring(0, 1).toUpperCase(Locale.ENGLISH);
			if (!isTheSame(currentSection, item)) {
				sectionPositions.put(currentPosition, item);
				currentSection = item;
				currentPosition++;
			}
			itemPositions.put(currentPosition, i);
			currentPosition++;
		}
	}

	@Override
	public synchronized int getCount() {
		return sectionPositions.size() + itemPositions.size();
	}

	@Override
	public synchronized Object getItem(final int position) {
		if (isSection(position)) {
			return sectionPositions.get(position);
		} else {
			final int linkedItemPosition = getLinkedPosition(position);
			return linkedAdapter.getItem(linkedItemPosition);
		}
	}

	public synchronized boolean isSection(final int position) {
		return sectionPositions.containsKey(position);
	}

	public synchronized String getSectionName(final int position) {
		if (isSection(position)) {
			return sectionPositions.get(position);
		} else {
			return null;
		}
	}

	@Override
	public long getItemId(final int position) {
		if (isSection(position)) {
			return sectionPositions.get(position).hashCode();
		} else {
			return linkedAdapter.getItemId(getLinkedPosition(position));
		}
	}

	protected Integer getLinkedPosition(final int position) {
		return itemPositions.get(position);
	}

	@Override
	public int getItemViewType(final int position) {
		if (isSection(position)) {
			return viewTypeCount - 1;
		}
		return linkedAdapter.getItemViewType(getLinkedPosition(position));
	}

	private View getSectionView(final View convertView, final String section) {
		View theView = convertView;
		if (theView == null) {
			theView = createNewSectionView();
		}
		setSectionText(section, theView);
		replaceSectionViewsInMaps(section, theView);
		return theView;
	}

	protected void setSectionText(final String section, final View sectionView) {
		final TextView textView = (TextView) sectionView
				.findViewById(R.id.listTextView);
		textView.setText(section);
	}

	protected synchronized void replaceSectionViewsInMaps(final String section,
			final View theView) {
		if (currentViewSections.containsKey(theView)) {
			currentViewSections.remove(theView);
		}
		currentViewSections.put(theView, section);
	}

	protected View createNewSectionView() {
		return inflater.inflate(R.layout.section_view, null);
	}

	@Override
	public View getView(final int position, final View convertView,
			final ViewGroup parent) {
		if (isSection(position)) {
			return getSectionView(convertView, sectionPositions.get(position));
		}
		return linkedAdapter.getView(getLinkedPosition(position), convertView,
				parent);
	}

	@Override
	public int getViewTypeCount() {
		return viewTypeCount;
	}

	@Override
	public boolean hasStableIds() {
		return linkedAdapter.hasStableIds();
	}

	@Override
	public boolean isEmpty() {
		return linkedAdapter.isEmpty();
	}

	@Override
	public void registerDataSetObserver(final DataSetObserver observer) {
		linkedAdapter.registerDataSetObserver(observer);
	}

	@Override
	public void unregisterDataSetObserver(final DataSetObserver observer) {
		linkedAdapter.unregisterDataSetObserver(observer);
	}

	@Override
	public boolean areAllItemsEnabled() {
		return linkedAdapter.areAllItemsEnabled();
	}

	@Override
	public boolean isEnabled(final int position) {
		if (isSection(position)) {
			return true;
		}
		return linkedAdapter.isEnabled(getLinkedPosition(position));
	}

	protected void sectionClicked(final String section) {
		// do nothing
	}

	@Override
	public void onItemClick(final AdapterView<?> parent, final View view,
			final int position, final long id) {
		if (isSection(position)) {
			sectionClicked(getSectionName(position));
		} else if (linkedListener != null) {
			linkedListener.onItemClick(parent, view,
					getLinkedPosition(position), id);
		}
	}

	public void setOnItemClickListener(final OnItemClickListener linkedListener) {
		this.linkedListener = linkedListener;
	}

	private String mSections = "#ABCDEFGHIJKLMNOPQRSTUVWXYZ";

	@Override
	public int getPositionForSection(int section) {
		// If there is no item for current section, previous section will be
		// selected
		for (int i = section; i >= 0; i--) {
			for (int j = 0; j < getCount(); j++) {
				if (i == 0) {
					// For numeric section
					for (int k = 0; k <= 9; k++) {
						if (match(
								String.valueOf(getItem(j).toString().charAt(0)),
								String.valueOf(k))) {
							return j;
						}
					}
				} else {
					if (match(String.valueOf(getItem(j).toString().charAt(0)),
							String.valueOf(mSections.charAt(i)))) {
						return j;
					}
				}
			}
		}
		return 0;
	}

	@Override
	public int getSectionForPosition(int position) {
		return -1;
	}

	@Override
	public Object[] getSections() {
		String[] sections = new String[mSections.length()];
		for (int i = 0; i < mSections.length(); i++)
			sections[i] = String.valueOf(mSections.charAt(i));
		return sections;
	}

	public boolean match(String value, String keyword) {
		if (value == null || keyword == null)
			return false;
		if (keyword.length() > value.length())
			return false;

		int i = 0, j = 0;
		do {
			if (keyword.charAt(j) == value.charAt(i)) {
				i++;
				j++;
			} else if (j > 0) {
				break;
			} else {
				i++;
			}
		} while (i < value.length() && j < keyword.length());

		return (j == keyword.length()) ? true : false;
	}
}
