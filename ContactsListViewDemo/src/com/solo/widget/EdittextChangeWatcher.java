package com.solo.widget;

import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

import com.solo.widget.BadgeView;

public class EdittextChangeWatcher implements TextWatcher {

	final EditText view;
	BadgeView b;
	AfterTextChangedListener listener;

	class badgeClick implements View.OnClickListener {
		EditText view;

		public badgeClick(EditText view) {
			this.view = view;
		}

		@Override
		public void onClick(View v) {
			this.view.setText("");
		}

	}

	public static interface AfterTextChangedListener {
		void afterTextChanged(Editable sequence);
	}

	public EdittextChangeWatcher(EditText view, BadgeView b) {
		this.view = view;
		if (b != null) {
			this.b = b;
			this.b.setOnClickListener(new badgeClick(this.view));
		}
	}

	public EdittextChangeWatcher(EditText view, BadgeView b,
			AfterTextChangedListener listener) {
		this(view, b);
		this.listener = listener;
	}

	@Override
	public void afterTextChanged(Editable arg0) {
		if (this.listener != null) {
			listener.afterTextChanged(arg0);
		}
	}

	@Override
	public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
			int arg3) {

	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		String val = view.getText().toString().trim();
		if (b != null) {
			view.requestFocus();
			if (!TextUtils.isEmpty(val)) {
				this.b.show();
			} else {
				this.b.hide();
			}
		}
	}
}