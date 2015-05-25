package com.adonai.wallet;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.adonai.wallet.database.DbProvider;
import com.adonai.wallet.database.EntityDao;
import com.adonai.wallet.entities.Operation;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import lecho.lib.hellocharts.formatter.AxisValueFormatter;
import lecho.lib.hellocharts.formatter.SimpleAxisValueFormatter;
import lecho.lib.hellocharts.formatter.SimpleLineChartValueFormatter;
import lecho.lib.hellocharts.gesture.ZoomType;
import lecho.lib.hellocharts.listener.DummyLineChartOnValueSelectListener;
import lecho.lib.hellocharts.listener.LineChartOnValueSelectListener;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.view.LineChartView;

/**
 * Fragment for showing various statistics charts
 */
public class StatisticsShowFragment extends Fragment {

    private LineChartView mChart;
    
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.common_statistics_flow, container, false);
        assert rootView != null;
        
        mChart = (LineChartView) rootView.findViewById(R.id.chart);
        mChart.setInteractive(true);
        mChart.setZoomEnabled(true);
        mChart.setZoomType(ZoomType.HORIZONTAL_AND_VERTICAL);
        mChart.setMaxZoom(5000f);
        mChart.setValueSelectionEnabled(true);

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();

        // get list of operations for last 3 months
        try {
            Calendar last3Months = Calendar.getInstance();
            last3Months.setLenient(true);
            last3Months.set(Calendar.DATE, last3Months.get(Calendar.DATE) - 90);
            
            EntityDao<Operation> dao = DbProvider.getHelper().getDao(Operation.class);
            List<Operation> expenseOperations = dao.queryBuilder().orderBy("time", false).where()
                    .ge("time", last3Months.getTime())
                    .and().isNotNull("orderer_id").and().isNull("beneficiar_id")
                    .query();

            List<Operation> benefitOperations = dao.queryBuilder().orderBy("time", false).where()
                    .ge("time", last3Months.getTime())
                    .and().isNotNull("beneficiar_id").and().isNull("orderer_id")
                    .query();
            
            // fill array of points
            List<PointValue> expensePoints = new ArrayList<>(expenseOperations.size());
            for(Operation op : expenseOperations) {
                expensePoints.add(new PointValue(op.getTime().getTime(), op.getAmount().floatValue()));
            }

            List<PointValue> benefitPoints = new ArrayList<>(benefitOperations.size());
            for(Operation op : benefitOperations) {
                benefitPoints.add(new PointValue(op.getTime().getTime(), op.getAmount().floatValue()));
            }
            
            // obtaining list of lines
            Line expenses = new Line(expensePoints).setColor(getResources().getColor(R.color.red_amount))
                    .setHasLabelsOnlyForSelected(true);
            Line benefits = new Line(benefitPoints).setColor(getResources().getColor(R.color.green_amount))
                    .setHasLabelsOnlyForSelected(true);
            List<Line> lines = new ArrayList<>();
            lines.add(expenses);
            lines.add(benefits);

            LineChartData lcd = new LineChartData(lines).setBaseValue(0);

            float start, stop;
            if(!expenseOperations.isEmpty()) {
                start = last3Months.getTimeInMillis();
                stop = System.currentTimeMillis();
            } else {
                start = expenseOperations.get(0).getTime().getTime();
                stop = expenseOperations.get(expenseOperations.size() - 1).getTime().getTime();
            }
            Axis xAxis = Axis.generateAxisFromRange(start, stop, TimeUnit.DAYS.toMillis(1))
                .setName(getString(R.string.time))
                    .setHasTiltedLabels(true)
                    .setTextColor(Color.BLACK)
                    .setMaxLabelChars(6);
            xAxis.setFormatter(new SimpleAxisValueFormatter() {
                private SimpleDateFormat sdf = new SimpleDateFormat("d MMM", Locale.getDefault());

                @Override
                public int formatValueForManualAxis(char[] chars, AxisValue axisValue) {
                    char[] str = sdf.format(new Date((long) axisValue.getValue())).toCharArray();
                    System.arraycopy(str, 0, chars, chars.length - str.length, str.length);
                    return str.length;
                }
            });
            Axis yAxis = new Axis().setName(getString(R.string.amount))
                    .setTextColor(Color.BLACK)
                    .setMaxLabelChars(6);
            lcd.setAxisYLeft(yAxis);
            lcd.setAxisXBottom(xAxis);
            
            mChart.setLineChartData(lcd);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
