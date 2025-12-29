package entities;

public class RunRecord {
    public String ts;
    public int difficulty;
    public int level;
    public double survivedSec;
    public int kills;
    public int score;

    public RunRecord(String ts, int difficulty, int level, double survivedSec, int kills, int score) {
        this.ts = ts;
        this.difficulty = difficulty;
        this.level = level;
        this.survivedSec = survivedSec;
        this.kills = kills;
        this.score = score;
    }
}
