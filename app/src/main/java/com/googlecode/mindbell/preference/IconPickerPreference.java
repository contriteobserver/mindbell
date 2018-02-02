/*
 * MindBell - Aims to give you a support for staying mindful in a busy life -
 *            for remembering what really counts
 *
 *     Copyright (C) 2018-2018 Uwe Damken
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.googlecode.mindbell.preference;

import android.app.AlertDialog;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.googlecode.mindbell.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Idea taken from https://stackoverflow.com/a/32226553
 *
 * IconPickerPreference requires an entries array (texts to be displayed next to the icon) and an entryValues
 * array (filenames of the icons) which are both the same length.
 */

public class IconPickerPreference extends ListPreferenceWithSummaryFix {

    private List<IconItem> iconItemList;

    private int currentIndex = 0; // default value if android:defaultValue is not set

    private ImageView selectedIconImageView;

    private TextView summaryTextView;

    public IconPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        CharSequence[] iconTextArray = getEntries();
        CharSequence[] iconFilenameArray = getEntryValues();

        if (iconTextArray == null || iconFilenameArray == null || iconTextArray.length != iconFilenameArray.length) {
            throw new IllegalStateException(
                    "IconPickerPreference requires an entries array and an entryValues array which are both the same length");
        }

        iconItemList = new ArrayList<IconItem>();
        for (int i = 0; i < iconTextArray.length; i++) {
            IconItem item = new IconItem(iconTextArray[i], iconFilenameArray[i], false);
            iconItemList.add(item);
        }
    }

    @Override
    public String getValue() {
        return String.valueOf(currentIndex);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        selectedIconImageView = (ImageView) view.findViewById(R.id.selectedIcon);
        summaryTextView = (TextView) view.findViewById(R.id.summary);

        updateIcon();
    }

    private void updateIcon() {
        IconItem selectedIconItem = iconItemList.get(currentIndex);
        int identifier =
                getContext().getResources().getIdentifier(selectedIconItem.iconFilename, "drawable", getContext().getPackageName());
        selectedIconImageView.setImageResource(identifier);
        summaryTextView.setText(selectedIconItem.iconText);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (iconItemList != null) {
            for (int i = 0; i < iconItemList.size(); i++) {
                IconItem item = iconItemList.get(i);
                if (item.isChecked) {
                        persistString(String.valueOf(i));
                        currentIndex = i;
                        updateIcon();
                        break;
                }
            }
        }
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        String newValue = null;
        if (restoreValue) {
            if (defaultValue == null) {
                newValue = getPersistedString("0");
            } else {
                newValue = getPersistedString(defaultValue.toString());
            }
        } else {
            newValue = defaultValue.toString();
        }
        currentIndex = Integer.parseInt(newValue);
        iconItemList.get(currentIndex).isChecked = true;
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        builder.setNegativeButton("Cancel", null);
        builder.setPositiveButton(null, null);
        CustomListPreferenceAdapter customListPreferenceAdapter =
                new CustomListPreferenceAdapter(getContext(), R.layout.icon_picker_preference_item, iconItemList);
        builder.setAdapter(customListPreferenceAdapter, null);
    }

    private class CustomListPreferenceAdapter extends ArrayAdapter<IconItem> {

        private int resource;

        public CustomListPreferenceAdapter(Context context, int resource, List<IconItem> objects) {
            super(context, resource, objects);
            this.resource = resource; // there is no way to get this back from super class
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(resource, parent, false);
            }
            TextView iconName = (TextView) convertView.findViewById(R.id.iconText);
            ImageView iconImage = (ImageView) convertView.findViewById(R.id.iconImage);
            RadioButton radioButton = (RadioButton) convertView.findViewById(R.id.iconRadio);

            IconItem iconItem = getItem(position);
            iconName.setText(iconItem.iconText);
            int identifier =
                    getContext().getResources().getIdentifier(iconItem.iconFilename, "drawable", getContext().getPackageName());
            iconImage.setImageResource(identifier);
            radioButton.setChecked(iconItem.isChecked);

            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    for (int i = 0; i < getCount(); i++) {
                        getItem(i).isChecked = (i == position);
                    }
                    getDialog().dismiss();
                }
            });

            return convertView;
        }
    }

    private class IconItem {

        private String iconText;

        private String iconFilename;

        private boolean isChecked;

        public IconItem(CharSequence iconText, CharSequence iconFilename, boolean isChecked) {
            this.iconText = iconText.toString();
            this.iconFilename = iconFilename.toString();
            this.isChecked = isChecked;
        }

    }

}