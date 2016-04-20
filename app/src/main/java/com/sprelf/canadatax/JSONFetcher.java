package com.sprelf.canadatax;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Chris on 27.12.2015.
 */
public class JSONFetcher
{

    private static String BASE_URL =
            "http://www.apilayer.net/api/live" +
                    "?access_key=2e893f6ee9b0432d3bfea16c8a6f7a82" +
                    //"&source=%s"+  // Can't switch source on free account
                    "&currencies=%c";


    public static String getCurrencyData(String source, String conversion)
    {
        Log.d("[HTTP]", "Starting HTTP connection:\n" +
                BASE_URL.replace("%s", source).replace("%c", conversion));

        HttpURLConnection connection = null;
        InputStream input = null;
        try
        {
            connection = (HttpURLConnection) (new URL(
                    BASE_URL.replace("%s", source).replace("%c", conversion))).openConnection();
            connection.setRequestMethod("GET");
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.connect();

            StringBuffer buffer = new StringBuffer();
            input = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            String readLine;

            while ((readLine = reader.readLine()) != null)
            {
                buffer.append(readLine + "\r\n");
            }

            input.close();
            connection.disconnect();
            return buffer.toString();
        } catch (Throwable t)
        {
            Log.d("[HTTP]", "Unable to establish HTTP connection", t);
        } finally
        {
            try
            {
                input.close();
            } catch (Throwable t)
            {
            }
            try
            {
                connection.disconnect();
            } catch (Throwable t)
            {
            }
        }

        return null;
    }

    public static String getTaxData(Context c)
    {
        BufferedReader reader;
        String returnString = "";

        try
        {
            reader = new BufferedReader(
                    new InputStreamReader(c.getAssets().open("brackets.json")));

            String line;
            while ((line = reader.readLine()) != null)
            {
                returnString += line;
            }

            reader.close();
        } catch (IOException e)
        {
            //log the exception
        }

        return returnString;

    }


}
