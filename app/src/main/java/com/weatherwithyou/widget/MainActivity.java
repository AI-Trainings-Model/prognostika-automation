package com.weatherwithyou.widget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.CheckBox;

public class MainActivity extends Activity {
    private static final int PICK_PHOTO = 42;
    private ImageView preview;
    private EditText city;
    private Uri selectedPhoto;

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
        note.setText("\nНастрой город и фотографию. Эти настройки применятся ко всем установленным виджетам.\n");
        note.setTextColor(Color.rgb(221, 232, 240));
        note.setTextSize(17);
        note.setGravity(Gravity.CENTER);
        box.addView(note);

        city = new EditText(this);
        city.setHint("Город, например Berlin или Minsk");
        city.setText(getSharedPreferences("settings", MODE_PRIVATE).getString("city", "Berlin"));
        city.setTextColor(Color.WHITE);
        city.setHintTextColor(Color.LTGRAY);
        box.addView(city, new LinearLayout.LayoutParams(-1, -2));

        preview = new ImageView(this);
        preview.setAdjustViewBounds(true);
        preview.setMaxHeight(500);
        String saved = getSharedPreferences("settings", MODE_PRIVATE).getString("photo", "");
        if (!saved.isEmpty()) {
            selectedPhoto = Uri.parse(saved);
            preview.setImageURI(selectedPhoto);
        }
        box.addView(preview, new LinearLayout.LayoutParams(-1, 420));

        Button pick = new Button(this);
        pick.setText("Выбрать своё фото");
        pick.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("image/*");
            startActivityForResult(i, PICK_PHOTO);
        });
        box.addView(pick);

        Button refresh = new Button(this);
        refresh.setText("Сохранить и обновить виджет");
        refresh.setOnClickListener(v -> {
            getSharedPreferences("settings", MODE_PRIVATE).edit()
                .putString("city", city.getText().toString().trim().isEmpty() ? "Berlin" : city.getText().toString().trim())
                .putString("photo", selectedPhoto == null ? "" : selectedPhoto.toString()).apply();
            AppWidgetManager manager = AppWidgetManager.getInstance(this);
            int[] ids = manager.getAppWidgetIds(new ComponentName(this, WeatherWidgetProvider.class));
            WeatherWidgetProvider.refresh(this, manager, ids);
        });
        box.addView(refresh);

        CheckBox automatic = new CheckBox(this);
        automatic.setText("Автоматически проверять обновления при запуске");
        automatic.setTextColor(Color.WHITE);
        automatic.setChecked(getSharedPreferences("settings", MODE_PRIVATE).getBoolean("auto_update", true));
        automatic.setOnCheckedChangeListener((button, checked) ->
            getSharedPreferences("settings", MODE_PRIVATE).edit().putBoolean("auto_update", checked).apply());
        box.addView(automatic);

        TextView updateStatus = new TextView(this);
        updateStatus.setTextColor(Color.LTGRAY);
        updateStatus.setGravity(Gravity.CENTER);
        box.addView(updateStatus);

        Button update = new Button(this);
        update.setText("Проверить обновление с GitHub");
        update.setOnClickListener(v -> UpdateManager.check(this, updateStatus, true));
        box.addView(update);

        setContentView(box);
        if (automatic.isChecked()) UpdateManager.check(this, updateStatus, false);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_PHOTO && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedPhoto = data.getData();
            try {
                getContentResolver().takePersistableUriPermission(selectedPhoto, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception ignored) {}
            preview.setImageURI(selectedPhoto);
        }
    }
}
