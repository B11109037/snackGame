package game;

public class GameState {
    public final int difficulty;

    // Tunables (cards will modify)
    public double moveInterval = 0.10;
    public double fireInterval = 0.18;
    public double enemyStepInterval = 0.25;

    public double bulletSpeed = 520;
    public int bulletDamage = 1;

    // Progress
    public int level = 1;
    public int exp = 0;
    public int expToNext = 6;

    public int kills = 0;
    public int score = 0;

    public GameState(int difficulty) {
        this.difficulty = clampInt(difficulty, 1, 10);
    }

    private static int clampInt(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
