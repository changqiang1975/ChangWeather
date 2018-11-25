package com.example.changqiang.changweather.util;

import android.text.TextUtils;

import com.example.changqiang.changweather.db.City;
import com.example.changqiang.changweather.db.County;
import com.example.changqiang.changweather.db.Province;
import com.example.changqiang.changweather.gson.Weather;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class Utility
{
    private Utility()
    {}

    public static boolean handleProvinceResponse(String response)
    {
        if(!TextUtils.isEmpty(response))
        {
            try
            {
                JSONArray allProvince = new JSONArray(response);
                for(int i=0; i<allProvince.length(); i++)
                {
                    JSONObject provinceObject = allProvince.getJSONObject(i);
                    Province province = new Province();
                    province.setProvinceCode(provinceObject.getInt("id"));
                    province.setProvinceName(provinceObject.getString("name"));
                    province.save();
                }
                return true;
            }
            catch (JSONException e)
            {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static boolean handleCityResponse(String response, int provinceId)
    {
        if(!TextUtils.isEmpty(response))
        {
            try
            {
                JSONArray allCity = new JSONArray(response);
                for(int i=0; i<allCity.length(); i++)
                {
                    JSONObject cityObject = allCity.getJSONObject(i);
                    City city = new City();
                    city.setCityCode(cityObject.getInt("id"));
                    city.setCityName(cityObject.getString("name"));
                    city.setProvinceId(provinceId);
                    city.save();
                }
                return true;
            }
            catch (JSONException e)
            {
                e.printStackTrace();
            }
        }

        return false;
    }

    public static boolean handleCountyResponse(String response, int cityId)
    {
        if(!TextUtils.isEmpty(response))
        {
            try
            {
                JSONArray allCounty = new JSONArray(response);
                for(int i=0; i<allCounty.length(); i++)
                {
                    JSONObject countyObject = allCounty.getJSONObject(i);
                    County county = new County();
                    county.setCountyCode(countyObject.getInt("id"));
                    county.setCountyName(countyObject.getString("name"));
                    county.setWeatherId(countyObject.getString("weather_id"));
                    county.setCityId(cityId);
                    county.save();
                }
                return true;
            }
            catch(JSONException e)
            {
                e.printStackTrace();
            }
        }

        return false;
    }


    public static Weather handleWeatherResponse(String response)
    {
        if(!TextUtils.isEmpty(response))
        {
            try
            {
                JSONObject jsonObject = new JSONObject(response);
                JSONArray jsonArray = jsonObject.getJSONArray("HeWeather");
                String weatherContent = jsonArray.getJSONObject(0).toString();
                return new Gson().fromJson(weatherContent, Weather.class);
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }

        return null;
    }
}
