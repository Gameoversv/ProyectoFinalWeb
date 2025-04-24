package modelo;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.bson.codecs.pojo.annotations.BsonCreator;
import org.bson.codecs.pojo.annotations.BsonProperty;
import java.io.Serializable;
import java.util.Date;

public class Stats implements Serializable {
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    private Date accessedAt;

    @BsonProperty("ipAddress")
    private String ipAddress;

    @BsonProperty("browser")
    private String browser;

    @BsonProperty("os")
    private String os;

    @BsonProperty("referrer")
    private String referrer;

    @BsonProperty("country")
    private String country;

    @BsonCreator
    public Stats(
            @BsonProperty("accessedAt") Date accessedAt,
            @BsonProperty("ipAddress") String ipAddress,
            @BsonProperty("browser") String browser,
            @BsonProperty("os") String os,
            @BsonProperty("referrer") String referrer,
            @BsonProperty("country") String country
    ) {
        this.accessedAt = accessedAt != null ? accessedAt : new Date();
        this.ipAddress = ipAddress;
        this.browser = browser;
        this.os = os;
        this.referrer = referrer;
        this.country = country;
    }

    // Getters y Setters
    public Date getAccessedAt() { return accessedAt; }
    public void setAccessedAt(Date accessedAt) { this.accessedAt = accessedAt; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getBrowser() { return browser; }
    public void setBrowser(String browser) { this.browser = browser; }

    public String getOs() { return os; }
    public void setOs(String os) { this.os = os; }

    public String getReferrer() { return referrer; }
    public void setReferrer(String referrer) { this.referrer = referrer; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
}