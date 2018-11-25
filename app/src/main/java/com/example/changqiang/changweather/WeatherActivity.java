package com.example.changqiang.changweather;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.changqiang.changweather.gson.Forecast;
import com.example.changqiang.changweather.gson.Weather;
import com.example.changqiang.changweather.util.HttpUtility;
import com.example.changqiang.changweather.util.Utility;

import org.w3c.dom.Text;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity
{
    private Weather weather = null;
    private ImageView bingImg;

    private TextView title_city, title_date;
    private TextView now_temperature, now_information;
    private TextView api_information, api_pm25;
    private LinearLayout forecastLayout;
    private TextView suggestion_comfort, suggestion_car_wash, suggestion_sport;
    public SwipeRefreshLayout swipeRefresh;
    private Button homeButton;
    public DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 21)
        {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }

        setContentView(R.layout.activity_weather);
        title_city = findViewById(R.id.title_text);
        title_date = findViewById(R.id.title_date);
        bingImg = findViewById(R.id.background_image);
        now_information = findViewById(R.id.now_information);
        now_temperature = findViewById(R.id.now_temperature);
        api_information = findViewById(R.id.api_info);
        api_pm25 = findViewById(R.id.api_pm25);
        forecastLayout = findViewById(R.id.forecast_layout);
        suggestion_comfort = findViewById(R.id.suggestion_comfort);
        suggestion_car_wash = findViewById(R.id.suggestion_carwash);
        suggestion_sport = findViewById(R.id.suggestion_sport);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        homeButton = findViewById(R.id.nav_button);
        drawerLayout = findViewById(R.id.drawer_layout);


        //swipeRefresh.setColorSchemeResources(R.color.colorPrimary);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        String weatherString = prefs.getString("weather", null);
        if(weatherString != null)
        {
            weather = Utility.handleWeatherResponse(weatherString);
            updateViews();
        }
        else
        {
            String weatherId = getIntent().getStringExtra("weather_id");
            getWeatherFromServer(weatherId);
        }

        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener()
        {
            @Override
            public void onRefresh()
            {
                getWeatherFromServer(weather.basic.weatherId);
            }
        });

        String bingPic = prefs.getString("bing_pic", null);
        if(bingPic != null)
        {
            Glide.with(this).load(bingPic).into(bingImg);
        }
        else
        {
            loadBackgroundImage();
        }

        homeButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
    }

    private void loadBackgroundImage()
    {
        String address = "http://guolin.tech/api/bing_pic";
        HttpUtility.sendOkHttpRequest(address, new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e)
            {
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Toast.makeText(WeatherActivity.this, "加载失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException
            {
                final String address = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                editor.putString("bing_pic", address);
                editor.apply();

                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Glide.with(WeatherActivity.this).load(address).into(bingImg);
                    }
                });
            }
        });
    }

    public void getWeatherFromServer(String weatherId)
    {
        if(weatherId.isEmpty())
            return;

        String address = "http://guolin.tech/api/weather?cityid=" + weatherId + "&key=35be2fb9e7f24c6098f28fa11a8a7674";
        HttpUtility.sendOkHttpRequest(address, new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e)
            {
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Toast.makeText(WeatherActivity.this, "加载失败", Toast.LENGTH_SHORT).show();
                        swipeRefresh.setRefreshing(false);
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException
            {
                String responseText = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                editor.putString("weather", responseText);
                editor.apply();

                weather = Utility.handleWeatherResponse(responseText);
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        if(weather != null && weather.status.equals("ok"))
                            updateViews();
                        else
                            Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                        if(swipeRefresh.isRefreshing())
                            swipeRefresh.setRefreshing(false);
                    }
                });
            }
        });
    }

    private void updateViews()
    {
        if(weather != null && weather.status.equals("ok"))
        {
            updateTitle();
            updateNow();
            updateAQI();
            updateForecast();
            updateSuggestion();
            Intent intent = new Intent(this, AutoUpdateService.class);
            startService(intent);
        }
    }

    private void updateSuggestion()
    {
        suggestion_comfort.setText("舒适度：" + weather.suggestion.comfort.info);
        suggestion_car_wash.setText("洗车指数：" + weather.suggestion.carWash.info);
        suggestion_sport.setText("运动指数：" + weather.suggestion.sport.info);
    }

    private void updateForecast()
    {
        View view;
        forecastLayout.removeAllViews();
        for(Forecast forecast : weather.forecastList)
        {
            view = getLayoutInflater().inflate(R.layout.forecast_item, forecastLayout, false);
            ((TextView)view.findViewById(R.id.forecast_date)).setText(forecast.date);
            ((TextView)view.findViewById(R.id.forecast_information)).setText(forecast.more.info);
            ((TextView)view.findViewById(R.id.forecast_max)).setText(forecast.temperature.max);
            ((TextView)view.findViewById(R.id.forecast_min)).setText(forecast.temperature.min);
            forecastLayout.addView(view);
        }
    }

    private void updateAQI()
    {
        api_pm25.setText(weather.aqi.city.pm25);
        api_information.setText(weather.aqi.city.aqi);
    }

    private void updateNow()
    {
        now_temperature.setText(weather.now.temperature + "°C");
        now_information.setText(weather.now.more.info);
    }

    private void updateTitle()
    {
        title_date.setText(weather.basic.update.updateTime.split(" ")[1]);
        title_city.setText(weather.basic.cityName);
    }
}
