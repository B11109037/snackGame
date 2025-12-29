package cards;

import game.GameState;

public interface Card {
    String name();
    String desc();
    void apply(GameState gs);
}
