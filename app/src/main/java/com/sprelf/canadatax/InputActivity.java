package com.sprelf.canadatax;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

public class InputActivity extends AppCompatActivity
{

    private Spinner currency, year, province, annually;
    private EditText income, rrsp, hours;
    private TextView errorText;

    private static final float HOURLY_RATE_WARNING_THRESHOLD = 1000f;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input);

        initializeFields(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.action_about:
                startActivity(new Intent(this, AboutActivity.class));
                break;
        }
        return true;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState)
    {
        savedInstanceState.putAll(bundleValues());
    }

    public void onContinueClick(View view)
    {
        if (annually.getSelectedItemPosition() == 1 &&
                Float.parseFloat(income.getText().toString()) >= HOURLY_RATE_WARNING_THRESHOLD)
        {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.Input_HourlyRateTooHighError_PopupTitle)
                    .setMessage(R.string.Input_HourlyRateTooHighError_PopupMessage)
                    .setPositiveButton(R.string.Input_HourlyRateTooHighError_PopupConfirm,
                                       new DialogInterface.OnClickListener()
                                       {
                                           @Override
                                           public void onClick(DialogInterface dialogInterface,
                                                               int i)
                                           {
                                               // Do nothing
                                           }
                                       })
                    .create().show();

        } else
            passValuesToSummary();
    }

    private void initializeFields(Bundle saved)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        currency = (Spinner) findViewById(R.id.Input_CurrencySpinner);
        year = (Spinner) findViewById(R.id.Input_YearSpinner);
        province = (Spinner) findViewById(R.id.Input_ProvinceSpinner);
        annually = (Spinner) findViewById(R.id.Input_AnnualHourlySpinner);
        hours = (EditText) findViewById(R.id.Input_HoursInput);
        income = (EditText) findViewById(R.id.Input_IncomeInput);
        rrsp = (EditText) findViewById(R.id.Input_RRSPInput);
        errorText = (TextView) findViewById(R.id.Input_ErrorText);

        if (saved != null && saved.containsKey("income") && saved.getFloat("income") != 0)
        {
            float incomeValue = saved.getFloat("income");
            if (saved.containsKey("annual") && !saved.getBoolean("annual") &&
                    saved.containsKey("hours") && saved.getFloat("hours") != 0)
            {
                incomeValue /= (saved.getFloat("hours") * 52);
            }
            if (saved.containsKey("currency") && saved.getString("currency") != null &&
                    !saved.getString("currency").equals("CAD"))
            {
                incomeValue = CanadaTax.exchanger.convertFromCADTo(saved.getString("currency"),
                                                               incomeValue);
            }
            income.setText(Float.toString(incomeValue));
        }

        if (saved != null && saved.containsKey("rrsp") && saved.getFloat("rrsp") != 0)
            rrsp.setText(Float.toString(saved.getFloat("rrsp")));
        if (saved != null && saved.containsKey("hours") && saved.getFloat("hours") != 0)
            hours.setText(Float.toString(saved.getFloat("hours")));
        else
            hours.setText(R.string.Input_DefaultHours);

        // Add listener to income box
        income.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2)
            {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2)
            {

            }

            @Override
            public void afterTextChanged(Editable editable)
            {
                if (annually.getSelectedItemPosition() == 1 && editable != null &&
                        !editable.toString().equals("") &&
                        Float.parseFloat(editable.toString()) > HOURLY_RATE_WARNING_THRESHOLD)
                {
                    errorText.setText(R.string.Input_HourlyRateTooHighError);
                } else if (errorText.getText() != null && errorText.getText().toString().equals(
                        getString(R.string.Input_HourlyRateTooHighError)))
                {
                    errorText.setText("");
                }

            }
        });

        // Construct Currency Spinner
        CanadaTax.constructCurrencySpinner(this,
                                           (Spinner) findViewById(R.id.Input_CurrencySpinner),
                                           "Input_Curr",
                                           null);


        // Construct Year Spinner
        ArrayAdapter<CharSequence> adapter2 =
                ArrayAdapter.createFromResource(this,
                                                R.array.Years,
                                                android.R.layout.simple_spinner_item);
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        year.setAdapter(adapter2);
        if (saved != null && saved.containsKey("year"))
            year.setSelection(adapter2.getPosition(saved.getString("year")));


        // Construct Province Spinner
        ArrayAdapter<CharSequence> adapter3 =
                ArrayAdapter.createFromResource(this,
                                                R.array.Provinces,
                                                android.R.layout.simple_spinner_item);
        adapter3.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        province.setAdapter(adapter3);
        String defaultProvince = prefs.getString("Input_Prov", "Alberta");
        province.setSelection(adapter3.getPosition(defaultProvince));
        province.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l)
            {
                SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(
                        getApplicationContext()).edit();
                edit.putString("Input_Prov", (String) adapterView.getItemAtPosition(i));
                edit.apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView)
            {

            }
        });

        // Construct Annual/Hourly Spinner
        // Construct Province Spinner
        ArrayAdapter<CharSequence> adapter4 =
                ArrayAdapter.createFromResource(this,
                                                R.array.AnnualHourly,
                                                android.R.layout.simple_spinner_item);
        adapter4.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        annually.setAdapter(adapter4);
        if (saved != null && saved.containsKey("annual"))
            annually.setSelection(saved.getBoolean("annual") ? 0 : 1);
        annually.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l)
            {

                if (i == 1)
                {
                    findViewById(R.id.Input_HourInputArea).setVisibility(View.VISIBLE);
                    if (income.getText() != null && !income.getText().toString().equals("") &&
                            Float.parseFloat(income.getText().toString()) >
                                    HOURLY_RATE_WARNING_THRESHOLD)
                        errorText.setText(R.string.Input_HourlyRateTooHighError);

                } else
                {
                    findViewById(R.id.Input_HourInputArea).setVisibility(View.GONE);

                    if (errorText.getText() != null && errorText.getText().toString().equals(
                            getString(R.string.Input_HourlyRateTooHighError)))
                        errorText.setText("");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView)
            {

            }
        });
    }

    private void passValuesToSummary()
    {
        Intent intent = new Intent(this, OutputActivity.class);

        intent.putExtras(bundleValues());

        startActivity(intent);
    }

    private Bundle bundleValues()
    {
        Bundle returnBundle = new Bundle();

        float incomeValue = income.getText() != null && !income.getText().toString().equals("") ?
                            Float.parseFloat(income.getText().toString()) : 0f;
        float rrspValue = rrsp.getText() != null && !rrsp.getText().toString().equals("") ?
                          Float.parseFloat(rrsp.getText().toString()) : 0f;
        String currencyValue = currency.getSelectedItem().toString();
        boolean annualValue = annually.getSelectedItem() == null ||
                annually.getSelectedItemPosition() == 0;
        float hoursValue = hours.getText() != null && !hours.getText().toString().equals("") ?
                           Float.parseFloat(hours.getText().toString()) : 40f;

        if (!annualValue)
            incomeValue *= hoursValue * 52;


        if (!currencyValue.equals("CAD"))
        {
            incomeValue = CanadaTax.exchanger.convertToCAD(currencyValue, incomeValue);
            rrspValue = CanadaTax.exchanger.convertToCAD(currencyValue, incomeValue);
        }


        returnBundle.putString("year", year.getSelectedItem() != null ?
                                       year.getSelectedItem().toString() : "2015");
        returnBundle.putString("province", province.getSelectedItem() != null ?
                                           province.getSelectedItem().toString() : "Alberta");
        returnBundle.putFloat("income", incomeValue);
        returnBundle.putFloat("rrsp", rrspValue);
        returnBundle.putFloat("hours", hoursValue);
        returnBundle.putBoolean("annual", annualValue);
        returnBundle.putString("currency", currencyValue);

        return returnBundle;
    }
}
