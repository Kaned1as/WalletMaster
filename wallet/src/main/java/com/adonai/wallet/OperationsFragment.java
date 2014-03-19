package com.adonai.wallet;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.PathShape;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.adonai.wallet.entities.Operation;
import com.daniel.lupianez.casares.PopoverView;

import java.util.EnumMap;
import java.util.Map;

import static com.adonai.wallet.Utils.VIEW_DATE_FORMAT;
import static com.adonai.wallet.Utils.convertDpToPixel;
import static com.adonai.wallet.Utils.convertPixelsToDp;
import static com.adonai.wallet.entities.Operation.OperationType;

/**
 * @author adonai
 */
public class OperationsFragment extends WalletBaseFragment {

    private ListView mOperationsList;
    private OperationsAdapter mOpAdapter;
    private Map<Operation.OperationType, Drawable> mDrawableMap;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        mOpAdapter = new OperationsAdapter();
        mDrawableMap = fillDrawableMap();

        final View rootView = inflater.inflate(R.layout.operations_flow, container, false);
        assert rootView != null;

        mOperationsList = (ListView) rootView.findViewById(R.id.operations_list);

        mOperationsList.setAdapter(mOpAdapter);
        mOperationsList.setOnItemLongClickListener(new OperationLongClickListener());
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
        public void bindView(final View view, Context context, final Cursor cursor) {
            final DatabaseDAO db = getWalletActivity().getEntityDAO();
            db.getAsyncOperation(cursor.getLong(DatabaseDAO.OperationsFields._id.ordinal()), new DatabaseDAO.AsyncDbQuery.Listener<Operation>() {
                @Override
                public void onFinishLoad(Operation op) {
                    view.setBackgroundDrawable(mDrawableMap.get(op.getOperationType()));
                    final LinearLayout chargeLayout = (LinearLayout) view.findViewById(R.id.charger_layout);
                    final LinearLayout beneficiarLayout = (LinearLayout) view.findViewById(R.id.beneficiar_layout);

                    final TextView chargeAcc = (TextView) view.findViewById(R.id.charge_account_label);
                    final TextView benefAcc = (TextView) view.findViewById(R.id.beneficiar_account_label);
                    final TextView benefAmount = (TextView) view.findViewById(R.id.beneficiar_amount_label);
                    final TextView chargeAmount = (TextView) view.findViewById(R.id.charge_amount_label);
                    final TextView description = (TextView) view.findViewById(R.id.operation_description_label);
                    final TextView operationTime = (TextView) view.findViewById(R.id.operation_time_label);

                    description.setText(op.getDescription());
                    operationTime.setText(VIEW_DATE_FORMAT.format(op.getTime().getTime()));

                    switch (op.getOperationType()) {
                        case TRANSFER:
                            chargeLayout.setVisibility(View.VISIBLE);
                            beneficiarLayout.setVisibility(View.VISIBLE);

                            chargeAcc.setText(op.getCharger().getName());
                            benefAcc.setText(op.getBeneficiar().getName());
                            chargeAmount.setText(op.getAmount().toPlainString());
                            benefAmount.setText(op.getAmountDelivered().toPlainString());
                            break;
                        case INCOME:
                            chargeLayout.setVisibility(View.GONE);
                            beneficiarLayout.setVisibility(View.VISIBLE);

                            benefAcc.setText(op.getBeneficiar().getName());
                            benefAmount.setText(op.getAmountDelivered().toPlainString());
                            break;
                        case EXPENSE:
                            chargeLayout.setVisibility(View.VISIBLE);
                            beneficiarLayout.setVisibility(View.GONE);

                            chargeAcc.setText(op.getCharger().getName());
                            chargeAmount.setText(op.getAmount().toPlainString());
                            break;
                    }
                }
            });
        }

        @Override
        public void handleUpdate() {
            changeCursor(getWalletActivity().getEntityDAO().getOperationsCursor());
        }
    }

    private class OperationLongClickListener implements AdapterView.OnItemLongClickListener {

        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, final long id) {
            final View newView = LayoutInflater.from(getActivity()).inflate(R.layout.account_list_item_menu, null, false);
            final PopoverView popover = new PopoverView(getActivity(), newView);
            popover.setContentSizeForViewInPopover(new Point((int) convertDpToPixel(100, getActivity()), (int) convertDpToPixel(50, getActivity())));

            final ImageButton delete = (ImageButton) newView.findViewById(R.id.delete_button);
            delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.confirm_action)
                            .setMessage(R.string.really_delete_operation)
                            .setNegativeButton(android.R.string.cancel, null)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    final DatabaseDAO db = getWalletActivity().getEntityDAO();
                                    final Operation op = db.getOperation(id);
                                    if (op != null) {
                                        if (!db.revertOperation(op))
                                            throw new IllegalStateException("Cannot delete operation!"); // should never happen!!
                                    }
                                }
                            }).create().show();
                    popover.dismissPopover(true);
                }
            });

            final ImageButton edit = (ImageButton) newView.findViewById(R.id.edit_button);
            edit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final Operation operation = getWalletActivity().getEntityDAO().getOperation(id);
                    new OperationDialogFragment(operation).show(getFragmentManager(), "opModify");
                    popover.dismissPopover(true);
                }
            });

            popover.showPopoverFromRectInViewGroup((ViewGroup) parent.getRootView(), PopoverView.getFrameForView(view), PopoverView.PopoverArrowDirectionUp, true);
            return true;
        }
    }

    private Map<OperationType, Drawable> fillDrawableMap() {
        Map<OperationType, Drawable> result = new EnumMap<>(OperationType.class);
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
        expenseDrawable.getPaint().setShader(new LinearGradient(0, 0, convertPixelsToDp(getActivity().getResources().getDisplayMetrics().widthPixels, getActivity()), 0,
                Color.argb(50, 255, 0, 0), Color.argb(0, 255, 0, 0), Shader.TileMode.CLAMP)); // RED

        result.put(OperationType.EXPENSE, expenseDrawable);

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
        incomeDrawable.getPaint().setShader(new LinearGradient(0, 0, convertPixelsToDp(getActivity().getResources().getDisplayMetrics().widthPixels, getActivity()), 0,
                Color.argb(0, 0, 255, 0), Color.argb(50, 0, 255, 0), Shader.TileMode.CLAMP)); // GREEN

        result.put(OperationType.INCOME, incomeDrawable);

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
        transferDrawable.getPaint().setShader(new LinearGradient(0, 0, convertPixelsToDp(getActivity().getResources().getDisplayMetrics().widthPixels, getActivity()), 0,
                Color.argb(50, 255, 0, 0), Color.argb(50, 0, 255, 0), Shader.TileMode.CLAMP)); // RED -> GREEN

        result.put(OperationType.TRANSFER, transferDrawable);

        return result;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getWalletActivity().getEntityDAO().unregisterDatabaseListener(DatabaseDAO.OPERATIONS_TABLE_NAME, mOpAdapter);
        mOpAdapter.changeCursor(null); // close opened cursor
    }
}
