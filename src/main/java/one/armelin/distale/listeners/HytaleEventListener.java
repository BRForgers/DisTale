package one.armelin.distale.listeners;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hypixel.hytale.builtin.adventure.memories.memories.npc.NPCMemory;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.event.EventPriority;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.PlayerSkin;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.event.KillFeedEvent;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.MessageUtil;
import net.dv8tion.jda.api.utils.MarkdownSanitizer;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import one.armelin.distale.DisTale;
import one.armelin.distale.utils.*;
import one.armelin.distale.utils.markdown.MarkdownConverter;

import java.util.Locale;
import java.util.UUID;

/**
 * Hytale event listener for DisTale
 * Listens to Hytale events and sends them to Discord
 */
public class HytaleEventListener {

    private static final MediaType JSON = MediaType.get("application/json");

    /**
     * Register all event listeners
     */
    public static void register(DisTale plugin, EventRegistry eventRegistry) {
        eventRegistry.register(PlayerConnectEvent.class, HytaleEventListener::onPlayerJoin);
        eventRegistry.registerAsyncGlobal(EventPriority.LAST ,PlayerChatEvent.class, future -> future.thenApply(event  -> {
            onPlayerChat(event);
            return event;
        }));
        eventRegistry.register(PlayerDisconnectEvent.class, HytaleEventListener::onPlayerLeave);

        DisTale.LOGGER.atInfo().log("Hytale event listeners registered");
    }

    /**
     * Handle player chat event
     * Sends player messages to Discord
     *
     * @param event Player chat event (placeholder type)
     */
    public static void onPlayerChat(PlayerChatEvent event) {
        if (DisTale.stop || DisTale.jda == null) {
            return;
        }

        PlayerRef ref = event.getSender();

        String playerName = ref.getUsername();
        String message = event.getContent();

        // Convert mentions
        Utils.Tuple<String, String> convertedPair = Utils.convertMentionsFromNames(message);
        String discordMessage = convertedPair.getFirst();
        String displayMessage = convertedPair.getSecond();

        // Send via webhook or regular message
        if (DisTale.config.isWebhookEnabled && !DisTale.webhookId.isEmpty()) {
            sendWebhookMessage(ref, discordMessage);
        } else {
            String formattedMessage = DisTale.config.texts.playerMessage
                    .replace("%playername%", playerName)
                    .replace("%playermessage%", discordMessage);

            DisTale.textChannel.sendMessage(formattedMessage).queue();
        }

        if(DisTale.config.modifyChatMessages){
            event.setCancelled(true);
            Message userPart = event.getFormatter().format(ref, "");
            Message contentPart = MarkdownConverter.toMessage(displayMessage.trim());
            Message gameMsg = Message.join(userPart, contentPart);

            DisTale.universe.sendMessage(gameMsg);
            HytaleLogger.getLogger().atInfo().log("[Modified by Distale] %s", MessageUtil.toAnsiString(gameMsg).toAnsi());
        }
    }

    /**
     * Handle player join event
     */
    private static void onPlayerJoin(PlayerConnectEvent event) {
        if (!DisTale.config.announcePlayers || DisTale.stop || DisTale.jda == null) {
            return;
        }

        String playerName = event.getPlayerRef().getUsername();

        if(DisTale.config.isWebhookEnabled && !DisTale.webhookId.isEmpty()){
            DisTale.LOGGER.atInfo().log("Fetching player skin in store for player: " + playerName);
            PlayerSkinComponent playerSkinComponent = event.getHolder().getComponent(PlayerSkinComponent.getComponentType());
            DisTale.playerSkinCache.put(event.getPlayerRef().getUuid(), playerSkinComponent.getPlayerSkin());
        }
        String message = DisTale.config.texts.joinServer
                .replace("%playername%", MarkdownSanitizer.escape(playerName));

        DisTale.textChannel.sendMessage(message).queue();
    }

    /**
     * Handle player leave event
     *
     * @param event Player leave event (placeholder type)
     */
    private static void onPlayerLeave(PlayerDisconnectEvent event) {
        if (!DisTale.config.announcePlayers || DisTale.stop || DisTale.jda == null) {
            return;
        }

        String playerName = event.getPlayerRef().getUsername();

        String message = DisTale.config.texts.leftServer
                .replace("%playername%", MarkdownSanitizer.escape(playerName));

        DisTale.textChannel.sendMessage(message).queue();
    }

    /**
     * Handle player death event
     */
    public static void onPlayerDeath(int index, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store, CommandBuffer<EntityStore> buffer, KillFeedEvent.Display event) {
        Ref<EntityStore> victimRef = chunk.getReferenceTo(index);
        PlayerRef playerRef = store.getComponent(victimRef, PlayerRef.getComponentType());

        if (!DisTale.config.announceDeaths || DisTale.stop || DisTale.jda == null || playerRef == null) {
            return;
        }

        String playerName = playerRef.getUsername();

        Damage.Source damageSource = event.getDamage().getSource();

        String deathMessage;

        if(damageSource instanceof Damage.EnvironmentSource){
            deathMessage = ((Damage.EnvironmentSource) damageSource).getType();
        }else if (damageSource instanceof Damage.EntitySource){
            deathMessage = buffer.getComponent(((Damage.EntitySource) damageSource).getRef(), DisplayNameComponent.getComponentType()).getDisplayName().getAnsiMessage();
        } else {
            DamageCause damageCauseAsset = (DamageCause)DamageCause.getAssetMap().getAsset(event.getDamage().getDamageCauseIndex());
            String causeId = damageCauseAsset != null ? damageCauseAsset.getId().toLowerCase(Locale.ROOT) : "unknown";
            Message damageCauseMessage = Message.translation("server.general.damageCauses." + causeId);
            deathMessage = damageCauseMessage.getAnsiMessage();
        }

        String message = DisTale.config.texts.deathMessage
                .replace("%playername%", MarkdownSanitizer.escape(playerName))
                .replace("%deathmessage%", MarkdownSanitizer.escape(deathMessage));

        DisTale.textChannel.sendMessage(message).queue();
    }

    /**
     * Handle player achievement event
     *
     * @param event Player achievement event (placeholder type)
     */
    private void onPlayerAchievement(Object event) {
        if (!DisTale.config.announceAdvancements || DisTale.stop || DisTale.jda == null) {
            return;
        }

        // TODO: Extract player and achievement from actual event
        String playerName = "Player";           // event.getPlayer().getName()
        String achievementName = "Achievement"; // event.getAchievement().getName()

        String message = DisTale.config.texts.achievement
                .replace("%playername%", MarkdownSanitizer.escape(playerName))
                .replace("%achievement%", MarkdownSanitizer.escape(achievementName));

        DisTale.textChannel.sendMessage(message).queue();
    }

    public static void onMemoryDiscovered(Player player, NPCMemory npcMemory) {
        if (!DisTale.config.announceMemories || DisTale.stop || DisTale.jda == null) {
            return;
        }

        String playerName = player.getDisplayName();
        String memoryName = Message.translation(npcMemory.getTitle()).getAnsiMessage();

        String message = DisTale.config.texts.memory
                .replace("%playername%", playerName)
                .replace("%memory%", memoryName);

        DisTale.textChannel.sendMessage(message).queue();
    }

    /**
     * Send message via Discord webhook
     *
     * @param playerRef Player
     * @param message Message content
     */
    public static void sendWebhookMessage(PlayerRef playerRef, String message) {
        try {
            String playerName = playerRef.getUsername();
            UUID playerUuid = playerRef.getUuid();

            JsonObject body = new JsonObject();
            body.addProperty("username", playerName);

            PlayerSkin playerSkin = DisTale.playerSkinCache.get(playerUuid);

            String avatarUrl = SkinUtils.buildAvatarUrl(DisTale.config.avatarUrl, playerSkin, playerUuid, playerName);

            body.addProperty("avatar_url", avatarUrl);

            // Configure allowed mentions
            JsonObject allowedMentions = new JsonObject();
            JsonArray parse = new JsonArray();
            parse.add("users");
            parse.add("roles");
            allowedMentions.add("parse", parse);
            body.add("allowed_mentions", allowedMentions);

            body.addProperty("content", message);

            // Send webhook request
            RequestBody requestBody = RequestBody.create(body.toString(), JSON);
            Request request = new Request.Builder()
                    .url(DisTale.config.webhookURL)
                    .post(requestBody)
                    .build();

            DisTale.webhookClient.newCall(request).execute().close();

        } catch (Exception ex) {
            DisTale.LOGGER.atSevere().withCause(ex).log("Failed to send webhook message");
        }
    }
}
