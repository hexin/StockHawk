package com.udacity.stockhawk.ui;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by kacper on 06.05.17.
 */

public class QuoteActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String EXTRAS_SYMBOL_KEY = "symbol";
    private static final int QUOTE_LOADER = 1;
    private String mSymbol;
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
        String price = data.getString(data.getColumnIndex(Contract.Quote.COLUMN_PRICE));
        priceTextView.setText(price);
        String symbol = data.getString(data.getColumnIndex(Contract.Quote.COLUMN_SYMBOL));
        symbolTextView.setText(symbol);
        float rawAbsoluteChange = data.getFloat(data.getColumnIndex(Contract.Quote.COLUMN_ABSOLUTE_CHANGE));
        if (rawAbsoluteChange > 0) {
            changeTextView.setBackgroundResource(R.drawable.percent_change_pill_green);
        } else {
            changeTextView.setBackgroundResource(R.drawable.percent_change_pill_red);
        }
        changeTextView.setText(Float.toString(rawAbsoluteChange));
        String stringifiedHistory = data.getString(data.getColumnIndex(Contract.Quote.COLUMN_HISTORY));
        setChartValues(convertStringifiedHistoryToPairs(stringifiedHistory));
    }

    private void setChartValues(List<Pair<Date, Float>> historyPairs) {
        Collections.sort(historyPairs, new Comparator<Pair<Date, Float>>() {
            @Override
            public int compare(Pair<Date, Float> o1, Pair<Date, Float> o2) {
                return o1.first.compareTo(o2.first);
            }
        });
        final Pair<Date, Float> firstPair = Iterables.getFirst(historyPairs, null);
        Iterable<Entry> transformedToEntries = Iterables.transform(historyPairs, new Function<Pair<Date, Float>, Entry>() {
            @Override
            public Entry apply(Pair<Date, Float> input) {
                float xAxisVal = (float) (input.first.getTime() - firstPair.first.getTime());
                return new Entry(xAxisVal, input.second);
            }
        });
        LineDataSet dataSet = new LineDataSet(Lists.newArrayList(transformedToEntries), "Quotes");
        LineData data = new LineData(dataSet);
        dataSet.setColors(ColorTemplate.COLORFUL_COLORS);
        historyChart.setData(data);
        historyChart.invalidate();
    }

    private List<Pair<Date, Float>> convertStringifiedHistoryToPairs(String stringifiedHistory) {
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

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
