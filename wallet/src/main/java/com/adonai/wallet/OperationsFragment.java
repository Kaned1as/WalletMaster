package com.adonai.wallet;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.PathShape;
import android.graphics.drawable.shapes.Shape;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.adonai.wallet.entities.Category;
import com.adonai.wallet.entities.Operation;

import java.text.SimpleDateFormat;

/**
 * @author adonai
 */
public class OperationsFragment extends WalletBaseFragment {

    private ListView mOperationsList;
    private OperationsAdapter mOpAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        mOpAdapter = new OperationsAdapter();

        final View rootView = inflater.inflate(R.layout.operations_flow, container, false);
        assert rootView != null;

        mOperationsList = (ListView) rootView.findViewById(R.id.operations_list);
        mOperationsList.setAdapter(mOpAdapter);
        getWalletActivity().getEntityDAO().registerDatabaseListener(DatabaseDAO.OPERATIONS_TABLE_NAME, mOpAdapter);

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.operations_flow, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_operation:
                final OperationDialogFragment opCreate = new OperationDialogFragment();
                opCreate.show(getFragmentManager(), "opCreate");
                break;
            default :
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private class OperationsAdapter extends CursorAdapter implements DatabaseDAO.DatabaseListener {
        public OperationsAdapter() {
            super(getActivity(), getWalletActivity().getEntityDAO().getOperationsCursor(), false);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            final LayoutInflater inflater = LayoutInflater.from(context);
            return inflater.inflate(R.layout.operation_list_item, viewGroup, false);
        }

        @Override
        @SuppressWarnings("deprecation") // for compat with older APIs
        public void bindView(View view, Context context, Cursor cursor) {
            final DatabaseDAO db = getWalletActivity().getEntityDAO();
            final Operation op = db.getOperation(cursor.getLong(DatabaseDAO.OperationsFields._id.ordinal()));
            final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
            final int accColor = op.getBeneficiar() != null
                    ? Color.BLUE
                    : op.getCategory().getType() == Category.EXPENSE
                        ? Color.RED
                        : Color.GREEN;

            final Path path = new Path();
            path.moveTo(0, 50);
            path.lineTo(15, 100);
            path.lineTo(400, 100);
            path.lineTo(400, 0);
            path.lineTo(15, 0);
            //path.lineTo(0, 50);
            path.close();


            Shape hexagonalShape = new PathShape(path, 400, 100);
            final ShapeDrawable mDrawable = new ShapeDrawable(hexagonalShape);
            mDrawable.getPaint().setShader(new LinearGradient(0, 0, context.getResources().getDisplayMetrics().widthPixels, 0,
                    Color.argb(50, Color.red(accColor), Color.green(accColor), Color.blue(accColor)),
                    Color.argb(0, Color.red(accColor), Color.green(accColor), Color.blue(accColor)), Shader.TileMode.CLAMP));
            view.setBackgroundDrawable(mDrawable);

            final TextView chargeAcc = (TextView) view.findViewById(R.id.charge_account_label);
            chargeAcc.setText(op.getCharger().getName());
            final TextView description = (TextView) view.findViewById(R.id.operation_description_label);
            description.setText(op.getDescription());
            final TextView chargeAmount = (TextView) view.findViewById(R.id.charge_amount_label);
            chargeAmount.setText(op.getAmountCharged().toPlainString());
            final TextView operationTime = (TextView) view.findViewById(R.id.operation_time_label);
            operationTime.setText(sdf.format(op.getTime().getTime()));
        }

        @Override
        public void handleUpdate() {
            changeCursor(getWalletActivity().getEntityDAO().getOperationsCursor());
        }
    }
}
