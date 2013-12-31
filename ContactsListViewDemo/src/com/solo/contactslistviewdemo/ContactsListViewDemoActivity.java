package com.solo.contactslistviewdemo;

import java.util.ArrayList;
import java.util.Arrays;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.solo.widget.BadgeView;
import com.solo.widget.EdittextChangeWatcher;
import com.solo.widget.EdittextChangeWatcher.AfterTextChangedListener;
import com.solo.widget.contactslistview.ContactsListAdapter;
import com.solo.widget.contactslistview.ContactsListView;

public class ContactsListViewDemoActivity extends Activity {
	EditText mSearchContext;
	ContactsListView mListView;
	StandardArrayAdapter arrayAdapter;
	ContactsListAdapter sectionAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.demo);

		mSearchContext = (EditText) findViewById(R.id.search_center);
		mSearchContext.addTextChangedListener(new EdittextChangeWatcher(
				mSearchContext, getEditTextBadgeView(mSearchContext),
				new AfterTextChangedListener() {
					@Override
					public void afterTextChanged(Editable sequence) {
						new StandardArrayAdapter(COUNTRIES).getFilter().filter(
								sequence.toString());
					}
				}));
		mListView = (ContactsListView) findViewById(R.id.section_list_view);
		mListView.setFastScrollEnabled(true);
		mListView.setSpinnedShadowEnabled(true);

		if (COUNTRIES_ARY.length > 0) {
			Arrays.sort(COUNTRIES_ARY);
			COUNTRIES = new ArrayList<Country>();
			for (int i = 0; i < COUNTRIES_ARY.length; i++) {
				COUNTRIES.add(new Country(i + 1, COUNTRIES_ARY[i]));
			}
			arrayAdapter = new StandardArrayAdapter(COUNTRIES);

			sectionAdapter = new ContactsListAdapter(getLayoutInflater(),
					arrayAdapter);
			mListView.setAdapter(sectionAdapter);
		}
	}

	protected BadgeView getEditTextBadgeView(View view, int img) {
		BadgeView b = new BadgeView(this, view);
		b.setBackgroundResource(img);
		b.setBadgePosition(BadgeView.POSITION_RIGHT);
		return b;
	}

	protected BadgeView getEditTextBadgeView(View view) {
		return getEditTextBadgeView(view, R.drawable.edit_del_selector);
	}

	class Country {
		public int id;
		public String name;

		public Country(int id, String name) {
			this.id = id;
			this.name = name;
		}

		@Override
		public String toString() {
			return this.name;
		}

	}

	private ArrayList<Country> COUNTRIES;
	static String[] COUNTRIES_ARY = new String[] { "East Timor", "Ecuador",
			"Egypt", "El Salvador", "Equatorial Guinea", "Eritrea", "Estonia",
			"Ethiopia", "Faeroe Islands", "Falkland Islands", "Fiji",
			"Finland", "Afghanistan", "Albania", "Algeria", "American Samoa",
			"Andorra", "Angola", "Anguilla", "Antarctica",
			"Antigua and Barbuda", "Argentina", "Armenia", "Aruba",
			"Australia", "Austria", "Azerbaijan", "Bahrain", "Bangladesh",
			"Barbados", "Belarus", "Belgium", "Monaco", "Mongolia",
			"Montserrat", "Morocco", "Mozambique", "Myanmar", "Namibia",
			"Nauru", "Nepal", "Netherlands", "Netherlands Antilles",
			"New Caledonia", "New Zealand", "Guyana", "Haiti",
			"Heard Island and McDonald Islands", "Honduras", "Hong Kong",
			"Hungary", "Iceland", "India", "Indonesia", "Iran", "Iraq",
			"Ireland", "Israel", "Italy", "Jamaica", "Japan", "Jordan",
			"Kazakhstan", "Kenya", "Kiribati", "Kuwait", "Kyrgyzstan", "Laos",
			"Latvia", "Lebanon", "Lesotho", "Liberia", "Libya",
			"Liechtenstein", "Lithuania", "Luxembourg", "Nicaragua", "Niger",
			"Nigeria", "Niue", "Norfolk Island", "North Korea",
			"Northern Marianas", "Norway", "Oman", "Pakistan", "Palau",
			"Panama", "Papua New Guinea", "Paraguay", "Peru", "Philippines",
			"Pitcairn Islands", "Poland", "Portugal", "Puerto Rico", "Qatar",
			"French Southern Territories", "Gabon", "Georgia", "Germany",
			"Ghana", "Gibraltar", "Greece", "Greenland", "Grenada",
			"Guadeloupe", "Guam", "Guatemala", "Guinea", "Guinea-Bissau",
			"Martinique", "Mauritania", "Mauritius", "Mayotte", "Mexico",
			"Micronesia", "Moldova", "Bosnia and Herzegovina", "Botswana",
			"Bouvet Island", "Brazil", "British Indian Ocean Territory",
			"Saint Vincent and the Grenadines", "Samoa", "San Marino",
			"Saudi Arabia", "Senegal", "Seychelles", "Sierra Leone",
			"Singapore", "Slovakia", "Slovenia", "Solomon Islands", "Somalia",
			"South Africa", "South Georgia and the South Sandwich Islands",
			"South Korea", "Spain", "Sri Lanka", "Sudan", "Suriname",
			"Svalbard and Jan Mayen", "Swaziland", "Sweden", "Switzerland",
			"Syria", "Taiwan", "Tajikistan", "Tanzania", "Thailand",
			"The Bahamas", "The Gambia", "Togo", "Tokelau", "Tonga",
			"Trinidad and Tobago", "Tunisia", "Turkey", "Turkmenistan",
			"Turks and Caicos Islands", "Tuvalu", "Uganda", "Ukraine",
			"United Arab Emirates", "United Kingdom", "United States",
			"United States Minor Outlying Islands", "Uruguay", "Uzbekistan",
			"Vanuatu", "Vatican City", "Venezuela", "Vietnam",
			"Virgin Islands", "Wallis and Futuna", "Western Sahara",
			"British Virgin Islands", "Brunei", "Bulgaria", "Burkina Faso",
			"Burundi", "Cote d'Ivoire", "Cambodia", "Cameroon", "Canada",
			"Cape Verde", "Cayman Islands", "Central African Republic", "Chad",
			"Chile", "China", "Reunion", "Romania", "Russia", "Rwanda",
			"Sqo Tome and Principe", "Saint Helena", "Saint Kitts and Nevis",
			"Saint Lucia", "Saint Pierre and Miquelon", "Belize", "Benin",
			"Bermuda", "Bhutan", "Bolivia", "Christmas Island",
			"Cocos (Keeling) Islands", "Colombia", "Comoros", "Congo",
			"Cook Islands", "Costa Rica", "Croatia", "Cuba", "Cyprus",
			"Czech Republic", "Democratic Republic of the Congo", "Denmark",
			"Djibouti", "Dominica", "Dominican Republic",
			"Former Yugoslav Republic of Macedonia", "France", "French Guiana",
			"French Polynesia", "Macau", "Madagascar", "Malawi", "Malaysia",
			"Maldives", "Mali", "Malta", "Marshall Islands", "Yemen",
			"Yugoslavia", "Zambia", "Zimbabwe" };

	private class StandardArrayAdapter extends BaseAdapter implements
			Filterable {

		private final ArrayList<Country> items;

		public StandardArrayAdapter(ArrayList<Country> args) {
			this.items = args;
		}

		@Override
		public View getView(final int position, final View convertView,
				final ViewGroup parent) {
			View view = convertView;
			if (view == null) {
				final LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = vi.inflate(R.layout.row, null);
			}

			Country item = items.get(position);

			TextView textId = (TextView) view.findViewById(R.id.row_id);
			if (textId != null) {
				textId.setText(String.valueOf(item.id));
			}
			TextView textTitle = (TextView) view.findViewById(R.id.row_title);
			if (textTitle != null) {
				textTitle.setText(item.name);
			}
			return view;
		}

		@Override
		public int getCount() {
			return items.size();
		}

		@Override
		public Filter getFilter() {
			Filter listfilter = new MyFilter();
			return listfilter;
		}

		@Override
		public Object getItem(int position) {
			return items.get(position);
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

	}

	public class MyFilter extends Filter {

		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			// NOTE: this function is *always* called from a background thread,
			// and
			// not the UI thread.
			constraint = mSearchContext.getText().toString();
			FilterResults result = new FilterResults();
			if (constraint != null && constraint.toString().length() > 0) {

				ArrayList<Country> filt = new ArrayList<Country>();
				ArrayList<Country> Items = new ArrayList<Country>();
				synchronized (this) {
					Items = COUNTRIES;
				}
				for (int i = 0; i < Items.size(); i++) {
					Country item = Items.get(i);
					if (item.toString().toLowerCase().startsWith(
							constraint.toString().toLowerCase())) {
						filt.add(item);
					}
				}

				result.count = filt.size();
				result.values = filt;
			} else {

				synchronized (this) {
					result.count = COUNTRIES.size();
					result.values = COUNTRIES;
				}

			}
			return result;
		}

		@Override
		protected void publishResults(CharSequence constraint,
				FilterResults results) {
			@SuppressWarnings("unchecked")
			ArrayList<Country> filtered = (ArrayList<Country>) results.values;
			arrayAdapter = new StandardArrayAdapter(filtered);
			sectionAdapter = new ContactsListAdapter(getLayoutInflater(),
					arrayAdapter);
			mListView.setAdapter(sectionAdapter);

		}

	}
}