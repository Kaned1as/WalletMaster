package com.adonai.wallet.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.adonai.wallet.R;
import com.adonai.wallet.entities.Entity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Array adapter for showing entities that have a field "name".
 * <br/>
 * Needed for filtering foreign fields in {@link com.adonai.wallet.WalletBaseFilterFragment}
 *
 * @author Adonai
 */
public class UUIDArrayAdapter extends ArrayAdapter<Entity> {

    public UUIDArrayAdapter(Context context, List<Entity> objects) {
        super(context, android.R.layout.simple_list_item_1, objects);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return newView(position, convertView, parent, R.layout.tall_list_item);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return newView(position, convertView, parent, android.R.layout.simple_spinner_item);
    }

    public View newView(int position, View convertView, ViewGroup parent, int resId) {
        final View view;
        final Entity item = getItem(position);
        if (convertView == null) {
            final LayoutInflater inflater = LayoutInflater.from(getContext());
            view = inflater.inflate(resId, parent, false);
        } else
            view = convertView;

        try {
            Method nameGetter = item.getClass().getDeclaredMethod("getName", (Class<?>[]) null);
            String name = (String) nameGetter.invoke(item);

            final TextView nameText = (TextView) view.findViewById(android.R.id.text1);
            nameText.setText(name);
        } catch (Exception e) { // workaround to API 19 multi-catch incompatibility
            e.printStackTrace();
        }

        return view;
    }
}
