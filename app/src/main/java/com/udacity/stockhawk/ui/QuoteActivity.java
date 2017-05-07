package com.udacity.stockhawk.ui;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;

import java.text.DecimalFormat;

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
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
