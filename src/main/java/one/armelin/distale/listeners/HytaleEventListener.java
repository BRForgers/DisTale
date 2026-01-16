package one.armelin.distale.listeners;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.event.KillFeedEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import net.dv8tion.jda.api.utils.MarkdownSanitizer;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import one.armelin.distale.DisTale;
import one.armelin.distale.utils.MarkdownParser;
import one.armelin.distale.utils.Utils;

import java.util.Locale;

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
        eventRegistry.registerAsyncUnhandled(PlayerChatEvent.class, future -> future.thenApply(event  -> {
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

        String playerName = event.getSender().getUsername();
        String message = event.getContent();

        // Convert mentions
        Utils.Tuple<String, String> convertedPair = Utils.convertMentionsFromNames(message);
        String discordMessage = convertedPair.getFirst();
        String displayMessage = convertedPair.getSecond();

        // Send via webhook or regular message
        if (DisTale.config.isWebhookEnabled && !DisTale.webhookId.isEmpty()) {
            sendWebhookMessage(playerName, event.getSender().getUuid().toString(), discordMessage);
        } else {
            String formattedMessage = DisTale.config.texts.playerMessage
                    .replace("%playername%", playerName)
                    .replace("%playermessage%", discordMessage);

            DisTale.textChannel.sendMessage(formattedMessage).queue();
        }

        if(DisTale.config.modifyChatMessages){
            event.setCancelled(true);
            String gameMessage = playerName + ": " + displayMessage;
            Message gameMsg = MarkdownParser.parseMarkdown(gameMessage);
            DisTale.universe.sendMessage(gameMsg);
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
     *
     * @param event     Player death event (placeholder type)
     * @param playerRef
     */
    static void onPlayerDeath(int index, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store, CommandBuffer<EntityStore> buffer, KillFeedEvent.Display event) {
        DisTale.LOGGER.atInfo().log("Player death event received");

        Ref<EntityStore> victimRef = chunk.getReferenceTo(index);
        PlayerRef playerRef = store.getComponent(victimRef, PlayerRef.getComponentType());

        if (!DisTale.config.announceDeaths || DisTale.stop || DisTale.jda == null || playerRef == null) {
            return;
        }

//        // TODO: Extract player and death message from actual event


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

    /**
     * Send message via Discord webhook
     *
     * @param playerName Player's name
     * @param playerUuid Player's UUID
     * @param message Message content
     */
    public static void sendWebhookMessage(String playerName, String playerUuid, String message) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("username", playerName);

            // Use UUID or nickname for avatar
            //String avatarId = DisTale.config.useUUIDInsteadNickname ? playerUuid : playerName;
            //body.addProperty("avatar_url", "https://placehold.co/400?text=" + playerName);

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
            DisTale.LOGGER.atSevere().log("Failed to send webhook message", ex);
        }
    }
}
