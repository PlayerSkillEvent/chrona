package dev.chrona.quest.action;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Abstraction over everything action are allowed to do.
 * You implement it and map it on:
 * - Flag system
 * - Dialogue system
 * - WorldState
 * - Items/Crates
 * - Bukkit-API (Messages, Titles, Sounds, Teleports, Commands)
 */
public interface ActionExecutionContext {

    /** Returns the player ID associated with this context.
     *
     * @return the player ID
     */
    UUID playerId();

    /** Returns the player associated with this context.
     *
     * @return the player, can be null
     */
    Player player(); // can be null, e.g. offline context

    /** Returns the value of a flag for a given player.
     *
     * @param playerId The unique identifier of the player.
     * @param key      The key of the flag.
     */
    void setFlag(UUID playerId, String key, boolean value);

    /** Starts a dialogue with an NPC for the player.
     *
     * @param player     The player to start the dialogue with.
     * @param npcId      The NPC identifier.
     * @param dialogueId The dialogue identifier.
     */
    void startDialogue(Player player, String npcId, String dialogueId);

    /** Teleports a player to a specified location.
     *
     * @param player   The player to teleport.
     * @param location The target location.
     */
    void teleport(Player player, Location location);

    /** Sends a chat message to a player.
     *
     * @param player  The player to send the message to.
     * @param message The message content.
     */
    void sendMessage(Player player, String message);

    /** Sends a title and subtitle to a player.
     *
     * @param player   The player to send the title to.
     * @param title    The title text.
     * @param subtitle The subtitle text.
     * @param fadeIn   The fade-in duration in millis.
     * @param stay     The stay duration in millis.
     * @param fadeOut  The fade-out duration in millis.
     */
    void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut);

    /** Plays a sound for a player.
     *
     * @param player   The player to play the sound for.
     * @param soundKey The sound identifier.
     * @param volume   The volume of the sound.
     * @param pitch    The pitch of the sound.
     */
    void playSound(Player player, String soundKey, float volume, float pitch);

    /** Executes a console command.
     *
     * @param command The command to execute.
     */
    void runConsoleCommand(String command);

    /** Gives an item to a player.
     *
     * @param player        The player to give the item to.
     * @param itemId        The item identifier.
     * @param amount        The amount of the item to give.
     * @param bindOnPickup  Whether the item should be bound on pickup.
     */
    void giveItem(Player player, String itemId, int amount, boolean bindOnPickup);

    /** Gives a crate to a player.
     *
     * @param player   The player to give the crate to.
     * @param crateId  The crate identifier.
     * @param amount   The amount of crates to give.
     */
    void giveCrate(Player player, String crateId, int amount);

    /** Grants a cosmetic to a player.
     *
     * @param player     The player to grant the cosmetic to.
     * @param cosmeticId The cosmetic identifier.
     * @param variant    The variant of the cosmetic.
     */
    void grantCosmetic(Player player, String cosmeticId, String variant);

    /** Sets a world state value.
     *
     * @param key            The key of the world state.
     * @param absoluteValue  The absolute value to set (can be null).
     * @param incrementBy    The value to increment by (can be null).
     */
    void setWorldState(String key, Integer absoluteValue, Integer incrementBy);

    /** Broadcasts a message to all players.
     *
     * @param message The message content.
     */
    void broadcast(String message);
}
