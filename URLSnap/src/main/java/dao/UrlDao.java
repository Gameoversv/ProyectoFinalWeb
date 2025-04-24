package dao;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import modelo.Stats;
import modelo.Url;
import config.MongoConfig;
import com.mongodb.client.MongoCollection;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;

public class UrlDao {

    private final MongoCollection<Url> urlsCollection;

    public UrlDao() {
        this.urlsCollection = MongoConfig.getUrlsCollection();
    }

    // Métodos corregidos y optimizados
    public Url getUrlByOriginalUrlAndUserId(String urlOriginal, String userId) {
        Bson filter = Filters.and(
                Filters.eq("originalUrl", urlOriginal),
                Filters.eq("userId", userId)
        );
        return urlsCollection.find(filter).first();
    }

    public List<Url> getAllUrls() {
        return urlsCollection.find().into(new ArrayList<>());
    }

    public void addUrl(Url url) {
        urlsCollection.insertOne(url);
    }

    public Url getUrlByShortCode(String shortCode) {
        return urlsCollection.find(Filters.eq("shortCode", shortCode)).first();
    }

    // Corregido: Usar String para userId
    public List<Url> getUrlsByUserId(String userId) {
        return urlsCollection.find(Filters.eq("userId", userId))
                .into(new ArrayList<>());
    }

    // Actualización específica de estadísticas
    public boolean agregarEstadistica(String shortCode, Stats stats) {
        UpdateResult result = urlsCollection.updateOne(
                Filters.eq("shortCode", shortCode),
                Updates.push("stats", stats)
        );
        return result.getModifiedCount() > 0;
    }

    // Eliminar versión con Integer
    public boolean deleteUrlByShortCodeAndUser(String shortCode, String userId) {
        DeleteResult result = urlsCollection.deleteOne(
                Filters.eq("shortCode", shortCode)
        );
        return result.getDeletedCount() > 0;
    }


    public String generarCodigoUnico() {
        String base62 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder code = new StringBuilder();
        do {
            code.setLength(0);
            for (int i = 0; i < 6; i++) {
                code.append(base62.charAt((int) (Math.random() * base62.length())));
            }
        } while (getUrlByShortCode(code.toString()) != null);
        return code.toString();
    }

}