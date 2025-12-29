package ui;

import javax.swing.*;

public class DifficultyDialog {
    public static int askDifficulty() {
        String s = JOptionPane.showInputDialog(
                null,
                "輸入難度 1~10：",
                "選擇難度",
                JOptionPane.QUESTION_MESSAGE
        );
        try {
            int v = Integer.parseInt(s);
            return Math.max(1, Math.min(10, v));
        } catch (Exception e) {
            return 5;
        }
    }
}
