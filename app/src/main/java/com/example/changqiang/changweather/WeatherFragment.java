package com.example.changqiang.changweather;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.changqiang.changweather.db.City;
import com.example.changqiang.changweather.db.County;
import com.example.changqiang.changweather.db.Province;
import com.example.changqiang.changweather.util.HttpUtility;
import com.example.changqiang.changweather.util.Utility;

import org.litepal.LitePal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherFragment extends Fragment
{
    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY = 2;

    private final String baseAddress = "http://guolin.tech/api/china";

    private int currentLevel;

    private TextView title_text;
    private ListView listView;
    private Button backButton;

    private ArrayAdapter<String> adapter;
    private List<String> dataList = new ArrayList<>();

    private Province currentProvince;
    private City currentCity;
    private County currentCounty;

    private List<Province> provinceList;
    private List<City> cityList;
    private List<County> countyList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.weather_fragment, container, false);
        title_text = view.findViewById(R.id.title_text);
        backButton = view.findViewById(R.id.back_button);
        listView = view.findViewById(R.id.list_view);
        adapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, dataList);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                switch(currentLevel)
                {
                    case LEVEL_PROVINCE:
                        currentProvince = provinceList.get(position);
                        queryCity();
                        break;

                    case LEVEL_CITY:
                        currentCity = cityList.get(position);
                        queryCounty();
                        break;

                    case LEVEL_COUNTY:
                        currentCounty = countyList.get(position);
                        String weatherId = currentCounty.getWeatherId();
                        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
                        editor.putString("weather_id", weatherId);
                        editor.apply();
                        if(getActivity() instanceof MainActivity)
                        {
                            Intent intent = new Intent(getContext(), WeatherActivity.class);
                            intent.putExtra("weather_id", weatherId);
                            startActivity(intent);
                            getActivity().finish();
                        }
                        else
                        {
                            WeatherActivity activity = (WeatherActivity)getActivity();
                            activity.drawerLayout.closeDrawers();
                            activity.swipeRefresh.setRefreshing(true);
                            activity.getWeatherFromServer(weatherId);
                        }
                        break;

                    default:
                        break;
                }
            }
        });

        backButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                switch(currentLevel)
                {
                    case LEVEL_COUNTY:
                        queryCity();
                        break;

                    case LEVEL_CITY:
                        queryProvince();
                        break;

                    case LEVEL_PROVINCE:
                        break;

                    default:
                        break;
                }
            }
        });
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        queryProvince();
    }

    private void queryProvince()
    {
        title_text.setText("中国");
        backButton.setVisibility(View.GONE);
        provinceList = LitePal.findAll(Province.class);

        if(provinceList.size() > 0)
        {
            dataList.clear();
            for(Province province : provinceList)
            {
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_PROVINCE;
        }
        else
        {
            String address = baseAddress;
            queryFromServer(address, "province");
        }
    }

    private void queryFromServer(String address, final String type)
    {
        HttpUtility.sendOkHttpRequest(address, new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e)
            {
                getActivity().runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Toast.makeText(getContext(), "加载失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException
            {
                String responseText = response.body().string();
                boolean result = false;

                if(type.equals("province"))
                {
                    result = Utility.handleProvinceResponse(responseText);
                }
                else if(type.equals("city"))
                {
                    result = Utility.handleCityResponse(responseText, currentProvince.getId());
                }
                else if(type.equals("county"))
                {
                    result = Utility.handleCountyResponse(responseText, currentCity.getId());
                }

                if(result)
                {
                    getActivity().runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            if(type.equals("province"))
                            {
                                queryProvince();
                            }
                            else if(type.equals("city"))
                            {
                                queryCity();
                            }
                            else if(type.equals("county"))
                            {
                                queryCounty();
                            }
                        }
                    });
                }
            }
        });
    }

    private void queryCounty()
    {
        title_text.setText(currentCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        countyList = LitePal.where("cityid = ?", currentCity.getId() + "").find(County.class);

        if(countyList.size() > 0)
        {
            dataList.clear();
            for(County county : countyList)
            {
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_COUNTY;
        }
        else
        {
            String address = baseAddress + "/" + currentCity.getCityCode() + "/" + currentCity.getCityCode();
            queryFromServer(address, "county");
        }
    }

    private void queryCity()
    {
        title_text.setText(currentProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);
        cityList = LitePal.where("provinceid = ?", currentProvince.getId() + "").find(City.class);

        if(cityList.size() > 0)
        {
            dataList.clear();
            for(City city : cityList)
            {
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_CITY;
        }
        else
        {
            String address = baseAddress + "/" + currentProvince.getProvinceCode();
            queryFromServer(address, "city");
        }
    }
}
