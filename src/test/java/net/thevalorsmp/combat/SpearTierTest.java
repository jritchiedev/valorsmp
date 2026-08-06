package net.thevalorsmp.combat;

import static org.assertj.core.api.Assertions.assertThat;

import net.thevalorsmp.combat.item.SpearTier;
import org.junit.jupiter.api.Test;

class SpearTierTest {

    @Test
    void fromString_parsesKnownTiersCaseInsensitively() {
        assertThat(SpearTier.fromString("iron")).contains(SpearTier.IRON);
        assertThat(SpearTier.fromString("NETHERITE")).contains(SpearTier.NETHERITE);
        assertThat(SpearTier.fromString(" wood ")).contains(SpearTier.WOOD);
    }

    @Test
    void fromString_unknownTier_isEmpty() {
        assertThat(SpearTier.fromString("bogus")).isEmpty();
    }

    @Test
    void displayName_isReadable() {
        assertThat(SpearTier.IRON.displayName()).isEqualTo("Iron Spear");
        assertThat(SpearTier.GOLD.displayName()).isEqualTo("Golden Spear");
    }
}
