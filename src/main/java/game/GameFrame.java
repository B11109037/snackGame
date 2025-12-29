package game;

import javax.swing.*;

public class GameFrame extends JFrame {
    public GameFrame(int difficulty) {
        super("snakeGame");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        GamePanel panel = new GamePanel(difficulty, this);
        setContentPane(panel);
        pack();
        setLocationRelativeTo(null);

        panel.start();
    }
}
