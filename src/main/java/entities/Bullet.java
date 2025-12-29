package entities;

public class Bullet {
    public double x, y;
    public double vx, vy;
    public int damage;

    public Bullet(double x, double y, double vx, double vy, int damage) {
        this.x = x; this.y = y;
        this.vx = vx; this.vy = vy;
        this.damage = damage;
    }
}
