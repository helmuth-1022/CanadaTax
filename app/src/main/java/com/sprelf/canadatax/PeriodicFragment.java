package com.sprelf.canadatax;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;
import android.widget.TextView;


/**
 * A simple {@link Fragment} subclass.
 */
public class PeriodicFragment extends Fragment implements CurrencyExchanger.ExchangerListener
{
    
    private Bundle calcData;
    private View rootView;

    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";
    
    public PeriodicFragment()
    {
    }
    
    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static PeriodicFragment newInstance(int sectionNumber)
    {
        PeriodicFragment fragment = new PeriodicFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        CanadaTax.exchanger.addListener(this);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        CanadaTax.exchanger.removeListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        rootView = inflater.inflate(R.layout.fragment_periodic, container, false);

        calcData = OutputActivity.calculateValues(getActivity(), getActivity().getIntent().getExtras());

        CanadaTax.constructCurrencySpinner(getActivity(),
                                           (Spinner) rootView.findViewById(
                                                   R.id.Output_Periodic_CurrencySpinner),
                                           "Periodic_Curr",
                                           new Runnable()
                                           {
                                               @Override
                                               public void run()
                                               {
                                                   calculateValues();
                                               }
                                           });

        calculateValues();

        return rootView;
    }

    private void calculateValues()
    {
        float netIncome = calcData.getFloat("net");

        String currency = (String) ((Spinner) rootView.findViewById(
                R.id.Output_Periodic_CurrencySpinner))
                .getSelectedItem();

        TextView headerText = (TextView) rootView.findViewById(R.id.Output_Periodic_Header);
        headerText.setText(Html.fromHtml(
                getString(R.string.Output_Periodic_Header)
                        .replace("%v", CanadaTax.exchanger.convertAndFormat(currency, netIncome))));



        ((TextView) rootView.findViewById(R.id.Output_Periodic_QuarterlyOutput))
                .setText(CanadaTax.exchanger.convertAndFormat(currency, netIncome/4));
        ((TextView) rootView.findViewById(R.id.Output_Periodic_MonthlyOutput))
                .setText(CanadaTax.exchanger.convertAndFormat(currency, netIncome/12));
        ((TextView) rootView.findViewById(R.id.Output_Periodic_BiWeeklyOutput))
                .setText(CanadaTax.exchanger.convertAndFormat(currency, netIncome/26));
        ((TextView) rootView.findViewById(R.id.Output_Periodic_WeeklyOutput))
                .setText(CanadaTax.exchanger.convertAndFormat(currency, netIncome/52));
        ((TextView) rootView.findViewById(R.id.Output_Periodic_Hourly40Output))
                .setText(CanadaTax.exchanger.convertAndFormat(currency, netIncome/(52*40)));
        ((TextView) rootView.findViewById(R.id.Output_Periodic_Hourly37Output))
                .setText(CanadaTax.exchanger.convertAndFormat(currency, netIncome/(52*37.5f)));

    }

    @Override
    public void onCurrencyUpdate()
    {
        calculateValues();
    }
}
