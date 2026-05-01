import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoClient;

public class TestMongo {
    public static void main(String[] args) {
        String uri = "mongodb+srv://gabbyreads2020_db_user:GLmJefZiVCnQbVvH@naijavehicle.p1mreuh.mongodb.net/?appName=naijaVehicle";
        try (MongoClient mongoClient = MongoClients.create(uri)) {
            mongoClient.listDatabaseNames().first();
            System.out.println("Connection successful!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
