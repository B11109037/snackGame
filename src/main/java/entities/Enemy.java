package entities;

public class Enemy {
    public int gx, gy;
    public int hp, maxHp;

    public Enemy(int gx, int gy, int hp) {
        this.gx = gx;
        this.gy = gy;
        this.hp = hp;
        this.maxHp = hp;
    }
}
