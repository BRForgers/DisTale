package one.armelin.distale.listeners.systems;

import com.hypixel.hytale.builtin.adventure.memories.MemoriesPlugin;
import com.hypixel.hytale.builtin.adventure.memories.component.PlayerMemories;
import com.hypixel.hytale.builtin.adventure.memories.memories.npc.NPCMemory;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import it.unimi.dsi.fastutil.objects.ObjectList;
import one.armelin.distale.listeners.HytaleEventListener;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Set;

public class BeforeGatherMemoriesSystem extends EntityTickingSystem<EntityStore> {

    public static final Query<EntityStore> QUERY = Query.and(
            TransformComponent.getComponentType(),
            Player.getComponentType(),
            PlayerMemories.getComponentType()
    );

    private final double radius;

    public BeforeGatherMemoriesSystem() {
        this.radius = 10.0;
    }

    @Override
    public void tick(
            float dt,
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        Player playerComponent = archetypeChunk.getComponent(index, Player.getComponentType());
        if (playerComponent == null) return;

        if (playerComponent.getGameMode() != GameMode.Adventure) return;

        TransformComponent transformComponent =
                archetypeChunk.getComponent(index, TransformComponent.getComponentType());
        if (transformComponent == null) return;

        Vector3d position = transformComponent.getPosition();

        SpatialResource<Ref<EntityStore>, EntityStore> npcSpatialResource =
                store.getResource(NPCPlugin.get().getNpcSpatialResource());

        ObjectList<Ref<EntityStore>> results = SpatialResource.getThreadLocalReferenceList();
        results.clear();

        npcSpatialResource.getSpatialStructure().collect(position, radius, results);

        if (results.isEmpty()) return;

        PlayerMemories playerMemories =
                archetypeChunk.getComponent(index, PlayerMemories.getComponentType());
        if (playerMemories == null) return;

        for (Ref<EntityStore> npcRef : results) {
            NPCEntity npc = commandBuffer.getComponent(npcRef, NPCEntity.getComponentType());
            if (npc == null) continue;

            Role role = npc.getRole();

            if (role == null || !role.isMemory()) continue;

            String npcRole = role.isMemoriesNameOverriden()
                    ? role.getMemoriesNameOverride()
                    : npc.getRoleName();

            String titleKey = role.getNameTranslationKey();
            boolean overridden = role.isMemoriesNameOverriden();

            NPCMemory temp = new NPCMemory(npcRole, titleKey, overridden);
            if (!MemoriesPlugin.get().hasRecordedMemory(temp)) {
                if (!playerMemories.getRecordedMemories().contains(temp)) {
                    HytaleEventListener.onMemoryDiscovered(playerComponent, temp);
                }
            }
        }
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return QUERY;
    }

    @NotNull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return Set.of(new SystemDependency<>(Order.BEFORE, NPCMemory.GatherMemoriesSystem.class));
    }
}


