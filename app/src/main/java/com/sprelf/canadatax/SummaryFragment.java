package com.sprelf.canadatax;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.CategorySeries;
import org.achartengine.renderer.DefaultRenderer;
import org.achartengine.renderer.SimpleSeriesRenderer;

import java.text.NumberFormat;


public class SummaryFragment extends Fragment implements CurrencyExchanger.ExchangerListener
{

    private Bundle calcData;
    private View rootView;

    private DefaultRenderer renderer = new DefaultRenderer();
    private CategorySeries series = new CategorySeries("");
    private GraphicalView chartView;

    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";

    public SummaryFragment()
    {
    }

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static SummaryFragment newInstance(int sectionNumber)
    {
        SummaryFragment fragment = new SummaryFragment();
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
        rootView = inflater.inflate(R.layout.fragment_summary, container, false);

        calcData = OutputActivity.calculateValues(getActivity(), getActivity().getIntent().getExtras());

        CanadaTax.constructCurrencySpinner(getActivity(),
                                           (Spinner) rootView.findViewById(
                                                   R.id.Output_Breakdown_CurrencySpinner),
                                           "Breakdown_Curr",
                                           new Runnable()
                                           {
                                               @Override
                                               public void run()
                                               {
                                                   calculateValues();
                                               }
                                           });


        calculateValues();

        prepareGraph();

        return rootView;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        constructGraph((LinearLayout) rootView.findViewById(R.id.Output_Breakdown_ChartArea));
    }

    private void calculateValues()
    {


        float fedTax = calcData.getFloat("fedTax");
        float provTax = calcData.getFloat("provTax");
        float income = calcData.getFloat("income");
        float rrspTax = calcData.getFloat("rrspTax");
        float savings = calcData.getFloat("savings");
        float net = calcData.getFloat("net");
        float rate = (fedTax + provTax + rrspTax) / income;

        String currency = (String) ((Spinner) rootView.findViewById(
                R.id.Output_Breakdown_CurrencySpinner))
                .getSelectedItem();


        ((TextView) rootView.findViewById(R.id.Output_Breakdown_Header)).setText(
                Html.fromHtml(
                        getString(R.string.Output_Breakdown_Header)
                                .replace("%v", CanadaTax.exchanger.convertAndFormat(currency, income))
                                .replace("%y", calcData.getString("year"))));


        ((TextView) rootView.findViewById(R.id.Output_Breakdown_Federal))
                .setText(CanadaTax.exchanger.convertAndFormat(currency, fedTax + rrspTax));

        ((TextView) rootView.findViewById(R.id.Output_Breakdown_Provincial))
                .setText(CanadaTax.exchanger.convertAndFormat(currency, provTax));

        ((TextView) rootView.findViewById(R.id.Output_Breakdown_Total))
                .setText(CanadaTax.exchanger.convertAndFormat(currency, fedTax + rrspTax + provTax));

        ((TextView) rootView.findViewById(R.id.Output_Breakdown_Net))
                .setText(CanadaTax.exchanger.convertAndFormat(currency, net));

        NumberFormat percFormat = NumberFormat.getPercentInstance();
        percFormat.setMaximumFractionDigits(1);
        ((TextView) rootView.findViewById(R.id.Output_Breakdown_Rate))
                .setText(percFormat.format(rate));

        ((TextView) rootView.findViewById(R.id.Output_Breakdown_Refund))
                .setText(CanadaTax.exchanger.convertAndFormat(currency, savings));

    }

    private void prepareGraph()
    {
        renderer.setApplyBackgroundColor(false);
        renderer.setChartTitleTextSize(0);
        renderer.setLabelsTextSize(18);
        renderer.setLabelsColor(Color.BLACK);
        renderer.setShowLegend(false);
        renderer.setStartAngle(180);
        NumberFormat format = NumberFormat.getPercentInstance();
        format.setMaximumFractionDigits(1);
        series.add(getString(R.string.Output_Breakdown_GraphFed)
                           .replace("$v", format.format(
                                   calcData.getFloat("fedTax") / calcData.getFloat("income"))),
                   calcData.getFloat("fedTax"));
        SimpleSeriesRenderer renderer1 = new SimpleSeriesRenderer();
        renderer1.setColor(getResources().getColor(R.color.Graph_Color1));
        renderer.addSeriesRenderer(renderer1);

        series.add(getString(R.string.Output_Breakdown_GraphProv)
                           .replace("$v", format.format(
                                   calcData.getFloat("provTax") / calcData.getFloat("income"))),
                   calcData.getFloat("provTax"));
        SimpleSeriesRenderer renderer2 = new SimpleSeriesRenderer();
        renderer2.setColor(getResources().getColor(R.color.Graph_Color2));
        renderer.addSeriesRenderer(renderer2);

        if (calcData.getFloat("rrspTax") > 0)
        {
            series.add(getString(R.string.Output_Breakdown_GraphRRSPTax)
                               .replace("$v", format.format(
                                       calcData.getFloat("rrspTax") / calcData.getFloat("income"))),
                       calcData.getFloat("rrspTax"));
            SimpleSeriesRenderer renderer4 = new SimpleSeriesRenderer();
            renderer4.setColor(getResources().getColor(R.color.Graph_Color4));
            renderer.addSeriesRenderer(renderer4);
        }

        /*
        if (calcData.getFloat("rrsp") > 0)
        {
            series.add(getString(R.string.Output_Breakdown_GraphRRSP)
                               .replace("$v", format.format(
                                       calcData.getFloat("rrsp") / calcData.getFloat("income"))),
                       calcData.getFloat("rrsp"));
            SimpleSeriesRenderer renderer5 = new SimpleSeriesRenderer();
            renderer5.setColor(getResources().getColor(R.color.colorLightBlue));
            renderer.addSeriesRenderer(renderer5);
        }*/

        series.add(getString(R.string.Output_Breakdown_GraphIncome)
                           .replace("$v", format.format(
                                   calcData.getFloat("net") / calcData.getFloat("income"))),
                   calcData.getFloat("net") - calcData.getFloat("rrsp"));
        SimpleSeriesRenderer renderer3 = new SimpleSeriesRenderer();
        renderer3.setColor(getResources().getColor(R.color.Graph_Color3));
        renderer.addSeriesRenderer(renderer3);

        if (chartView != null)
            chartView.repaint();

    }

    private void constructGraph(LinearLayout parent)
    {
        if (chartView == null)
        {
            chartView = ChartFactory.getPieChartView(getActivity(), series, renderer);

            renderer.setClickEnabled(false);
            renderer.setPanEnabled(false);
            renderer.setZoomEnabled(false);
            parent.addView(chartView);
        } else
        {
            chartView.repaint();
        }

    }

    @Override
    public void onCurrencyUpdate()
    {
        calculateValues();
    }
}
