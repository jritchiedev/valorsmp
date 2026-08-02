package net.thevalorsmp.progression;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import net.thevalorsmp.progression.model.ValorTier;
import org.junit.jupiter.api.Test;

class ValorTierTest {

    private static final List<Integer> THRESHOLDS = List.of(0, 5, 9, 13, 17);

    @Test
    void fromScore_mapsEachRangeToTier() {
        assertThat(ValorTier.fromScore(0, THRESHOLDS)).isEqualTo(ValorTier.I);
        assertThat(ValorTier.fromScore(4, THRESHOLDS)).isEqualTo(ValorTier.I);
        assertThat(ValorTier.fromScore(5, THRESHOLDS)).isEqualTo(ValorTier.II);
        assertThat(ValorTier.fromScore(8, THRESHOLDS)).isEqualTo(ValorTier.II);
        assertThat(ValorTier.fromScore(9, THRESHOLDS)).isEqualTo(ValorTier.III);
        assertThat(ValorTier.fromScore(12, THRESHOLDS)).isEqualTo(ValorTier.III);
        assertThat(ValorTier.fromScore(13, THRESHOLDS)).isEqualTo(ValorTier.IV);
        assertThat(ValorTier.fromScore(16, THRESHOLDS)).isEqualTo(ValorTier.IV);
        assertThat(ValorTier.fromScore(17, THRESHOLDS)).isEqualTo(ValorTier.V);
        assertThat(ValorTier.fromScore(20, THRESHOLDS)).isEqualTo(ValorTier.V);
        assertThat(ValorTier.fromScore(100, THRESHOLDS)).isEqualTo(ValorTier.V);
    }

    @Test
    void perks_matchSpecification() {
        assertThat(ValorTier.I.extendsPotionEffects()).isFalse();
        assertThat(ValorTier.II.extendsPotionEffects()).isTrue();
        assertThat(ValorTier.III.hasPermanentSpeed()).isTrue();
        assertThat(ValorTier.III.defaultSpeedAmplifier()).isZero();
        assertThat(ValorTier.III.hasPermanentStrength()).isFalse();
        assertThat(ValorTier.IV.defaultSpeedAmplifier()).isEqualTo(1);
        assertThat(ValorTier.IV.strengthAmplifier()).isZero();
        assertThat(ValorTier.IV.isSpeedSwitchable()).isTrue();
        assertThat(ValorTier.V.strengthAmplifier()).isEqualTo(1);
        assertThat(ValorTier.V.isSpeedSwitchable()).isTrue();
    }
}
