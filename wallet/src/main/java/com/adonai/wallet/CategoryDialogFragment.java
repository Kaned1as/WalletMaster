package com.adonai.wallet;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;

import com.adonai.wallet.database.DbProvider;
import com.adonai.wallet.entities.Category;

import java.util.UUID;

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

            Category tmp = DbProvider.getHelper().getCategoryDao().queryForId(UUID.fromString(getArguments().getString(CATEGORY_REFERENCE)));
            mCategoryName.setText(tmp.getName());
            if(tmp.getPreferredAccount() != null) // optional
                mPreferredAccSpinner.setSelection(mAccountAdapter.getPosition(tmp.getPreferredAccount().getId()));
        } else { // new category create
            builder.setTitle(R.string.create_new_category);
            builder.setPositiveButton(R.string.create, this);

            mCategoryType = getArguments().getInt(CATEGORY_TYPE_REFERENCE);
        }

        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        Category tmp;
        if(getArguments() != null && getArguments().containsKey(CATEGORY_REFERENCE)) { // modifying existing category
            tmp = DbProvider.getHelper().getCategoryDao().queryForId(UUID.fromString(getArguments().getString(CATEGORY_REFERENCE)));
        } else {
            tmp = new Category();
        }

        tmp.setName(mCategoryName.getText().toString());
        tmp.setType(Category.CategoryType.values()[mCategoryType]);
        if(mPreferredAccSpinner.getSelectedItem() != null) {
            tmp.setPreferredAccount(DbProvider.getHelper().getAccountDao().queryForId(mAccountAdapter.getItemUUID(mPreferredAccSpinner.getSelectedItemPosition())));
        } else if (tmp.getPreferredAccount() != null) {
            tmp.setPreferredAccount(null);
        }
        DbProvider.getHelper().getCategoryDao().createOrUpdate(tmp);
        dismiss();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mAccountAdapter.closeCursor(); // close opened cursor
    }
}
