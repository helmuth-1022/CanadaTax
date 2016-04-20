package com.sprelf.canadatax;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

/**
 * Created by Chris on 27.12.2015.
 */
public class CanadaTax extends Application
{

    public static CurrencyExchanger exchanger;
    @Override
    public void onCreate()
    {
        exchanger = new CurrencyExchanger(this);
        exchanger.updateOnline();
        super.onCreate();
    }

    public static void constructCurrencySpinner(final Context c, Spinner currency,
                                                final String prefName,
                                                final Runnable updater)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        // Construct Currency Spinner
        ArrayAdapter<CharSequence> adapter =
                new ArrayAdapter<>(c,
                                   android.R.layout.simple_spinner_item,
                                   CanadaTax.exchanger.getCurrencyArray());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        String defaultCurrency = prefs.getString(prefName, "CAD");
        currency.setAdapter(adapter);
        currency.setSelection(adapter.getPosition(defaultCurrency));
        currency.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l)
            {
                SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(c)
                                                                 .edit();
                edit.putString(prefName, (String)adapterView.getItemAtPosition(i));
                edit.apply();
                if (updater!=null)
                    updater.run();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView)
            {

            }
        });
    }

}
