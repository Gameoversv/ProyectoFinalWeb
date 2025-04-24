package util;

import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class PreviewUtil {
    public static String fetchPreviewImageBase64(String url) {
        try {
            String apiUrl = "https://api.microlink.io?url=" + URLEncoder.encode(url, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(apiUrl)).build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

            JSONObject json = new JSONObject(response.body());
            String imageUrl = json.getJSONObject("data").getString("image");

            BufferedImage img = ImageIO.read(new URL(imageUrl));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            System.err.println("❌ Error obteniendo vista previa: " + e.getMessage());
            return "";
        }
    }
}