package net.stormdev.mariokart.powerup;

import net.stormdev.mariokart.MarioKart;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class PowerupItem extends ItemStack {
    public PowerupItem(PowerupData powerupRaw) {
        super(powerupRaw.raw);
        this.setDurability(powerupRaw.raw.getDurability());
        this.setAmount(powerupRaw.raw.getAmount());
        Powerup powerup = powerupRaw.powerup;
        String pow = powerup.toString().toLowerCase();
        if (pow.length() > 1) {
            String body = pow.substring(1);
            String start = pow.substring(0, 1);
            pow = start.toUpperCase() + body;
        }
        ItemMeta meta = this.getItemMeta();
        meta.setDisplayName(MarioKart.colors.getInfo() + pow);
        meta.setLore(powerup.getLore());
        this.setItemMeta(meta);
    }
}