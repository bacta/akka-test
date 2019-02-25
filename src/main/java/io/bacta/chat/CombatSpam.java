package io.bacta.chat;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CombatSpam {
    private final String attackerName;
    private final String defenderName;
    private final String attackName;
    private final String stat;
    private final int value;

    @Override
    public String toString() {
        return String.format("%s attacks %s with %s for %d of %s damage.", attackerName, defenderName, attackName, value, stat);
    }
}
