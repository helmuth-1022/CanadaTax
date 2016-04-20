package com.sprelf.canadatax;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.Currency;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Created by Chris on 27.12.2015.
 */
public class CurrencyExchanger
{
    private Map<String, Locale> locales;
    private Map<String, Float> rates;
    private Set<ExchangerListener> listeners;

    private Context c;

    private static final long MIN_UPDATE_WAIT = 60 * 60 * 1000;  // One hour

    public CurrencyExchanger(Context c)
    {
        locales = new HashMap<>();
        rates = new HashMap<>();
        listeners = new HashSet<>();

        locales.put("CAD", Locale.CANADA);
        locales.put("USD", Locale.US);
        locales.put("GBP", Locale.UK);
        locales.put("EUR", null);
        locales.put("AUD", Locale.CANADA);
        locales.put("CNY", Locale.CHINA);
        locales.put("JPY", Locale.JAPAN);
        locales.put("RUB", null);
        locales.put("LKR", null);

        this.c = c;


        getHistoricalRates();
    }

    public Locale getLocale(String id)
    {
        return locales.get(id);
    }

    public float getRate(String id)
    {
        return rates.get(id);
    }

    public Set<String> getCurrencies()
    {
        return rates.keySet();
    }

    public CharSequence[] getCurrencyArray()
    {
        CharSequence[] returnArray = new CharSequence[rates.size()];
        int i = 0;
        for (String s : rates.keySet())
        {
            returnArray[i] = s;
            i++;
        }
        return returnArray;
    }

    public float convertFromCADTo(String id, float value)
    {
        return value * rates.get(id);
    }

    public float convertToCAD(String id, float value)
    {
        return value / rates.get(id);
    }

    public float convert(String from, String to, float value)
    {
        return value * (rates.get(to) / rates.get(from));
    }

    public String format(String id, float value)
    {
        if (locales.get(id) == null)
        {
            NumberFormat format = NumberFormat.getCurrencyInstance();
            format.setCurrency(Currency.getInstance(id));
            return format.format(value);
        } else
        {
            return NumberFormat.getCurrencyInstance(locales.get(id)).format(value);
        }
    }

    // Convert from CAD
    public String convertAndFormat(String id, float value)
    {
        return format(id, convertFromCADTo(id, value));
    }

    public void getHistoricalRates()
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);

        // Default values are as of 27.12.15, values in relation to CAD
        rates.put("CAD", prefs.getFloat("CAD", 1f));
        rates.put("USD", prefs.getFloat("USD", 0.72f));
        rates.put("EUR", prefs.getFloat("EUR", 0.66f));
        rates.put("AUD", prefs.getFloat("AUD", 0.99f));
        rates.put("GBP", prefs.getFloat("GBP", 0.49f));
        rates.put("CNY", prefs.getFloat("CNY", 4.68f));
        rates.put("JPY", prefs.getFloat("JPY", 87.10f));
        rates.put("LKR", prefs.getFloat("LKR", 103.80f));
        rates.put("RUB", prefs.getFloat("RUB", 51.17f));
    }

    public void storeHistoricalRates()
    {
        SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(c).edit();

        for (Map.Entry<String, Float> entry : rates.entrySet())
            edit.putFloat(entry.getKey(), entry.getValue());

        edit.commit();

        callUpdateListeners();
    }


    public void addListener(ExchangerListener listener)
    {
        listeners.add(listener);
    }

    public void removeListener(ExchangerListener listener)
    {
        listeners.remove(listener);
    }

    private void callUpdateListeners()
    {
        for (ExchangerListener o : listeners)
            if (o != null)
                o.onCurrencyUpdate();
    }

    public void updateOnline()
    {

        Log.d("[Currencies]", "Attempting to start HTTP query...");

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);

        long lastUpdate = prefs.getLong("LastCurrencyUpdate", 0);
        if (lastUpdate != 0 && System.currentTimeMillis()-lastUpdate < MIN_UPDATE_WAIT)
            return;

        prefs.edit().putLong("LastCurrencyUpdate", System.currentTimeMillis()).apply();

        (new AsyncTask<String, String, String>()
        {

            @Override
            protected String doInBackground(String... strings)
            {
                String currencies = "";
                for (String s : rates.keySet())
                    currencies += "," + s;
                currencies = currencies.substring(1); // Remove initial comma
                // Borrowed code for HTTP queries
                String JSONString = JSONFetcher.getCurrencyData("CAD", currencies);

                if (JSONString != null && !JSONString.equals(""))
                {
                    try
                    {

                        JSONObject jObj = new JSONObject(JSONString);

                        String jObjString = jObj.toString();
                        Log.d("[HTTP]", "Success!\n" +
                                "JSON = " + jObjString.substring(0, (jObjString.length() < 100) ?
                                                                    jObjString.length() : 100) + "\n[...]");

                        JSONObject jsonRates = jObj.getJSONObject("quotes");


                        double baseRate = 1.;
                        String source = jObj.getString("source");
                        if (!source.equals("CAD"))
                        {
                            baseRate = jsonRates.getDouble(source + "CAD");
                        }

                        Iterator<String> keys = jsonRates.keys();
                        while (keys.hasNext())
                        {
                            String key = keys.next();

                            String from = key.substring(0, 3);
                            String to = key.substring(3);
                            double value = jsonRates.getDouble(key) / baseRate;

                            rates.put(to, (float) value);
                            Log.d("[Query]", to + ": " + rates.get(to));
                        }

                        Looper.prepare();
                        // Save changes
                        new Handler().post(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                storeHistoricalRates();
                            }
                        });

                    } catch (JSONException e)
                    {
                        // TODO:  Handle exception
                    }
                }
                return null;
            }
        }).execute();
    }

    public interface ExchangerListener
    {
        void onCurrencyUpdate();
    }


}
