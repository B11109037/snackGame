package game;

import ui.DifficultyDialog;

import javax.swing.*;

public class SnakeGame{
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            int diff = DifficultyDialog.askDifficulty();
            new GameFrame(diff).setVisible(true);
        });
    }
}
