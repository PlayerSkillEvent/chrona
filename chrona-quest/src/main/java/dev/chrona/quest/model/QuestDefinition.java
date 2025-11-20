package dev.chrona.quest.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Definition of a quest, including its metadata, objectives, requirements, and rewards.
 */
public final class QuestDefinition {

    private final String id;
    private final int version;

    private final QuestType type;
    private final QuestRepeatability repeatability;

    private final String arc;
    private final Integer chapter;
    private final String category;

    private final String title;
    private final String shortTitle;
    private final String description;

    // UI
    private final boolean showInLog;
    private final int sortOrder;
    private final String icon;

    // Flow
    private final QuestFlowMode flowMode;
    private final boolean autoStartOnAccept;
    private final boolean autoCompleteOnLastObjective;

    private final QuestRequirements requirements;
    private final List<ObjectiveDef> objectives;
    private final RewardDef rewards;
    private final QuestTiming timing;

    public QuestDefinition(
            String id,
            int version,
            QuestType type,
            QuestRepeatability repeatability,
            String arc,
            Integer chapter,
            String category,
            String title,
            String shortTitle,
            String description,
            boolean showInLog,
            int sortOrder,
            String icon,
            QuestFlowMode flowMode,
            boolean autoStartOnAccept,
            boolean autoCompleteOnLastObjective,
            QuestRequirements requirements,
            List<ObjectiveDef> objectives,
            RewardDef rewards,
            QuestTiming timing
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.version = version;
        this.type = Objects.requireNonNull(type, "type");
        this.repeatability = Objects.requireNonNull(repeatability, "repeatability");
        this.arc = arc;
        this.chapter = chapter;
        this.category = category;
        this.title = Objects.requireNonNullElse(title, id);
        this.shortTitle = shortTitle != null ? shortTitle : this.title;
        this.description = description;
        this.showInLog = showInLog;
        this.sortOrder = sortOrder;
        this.icon = icon != null ? icon : "PAPER";
        this.flowMode = flowMode != null ? flowMode : QuestFlowMode.SEQUENTIAL;
        this.autoStartOnAccept = autoStartOnAccept;
        this.autoCompleteOnLastObjective = autoCompleteOnLastObjective;
        this.requirements = requirements != null ? requirements : new QuestRequirements(null);
        this.objectives = objectives != null ? List.copyOf(objectives) : List.of();
        this.rewards = rewards;
        this.timing = timing;
    }

    /** Returns the unique identifier of the quest.
     *
     * @return the quest ID
     */
    public String id() {
        return id;
    }

    /** Returns the version number of the quest definition.
     *
     * @return the quest version
     */
    public int version() {
        return version;
    }

    /** Returns the type of the quest.
     *
     * @return the quest type
     */
    public QuestType type() {
        return type;
    }

    /** Returns the repeatability of the quest.
     *
     * @return the quest repeatability
     */
    public QuestRepeatability repeatability() {
        return repeatability;
    }

    /** Returns the arc of the quest.
     *
     * @return the quest arc
     */
    public String arc() {
        return arc;
    }

    /** Returns the chapter number of the quest.
     *
     * @return the quest chapter
     */
    public Integer chapter() {
        return chapter;
    }

    /** Returns the category of the quest.
     *
     * @return the quest category
     */
    public String category() {
        return category;
    }

    /** Returns the title of the quest.
     *
     * @return the quest title
     */
    public String title() {
        return title;
    }

    /** Returns the short title of the quest.
     *
     * @return the quest short title
     */
    public String shortTitle() {
        return shortTitle;
    }

    /** Returns the description of the quest.
     *
     * @return the quest description
     */
    public String description() {
        return description;
    }

    /** Indicates whether the quest should be shown in the quest log.
     *
     * @return true if shown in log, false otherwise
     */
    public boolean showInLog() {
        return showInLog;
    }

    /** Returns the sort order of the quest in the quest log.
     *
     * @return the quest sort order
     */
    public int sortOrder() {
        return sortOrder;
    }

    /** Returns the icon associated with the quest.
     *
     * @return the quest icon
     */
    public String icon() {
        return icon;
    }

    /** Returns the flow mode of the quest.
     *
     * @return the quest flow mode
     */
    public QuestFlowMode flowMode() {
        return flowMode;
    }

    /** Indicates whether the quest should auto-start upon acceptance.
     *
     * @return true if auto-starts on accept, false otherwise
     */
    public boolean autoStartOnAccept() {
        return autoStartOnAccept;
    }

    /** Indicates whether the quest should auto-complete upon completing the last objective.
     *
     * @return true if auto-completes on last objective, false otherwise
     */
    public boolean autoCompleteOnLastObjective() {
        return autoCompleteOnLastObjective;
    }

    /** Returns the requirements for the quest.
     *
     * @return the quest requirements
     */
    public QuestRequirements requirements() {
        return requirements;
    }

    /** Returns an unmodifiable list of objectives for the quest.
     *
     * @return the list of quest objectives
     */
    public List<ObjectiveDef> objectives() {
        return objectives;
    }

    /** Returns the rewards for the quest.
     *
     * @return the quest rewards
     */
    public RewardDef rewards() {
        return rewards;
    }

    /** Returns the timing information for the quest.
     *
     * @return the quest timing
     */
    public QuestTiming timing() {
        return timing;
    }
}
