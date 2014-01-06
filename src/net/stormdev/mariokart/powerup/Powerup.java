package net.stormdev.mariokart.powerup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum Powerup {
        RED_SHELL(Arrays.asList("+Slows down the victim", "*Right click to deploy"), "redShell"),
        GREEN_SHELL(Arrays.asList("+Slows down the victim", "*Left click to throw forwards", "*Right click to throw backwards"), "greenShell"),
        BLUE_SHELL(Arrays.asList("+Targets and slows the leader", "*Right click to deploy"), "blueShell"),
        BANANA(Arrays.asList("+Slows players down", "*Right click to deploy"), "banana"),
        STAR(Arrays.asList("+Applies a large speed boost", "+Immunity to other powerups", "*Right click to use"), "star"),
        LIGHTNING(Arrays.asList("+Strikes all lightning on enemies", "*Right click to deploy"), "lightning"),
        BOMB(Arrays.asList("+Throws an ignited bomb", "*Right click to deploy"), "bomb"),
        POW(Arrays.asList("+Freezes other players", "*Right click to deploy"), "pow"),
        MUSHROOM(Arrays.asList("+Applies a short speed boost", "*Right click to use"), "mushroom"),
        BOO(Arrays.asList("+Invisible for 6s", "+Apply nausea to racer ahead", "*Right click to deploy"), "boo"),
        RANDOM(Arrays.asList("+Gives a random powerup", "*Right click to use"), "random");
        private String path;
        private List<String> lore = new ArrayList<String>();
        Powerup(List<String> lore, String path) {
                this.lore = lore;
                this.path = "mariokart." + path;
        }
        public String getPath() {
        	return path;
        }
        public List<String> getLore() {
        	return lore;
        }
}