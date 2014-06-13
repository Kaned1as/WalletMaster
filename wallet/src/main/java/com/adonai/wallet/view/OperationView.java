package com.adonai.wallet.view;

import android.animation.LayoutTransition;
import android.content.Context;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.PathShape;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.adonai.wallet.R;
import com.adonai.wallet.entities.Operation;

import java.util.HashMap;
import java.util.Map;

import static com.adonai.wallet.Utils.VIEW_DATE_FORMAT;
import static com.adonai.wallet.Utils.convertPixelsToDp;

/**
 * Created by adonai on 12.06.14.
 */
public class OperationView extends FrameLayout {

    private enum State {
        COLLAPSED,
        EXPANDED
    }

    private Operation mOperation;
    private State mState = State.COLLAPSED;
    private final static Map<Operation.OperationType, Drawable> mDrawableMap = new HashMap<>(3);

    private View mCollapsedView;

    public OperationView(Context context) {
        super(context);
        setLayoutTransition(new LayoutTransition());
        final LayoutInflater inflater = LayoutInflater.from(context);
        mCollapsedView = inflater.inflate(R.layout.operation_list_item, this, true);
        synchronized (mDrawableMap) {
            if (mDrawableMap.isEmpty())
                fillDrawableMap(context, mDrawableMap);
        }
    }

    public Operation getOperation() {
        return mOperation;
    }

    public void setOperation(Operation operation) {
        this.mOperation = operation;
        onOperationChanged();
    }

    private void onOperationChanged() {
        findViewById(R.id.main_content_layout).setBackgroundDrawable(mDrawableMap.get(mOperation.getOperationType()));

        final TextView chargeAcc = (TextView) findViewById(R.id.charge_account_label);
        final TextView benefAcc = (TextView) findViewById(R.id.beneficiar_account_label);
        final TextView benefAmount = (TextView) findViewById(R.id.beneficiar_amount_label);
        final TextView chargeAmount = (TextView) findViewById(R.id.charge_amount_label);
        final TextView description = (TextView) findViewById(R.id.operation_description_label);
        final TextView operationTime = (TextView) findViewById(R.id.operation_time_label);
        final TextView operationCategory = (TextView) findViewById(R.id.operation_category_label);

        description.setText(mOperation.getDescription());
        operationTime.setText(VIEW_DATE_FORMAT.format(mOperation.getTime().getTime()));
        operationCategory.setText(mOperation.getCategory().getName());

        switch (mOperation.getOperationType()) {
            case TRANSFER:
                chargeAcc.setText(mOperation.getCharger().getName());
                benefAcc.setText(mOperation.getBeneficiar().getName());
                chargeAmount.setText(mOperation.getAmount().toPlainString());
                benefAmount.setText(mOperation.getAmountDelivered().toPlainString());
                break;
            case INCOME:
                chargeAcc.setText("");
                chargeAmount.setText("");

                benefAcc.setText(mOperation.getBeneficiar().getName());
                benefAmount.setText(mOperation.getAmountDelivered().toPlainString());
                break;
            case EXPENSE:
                benefAcc.setText("");
                benefAmount.setText("");

                chargeAcc.setText(mOperation.getCharger().getName());
                chargeAmount.setText(mOperation.getAmount().toPlainString());
                break;
        }
    }

    private Map<Operation.OperationType, Drawable> fillDrawableMap(Context context, Map<Operation.OperationType, Drawable> result) {
        // EXPENSE
        final Path expensePath = new Path();
        expensePath.moveTo(0, 50);
        expensePath.lineTo(15, 100);
        expensePath.lineTo(400, 100);
        expensePath.lineTo(400, 0);
        expensePath.lineTo(15, 0);
        //path.lineTo(0, 50);
        expensePath.close();

        final ShapeDrawable expenseDrawable = new ShapeDrawable(new PathShape(expensePath, 400, 100));
        expenseDrawable.getPaint().setShader(new LinearGradient(0, 0, convertPixelsToDp(context.getResources().getDisplayMetrics().widthPixels, context), 0,
                Color.argb(50, 255, 0, 0), Color.argb(0, 255, 0, 0), Shader.TileMode.CLAMP)); // RED

        result.put(Operation.OperationType.EXPENSE, expenseDrawable);

        // INCOME
        final Path incomePath = new Path();
        incomePath.moveTo(0, 0);
        incomePath.lineTo(0, 100);
        incomePath.lineTo(385, 100);
        incomePath.lineTo(400, 50);
        incomePath.lineTo(385, 0);
        //path.lineTo(0, 0);
        incomePath.close();

        final ShapeDrawable incomeDrawable = new ShapeDrawable(new PathShape(incomePath, 400, 100));
        incomeDrawable.getPaint().setShader(new LinearGradient(0, 0, convertPixelsToDp(context.getResources().getDisplayMetrics().widthPixels, context), 0,
                Color.argb(0, 0, 255, 0), Color.argb(50, 0, 255, 0), Shader.TileMode.CLAMP)); // GREEN

        result.put(Operation.OperationType.INCOME, incomeDrawable);

        // TRANSFER
        final Path transferPath = new Path();
        transferPath.moveTo(0, 50);
        transferPath.lineTo(15, 100);
        transferPath.lineTo(385, 100);
        transferPath.lineTo(400, 50);
        transferPath.lineTo(385, 0);
        transferPath.lineTo(15, 0);
        //path.lineTo(0, 50);
        transferPath.close();

        final ShapeDrawable transferDrawable = new ShapeDrawable(new PathShape(transferPath, 400, 100));
        transferDrawable.getPaint().setShader(new LinearGradient(0, 0, convertPixelsToDp(context.getResources().getDisplayMetrics().widthPixels, context), 0,
                Color.argb(50, 255, 0, 0), Color.argb(50, 0, 255, 0), Shader.TileMode.CLAMP)); // RED -> GREEN

        result.put(Operation.OperationType.TRANSFER, transferDrawable);

        return result;
    }

    public void expand() {
        removeView(mCollapsedView);
    }
}
