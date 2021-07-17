package dev.rosewood.rosestacker.conversion.handler;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosestacker.conversion.ConversionData;
import dev.rosewood.rosestacker.stack.Stack;
import dev.rosewood.rosestacker.stack.StackType;
import dev.rosewood.rosestacker.stack.StackedEntity;
import dev.rosewood.rosestacker.utils.PersistentDataUtils;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class WildStackerEntityConversionHandler extends ConversionHandler {

    private static final NamespacedKey STACK_KEY = new NamespacedKey("wildstacker", "stackamount");

    public WildStackerEntityConversionHandler(RosePlugin rosePlugin) {
        super(rosePlugin, StackType.ENTITY, true);
    }

    @Override
    public Set<Stack<?>> handleConversion(Set<ConversionData> conversionData) {
        Set<LivingEntity> entities = conversionData.stream()
                .map(ConversionData::getEntity)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Stack<?>> stacks = new HashSet<>();

        for (LivingEntity entity : entities) {
            PersistentDataContainer dataContainer = entity.getPersistentDataContainer();
            if (dataContainer.has(PersistentDataUtils.CONVERTED_KEY, PersistentDataType.INTEGER))
                continue;

            int stackSize = dataContainer.getOrDefault(STACK_KEY, PersistentDataType.INTEGER, -1);
            if (stackSize == -1)
                continue;

            dataContainer.set(PersistentDataUtils.CONVERTED_KEY, PersistentDataType.INTEGER, 1);
            StackedEntity stackedEntity = new StackedEntity(entity, this.createEntityStackNBT(entity.getType(), stackSize, entity.getLocation()));
            this.stackManager.addEntityStack(stackedEntity);
            stacks.add(stackedEntity);
        }

        return stacks;
    }

}
