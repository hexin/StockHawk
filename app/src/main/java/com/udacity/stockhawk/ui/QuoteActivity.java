package com.udacity.stockhawk.ui;

import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by kacper on 06.05.17.
 */

public class QuoteActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String EXTRAS_SYMBOL_KEY = "symbol";
    private static final int QUOTE_LOADER = 1;
    private static final DecimalFormat dollarFormatWithPlus;
    private static final DecimalFormat dollarFormat;
    private static final DecimalFormat percentageFormat;
    static {
        dollarFormat = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
        dollarFormatWithPlus = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
        dollarFormatWithPlus.setPositivePrefix("+$");
        percentageFormat = (DecimalFormat) NumberFormat.getPercentInstance(Locale.getDefault());
        percentageFormat.setMaximumFractionDigits(2);
        percentageFormat.setMinimumFractionDigits(2);
        percentageFormat.setPositivePrefix("+");
    }
    private String mSymbol;
    private QuoteData mQuoteData;
    @BindView(R.id.symbol)
    TextView symbolTextView;
    @BindView(R.id.price)
    TextView priceTextView;
    @BindView(R.id.change)
    TextView changeTextView;
    @BindView(R.id.history_chart)
    LineChart historyChart;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quote);
        ButterKnife.bind(this);
        mSymbol = getIntent().getStringExtra(EXTRAS_SYMBOL_KEY);
        getSupportLoaderManager().initLoader(QUOTE_LOADER, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this,
                Contract.Quote.makeUriForStock(mSymbol),
                Contract.Quote.QUOTE_COLUMNS.toArray(new String[]{}),
                null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data.getCount() != 1) {
            Toast.makeText(this, "Something bad happened to data", Toast.LENGTH_LONG).show();
            return;
        }
        data.moveToFirst();
        mQuoteData = new QuoteData(data);
        setSummaryContent(mQuoteData);
        setChartValues(prepareChartValues(mQuoteData));
        initializeChartView(mQuoteData);
    }

    private void initializeChartView(QuoteData quoteData) {
        XAxis xAxis = historyChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextSize(12f);
        xAxis.setLabelRotationAngle(55f);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawAxisLine(true);
        xAxis.setDrawGridLines(true);
        xAxis.setValueFormatter(new DateChartFormatter(quoteData.getFirstSortedHistoryPair().first));

        YAxis left = historyChart.getAxisLeft();
        left.setTextColor(Color.WHITE);
        left.setTextSize(13f);
        left.setValueFormatter(new MoneyChartFormatter(dollarFormat));

        YAxis yAxisRight = historyChart.getAxisRight();
        yAxisRight.setEnabled(false);
    }

    private void setSummaryContent(QuoteData quoteData) {
        priceTextView.setText(dollarFormat.format(quoteData.getPrice()));
        symbolTextView.setText(quoteData.getSymbol());
        if (quoteData.getAbsoluteChange() > 0) {
            changeTextView.setBackgroundResource(R.drawable.percent_change_pill_green);
        } else {
            changeTextView.setBackgroundResource(R.drawable.percent_change_pill_red);
        }
        if (PrefUtils.getDisplayMode(this)
                .equals(getString(R.string.pref_display_mode_absolute_key))) {
            changeTextView.setText(dollarFormatWithPlus.format(quoteData.getAbsoluteChange()));
        } else {
            changeTextView.setText(percentageFormat.format(quoteData.getPercentageChange() / 100));
        }
    }

    private void setChartValues(List<Entry> chartValues) {
        LineDataSet dataSet = new LineDataSet(chartValues, "Quotes");
        LineData lineData = new LineData(dataSet);
        dataSet.setColors(Color.YELLOW);
        historyChart.setData(lineData);
        historyChart.invalidate();
    }

    private List<Entry> prepareChartValues(QuoteData quoteData) {
        List<Pair<Date, Float>> sortedHistoryPairs = quoteData.getSortedHistoryPairs();
        final Pair<Date, Float> firstPair = Iterables.getFirst(sortedHistoryPairs, null);
        Iterable<Entry> transformedToEntries = Iterables.transform(sortedHistoryPairs, new Function<Pair<Date, Float>, Entry>() {
            @Override
            public Entry apply(Pair<Date, Float> input) {
                float xAxisVal = (float) ((input.first.getTime() / 1_000) - (firstPair.first.getTime() / 1_000));
                return new Entry(xAxisVal, input.second);
            }
        });
        return Lists.newArrayList(transformedToEntries);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    private static class QuoteData {

        private int id;
        private String symbol;
        private float price;
        private float absoluteChange;
        private float percentageChange;
        private List<Pair<Date, Float>> sortedHistoryPairs;

        QuoteData(Cursor cursor) {
            price = cursor.getFloat(cursor.getColumnIndex(Contract.Quote.COLUMN_PRICE));
            symbol = cursor.getString(cursor.getColumnIndex(Contract.Quote.COLUMN_SYMBOL));
            absoluteChange = cursor.getFloat(cursor.getColumnIndex(Contract.Quote.COLUMN_ABSOLUTE_CHANGE));
            percentageChange = cursor.getFloat(cursor.getColumnIndex(Contract.Quote.COLUMN_PERCENTAGE_CHANGE));
            sortedHistoryPairs = sortHistory(parseHistory(cursor.getString(cursor.getColumnIndex(Contract.Quote.COLUMN_HISTORY))));
        }

        public int getId() {
            return id;
        }

        public String getSymbol() {
            return symbol;
        }

        public float getPrice() {
            return price;
        }

        public float getAbsoluteChange() {
            return absoluteChange;
        }

        public float getPercentageChange() {
            return percentageChange;
        }

        public Pair<Date, Float> getFirstSortedHistoryPair() {
            return Iterables.getFirst(sortedHistoryPairs, null);
        }

        public List<Pair<Date, Float>> getSortedHistoryPairs() {
            return sortedHistoryPairs;
        }

        private List<Pair<Date, Float>> sortHistory(List<Pair<Date, Float>> historyPairs) {
            Collections.sort(historyPairs, new Comparator<Pair<Date, Float>>() {
                @Override
                public int compare(Pair<Date, Float> o1, Pair<Date, Float> o2) {
                    return o1.first.compareTo(o2.first);
                }
            });
            return historyPairs;
        }

        private List<Pair<Date, Float>> parseHistory(String stringifiedHistory) {
            if (Strings.isNullOrEmpty(stringifiedHistory)) {
                return Collections.EMPTY_LIST;
            }
            List<String> splitedToEntries = Arrays.asList(stringifiedHistory.split("\n"));
            Iterable<Pair<Date, Float>> transformed = Iterables.transform(splitedToEntries, new Function<String, Pair<Date, Float>>() {
                @Override
                public Pair<Date, Float> apply(String input) {
                    String[] splitedEntry = input.trim().split(", ");
                    Date date = new Date(Long.valueOf(splitedEntry[0].trim()));
                    Float closed = Float.valueOf(splitedEntry[1].trim());
                    return new Pair<>(date, closed);
                }
            });
            return Lists.newArrayList(transformed);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_settings, menu);
        MenuItem item = menu.findItem(R.id.action_change_units);
        item.setTitle(getString(R.string.action_change_units_title));
        setDisplayModeMenuItemIcon(item);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_change_units) {
            PrefUtils.toggleDisplayMode(this);
            setDisplayModeMenuItemIcon(item);
            setSummaryContent(mQuoteData);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setDisplayModeMenuItemIcon(MenuItem item) {
        if (PrefUtils.getDisplayMode(this)
                .equals(getString(R.string.pref_display_mode_absolute_key))) {
            item.setIcon(R.drawable.ic_percentage);
        } else {
            item.setIcon(R.drawable.ic_dollar);
        }
    }
}
