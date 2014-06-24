package com.adonai.wallet;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.adonai.wallet.entities.Account;
import com.adonai.wallet.entities.Category;
import com.adonai.wallet.entities.UUIDSpinnerAdapter;

/**
 * Dialog fragment for category creation/deletion
 *
 * @author Adonai
 */
public class CategoryDialogFragment extends WalletBaseDialogFragment implements DialogInterface.OnClickListener {

    private static final String CATEGORY_REFERENCE = "category.reference";
    private static final String CATEGORY_TYPE_REFERENCE = "category.type.reference";

    private Spinner mPreferredAccSpinner;
    private AccountsWithNoneAdapter mAccountAdapter;
    private EditText mCategoryName;
    private int mCategoryType;

    private Category mCategory;
    private OnCategoryCreateListener mListener;

    public interface OnCategoryCreateListener {
        void handleCategoryCreate(String categoryId);
    }

    public void setOnCategoryCreateListener(OnCategoryCreateListener mListener) {
        this.mListener = mListener;
    }

    public static CategoryDialogFragment forCategory(String categoryId) {
        final CategoryDialogFragment fragment = new CategoryDialogFragment();
        final Bundle args = new Bundle();
        args.putString(CATEGORY_REFERENCE, categoryId);
        fragment.setArguments(args);
        return fragment;
    }

    public static CategoryDialogFragment newInstance(int categoryType) {
        final CategoryDialogFragment fragment = new CategoryDialogFragment();
        final Bundle args = new Bundle();
        args.putInt(CATEGORY_TYPE_REFERENCE, categoryType);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if(getArguments() == null)
            throw new IllegalArgumentException("Should provide category type or existing category as parameter!");

        final View dialog = LayoutInflater.from(getActivity()).inflate(R.layout.category_create_modify_dialog, null);
        assert dialog != null;


        mPreferredAccSpinner = (Spinner) dialog.findViewById(R.id.preferred_account_spinner);
        mCategoryName = (EditText) dialog.findViewById(R.id.name_edit);

        mAccountAdapter = new AccountsWithNoneAdapter(R.string.none);
        mPreferredAccSpinner.setAdapter(mAccountAdapter);

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(dialog);

        // modifying existing category
        if(getArguments().containsKey(CATEGORY_REFERENCE)) {
            builder.setTitle(R.string.edit_category);
            builder.setPositiveButton(R.string.confirm, this);

            mCategory = Category.getFromDB(getWalletActivity().getEntityDAO(), getArguments().getString(CATEGORY_REFERENCE));
            mCategoryName.setText(mCategory.getName());
            if(mCategory.getPreferredAccount() != null) // optional
                mPreferredAccSpinner.setSelection(mAccountAdapter.getPosition(mCategory.getPreferredAccount().getId()));
        } else { // new category create
            builder.setTitle(R.string.create_new_category);
            builder.setPositiveButton(R.string.create, this);

            mCategoryType = getArguments().getInt(CATEGORY_TYPE_REFERENCE);
        }

        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if(mCategory != null) { // modifying existing category
            mCategory.setName(mCategoryName.getText().toString());
            if(mPreferredAccSpinner.getSelectedItem() != null)
                mCategory.setPreferredAccount(Account.getFromDB(getWalletActivity().getEntityDAO(), mAccountAdapter.getItemUUID(mPreferredAccSpinner.getSelectedItemPosition())));
            else if (mCategory.getPreferredAccount() != null)
                mCategory.setPreferredAccount(null);
            if(getWalletActivity().getEntityDAO().makeAction(DatabaseDAO.ActionType.MODIFY, mCategory))
                dismiss();
            else
                Toast.makeText(getActivity(), R.string.category_not_found, Toast.LENGTH_SHORT).show();
        } else { // new category
            final Category tempCat = new Category(mCategoryName.getText().toString(), mCategoryType);
            if(mPreferredAccSpinner.getSelectedItem() != null)
                tempCat.setPreferredAccount(Account.getFromDB(getWalletActivity().getEntityDAO(), mAccountAdapter.getItemUUID(mPreferredAccSpinner.getSelectedItemPosition())));
            if(getWalletActivity().getEntityDAO().makeAction(DatabaseDAO.ActionType.ADD, tempCat)) {
                if(mListener != null)
                    mListener.handleCategoryCreate(tempCat.getId()); // we have it set at this moment
                dismiss();
            }
            else
                Toast.makeText(getActivity(), R.string.category_exists, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mAccountAdapter.changeCursor(null); // close opened cursor
    }
}
