package dev.chrona.quest.condition;

import dev.chrona.quest.state.QuestRunState;

import java.time.LocalTime;
import java.util.UUID;

/**
 * Context for evaluating conditions related to a player's state.
 * Provides methods to check flags, quest states, job levels, housing levels,
 * rank, region visits, world states, event activity, and time ranges.
 */
public interface ConditionContext {

    UUID playerId();

    // FLAGS
    boolean flagEquals(String key, boolean expected);

    // QUEST STATE
    QuestRunState questState(String questId); // UNKNOWN -> null

    // JOBS
    int jobLevel(String jobId);
    boolean isJobActive(String jobId);

    // HOUSING
    int housingLevel();

    // RANK
    /**
     * Gibt den Rang-Wert des Spielers zurück.
     * Z.B. für "MEMBER" = 1, "VIP" = 2, "MVP" = 3, "ADMIN" = 100, etc.
     */
    int rankValue();

    // REGIONS
    boolean hasVisitedRegion(String regionId);
    boolean isInsideRegion(String regionId);

    // WORLD STATE (integer-based world state, z.B. megaproject phase)
    int worldStateInt(String key, int defaultValue);

    // EVENTS
    boolean isEventActive(String eventId);

    // TIME RANGE
    /**
     * Prüft, ob die aktuelle Uhrzeit innerhalb des angegebenen Bereichs liegt.
     * @param startHour Startstunde (0-23)
     * @param endHour Endstunde (0-23)
     * @return true, wenn die aktuelle Uhrzeit im Bereich liegt
     */
    default boolean isTimeInRange(int startHour, int endHour) {
        int now = LocalTime.now().getHour();
        if (startHour == endHour)
            return true; // 0-24: always
        if (startHour < endHour)
            return now >= startHour && now < endHour;
        // Wrap over midnight (z.B. 22-4)
        return now >= startHour || now < endHour;
    }
}
