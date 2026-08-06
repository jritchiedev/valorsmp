package net.thevalorsmp.combat;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import net.thevalorsmp.combat.cooldown.AbilityCooldownManager;
import org.junit.jupiter.api.Test;

class AbilityCooldownManagerTest {

    private static final String ABILITY = "mace";

    @Test
    void start_thenBeforeExpiry_isOnCooldownAndCountsDown() {
        MutableClock clock = new MutableClock(Instant.EPOCH);
        AbilityCooldownManager manager = new AbilityCooldownManager(clock);
        UUID player = UUID.randomUUID();

        assertThat(manager.isOnCooldown(player, ABILITY)).isFalse();

        manager.start(player, ABILITY, Duration.ofSeconds(30));
        assertThat(manager.isOnCooldown(player, ABILITY)).isTrue();
        assertThat(manager.remainingSeconds(player, ABILITY)).isEqualTo(30);

        clock.advance(Duration.ofSeconds(29));
        assertThat(manager.isOnCooldown(player, ABILITY)).isTrue();
        assertThat(manager.remainingSeconds(player, ABILITY)).isEqualTo(1);

        clock.advance(Duration.ofSeconds(1));
        assertThat(manager.isOnCooldown(player, ABILITY)).isFalse();
        assertThat(manager.remainingSeconds(player, ABILITY)).isZero();
    }

    @Test
    void clear_removesActiveCooldown() {
        MutableClock clock = new MutableClock(Instant.EPOCH);
        AbilityCooldownManager manager = new AbilityCooldownManager(clock);
        UUID player = UUID.randomUUID();
        manager.start(player, ABILITY, Duration.ofSeconds(10));

        manager.clear(player);

        assertThat(manager.isOnCooldown(player, ABILITY)).isFalse();
    }
}
