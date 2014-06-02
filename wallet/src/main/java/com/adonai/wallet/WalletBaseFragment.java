package com.adonai.wallet;


import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.widget.CheckBox;
import android.widget.ListView;

import com.adonai.wallet.entities.Entity;
import com.adonai.wallet.entities.UUIDCursorAdapter;

import org.thirdparty.contrib.SwipeDismissListViewTouchListener;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static com.adonai.wallet.WalletPreferencesFragment.ASK_FOR_DELETE;

/**
 * All wallet master fragments must extend this class
 *
 * @author adonai
 */
public abstract class WalletBaseFragment extends Fragment {

    final public WalletBaseActivity getWalletActivity() {
        return (WalletBaseActivity) getActivity();
    }

    protected class EntityDeleteListener<T extends Entity> implements SwipeDismissListViewTouchListener.DismissCallbacks {

        private final int mMessageId;
        private final UUIDCursorAdapter mAdapter;
        private final Class<T> mEntityClass;

        public EntityDeleteListener(UUIDCursorAdapter adapter, Class<T> clazz, int deleteMessageResource) {
            mMessageId = deleteMessageResource;
            mAdapter = adapter;
            mEntityClass = clazz;
        }

        @Override
        public boolean canDismiss(int position) {
            return true;
        }

        @Override
        public void onDismiss(ListView listView, final int[] reverseSortedPositions) {
            final LayoutInflater inflater = LayoutInflater.from(getActivity());
            final DatabaseDAO db = getWalletActivity().getEntityDAO();
            if (getWalletActivity().getPreferences().getBoolean(ASK_FOR_DELETE, true)) { // check to assure we want to delete entity
                final CheckBox dontShowAgain = (CheckBox) inflater.inflate(R.layout.remember_choice_checkbox, null, false);
                dontShowAgain.setOnCheckedChangeListener(new WalletPreferencesFragment.DontAskForDelete(getWalletActivity()));
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.confirm_action)
                        .setMessage(mMessageId)
                        .setView(dontShowAgain)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                deleteItems(db, reverseSortedPositions);
                            }
                        }).create().show();
            } else // just delete without confirm
                deleteItems(db, reverseSortedPositions);
        }

        @SuppressWarnings("unchecked")
        private void deleteItems(DatabaseDAO db, int[] items) {
            try {
                final Method filler = mEntityClass.getDeclaredMethod("getFromDB", DatabaseDAO.class, String.class);

                for (int position : items) {
                    final String itemUUID = mAdapter.getItemUUID(position);
                    final T newEntity = (T) filler.invoke(null, db, itemUUID);
                    db.makeAction(DatabaseDAO.ActionType.DELETE, newEntity);
                }
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("No db getter method in entity class!");
            }
        }
    }
}
