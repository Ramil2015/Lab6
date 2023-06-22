package com.example.lab6;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private ImageView catImageView;
    private ProgressBar progressBar;
    String TAG = "TAG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        catImageView = findViewById(R.id.catImageView);
        progressBar = findViewById(R.id.progressBar);

        CatImages catImages = new CatImages();
        catImages.execute();
    }

    private class CatImages extends AsyncTask<Void, Integer, String> {

        private Bitmap currentCatBitmap;

        @Override
        protected String doInBackground(Void... voids) {
            while (true) {
                try {
                    String imageUrl = "https://cataas.com/cat?json=true";
                    String jsonResult = downloadJsonData(imageUrl);
                    String imageId = getImageIdFromJson(jsonResult);


                    File imageFile = getImageFile(imageId);
                    if (imageFile.exists()) {
                        // Cat image already exists, load from local storage
                        currentCatBitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                    } else {
                        // Download the cat image and save to local storage
                         imageUrl = getImageUrlFromJson(jsonResult);
                        currentCatBitmap = downloadImage(imageUrl);
                        saveImageToFile(currentCatBitmap, imageFile);
                    }
                    Log.d(TAG, "doInBackground: " +
                            "\njsonResult " +jsonResult+
                            "\nimageId " +imageId+
                            "\ncurrentCatBitmap " +currentCatBitmap+
                            ""
                    );
                    publishProgress(0); // Update ImageView with the new cat image

                    for (int i = 0; i < 100; i++) {
                        Thread.sleep(30);
                        publishProgress(i + 1); // Update ProgressBar with the current progress
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            int progress = values[0];
            progressBar.setProgress(progress);
            if (currentCatBitmap != null) {
                catImageView.setImageBitmap(currentCatBitmap);
            }
        }

        private String downloadJsonData(String urlString) throws IOException {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            InputStream inputStream = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }

            reader.close();
            inputStream.close();
            connection.disconnect();

            return stringBuilder.toString();
        }

        private String getImageIdFromJson(String json) throws JSONException {
            JSONObject jsonObject = new JSONObject(json);
            return jsonObject.getString("_id");
        }

        private String getImageUrlFromJson(String json) throws JSONException {
            JSONObject jsonObject = new JSONObject(json);
            return "https://cataas.com"+jsonObject.getString("url");
        }

        private Bitmap downloadImage(String imageUrl) throws IOException {
            URL url = new URL(imageUrl);
            return BitmapFactory.decodeStream(url.openStream());
        }

        private File getImageFile(String imageId) {
            File directory = getFilesDir();
            return new File(directory, imageId + ".png");
        }

        private void saveImageToFile(Bitmap bitmap, File file) throws IOException {
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
        }
    }
}
