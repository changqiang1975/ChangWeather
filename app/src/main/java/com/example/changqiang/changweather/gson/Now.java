package com.example.changqiang.changweather.gson;

import com.google.gson.annotations.SerializedName;

public class Now
{
    @SerializedName("tmp")
    public String temperature;

    public class More
    {
        @SerializedName("txt")
        public String info;
    }

    @SerializedName("cond")
    public More more;
}
