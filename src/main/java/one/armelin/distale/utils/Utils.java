package one.armelin.distale.utils;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import one.armelin.distale.DisTale;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for DisTale
 * Handles text sanitization and Discord mention conversions
 */
public class Utils {

    /**
     * Sanitize text for display
     * Removes or replaces special characters that could cause formatting issues
     *
     * @param text Text to sanitize
     * @param sanitizeLineBreak Whether to remove line breaks
     * @return Sanitized text
     */
    public static String sanitize(String text, boolean sanitizeLineBreak) {
        if (DisTale.config.texts.removeVanillaFormattingFromDiscord) {
            text = text.replace("§", "&");
        }

        if (sanitizeLineBreak && DisTale.config.texts.removeLineBreakFromDiscord) {
            text = text.replace("\n", " ");
        }

        return text;
    }

    /**
     * Convert Discord mentions to formatted text and vice versa
     * Converts @username mentions to <@id> format and back
     *
     * @param message Message to convert
     * @return Tuple of (Discord format, Display format)
     */
    public static Tuple<String, String> convertMentionsFromNames(String message) {
        String discordMessage = message;
        String displayMessage = message;

        if (!DisTale.config.membersIntents || DisTale.jda == null) {
            return new Tuple<>(discordMessage, displayMessage);
        }

        try {
            // Get all guild members
            if (DisTale.textChannel.getType().isGuild()) {
                var guildChannel = (net.dv8tion.jda.api.entities.channel.concrete.TextChannel) DisTale.textChannel;
                var guild = guildChannel.getGuild();
                List<Member> members = guild.getMembers();

                // Pattern to match @username or @"username with spaces"
                Pattern mentionPattern = Pattern.compile("@(?:\"([^\"]+)\"|([^\\s]+))");
                Matcher matcher = mentionPattern.matcher(message);

                while (matcher.find()) {
                    String username = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);

                    // Find member by nickname or effective name
                    for (Member member : members) {
                        if (member.getEffectiveName().equalsIgnoreCase(username) ||
                            member.getUser().getName().equalsIgnoreCase(username)) {

                            // Replace in Discord message with mention format
                            String fullMatch = matcher.group(0);
                            discordMessage = discordMessage.replace(fullMatch, "<@" + member.getId() + ">");

                            // Keep @ mention in display message
                            break;
                        }
                    }
                }

                // Handle role mentions
                Pattern roleMentionPattern = Pattern.compile("@&(?:\"([^\"]+)\"|([^\\s]+))");
                Matcher roleMatcher = roleMentionPattern.matcher(message);

                while (roleMatcher.find()) {
                    String roleName = roleMatcher.group(1) != null ? roleMatcher.group(1) : roleMatcher.group(2);

                    // Find role by name
                    for (Role role : guild.getRoles()) {
                        if (role.getName().equalsIgnoreCase(roleName)) {
                            String fullMatch = roleMatcher.group(0);
                            discordMessage = discordMessage.replace(fullMatch, "<@&" + role.getId() + ">");
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            DisTale.LOGGER.atSevere().log("Error converting mentions" + e.getMessage());
        }

        return new Tuple<>(discordMessage, displayMessage);
    }

    /**
     * Simple Tuple class to return two values
     */
    public static class Tuple<A, B> {
        private final A first;
        private final B second;

        public Tuple(A first, B second) {
            this.first = first;
            this.second = second;
        }

        public A getFirst() {
            return first;
        }

        public B getSecond() {
            return second;
        }
    }

    /**
     * Get player name from a player object
     * (Placeholder - will be implemented with actual Hytale API)
     *
     * @param player Player object
     * @return Player name
     */
    public static String getPlayerName(Object player) {
        // TODO: Implement with actual Hytale player API
        return player.toString();
    }

    /**
     * Get player UUID from a player object
     * (Placeholder - will be implemented with actual Hytale API)
     *
     * @param player Player object
     * @return Player UUID
     */
    public static String getPlayerUUID(Object player) {
        // TODO: Implement with actual Hytale player API
        return "";
    }
}
