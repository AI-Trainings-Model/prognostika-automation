package com.weatherwithyou.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class WeatherWidgetProvider extends AppWidgetProvider {
    private static final String API = "https://api.open-meteo.com/v1/forecast?latitude=52.52&longitude=13.405&current=temperature_2m,apparent_temperature,weather_code&hourly=temperature_2m,precipitation_probability&daily=temperature_2m_max,temperature_2m_min&timezone=Europe%2FBerlin&forecast_days=2";

    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        refresh(context, manager, ids);
    }

    @Override public void onAppWidgetOptionsChanged(Context context, AppWidgetManager manager, int id, Bundle options) {
        render(context, manager, id, load(context), options);
    }

    public static void refresh(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) render(context, manager, id, load(context), manager.getAppWidgetOptions(id));
        new Thread(() -> {
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(API).openConnection();
                c.setConnectTimeout(12000); c.setReadTimeout(12000);
                BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
                StringBuilder sb = new StringBuilder(); String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();
                Weather w = parse(new JSONObject(sb.toString()));
                save(context, w);
                for (int id : ids) render(context, manager, id, w, manager.getAppWidgetOptions(id));
            } catch (Exception ignored) { }
        }).start();
    }

    private static Weather parse(JSONObject root) throws Exception {
        JSONObject cur = root.getJSONObject("current");
        JSONObject hourly = root.getJSONObject("hourly");
        JSONObject daily = root.getJSONObject("daily");
        Weather w = new Weather();
        w.temp = cur.getDouble("temperature_2m");
        w.feels = cur.getDouble("apparent_temperature");
        w.code = cur.getInt("weather_code");
        w.min = daily.getJSONArray("temperature_2m_min").getDouble(0);
        w.max = daily.getJSONArray("temperature_2m_max").getDouble(0);
        JSONArray times = hourly.getJSONArray("time");
        JSONArray temps = hourly.getJSONArray("temperature_2m");
        JSONArray rain = hourly.getJSONArray("precipitation_probability");
        OffsetDateTime now = OffsetDateTime.now();
        int start = 0;
        for (int i=0; i<times.length(); i++) {
            if (times.getString(i).compareTo(now.toLocalDateTime().toString().substring(0,13)+":00") >= 0) { start=i; break; }
        }
        StringBuilder h = new StringBuilder();
        int bestRain = -1; String rainAt = "";
        for (int i=start; i<Math.min(start+6, times.length()); i++) {
            String time = times.getString(i).substring(11,16);
            int p = rain.getInt(i);
            if (i < start+4) {
                if (h.length()>0) h.append("   ");
                h.append(time).append("  ").append(Math.round(temps.getDouble(i))).append("°");
            }
            if (p >= 50 && bestRain < 0) { bestRain=p; rainAt=time; }
        }
        w.hourly=h.toString();
        w.action = bestRain >= 0 ? "☂ Дождь в " + rainAt + " · " + bestRain + "%" : advice(w.feels, w.code);
        w.updated = java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        return w;
    }

    private static String advice(double feels, int code) {
        if (code >= 71 && code <= 86) return "❄ Возможен снег — обувь потеплее";
        if (code >= 51 && code <= 67) return "☂ Возьми зонт";
        if (feels < 4) return "🧣 Морозно — шапка, шарф и перчатки";
        if (feels < 12) return "🧥 Нужна тёплая куртка";
        if (feels < 20) return "🧥 Возьми лёгкую куртку";
        if (feels >= 28) return "☀ Жарко — вода и защита от солнца";
        return "Лёгкая одежда по погоде";
    }

    private static String condition(int code) {
        if (code == 0) return "Ясно";
        if (code <= 3) return "Облачно";
        if (code <= 48) return "Туман";
        if (code <= 67) return "Дождь";
        if (code <= 86) return "Снег";
        return "Гроза";
    }

    private static void render(Context context, AppWidgetManager manager, int id, Weather w, Bundle options) {
        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_weather);
        rv.setTextViewText(R.id.temperature, Math.round(w.temp) + "°");
        rv.setTextViewText(R.id.condition, condition(w.code));
        rv.setTextViewText(R.id.action, w.action);
        rv.setTextViewText(R.id.details, "Ощущается " + Math.round(w.feels) + "°  ·  " + Math.round(w.min) + "°/" + Math.round(w.max) + "°  ·  " + w.updated);
        rv.setTextViewText(R.id.hourly, w.hourly);
        int width = options == null ? 110 : options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 110);
        int height = options == null ? 110 : options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110);
        rv.setViewVisibility(R.id.details, width >= 220 ? View.VISIBLE : View.GONE);
        rv.setViewVisibility(R.id.hourly, width >= 220 && height >= 180 ? View.VISIBLE : View.GONE);
        Intent intent = new Intent(context, MainActivity.class);
        rv.setOnClickPendingIntent(R.id.widget_root, PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        manager.updateAppWidget(id, rv);
    }

    private static void save(Context c, Weather w) {
        c.getSharedPreferences("weather", Context.MODE_PRIVATE).edit()
            .putFloat("temp",(float)w.temp).putFloat("feels",(float)w.feels)
            .putFloat("min",(float)w.min).putFloat("max",(float)w.max)
            .putInt("code",w.code).putString("action",w.action)
            .putString("hourly",w.hourly).putString("updated",w.updated).apply();
    }

    private static Weather load(Context c) {
        SharedPreferences p=c.getSharedPreferences("weather", Context.MODE_PRIVATE);
        Weather w=new Weather(); w.temp=p.getFloat("temp",12); w.feels=p.getFloat("feels",10);
        w.min=p.getFloat("min",8); w.max=p.getFloat("max",16); w.code=p.getInt("code",3);
        w.action=p.getString("action","Обновляем прогноз…"); w.hourly=p.getString("hourly","");
        w.updated=p.getString("updated","—"); return w;
    }

    private static class Weather {
        double temp, feels, min, max; int code; String action="", hourly="", updated="—";
    }
}
