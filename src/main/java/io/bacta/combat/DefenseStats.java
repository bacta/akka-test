package io.bacta.combat;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class DefenseStats {
    private final String name;
    private final CreatureStats ham;
}
