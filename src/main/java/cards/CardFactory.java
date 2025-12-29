package cards;

import game.GameState;

import java.util.ArrayList;
import java.util.List;

public class CardFactory {

    public static List<Card> createDefaultCards() {
        List<Card> list = new ArrayList<>();

        list.add(new Card() {
            public String name() { return "Rapid Fire"; }
            public String desc() { return "射速提升"; }
            public void apply(GameState gs) { gs.fireInterval = Math.max(0.07, gs.fireInterval * 0.85); }
        });

        list.add(new Card() {
            public String name() { return "Move Boost"; }
            public String desc() { return "移動更快"; }
            public void apply(GameState gs) { gs.moveInterval = Math.max(0.05, gs.moveInterval * 0.85); }
        });

        list.add(new Card() {
            public String name() { return "Damage +1"; }
            public String desc() { return "子彈傷害+1"; }
            public void apply(GameState gs) { gs.bulletDamage += 1; }
        });

        list.add(new Card() {
            public String name() { return "Bullet Speed"; }
            public String desc() { return "子彈更快"; }
            public void apply(GameState gs) { gs.bulletSpeed *= 1.12; }
        });

        list.add(new Card() {
            public String name() { return "Enemy Slow"; }
            public String desc() { return "敵人走慢一點"; }
            public void apply(GameState gs) { gs.enemyStepInterval *= 1.08; }
        });

        return list;
    }
}
