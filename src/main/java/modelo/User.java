package modelo;

import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.types.ObjectId;
import java.io.Serializable;
import java.util.Date;

public class User implements Serializable {
    @BsonId
    private ObjectId id;
    private String nombre;
    private String username;
    private String password;
    private boolean admin;
    private Date createdAt;

    // Constructor vacío necesario para MongoDB
    public User() {}

    // Constructor completo
    public User(String nombre, String username, String password, boolean admin) {
        this.nombre = nombre;
        this.username = username;
        this.password = password;
        this.admin = admin;
        this.createdAt = new Date();
    }

    // Getters y Setters
    public ObjectId getId() { return id; }
    public void setId(ObjectId id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public boolean isAdmin() { return admin; }
    public void setAdmin(boolean admin) { this.admin = admin; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}