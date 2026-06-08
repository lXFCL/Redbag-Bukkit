package com.codex.minecraft.redbag;

import java.util.UUID;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

final class RedbagGuiHolder implements InventoryHolder {
    private final UUID playerId;
    private Inventory inventory;

    RedbagGuiHolder(UUID playerId) {
        this.playerId = playerId;
    }

    UUID getPlayerId() {
        return playerId;
    }

    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
