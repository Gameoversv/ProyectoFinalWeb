package util;

import org.bson.types.ObjectId;

public class MongoUtils {
    public static boolean isValidObjectId(String id) {
        return ObjectId.isValid(id);
    }
}