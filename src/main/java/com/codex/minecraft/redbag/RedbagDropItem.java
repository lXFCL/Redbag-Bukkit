package com.codex.minecraft.redbag;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

final class RedbagDropItem {
    private final Material material;
    private final String materialName;
    private final String displayName;
    private final int slot;

    RedbagDropItem(Material material, String materialName, String displayName, int slot) {
        this.material = material;
        this.materialName = materialName;
        this.displayName = displayName;
        this.slot = slot;
    }

    Material getMaterial() {
        return material;
    }

    String getMaterialName() {
        return materialName;
    }

    String getDisplayName() {
        return displayName;
    }

    int getSlot() {
        return slot;
    }

    static RedbagDropItem fromConfig(String key, ConfigurationSection section, int fallbackSlot) {
        String materialName = section.getString("material", key).trim().toUpperCase();
        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            material = Material.matchMaterial(key);
            materialName = key.trim().toUpperCase();
        }
        if (material == null) {
            return null;
        }
        String displayName = section.getString("name", material.name());
        int slot = section.getInt("slot", fallbackSlot);
        return new RedbagDropItem(material, material.name(), displayName, slot);
    }
}
