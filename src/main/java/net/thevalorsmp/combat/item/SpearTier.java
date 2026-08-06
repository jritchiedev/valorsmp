package net.thevalorsmp.combat.item;

import java.util.Locale;
import java.util.Optional;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

/**
 * The six spear tiers (docs/combat.md). Spears are custom items on a Paper server, so each tier is
 * backed by an existing base material (reskinned via resource pack later) and identified by a
 * persistent-data tag rather than a distinct item id.
 */
public enum SpearTier {

    /** Wooden spear. */
    WOOD(Material.WOODEN_SWORD, "Wooden"),
    /** Copper spear (stone-sword base until a copper base/resource pack lands). */
    COPPER(Material.STONE_SWORD, "Copper"),
    /** Iron spear. */
    IRON(Material.IRON_SWORD, "Iron"),
    /** Gold spear. */
    GOLD(Material.GOLDEN_SWORD, "Golden"),
    /** Diamond spear. */
    DIAMOND(Material.DIAMOND_SWORD, "Diamond"),
    /** Netherite spear. */
    NETHERITE(Material.NETHERITE_SWORD, "Netherite");

    private final Material baseMaterial;
    private final String label;

    SpearTier(Material baseMaterial, String label) {
        this.baseMaterial = baseMaterial;
        this.label = label;
    }

    /**
     * The vanilla item this spear tier is built on.
     *
     * @return the base material
     */
    public @NotNull Material baseMaterial() {
        return baseMaterial;
    }

    /**
     * The player-facing item name, e.g. {@code "Iron Spear"}.
     *
     * @return the display name
     */
    public @NotNull String displayName() {
        return label + " Spear";
    }

    /**
     * Parses a tier from user input (case-insensitive), e.g. {@code "iron"}.
     *
     * @param raw the input token
     * @return the matching tier, or empty if none matches
     */
    public static @NotNull Optional<SpearTier> fromString(@NotNull String raw) {
        try {
            return Optional.of(valueOf(raw.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
