package com.sprelf.canadatax;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AboutActivity extends AppCompatActivity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        long lastUpdate = PreferenceManager.getDefaultSharedPreferences(this)
                                           .getLong("LastCurrencyUpdate",
                                                    1451217600000l); // 27.12.2015 12:00
        SimpleDateFormat format = new SimpleDateFormat("dd MMM, yyyy hh:mm", Locale.CANADA);

        TextView aboutText = (TextView) findViewById(R.id.About_Text);
        aboutText.setText(Html.fromHtml(
                getString(R.string.About_Text)
                        .replace("%t", format.format(new Date(lastUpdate)))));
    }
}
