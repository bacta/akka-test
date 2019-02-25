package io.bacta.sim;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class BattleSimulationResult {
    private final int id;
    private final int rounds;
    private final int totalParticipants;
    private final List<String> winningParticipants;
}
