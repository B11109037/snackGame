package data;

import com.mongodb.client.*;
import com.mongodb.client.model.Sorts;
import entities.RunRecord;
import org.bson.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class MongoService implements AutoCloseable {

    private final MongoClient client;
    private final MongoDatabase db;

    public MongoService(String uri, String dbName) {
        this.client = MongoClients.create(uri);
        this.db = client.getDatabase(dbName);
    }

    private MongoCollection<Document> col() {
        return db.getCollection("runs");
    }

    public void saveRun(int difficulty, int level, double survivedSec, int kills, int score) {
        Document doc = new Document()
                .append("ts", Instant.now().toString())
                .append("difficulty", difficulty)
                .append("level", level)
                .append("survivedSec", survivedSec)
                .append("kills", kills)
                .append("score", score);

        col().insertOne(doc);
    }

    public List<RunRecord> fetchRecentRuns(int n) {
        List<RunRecord> out = new ArrayList<>();
        FindIterable<Document> it = col().find().sort(Sorts.descending("ts")).limit(n);

        for (Document d : it) {
            out.add(new RunRecord(
                    d.getString("ts"),
                    d.getInteger("difficulty", -1),
                    d.getInteger("level", -1),
                    d.getDouble("survivedSec") == null ? 0 : d.getDouble("survivedSec"),
                    d.getInteger("kills", 0),
                    d.getInteger("score", 0)
            ));
        }
        return out;
    }

    @Override
    public void close() {
        client.close();
    }
}
