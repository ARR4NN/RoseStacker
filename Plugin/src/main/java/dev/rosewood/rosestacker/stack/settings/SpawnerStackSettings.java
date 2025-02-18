package dev.rosewood.rosestacker.stack.settings;

import dev.rosewood.rosegarden.config.CommentedFileConfiguration;
import dev.rosewood.rosestacker.RoseStacker;
import dev.rosewood.rosestacker.manager.ConfigurationManager.Setting;
import dev.rosewood.rosestacker.manager.StackSettingManager;
import dev.rosewood.rosestacker.nms.spawner.SpawnerType;
import dev.rosewood.rosestacker.stack.settings.conditions.spawner.ConditionTag;
import dev.rosewood.rosestacker.stack.settings.conditions.spawner.ConditionTags;
import dev.rosewood.rosestacker.utils.StackerUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

public class SpawnerStackSettings extends StackSettings {

    private final SpawnerType spawnerType;
    private final boolean enabled;
    private final String displayName;
    private final int maxStackSize;
    private final Boolean disableMobAI;
    private final int spawnCountStackSizeMultiplier;
    private final int minSpawnDelay;
    private final int maxSpawnDelay;
    private final int entitySearchRange;
    private final int playerActivationRange;
    private final int spawnRange;
    private final List<ConditionTag> spawnRequirements;

    public SpawnerStackSettings(CommentedFileConfiguration settingsConfiguration, SpawnerType spawnerType) {
        super(settingsConfiguration);
        this.spawnerType = spawnerType;

        this.setDefaults();

        this.enabled = this.settingsConfiguration.getBoolean("enabled");
        this.displayName = this.settingsConfiguration.getString("display-name");

        if (!this.spawnerType.isEmpty()) {
            this.maxStackSize = this.settingsConfiguration.getInt("max-stack-size");
            this.disableMobAI = this.settingsConfiguration.getDefaultedBoolean("disable-mob-ai");
            this.spawnCountStackSizeMultiplier = this.settingsConfiguration.getInt("spawn-count-stack-size-multiplier");
            this.minSpawnDelay = this.settingsConfiguration.getInt("spawn-delay-minimum");
            this.maxSpawnDelay = this.settingsConfiguration.getInt("spawn-delay-maximum");
            this.entitySearchRange = this.settingsConfiguration.getInt("entity-search-range");
            this.playerActivationRange = this.settingsConfiguration.getInt("player-activation-range");
            this.spawnRange = this.settingsConfiguration.getInt("spawn-range");

            this.spawnRequirements = new ArrayList<>();

            List<String> requirementStrings = this.settingsConfiguration.getStringList("spawn-requirements");
            for (String requirement : requirementStrings) {
                try {
                    this.spawnRequirements.add(ConditionTags.parse(requirement));
                } catch (Exception e) {
                    RoseStacker.getInstance().getLogger().warning(String.format("Invalid Spawner Requirement Tag: %s", requirement));
                }
            }

            if (Setting.SPAWNER_DONT_SPAWN_INTO_BLOCKS.getBoolean() && (requirementStrings.stream().noneMatch(x -> x.startsWith("fluid") || x.startsWith("air"))))
                this.spawnRequirements.add(ConditionTags.parse("air")); // All entities that don't require fluids will require air

            if (requirementStrings.stream().noneMatch(x -> x.startsWith("max-nearby-entities")))
                this.spawnRequirements.add(ConditionTags.parse("max-nearby-entities:" + Setting.SPAWNER_SPAWN_MAX_NEARBY_ENTITIES.getInt()));
        } else {
            this.maxStackSize = -1;
            this.disableMobAI = null;
            this.spawnCountStackSizeMultiplier = -1;
            this.minSpawnDelay = -1;
            this.maxSpawnDelay = -1;
            this.entitySearchRange = -1;
            this.playerActivationRange = -1;
            this.spawnRange = -1;
            this.spawnRequirements = List.of();
        }
    }

    @Override
    protected void setDefaults() {
        super.setDefaults();

        this.setIfNotExists("enabled", true);
        this.setIfNotExists("display-name", StackerUtils.formatName(this.spawnerType.get().map(EntityType::name).orElse("Empty") + '_' + Material.SPAWNER.name()));
        this.setIfNotExists("max-stack-size", -1);

        this.spawnerType.get().ifPresent(entityType -> {
            this.setIfNotExists("disable-mob-ai", "default");
            this.setIfNotExists("spawn-count-stack-size-multiplier", -1);
            this.setIfNotExists("spawn-delay-minimum", -1);
            this.setIfNotExists("spawn-delay-maximum", -1);
            this.setIfNotExists("entity-search-range", -1);
            this.setIfNotExists("player-activation-range", -1);
            this.setIfNotExists("spawn-range", -1);

            EntityStackSettings entityStackSettings = RoseStacker.getInstance().getManager(StackSettingManager.class).getEntityStackSettings(entityType);
            List<String> defaultSpawnRequirements = new ArrayList<>(entityStackSettings.getEntityTypeData().defaultSpawnRequirements());
            this.setIfNotExists("spawn-requirements", defaultSpawnRequirements);
        });
    }

    @Override
    protected String getConfigurationSectionKey() {
        return this.spawnerType.getEnumName();
    }

    @Override
    public boolean isStackingEnabled() {
        return this.enabled;
    }

    @Override
    public String getDisplayName() {
        return this.displayName;
    }

    @Override
    public int getMaxStackSize() {
        if (this.maxStackSize != -1)
            return this.maxStackSize;
        return Setting.SPAWNER_MAX_STACK_SIZE.getInt();
    }

    public SpawnerType getSpawnerType() {
        return this.spawnerType;
    }

    public boolean isMobAIDisabled() {
        if (this.disableMobAI != null)
            return this.disableMobAI;
        return Setting.SPAWNER_DISABLE_MOB_AI.getBoolean();
    }

    public List<ConditionTag> getSpawnRequirements() {
        return this.spawnRequirements;
    }

    public int getSpawnCountStackSizeMultiplier() {
        if (this.spawnCountStackSizeMultiplier != -1)
            return Math.max(this.spawnCountStackSizeMultiplier, 1);
        int configValue = Setting.SPAWNER_SPAWN_COUNT_STACK_SIZE_MULTIPLIER.getInt();
        if (configValue != -1)
            configValue = Math.max(configValue, 1);
        return configValue;
    }

    public int getMinSpawnDelay() {
        if (this.minSpawnDelay != -1)
            return Math.max(this.minSpawnDelay, 5);
        return this.maxIfNotNegativeOne(Setting.SPAWNER_SPAWN_DELAY_MINIMUM.getInt(), 5);
    }

    public int getMaxSpawnDelay() {
        if (this.maxSpawnDelay != -1)
            return Math.max(this.maxSpawnDelay, this.getMinSpawnDelay());
        return this.maxIfNotNegativeOne(Setting.SPAWNER_SPAWN_DELAY_MAXIMUM.getInt(), this.getMinSpawnDelay());
    }

    public int getEntitySearchRange() {
        if (this.entitySearchRange != -1)
            return this.maxIfNotNegativeOne(this.entitySearchRange, 1);

        int globalRange = Setting.SPAWNER_SPAWN_ENTITY_SEARCH_RANGE.getInt();
        if (globalRange == -1)
            return this.getSpawnRange();
        return this.maxIfNotNegativeOne(globalRange, 1);
    }

    public int getPlayerActivationRange() {
        if (this.hasUnlimitedPlayerActivationRange())
            return Integer.MAX_VALUE;
        if (this.playerActivationRange != -1)
            return Math.max(this.playerActivationRange, 1);
        return this.maxIfNotNegativeOne(Setting.SPAWNER_SPAWN_PLAYER_ACTIVATION_RANGE.getInt(), 1);
    }

    public boolean hasUnlimitedPlayerActivationRange() {
        return this.playerActivationRange == -2 || Setting.SPAWNER_SPAWN_PLAYER_ACTIVATION_RANGE.getInt() == -2;
    }

    public int getSpawnRange() {
        if (this.spawnRange != -1)
            return Math.max(this.spawnRange, 1);
        return this.maxIfNotNegativeOne(Setting.SPAWNER_SPAWN_RANGE.getInt(), 1);
    }

    private int maxIfNotNegativeOne(int value, int max) {
        if (value == -1)
            return value;
        return Math.max(value, max);
    }

}
