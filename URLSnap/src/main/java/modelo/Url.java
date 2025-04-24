package modelo;

import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.types.ObjectId;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Url implements Serializable {
    @BsonId
    private ObjectId id;
    private String originalUrl;
    private String shortCode;
    private String userId;
    private Date createdAt;
    private List<Stats> stats = new ArrayList<>();
    private String previewImage; // ✅ Nuevo campo para imagen en base64

    // Constructor vacío necesario para MongoDB
    public Url() {}

    // Getters y Setters
    public ObjectId getId() { return id; }
    public void setId(ObjectId id) { this.id = id; }

    public String getOriginalUrl() { return originalUrl; }
    public void setOriginalUrl(String originalUrl) { this.originalUrl = originalUrl; }

    public String getShortCode() { return shortCode; }
    public void setShortCode(String shortCode) { this.shortCode = shortCode; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public List<Stats> getStats() { return stats; }
    public void setStats(List<Stats> stats) { this.stats = stats; }

    // ✅ Getter y Setter de la vista previa
    public String getPreviewImage() { return previewImage; }
    public void setPreviewImage(String previewImage) { this.previewImage = previewImage; }
}
