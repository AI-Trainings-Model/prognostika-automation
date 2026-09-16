package com.weatherwithyou.widget;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.widget.TextView;
import androidx.core.content.FileProvider;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public final class UpdateManager {
    private static final String LATEST =
        "https://api.github.com/repos/AI-Trainings-Model/prognostika-automation/releases/latest";

    public static void check(Activity activity, TextView status, boolean manual) {
        status.setText("Проверяем GitHub…");
        new Thread(() -> {
            try {
                JSONObject release = json(LATEST);
                String tag = release.getString("tag_name").replaceFirst("^v", "");
                String current = activity.getPackageManager()
                    .getPackageInfo(activity.getPackageName(), 0).versionName;
                String url = apkUrl(release.getJSONArray("assets"));
                if (url == null) throw new Exception("В релизе нет APK");
                if (compare(tag, current) <= 0) {
                    activity.runOnUiThread(() -> status.setText("Установлена актуальная версия " + current));
                    return;
                }
                activity.runOnUiThread(() -> {
                    status.setText("Доступна версия " + tag);
                    new AlertDialog.Builder(activity)
                        .setTitle("Обновление " + tag)
                        .setMessage("Скачать обновление с GitHub и установить поверх текущей версии?")
                        .setNegativeButton("Позже", null)
                        .setPositiveButton("Скачать", (d, w) -> download(activity, status, url, tag))
                        .show();
                });
            } catch (Exception e) {
                activity.runOnUiThread(() -> status.setText(manual ? "Не удалось проверить: " + e.getMessage() : ""));
            }
        }).start();
    }

    private static void download(Activity activity, TextView status, String address, String version) {
        status.setText("Скачиваем " + version + "…");
        new Thread(() -> {
            try {
                File dir = new File(activity.getCacheDir(), "updates");
                if (!dir.exists() && !dir.mkdirs()) throw new Exception("Не удалось создать папку");
                File apk = new File(dir, "Weather_With_You_" + version + ".apk");
                HttpURLConnection c = (HttpURLConnection) new URL(address).openConnection();
                c.setConnectTimeout(15000); c.setReadTimeout(30000);
                try (InputStream in=c.getInputStream(); FileOutputStream out=new FileOutputStream(apk)) {
                    byte[] b=new byte[8192]; int n;
                    while ((n=in.read(b))>0) out.write(b,0,n);
                }
                Uri uri = FileProvider.getUriForFile(activity, activity.getPackageName()+".files", apk);
                Intent install = new Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, "application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.runOnUiThread(() -> {
                    status.setText("Обновление скачано");
                    activity.startActivity(install);
                });
            } catch (Exception e) {
                activity.runOnUiThread(() -> status.setText("Ошибка загрузки: " + e.getMessage()));
            }
        }).start();
    }

    private static JSONObject json(String address) throws Exception {
        HttpURLConnection c=(HttpURLConnection)new URL(address).openConnection();
        c.setRequestProperty("Accept","application/vnd.github+json");
        c.setConnectTimeout(12000); c.setReadTimeout(12000);
        try (InputStream in=c.getInputStream()) {
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            byte[] buffer=new byte[4096]; int n;
            while((n=in.read(buffer))>0) out.write(buffer,0,n);
            return new JSONObject(new String(out.toByteArray(), java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    private static String apkUrl(JSONArray assets) throws Exception {
        for(int i=0;i<assets.length();i++) {
            JSONObject a=assets.getJSONObject(i);
            if(a.getString("name").toLowerCase().endsWith(".apk"))
                return a.getString("browser_download_url");
        }
        return null;
    }

    private static int compare(String a, String b) {
        String[] x=a.split("\\."), y=b.split("\\.");
        for(int i=0;i<Math.max(x.length,y.length);i++) {
            int xi=i<x.length?number(x[i]):0, yi=i<y.length?number(y[i]):0;
            if(xi!=yi) return Integer.compare(xi,yi);
        }
        return 0;
    }

    private static int number(String s) {
        try { return Integer.parseInt(s.replaceAll("[^0-9].*$","")); }
        catch(Exception e) { return 0; }
    }
}
