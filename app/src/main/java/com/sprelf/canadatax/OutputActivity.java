package com.sprelf.canadatax;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class OutputActivity extends AppCompatActivity
{

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ouput);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        toolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);

        toolbar.setNavigationOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                onBackPressed();
            }
        });
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);


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


    public static Bundle calculateValues(Context c, Bundle calcData)
    {

        String province = calcData.getString("province");
        int year = Integer.parseInt(calcData.getString("year"));
        float income = calcData.getFloat("income");
        float rrsp = calcData.getFloat("rrsp");

        float provTaxIncome = income;
        float federalTaxIncome = income;
        float federalTax = 0;
        float fedTaxWithoutRRSP = 0;
        float provincialTax = 0;
        float provTaxWithoutRRSP = 0;

        String JSONString = JSONFetcher.getTaxData(c);
        try
        {
            JSONObject jObj = new JSONObject(JSONString);

            JSONArray years = jObj.getJSONArray("years");
            JSONObject currentYear = null;
            for (int i = 0; i < years.length(); i++)
            {
                if (years.getJSONObject(i).getInt("year") == year)
                {
                    currentYear = years.getJSONObject(i);
                    break;
                }
            }

            if (currentYear != null)
            {
                JSONObject fedObject = currentYear.getJSONObject("federal");
                federalTax = taxCalc(federalTaxIncome - rrsp, fedObject);
                fedTaxWithoutRRSP = taxCalc(federalTaxIncome, fedObject);

                JSONArray provinces = currentYear.getJSONArray("provinces");
                if (currentYear.getJSONArray("provinces").length() == 0)
                {
                    for (int i = 0; i < years.length(); i++)
                    {
                        if (years.getJSONObject(i).getInt("year") == 2015)
                        {
                            provinces = years.getJSONObject(i).getJSONArray("provinces");
                            break;
                        }
                    }
                }

                for (int i = 0; i < provinces.length(); i++)
                {
                    JSONObject provObj = provinces.getJSONObject(i);
                    if (provObj.getString("name").equals(province))
                    {
                        provincialTax = taxCalc(provTaxIncome - rrsp, provObj);
                        provTaxWithoutRRSP = taxCalc(provTaxIncome, provObj);
                        break;
                    }
                }

            }


        } catch (JSONException e)
        {
            // TODO:  Handle exception
        }

        float rrspLimit = Math.min((income * 0.18f) + 2000, 24930f + 2000f);
        float rrspTax = (rrsp > rrspLimit) ?
            // 1% monthly tax on excess contributions
            (rrsp - rrspLimit + 2000) * 0.12f : 0;

        float savings = (fedTaxWithoutRRSP + provTaxWithoutRRSP) -
                (federalTax + provincialTax + rrspTax);

        calcData.putFloat("fedTax", fedTaxWithoutRRSP);
        calcData.putFloat("provTax", provTaxWithoutRRSP);
        calcData.putFloat("rrspTax", rrspTax);
        calcData.putFloat("savings", savings);
        calcData.putFloat("net", income - fedTaxWithoutRRSP - provTaxWithoutRRSP - rrspTax);


        return calcData;
    }


    private static float taxCalc(float income, JSONObject taxObj) throws JSONException
    {
        if (taxObj.getString("type").equals("complexTier"))
            return complexTaxCalc(income, taxObj.getJSONArray("tiers"),
                                  taxObj.getDouble("credit"));
        else if (taxObj.getString("type").equals("simpleTier"))
            return simpleTaxCalc(income, taxObj.getJSONArray("tiers"),
                                 taxObj.getDouble("credit"));
        else
            return -1;
    }

    private static float complexTaxCalc(float income, JSONArray tierData, double credit) throws JSONException
    {
        double returnVal = 0;
        double prevTier = 0;
        income -= credit;

        for (int i = tierData.length() - 1; i >= 0; i--)
        {
            JSONObject obj = tierData.getJSONObject(i);
            double tier = obj.getDouble("tier");
            double rate = obj.getDouble("rate");

            if (income > tier)
            {
                if (returnVal == 0)
                    returnVal += (income - tier) * rate;
                else
                    returnVal += (prevTier - tier) * rate;

            }

            prevTier = tier;
        }

        return (float) returnVal;
    }

    private static float simpleTaxCalc(float income, JSONArray tierData, double credit)
            throws JSONException
    {
        income -= credit;
        for (int i = tierData.length() - 1; i >= 0; i--)
        {
            JSONObject obj = tierData.getJSONObject(i);
            double tier = obj.getDouble("tier");
            double rate = obj.getDouble("rate");

            if (income > tier)
                return (float) (income * rate);
        }
        return -1f;
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter
    {

        public SectionsPagerAdapter(FragmentManager fm)
        {
            super(fm);
        }

        @Override
        public Fragment getItem(int position)
        {
            switch (position)
            {
                case 0:
                    return SummaryFragment.newInstance(position + 1);
                case 1:
                    return PeriodicFragment.newInstance(position + 1);
                default:
                    return null;
            }
        }

        @Override
        public int getCount()
        {
            // Show 2 total pages.
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position)
        {
            switch (position)
            {
                case 0:
                    return getString(R.string.Output_Breakdown_Title);
                case 1:
                    return getString(R.string.Output_Periodic_Title);
            }
            return null;
        }
    }
}
