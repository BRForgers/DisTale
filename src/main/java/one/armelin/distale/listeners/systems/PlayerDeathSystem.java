package one.armelin.distale.listeners.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.event.KillFeedEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import one.armelin.distale.listeners.HytaleEventListener;
import org.jetbrains.annotations.Nullable;

public class PlayerDeathSystem extends EntityEventSystem<EntityStore, KillFeedEvent.Display> {

    public PlayerDeathSystem() {
        super(KillFeedEvent.Display.class);
    }

    @Override
    public void handle(int index, ArchetypeChunk<EntityStore> chunk,
                       Store<EntityStore> store, CommandBuffer<EntityStore> buffer,
                       KillFeedEvent.Display event) {
        HytaleEventListener.onPlayerDeath(index, chunk, store, buffer, event);
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }
}
