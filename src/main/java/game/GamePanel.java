package game;

import entities.*;

import ui.DifficultyDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class GamePanel extends JPanel {

    private static final int CELL = 32;
    private static final int COLS = 28;
    private static final int ROWS = 18;
    private static final int W = COLS * CELL;
    private static final int H = ROWS * CELL;

    private static final int TICK_MS = 16;

    private final JFrame owner;
    private final GameEngine engine;

    private boolean up, down, left, right;

    private long lastNs = 0;
    private Timer timer;

    // sprites
    private final SpriteStore sprites = new SpriteStore();
    private final Image playerImg;
    private final Image enemyImg;
    private final Image mateImg;
    private final Image bulletImg;
    private final Image expImg;

    public GamePanel(int difficulty, JFrame owner) {
        this.owner = owner;
        this.engine = new GameEngine(difficulty, owner);

        playerImg = sprites.loadAndScale("assets/player.png", 50, 50);
        enemyImg  = sprites.loadAndScale("assets/enemy.png", 50, 50);
        mateImg   = sprites.loadAndScale("assets/teammate.png", 50, 50);
        bulletImg = sprites.loadAndScale("assets/bullet.png", 20, 20);
        expImg    = sprites.loadAndScale("assets/exp.png", 40, 40);

        setPreferredSize(new Dimension(W, H));
        setBackground(new Color(0x10, 0x12, 0x14));
        setFocusable(true);

        setupKeyBindings();
    }

    public void start() {
        requestFocusInWindow();
        lastNs = System.nanoTime();
        timer = new Timer(TICK_MS, e -> tick());
        timer.start();
    }

    private void clearInputFlags() {
        up = down = left = right = false;
    }


    private void tick() {
        long now = System.nanoTime();
        double dt = (now - lastNs) / 1_000_000_000.0;
        lastNs = now;

        engine.tick(dt, up, down, left, right);
        if (engine.needClearInputOnResume) {
            clearInputFlags();
            engine.needClearInputOnResume = false;

            // 重新拿回焦點，避免鍵盤事件沒回來
            requestFocusInWindow();

            // 重置時間，避免 dt 因為對話框阻塞而暴衝
            lastNs = System.nanoTime();
        }
        
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawGrid(g2);

        // HUD
        g2.setColor(Color.WHITE);
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 16f));
        String hud = String.format(
                "snakeGame | Lv:%d EXP:%d/%d Team:%d Diff:%d Time:%s | K:%d Score:%d",
                engine.gs.level, engine.gs.exp, engine.gs.expToNext,
                engine.teammates.size(), engine.getDifficulty(),
                formatTime(engine.survivedSec), engine.gs.kills, engine.gs.score
        );
        g2.drawString(hud, 12, 22);

        // exp
        for (ExpOrb orb : engine.expOrbs)
            drawCentered(g2, expImg, engine.centerX(orb.gx), engine.centerY(orb.gy));

        // bullets
        for (Bullet b : engine.bullets)
            drawCentered(g2, bulletImg, b.x, b.y);

        // enemies
        for (Enemy e : engine.enemies) {
            drawCentered(g2, enemyImg, engine.centerX(e.gx), engine.centerY(e.gy));
            drawHpBar(g2, engine.centerX(e.gx), engine.centerY(e.gy) - 20, 30, 6, e.hp, e.maxHp);
        }

        // teammates
        for (GridPos t : engine.teammates)
            drawCentered(g2, mateImg, engine.centerX(t.x), engine.centerY(t.y));

        // player
        drawCentered(g2, playerImg, engine.centerX(engine.px), engine.centerY(engine.py));

        if (!engine.alive) drawGameOverOverlay(g2);

        g2.dispose();
    }

    private void drawCentered(Graphics2D g2, Image img, double cx, double cy) {
        if (img == null) return;
        int w = img.getWidth(null);
        int h = img.getHeight(null);
        if (w <= 0 || h <= 0) return;
        g2.drawImage(img, (int)(cx - w / 2.0), (int)(cy - h / 2.0), null);
    }



    private void drawGrid(Graphics2D g2) {
        g2.setColor(new Color(255, 255, 255, 18));
        for (int c = 0; c <= COLS; c++) {
            int x = c * CELL;
            g2.drawLine(x, 0, x, H);
        }
        for (int r = 0; r <= ROWS; r++) {
            int y = r * CELL;
            g2.drawLine(0, y, W, y);
        }
    }

    private void drawHpBar(Graphics2D g2, double x, double y, int w, int h, int hp, int maxHp) {
        int ix = (int) Math.round(x - w / 2.0);
        int iy = (int) Math.round(y - h / 2.0);

        g2.setColor(new Color(0, 0, 0, 140));
        g2.fillRect(ix, iy, w, h);

        double ratio = Math.max(0, Math.min(1.0, hp / (double) maxHp));
        int fw = (int) Math.round((w - 2) * ratio);

        g2.setColor(new Color(0x45, 0xE0, 0x4B));
        g2.fillRect(ix + 1, iy + 1, fw, h - 2);

        g2.setColor(new Color(255, 255, 255, 90));
        g2.drawRect(ix, iy, w, h);
    }

    private void drawGameOverOverlay(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 170));
        g2.fillRect(0, 0, getWidth(), getHeight());

        // Title
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 52f));
        g2.setColor(new Color(0xFF5A5A));
        String over = "GAME OVER";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(over, (getWidth() - fm.stringWidth(over)) / 2, 140);

        // Current run
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 20f));
        g2.setColor(Color.WHITE);
        String cur = String.format("This Run | Level:%d  Time:%s  K:%d  Score:%d",
                engine.gs.level, formatTime(engine.survivedSec), engine.gs.kills, engine.gs.score);
        g2.drawString(cur, (getWidth() - g2.getFontMetrics().stringWidth(cur)) / 2, 180);

        // History header
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 18f));
        g2.setColor(new Color(255, 255, 255, 220));
        g2.drawString("History (latest):", 90, 230);

        // History lines
        int y = 260;
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 16f));

        if (engine.recentHistory == null || engine.recentHistory.isEmpty()) {
            g2.drawString("- (no records yet)", 110, y);
        } else {
            int rank = 1;
            for (RunRecord r : engine.recentHistory) {
                String line = String.format(
                        "%d) Lv:%d Time:%s Diff:%d K:%d Score:%d %s",
                        rank,
                        r.level,
                        formatTime(r.survivedSec),
                        r.difficulty,
                        r.kills,
                        r.score,
                        r.ts
                );
                g2.drawString(line, 110, y);
                y += 22;
                rank++;
                if (y > getHeight() - 90) break;
            }
        }

        // Hint
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 18f));
        g2.setColor(Color.WHITE);
        String hint = "Press R or ESC to restart";
        g2.drawString(hint, (getWidth() - g2.getFontMetrics().stringWidth(hint)) / 2, getHeight() - 60);
    }


    // ===== Key Bindings =====
    private void setupKeyBindings() {
        InputMap im = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();

        bind(im, am, "W_P", KeyStroke.getKeyStroke("pressed W"), () -> up = true);
        bind(im, am, "W_R", KeyStroke.getKeyStroke("released W"), () -> up = false);

        bind(im, am, "S_P", KeyStroke.getKeyStroke("pressed S"), () -> down = true);
        bind(im, am, "S_R", KeyStroke.getKeyStroke("released S"), () -> down = false);

        bind(im, am, "A_P", KeyStroke.getKeyStroke("pressed A"), () -> left = true);
        bind(im, am, "A_R", KeyStroke.getKeyStroke("released A"), () -> left = false);

        bind(im, am, "D_P", KeyStroke.getKeyStroke("pressed D"), () -> right = true);
        bind(im, am, "D_R", KeyStroke.getKeyStroke("released D"), () -> right = false);

        bind(im, am, "R_P", KeyStroke.getKeyStroke("pressed R"), this::restartWithDifficultySelect);
        bind(im, am, "ESC_P", KeyStroke.getKeyStroke("pressed ESCAPE"), this::restartWithDifficultySelect);
    }

    private void restartWithDifficultySelect() {
        up = down = left = right = false;
        if (timer != null) timer.stop();
        engine.close();

        int newDiff = DifficultyDialog.askDifficulty();
        SwingUtilities.invokeLater(() -> {
            owner.dispose();
            new GameFrame(newDiff).setVisible(true);
        });
    }

    private void bind(InputMap im, ActionMap am, String name, KeyStroke ks, Runnable r) {
        im.put(ks, name);
        am.put(name, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { r.run(); }
        });
    }

    private String formatTime(double sec) {
        int s = (int)Math.floor(sec);
        int m = s / 60;
        int r = s % 60;
        return String.format("%02d:%02d", m, r);
    }
}
