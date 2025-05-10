package net.neevan.mobbattlesmod.command.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

public class MobBattlesData extends SavedData {

    private static final String DATA_NAME = "mob_battles_data";
    private final Map<ResourceLocation, List<ResourceLocation>> prioritizedTypeTargets = new HashMap<>();

    public void addTargetPriority(ResourceLocation attackerType, ResourceLocation targetType) {
        prioritizedTypeTargets.computeIfAbsent(attackerType, k -> new ArrayList<>());
        List<ResourceLocation> list = prioritizedTypeTargets.get(attackerType);
        if (!list.contains(targetType)) {
            list.add(targetType);
            setDirty();
        }
    }

    public void removeTargetPriority(ResourceLocation attackerType, ResourceLocation targetType) {
        List<ResourceLocation> list = prioritizedTypeTargets.get(attackerType);
        if (list != null && list.remove(targetType)) {
            if (list.isEmpty()) {
                prioritizedTypeTargets.remove(attackerType);
            }
            setDirty();
        }
    }

    public void clearTargetPriorities(ResourceLocation attackerType) {
        if (prioritizedTypeTargets.remove(attackerType) != null) {
            setDirty();
        }
    }

    public List<ResourceLocation> getPrioritizedTargets(ResourceLocation attackerType) {
        return prioritizedTypeTargets.getOrDefault(attackerType, Collections.emptyList());
    }

    public boolean hasPrioritizedTargets(ResourceLocation attackerType) {
        return prioritizedTypeTargets.containsKey(attackerType);
    }

    @Override
    public CompoundTag save(CompoundTag compoundTag) {
        ListTag entries = new ListTag();
        for (Map.Entry<ResourceLocation, List<ResourceLocation>> entry : prioritizedTypeTargets.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString("Attacker", entry.getKey().toString());

            ListTag targetList = new ListTag();
            for (ResourceLocation target : entry.getValue()) {
                targetList.add(StringTag.valueOf(target.toString()));
            }
            entryTag.put("Targets", targetList);
            entries.add(entryTag);
        }

        compoundTag.put("PrioritizedTargets", entries);
        return compoundTag;
    }

    public static MobBattlesData load(CompoundTag tag) {
        MobBattlesData data = new MobBattlesData();

        ListTag entries = tag.getList("PrioritizedTargets", Tag.TAG_COMPOUND);
        for (Tag raw : entries) {
            CompoundTag entryTag = (CompoundTag) raw;
            ResourceLocation attackerType = ResourceLocation.parse(entryTag.getString("Attacker"));

            ListTag targetList = entryTag.getList("Targets", Tag.TAG_STRING);
            List<ResourceLocation> targets = new ArrayList<>();
            for (Tag targetTag : targetList) {
                targets.add(ResourceLocation.parse(targetTag.getAsString()));
            }

            data.prioritizedTypeTargets.put(attackerType, targets);
        }

        return data;
    }

    public static MobBattlesData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                MobBattlesData::load,
                MobBattlesData::new,
                DATA_NAME
        );
    }
}
