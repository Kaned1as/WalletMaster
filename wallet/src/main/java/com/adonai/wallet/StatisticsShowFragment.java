package com.adonai.wallet;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

import com.adonai.wallet.adapters.WithDefaultAdapter;
import com.adonai.wallet.database.DbProvider;
import com.adonai.wallet.database.EntityDao;
import com.adonai.wallet.entities.Account;
import com.adonai.wallet.entities.Category;
import com.adonai.wallet.entities.Operation;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import lecho.lib.hellocharts.formatter.SimpleAxisValueFormatter;
import lecho.lib.hellocharts.gesture.ZoomType;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.SubcolumnValue;
import lecho.lib.hellocharts.view.ColumnChartView;
import lecho.lib.hellocharts.view.LineChartView;

/**
 * Fragment for showing various statistics charts
 */
public class StatisticsShowFragment extends Fragment {

    private ColumnChartView mChart;
    private Spinner mAccountSpinner, mCategorySpinner;
    private WithDefaultAdapter<Account> mAccountAdapter;
    private WithDefaultAdapter<Category> mCategoryAdapter;
    
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.common_statistics_flow, container, false);
        assert rootView != null;
        
        mChart = (ColumnChartView) rootView.findViewById(R.id.chart);
        mChart.setInteractive(true);
        mChart.setZoomEnabled(true);
        mChart.setZoomType(ZoomType.HORIZONTAL_AND_VERTICAL);
        mChart.setMaxZoom(5000f);
        mChart.setValueSelectionEnabled(true);

        mAccountSpinner = (Spinner) rootView.findViewById(R.id.account_spinner);
        mAccountAdapter = new WithDefaultAdapter<>(this, Account.class, R.string.all);
        mAccountSpinner.setAdapter(mAccountAdapter);
        mCategorySpinner = (Spinner) rootView.findViewById(R.id.category_spinner);
        mCategoryAdapter = new WithDefaultAdapter<>(this, Category.class, R.string.all);
        mCategorySpinner.setAdapter(mCategoryAdapter);
        
        mAccountSpinner.setOnItemSelectedListener(new SelectListener());
        mCategorySpinner.setOnItemSelectedListener(new SelectListener());

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    private void fillOperationsByTime(Account acc, Category cat) {
        // get list of operations for last 3 months
        try {
            Calendar last3Months = Calendar.getInstance();
            last3Months.setLenient(true);
            last3Months.set(Calendar.DATE, last3Months.get(Calendar.DATE) - 90);
            
            EntityDao<Operation> dao = DbProvider.getHelper().getDao(Operation.class);
            QueryBuilder<Operation, UUID> qb = dao.queryBuilder().orderBy("time", false);
            Where<Operation, UUID> whereExpense = qb.where().ge("time", last3Months.getTime());
            Where<Operation, UUID> whereBeneficiar = dao.queryBuilder().where().ge("time", last3Months.getTime());
            if(acc != null) {
                whereExpense.and().eq("orderer_id", acc).and().isNull("beneficiar_id");
                whereBeneficiar.and().eq("beneficiar_id", acc).and().isNull("orderer_id");
            } else {
                whereExpense.and().isNotNull("orderer_id").and().isNull("beneficiar_id");
                whereBeneficiar.and().isNotNull("beneficiar_id").and().isNull("orderer_id");
            }
            if(cat != null) {
                whereExpense.and().eq("category_id", cat);
                whereBeneficiar.and().eq("category_id", cat);
            }

            qb.setWhere(whereExpense);
            List<Operation> expenseOperations = qb.query();

            qb.setWhere(whereBeneficiar);
            List<Operation> benefitOperations = qb.query();
            
            // fill array of points
            List<SubcolumnValue> expensePoints = new ArrayList<>(expenseOperations.size());
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
            if(expenseOperations.isEmpty()) {
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

    private class SelectListener implements AdapterView.OnItemSelectedListener {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            fillOperationsByTime(
                    mAccountAdapter.getItem(mAccountSpinner.getSelectedItemPosition()), 
                    mCategoryAdapter.getItem(mCategorySpinner.getSelectedItemPosition()));
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    }

    private long floorToDay(Date date) {
        Calendar current = Calendar.getInstance();
        current.setTime(date);
        current.set(Calendar.HOUR, 0);
        current.set(Calendar.MINUTE, 0);
        current.set(Calendar.SECOND, 0);
        current.set(Calendar.MILLISECOND, 0);
        return current.getTimeInMillis();
    }
}
