package com.adonai.wallet;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Pair;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by adonai on 12.06.14.
 */
public class WalletBaseFilterFragment extends WalletBaseDialogFragment {

    public enum FilterType {
        AMOUNT,
        TEXT,
        DATE,
        FOREIGN_ID
    }

                   /* caption,     filter    , column    */
    private final Map<String, Pair<FilterType, String>> mFilterAllowedMap = new HashMap<>(10);

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return super.onCreateDialog(savedInstanceState);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
    }
}
