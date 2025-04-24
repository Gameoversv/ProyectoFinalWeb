package dao;

import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import modelo.User;
import config.MongoConfig;
import com.mongodb.client.MongoCollection;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

public class UserDao {

    private final MongoCollection<User> usersCollection;

    public UserDao() {
        this.usersCollection = MongoConfig.getUsersCollection();
    }

    public List<User> getAllUsers() {
        return usersCollection.find().into(new ArrayList<>());
    }

    public boolean deleteUserById(ObjectId id) {
        DeleteResult result = usersCollection.deleteOne(Filters.eq("_id", id));
        return result.getDeletedCount() > 0;
    }

    public User getUserById(ObjectId id) {
        return usersCollection.find(Filters.eq("_id", id)).first();
    }

    public void addUser(User newUser) {
        usersCollection.insertOne(newUser);
    }

    public User getUserByUsername(String username) {
        return usersCollection.find(Filters.eq("username", username)).first();
    }

    public boolean updateUser(String username, User updatedUser) {
        UpdateResult result = usersCollection.replaceOne(
                Filters.eq("username", username),
                updatedUser
        );
        return result.getModifiedCount() > 0;
    }

    public boolean deleteUser(String username) {
        DeleteResult result = usersCollection.deleteOne(Filters.eq("username", username));
        return result.getDeletedCount() > 0;
    }
    // Obtener usuario por ID como String (conversión segura)
    public User getById(String id) {
        try {
            return getUserById(new ObjectId(id));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // Actualizar usuario completo por su ObjectId
    public boolean update(User user) {
        UpdateResult result = usersCollection.replaceOne(
                Filters.eq("_id", user.getId()),
                user
        );
        return result.getModifiedCount() > 0;
    }


}