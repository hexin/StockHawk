package com.udacity.stockhawk.ui;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import java.text.DecimalFormat;

/**
 * Created by kacper on 07.05.17.
 */

class MoneyChartFormatter implements IAxisValueFormatter {

    private final DecimalFormat format;

    public MoneyChartFormatter(DecimalFormat format) {
        this.format = format;
    }

    @Override
    public String getFormattedValue(float value, AxisBase axis) {
        return format.format(value);
    }
}
