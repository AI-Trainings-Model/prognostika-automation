package com.weatherwithyou.widget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(48, 48, 48, 48);
        box.setBackgroundColor(Color.rgb(25, 35, 47));

        TextView title = new TextView(this);
        title.setText("Погода с тобой");
        title.setTextColor(Color.WHITE);
        title.setTextSize(30);
        title.setGravity(Gravity.CENTER);
        box.addView(title);

        TextView note = new TextView(this);
        note.setText("\nПервый прототип показывает живую погоду для Берлина. Добавь виджет на домашний экран и меняй его размер: 2×2, 4×2 или 4×3.\n");
        note.setTextColor(Color.rgb(221, 232, 240));
        note.setTextSize(17);
        note.setGravity(Gravity.CENTER);
        box.addView(note);

        Button refresh = new Button(this);
        refresh.setText("Обновить виджет");
        refresh.setOnClickListener(v -> {
            AppWidgetManager manager = AppWidgetManager.getInstance(this);
            int[] ids = manager.getAppWidgetIds(new ComponentName(this, WeatherWidgetProvider.class));
            WeatherWidgetProvider.refresh(this, manager, ids);
        });
        box.addView(refresh);
        setContentView(box);
    }
}
