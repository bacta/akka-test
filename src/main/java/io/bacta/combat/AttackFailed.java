package io.bacta.combat;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class AttackFailed {
    private final String reason;
}