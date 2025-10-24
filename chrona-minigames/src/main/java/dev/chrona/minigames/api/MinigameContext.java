package dev.chrona.minigames.api;

import org.bukkit.entity.Player;

import java.awt.*;
import java.util.Map;

public record MinigameContext(Player player, Component title, Map<String,Object> data) {}

