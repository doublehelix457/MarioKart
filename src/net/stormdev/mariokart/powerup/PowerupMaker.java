package net.stormdev.mariokart.powerup;

import net.stormdev.mariokart.MarioKart;

import org.bukkit.inventory.ItemStack;

import com.useful.ucars.ItemStackFromId;

public class PowerupMaker {
        public static PowerupData getPowerupRaw(Powerup powerup, int amount) {
                PowerupData toReturn = new PowerupData(powerup, ItemStackFromId.get(MarioKart.config.getString(powerup.getPath())));
                toReturn.raw.setAmount(amount);
                return toReturn;
        }
        public static ItemStack getPowerup(Powerup powerup, int amount) {
                PowerupData data = PowerupMaker.getPowerupRaw(powerup, amount);
                PowerupItem item = new PowerupItem(data);
                return item;
        }

}