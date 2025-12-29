package game;

import cards.Card;
import cards.CardDialog;
import cards.CardFactory;
import data.MongoService;
import entities.*;

import javax.swing.*;
import java.time.Instant;
import java.util.*;

public class GameEngine {

    private static final int CELL = 32;
    private static final int COLS = 28;
    private static final int ROWS = 18;
    private static final int W = COLS * CELL;
    private static final int H = ROWS * CELL;

    private final JFrame owner;
    private final int difficulty;
    private final double enemySpawnInterval;

    public final GameState gs;
    public boolean needClearInputOnResume = false;

    // time / status
    public double survivedSec = 0;
    public boolean alive = true;

    // player
    public int px = COLS / 2, py = ROWS / 2;

    // cooldowns
    private double moveCd = 0;
    private double fireCd = 0;
    private double enemyStepCd = 0;
    private double spawnCd = 0;

    // objects
    public final List<Enemy> enemies = new ArrayList<>();
    public final List<Bullet> bullets = new ArrayList<>();
    public final List<ExpOrb> expOrbs = new ArrayList<>();

    // teammates
    public final List<GridPos> teammates = new ArrayList<>();
    private final Deque<GridPos> trail = new ArrayDeque<>();
    private final int trailMax = 6000;

    private final Random rng = new Random();

    // cards
    private final List<Card> cardPool = CardFactory.createDefaultCards();
    public final List<String> pickedCards = new ArrayList<>();

    // mongo
    private final MongoService mongo = new MongoService("mongodb://localhost:27017", "snakeGame");
    public List<RunRecord> recentHistory = new ArrayList<>();
    private static final int HISTORY_SHOW_N = 8;

    public GameEngine(int difficulty, JFrame owner) {
        this.owner = owner;
        this.difficulty = clampInt(difficulty, 1, 10);
        this.enemySpawnInterval = mapDifficultyToSpawnInterval(this.difficulty);
        this.gs = new GameState(this.difficulty);
    }

    public void close() {
        try { mongo.close(); } catch (Exception ignored) {}
    }

    /** GamePanel 每帧呼叫 */
    public void tick(double dt, boolean up, boolean down, boolean left, boolean right) {
        if (!alive) return;

        survivedSec += dt;
        moveCd -= dt;
        fireCd -= dt;
        spawnCd -= dt;
        enemyStepCd -= dt;

        // --- move ---
        if (moveCd <= 0) {
            int nx = px, ny = py;

            if (up && !down) ny--;
            else if (down && !up) ny++;
            else if (left && !right) nx--;
            else if (right && !left) nx++;

            if ((nx != px || ny != py) && inBounds(nx, ny)) {
                px = nx; py = ny;
                moveCd = gs.moveInterval;

                trail.addLast(new GridPos(px, py));
                while (trail.size() > trailMax) trail.removeFirst();
                updateTeammatesFollow();
            }
        }

        // --- enemy step ---
        if (enemyStepCd <= 0) {
            enemyStepCd = gs.enemyStepInterval;
            for (Enemy e : enemies) {
                stepEnemyTowardPlayer(e);
                if (e.gx == px && e.gy == py) {
                    gameOver();
                    return;
                }
            }
        }

        // --- spawn ---
        if (spawnCd <= 0) {
            spawnCd = enemySpawnInterval;
            spawnEnemyAtBorder();
        }

        // --- fire ---
        if (fireCd <= 0) {
            fireCd = gs.fireInterval;

            Enemy target = findNearestEnemyPixel(centerX(px), centerY(py));
            if (target != null) spawnBullet(centerX(px), centerY(py), centerX(target.gx), centerY(target.gy));

            for (GridPos t : teammates) {
                Enemy tt = findNearestEnemyPixel(centerX(t.x), centerY(t.y));
                if (tt != null) spawnBullet(centerX(t.x), centerY(t.y), centerX(tt.gx), centerY(tt.gy));
            }
        }

        // --- bullets update (homing) ---
        for (int i = bullets.size() - 1; i >= 0; i--) {
            Bullet b = bullets.get(i);

            Enemy t = findNearestEnemyPixel(b.x, b.y);
            if (t != null) {
                double tx = centerX(t.gx), ty = centerY(t.gy);
                double dx = tx - b.x, dy = ty - b.y;
                double len = Math.hypot(dx, dy);
                if (len > 0) {
                    b.vx = (dx / len) * gs.bulletSpeed;
                    b.vy = (dy / len) * gs.bulletSpeed;
                }
            }

            b.x += b.vx * dt;
            b.y += b.vy * dt;

            if (b.x < -50 || b.x > W + 50 || b.y < -50 || b.y > H + 50) {
                bullets.remove(i);
                continue;
            }

            for (int j = enemies.size() - 1; j >= 0; j--) {
                Enemy e = enemies.get(j);
                double ex = centerX(e.gx), ey = centerY(e.gy);

                if (dist2(b.x, b.y, ex, ey) <= sq(12)) {
                    e.hp -= b.damage;
                    bullets.remove(i);

                    if (e.hp <= 0) {
                        enemies.remove(j);
                        gs.kills++;
                        gs.score += 10;
                        expOrbs.add(new ExpOrb(e.gx, e.gy));
                    }
                    break;
                }
            }
        }

        // --- pick exp ---
        for (int i = expOrbs.size() - 1; i >= 0; i--) {
            ExpOrb orb = expOrbs.get(i);
            if (orb.gx == px && orb.gy == py) {
                gs.exp++;
                expOrbs.remove(i);
                if (gs.exp >= gs.expToNext) levelUp();
            }
        }
    }

    private void levelUp() {
        gs.level++;
        gs.exp = 0;
        gs.expToNext += 2;

        teammates.add(new GridPos(px, py));

        // 抽卡（阻塞式 dialog）
        List<Card> options = pick3Cards();
        Card chosen = CardDialog.choose(owner, gs.level, options);
        if (chosen != null) {
            chosen.apply(gs);
            pickedCards.add(chosen.name());
        }
          needClearInputOnResume = true;
    }

    private List<Card> pick3Cards() {
        List<Card> copy = new ArrayList<>(cardPool);
        Collections.shuffle(copy);
        return copy.subList(0, Math.min(3, copy.size()));
    }

    private void gameOver() {
        alive = false;
        try {
            mongo.saveRun(difficulty, gs.level, survivedSec, gs.kills, gs.score);
            recentHistory = mongo.fetchRecentRuns(HISTORY_SHOW_N);
        } catch (Exception ex) {
            ex.printStackTrace();
            recentHistory = Collections.singletonList(
                    new RunRecord(Instant.now().toString(), -1, -1, 0, 0, 0)
            );
        }
    }

    // ===== AI / spawn / helpers =====

    private void stepEnemyTowardPlayer(Enemy e) {
        int dx = px - e.gx;
        int dy = py - e.gy;

        int nx = e.gx, ny = e.gy;
        if (Math.abs(dx) >= Math.abs(dy)) nx += Integer.compare(dx, 0);
        else ny += Integer.compare(dy, 0);

        if (inBounds(nx, ny)) { e.gx = nx; e.gy = ny; }
    }

    private void spawnEnemyAtBorder() {
        int side = rng.nextInt(4);
        int gx, gy;

        switch (side) {
            case 0: gx = rng.nextInt(COLS); gy = 0; break;
            case 1: gx = COLS - 1; gy = rng.nextInt(ROWS); break;
            case 2: gx = rng.nextInt(COLS); gy = ROWS - 1; break;
            default: gx = 0; gy = rng.nextInt(ROWS); break;
        }

        int timeBonus = (int)(survivedSec / 20.0) * 2;
        int baseHp = 3 + difficulty + timeBonus;
        enemies.add(new Enemy(gx, gy, baseHp));
    }

    private void spawnBullet(double sx, double sy, double tx, double ty) {
        double vx = tx - sx;
        double vy = ty - sy;
        double len = Math.hypot(vx, vy);
        if (len == 0) return;
        vx /= len; vy /= len;

        bullets.add(new Bullet(sx, sy, vx * gs.bulletSpeed, vy * gs.bulletSpeed, gs.bulletDamage));
    }

    private Enemy findNearestEnemyPixel(double sx, double sy) {
        if (enemies.isEmpty()) return null;
        Enemy best = null;
        double bestD2 = Double.MAX_VALUE;
        for (Enemy e : enemies) {
            double ex = centerX(e.gx), ey = centerY(e.gy);
            double d2 = dist2(sx, sy, ex, ey);
            if (d2 < bestD2) { bestD2 = d2; best = e; }
        }
        return best;
    }

    private void updateTeammatesFollow() {
        if (teammates.isEmpty() || trail.isEmpty()) return;
        GridPos[] arr = trail.toArray(new GridPos[0]);
        int n = arr.length;

        for (int i = 0; i < teammates.size(); i++) {
            int idx = n - 1 - (i + 1);
            if (idx < 0) break;
            teammates.get(i).x = arr[idx].x;
            teammates.get(i).y = arr[idx].y;
        }
    }

    public boolean inBounds(int gx, int gy) { return gx >= 0 && gx < COLS && gy >= 0 && gy < ROWS; }
    public double centerX(int gx) { return gx * CELL + CELL / 2.0; }
    public double centerY(int gy) { return gy * CELL + CELL / 2.0; }

    public static int clampInt(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
    public static double sq(double v) { return v * v; }
    public static double dist2(double ax, double ay, double bx, double by) {
        double dx = ax - bx, dy = ay - by;
        return dx * dx + dy * dy;
    }

    public static double mapDifficultyToSpawnInterval(int diff) {
        double slow = 1.8;
        double fast = 0.25;
        double t = (diff - 1) / 9.0;
        return slow + (fast - slow) * t;
    }

    public int getDifficulty() { return difficulty; }
}
