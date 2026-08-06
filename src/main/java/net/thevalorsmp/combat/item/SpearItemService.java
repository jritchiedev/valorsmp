package net.thevalorsmp.combat.item;

import java.util.List;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Builds and identifies custom spear items. Spears are tagged in their {@link PersistentDataContainer}
 * so they can be recognised regardless of display name or (future) resource-pack model.
 */
public final class SpearItemService {

    private final NamespacedKey spearKey;
    private final NamespacedKey tierKey;
    private final NamespacedKey lungeKey;

    /**
     * Creates the service, deriving its data keys from the owning plugin.
     *
     * @param plugin the owning plugin
     */
    public SpearItemService(@NotNull Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        this.spearKey = new NamespacedKey(plugin, "spear");
        this.tierKey = new NamespacedKey(plugin, "spear_tier");
        this.lungeKey = new NamespacedKey(plugin, "lunge");
    }

    /**
     * Creates a spear item of the given tier, tagged and enchantable-ability marked with lunge.
     *
     * @param tier the spear tier
     * @return a new spear item stack
     */
    public @NotNull ItemStack create(@NotNull SpearTier tier) {
        Objects.requireNonNull(tier, "tier");
        ItemStack item = new ItemStack(tier.baseMaterial());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(tier.displayName(), NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Valor Spear", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("Lunge — right-click to dash", NamedTextColor.DARK_AQUA)
                        .decoration(TextDecoration.ITALIC, false)));
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(spearKey, PersistentDataType.BYTE, (byte) 1);
        pdc.set(tierKey, PersistentDataType.STRING, tier.name());
        pdc.set(lungeKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Whether the item is a Valor spear.
     *
     * @param item the item to test (may be {@code null})
     * @return {@code true} if tagged as a spear
     */
    public boolean isSpear(@Nullable ItemStack item) {
        return has(item, spearKey);
    }

    /**
     * Whether the item is a spear with the lunge ability.
     *
     * @param item the item to test (may be {@code null})
     * @return {@code true} if tagged with lunge
     */
    public boolean hasLunge(@Nullable ItemStack item) {
        return has(item, lungeKey);
    }

    private boolean has(@Nullable ItemStack item, NamespacedKey key) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }
}
