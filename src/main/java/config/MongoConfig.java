package config;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.bson.codecs.configuration.CodecRegistry;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import org.bson.codecs.pojo.PojoCodecProvider;
import modelo.User;
import modelo.Url;

public class MongoConfig {

    private static final String CONNECTION_STRING = "mongodb+srv://arturo:picapollo@prueba.nntwmmv.mongodb.net/?appName=Prueba";
    private static final String DB_NAME = "prueba3";

    private static MongoClient mongoClient;
    private static MongoDatabase database;


    // Inicializa la conexión a MongoDB, configura el codec para POJOs y crea las colecciones si no existen.

    public static void init() {
        // Configurar el codec para POJOs de forma automática
        CodecRegistry pojoCodecRegistry = fromRegistries(
                MongoClientSettings.getDefaultCodecRegistry(),
                fromProviders(PojoCodecProvider.builder().automatic(true).build())
        );

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new com.mongodb.ConnectionString(CONNECTION_STRING))
                .codecRegistry(pojoCodecRegistry)
                .build();

        mongoClient = MongoClients.create(settings);
        database = mongoClient.getDatabase(DB_NAME);

        // Inicializar colecciones (si es necesario, MongoDB las crea al insertar el primer documento,
        // pero podemos crearlas manualmente para agregar índices u otras configuraciones)
        if (!collectionExists("users")) {
            database.createCollection("users");
            // Ejemplo: crear índice único en username
            MongoCollection<User> usersCollection = database.getCollection("users", User.class);
            usersCollection.createIndex(Indexes.ascending("username"), new IndexOptions().unique(true));
        }
        if (!collectionExists("urls")) {
            database.createCollection("urls");
            // Se pueden crear índices para campos que se utilicen en consultas, por ejemplo shortCode
            MongoCollection<Url> urlsCollection = database.getCollection("urls", Url.class);
            urlsCollection.createIndex(Indexes.ascending("shortCode"), new IndexOptions().unique(true));
        }
    }

    //Comprueba si una colección existe en la base de datos
    private static boolean collectionExists(String collectionName) {
        for (String name : database.listCollectionNames()) {
            if (name.equalsIgnoreCase(collectionName)) {
                return true;
            }
        }
        return false;
    }

    public static MongoCollection<User> getUsersCollection() {
        return database.getCollection("users", User.class);
    }

    public static MongoCollection<Url> getUrlsCollection() {
        return database.getCollection("urls", Url.class);
    }

    // Cerrar conexión al finalizar la aplicación (opcional)
    public static void close() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }
}