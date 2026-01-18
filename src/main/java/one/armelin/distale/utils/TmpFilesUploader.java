package one.armelin.distale.utils;

import okhttp3.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.json.JSONObject;

public class TmpFilesUploader {

    public static String uploadImage(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, format, baos);
        byte[] imageBytes = baos.toByteArray();

        OkHttpClient client = new OkHttpClient();

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", "image." + format,
                        RequestBody.create(imageBytes, MediaType.parse("image/" + format)))
                .build();

        Request request = new Request.Builder()
                .url("https://tmpfiles.org/api/v1/upload")
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String responseBody = response.body().string();
                JSONObject json = new JSONObject(responseBody);

                if (json.getString("status").equals("success")) {
                    return json.getJSONObject("data").getString("url").replace("http://tmpfiles.org/", "https://tmpfiles.org/dl/");
                } else {
                    throw new IOException("Upload failed: " + responseBody);
                }
            } else {
                throw new IOException("Upload failed: " + response.code());
            }
        }
    }

    public static String uploadImage(BufferedImage image) throws IOException {
        return uploadImage(image, "png");
    }
}
