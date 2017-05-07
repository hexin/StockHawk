package com.udacity.stockhawk.ui;

import android.support.v4.util.Pair;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * Created by kacper on 07.05.17.
 */

public class DateChartFormatter implements IAxisValueFormatter {

    private final Date firstDate;
    private final SimpleDateFormat dateFormat;

    public DateChartFormatter(Date firstDate) {
        this.firstDate = firstDate;
        this.dateFormat = new SimpleDateFormat("dd MMM yyyy");
    }

    @Override
    public String getFormattedValue(float value, AxisBase axis) {
        return dateFormat.format(findProperDate(value));
    }

    private Date findProperDate(float diffFromFirstInSeconds) {
        long diffFromFirstInMillis = (long)diffFromFirstInSeconds * 1000;
        long foundTimestampInMillis = firstDate.getTime() + diffFromFirstInMillis;
        return new Date(foundTimestampInMillis);
    }
}
