package cards;

import javax.swing.*;
import java.util.List;

public class CardDialog {

    public static Card choose(JFrame owner, int level, List<Card> options) {
        if (options == null || options.isEmpty()) return null;

        String[] names = new String[options.size()];
        for (int i = 0; i < options.size(); i++) {
            Card c = options.get(i);
            names[i] = c.name() + " - " + c.desc();
        }

        int pick = JOptionPane.showOptionDialog(
                owner,
                "Level " + level + "：選一張卡",
                "Pick a Card",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                names,
                names[0]
        );

        if (pick < 0 || pick >= options.size()) return null;
        return options.get(pick);
    }
}
