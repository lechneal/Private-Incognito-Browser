package com.lechneralexander.privatebrowser.search;

import android.app.Application;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.inject.Inject;

import com.lechneralexander.privatebrowser.R;
import com.lechneralexander.privatebrowser.app.BrowserApp;
import com.lechneralexander.privatebrowser.database.BookmarkManager;
import com.lechneralexander.privatebrowser.database.HistoryItem;
import com.lechneralexander.privatebrowser.preference.PreferenceManager;
import com.lechneralexander.privatebrowser.utils.ThemeUtils;

public class SuggestionsAdapter extends BaseAdapter implements Filterable, SuggestionsResult {

    private static long timestamp = System.currentTimeMillis();

    private static final String TAG = SuggestionsAdapter.class.getSimpleName();

    private final List<HistoryItem> mHistory = new ArrayList<>(5);
    private final List<HistoryItem> mBookmarks = new ArrayList<>(5);
    private final List<HistoryItem> mSuggestions = new ArrayList<>(5);
    private final List<HistoryItem> mFilteredList = new ArrayList<>(5);
    private final List<HistoryItem> mAllBookmarks = new ArrayList<>(5);

    private boolean mUseGoogle = true;
    private boolean mIsExecuting = false;
    private final boolean mDarkTheme;
    private final boolean mIncognito;
    private static final String CACHE_FILE_TYPE = ".sgg";
    private static final long INTERVAL_DAY = 86400000;
    private static final int MAX_SUGGESTIONS = 5;
    private static final SuggestionsComparator sComparator = new SuggestionsComparator();

    @NonNull private final Context mContext;
    @Nullable private SearchFilter mFilter;
    @NonNull private final Drawable mSearchDrawable;
    @NonNull private final Drawable mHistoryDrawable;
    @NonNull private final Drawable mBookmarkDrawable;

    @Inject BookmarkManager mBookmarkManager;
    @Inject PreferenceManager mPreferenceManager;

    public SuggestionsAdapter(@NonNull Context context, boolean dark, boolean incognito) {
        BrowserApp.getAppComponent().inject(this);
        mAllBookmarks.addAll(mBookmarkManager.getAllBookmarks(true));
        mUseGoogle = mPreferenceManager.getGoogleSearchSuggestionsEnabled();
        mContext = context;
        mDarkTheme = dark /*|| incognito*/;
        mIncognito = incognito;
        BrowserApp.getTaskThread().execute(new ClearCacheRunnable(BrowserApp.get(context)));
        mSearchDrawable = ThemeUtils.getThemedDrawable(context, R.drawable.ic_search, mDarkTheme);
        mBookmarkDrawable = ThemeUtils.getThemedDrawable(context, R.drawable.ic_bookmark, mDarkTheme);
        mHistoryDrawable = ThemeUtils.getThemedDrawable(context, R.drawable.ic_history, mDarkTheme);
    }

    public void refreshPreferences() {
        mUseGoogle = mPreferenceManager.getGoogleSearchSuggestionsEnabled();
        if (!mUseGoogle) {
            synchronized (mSuggestions) {
                mSuggestions.clear();
            }
        }
    }

    public void refreshBookmarks() {
        synchronized (SuggestionsAdapter.this) {
            mAllBookmarks.clear();
            mAllBookmarks.addAll(mBookmarkManager.getAllBookmarks(true));
        }
    }

    @Override
    public int getCount() {
        return mFilteredList.size();
    }

    @Override
    public Object getItem(int position) {
        return mFilteredList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    private static class SuggestionHolder {
        ImageView mImage;
        TextView mTitle;
        TextView mUrl;
    }

    @Nullable
    @Override
    public View getView(int position, @Nullable View convertView, ViewGroup parent) {
        SuggestionHolder holder;

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            convertView = inflater.inflate(R.layout.two_line_autocomplete, parent, false);

            holder = new SuggestionHolder();
            holder.mTitle = (TextView) convertView.findViewById(R.id.title);
            holder.mUrl = (TextView) convertView.findViewById(R.id.url);
            holder.mImage = (ImageView) convertView.findViewById(R.id.suggestionIcon);
            convertView.setTag(holder);
        } else {
            holder = (SuggestionHolder) convertView.getTag();
        }
        HistoryItem web;
        web = mFilteredList.get(position);
        holder.mTitle.setText(web.getTitle());
        holder.mUrl.setText(web.getUrl());

        Drawable image;
        switch (web.getImageId()) {
            case R.drawable.ic_bookmark: {
                if (mDarkTheme)
                    holder.mTitle.setTextColor(Color.WHITE);
                image = mBookmarkDrawable;
                break;
            }
            case R.drawable.ic_search: {
                if (mDarkTheme)
                    holder.mTitle.setTextColor(Color.WHITE);
                image = mSearchDrawable;
                break;
            }
            case R.drawable.ic_history: {
                if (mDarkTheme)
                    holder.mTitle.setTextColor(Color.WHITE);
                image = mHistoryDrawable;
                break;
            }
            default:
                if (mDarkTheme)
                    holder.mTitle.setTextColor(Color.WHITE);
                image = mSearchDrawable;
                break;
        }

        holder.mImage.setImageDrawable(image);

        return convertView;
    }

    @Override
    public Filter getFilter() {
        if (mFilter == null) {
            mFilter = new SearchFilter();
        }
        return mFilter;
    }

    private static class ClearCacheRunnable implements Runnable {

        @NonNull
        private final Application app;

        public ClearCacheRunnable(@NonNull Application app) {
            this.app = app;
        }

        @Override
        public void run() {
            if (app == null || app.getCacheDir() == null) {
                Log.w(TAG, "app or cache dir is null");
                return;
            }
            File dir = new File(app.getCacheDir().toString());
            String[] fileList = dir.list(new NameFilter());
            long earliestTimeAllowed = System.currentTimeMillis() - INTERVAL_DAY;
            for (String fileName : fileList) {
                File file = new File(dir.getPath() + fileName);
                if (file.exists() && earliestTimeAllowed > file.lastModified()) {
                    file.delete();
                }
            }
        }

        private static class NameFilter implements FilenameFilter {

            @Override
            public boolean accept(File dir, @NonNull String filename) {
                return filename.endsWith(CACHE_FILE_TYPE);
            }

        }

    }

    private class SearchFilter extends Filter {

        @NonNull
        @Override
        protected FilterResults performFiltering(@Nullable CharSequence constraint) {
            timestamp = System.currentTimeMillis();
            FilterResults results = new FilterResults();
            if (constraint == null) {
                results.count = 0;
                return results;
            }
            String query = constraint.toString().toLowerCase(Locale.getDefault());
//            if (mUseGoogle && !mIncognito && !mIsExecuting) {
//                mIsExecuting = true;
//                new RetrieveSuggestionsTask(query, SuggestionsAdapter.this, BrowserApp.get(mContext)).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
//            }
            ArrayList<HistoryItem> filteredResults = new ArrayList<>();
            int maxSize = Math.min(mAllBookmarks.size(), MAX_SUGGESTIONS);
            for (int n = 0; n < maxSize ; n++) {
                if (mAllBookmarks.get(n).getTitle().toLowerCase(Locale.getDefault())
                        .startsWith(query)) {
                    filteredResults.add(mAllBookmarks.get(n));
                } else if (mAllBookmarks.get(n).getUrl().contains(query)) {
                    filteredResults.add(mAllBookmarks.get(n));
                }
            }

//            Collections.sort(filteredResults, sComparator);
            results.values = filteredResults;
            results.count = ((List)results.values).size();
            return results;
        }

        @Override
        public CharSequence convertResultToString(@NonNull Object resultValue) {
            return ((HistoryItem) resultValue).getUrl();
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            synchronized (mFilteredList) {
                mFilteredList.clear();
                if (results.count > 0) {
                    mFilteredList.addAll((List<HistoryItem>) results.values);
                }
            }
            notifyDataSetChanged();
            Log.w(TAG, "publishResults: Time: " + (System.currentTimeMillis() - timestamp));
        }

    }

    @Override
    public void resultReceived(@NonNull List<HistoryItem> searchResults) {
        mIsExecuting = false;
        synchronized (mSuggestions) {
            mSuggestions.clear();
            mSuggestions.addAll(searchResults);
        }
        synchronized (mFilteredList) {
            mFilteredList.clear();
            List<HistoryItem> filtered = getFilteredList();
            Collections.sort(filtered, sComparator);
            mFilteredList.addAll(filtered);
            notifyDataSetChanged();
        }
    }

    @NonNull
    private List<HistoryItem> getFilteredList() {
        return mBookmarks.subList(0, Math.min(MAX_SUGGESTIONS, mBookmarks.size()));
    }

    private static class SuggestionsComparator implements Comparator<HistoryItem> {

        @Override
        public int compare(@NonNull HistoryItem lhs, @NonNull HistoryItem rhs) {
            if (lhs.getImageId() == rhs.getImageId()) return 0;
            if (lhs.getImageId() == R.drawable.ic_bookmark) return -1;
            if (rhs.getImageId() == R.drawable.ic_bookmark) return 1;
            if (lhs.getImageId() == R.drawable.ic_history) return -1;
            return 1;
        }
    }

}
