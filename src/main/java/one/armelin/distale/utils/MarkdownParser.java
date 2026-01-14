package one.armelin.distale.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Markdown parser for DisTale
 * Converts Discord markdown formatting to game-compatible formatting
 */
public class MarkdownParser {

    // Markdown patterns
    private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*(.+?)\\*\\*");
    private static final Pattern ITALIC_PATTERN = Pattern.compile("\\*(.+?)\\*|_(.+?)_");
    private static final Pattern UNDERLINE_PATTERN = Pattern.compile("__(.+?)__");
    private static final Pattern STRIKETHROUGH_PATTERN = Pattern.compile("~~(.+?)~~");
    private static final Pattern CODE_PATTERN = Pattern.compile("`(.+?)`");
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```(?:[a-z]+\\n)?(.+?)```", Pattern.DOTALL);

    /**
     * Parse Discord markdown and convert to formatted text
     * Uses § codes for Minecraft-like formatting
     *
     * @param text Text with Discord markdown
     * @return Formatted text
     */
    public static String parseMarkdown(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // Parse code blocks first (they should ignore other formatting)
        text = parseCodeBlocks(text);

        // Parse inline code
        text = parseInlineCode(text);

        // Parse bold (before italic to handle ** before *)
        text = parseBold(text);

        // Parse italic
        text = parseItalic(text);

        // Parse underline
        text = parseUnderline(text);

        // Parse strikethrough
        text = parseStrikethrough(text);

        return text;
    }

    /**
     * Parse bold markdown (**text**)
     */
    private static String parseBold(String text) {
        Matcher matcher = BOLD_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String content = matcher.group(1);
            // §l = bold in Minecraft formatting
            matcher.appendReplacement(result, "§l" + content + "§r");
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Parse italic markdown (*text* or _text_)
     */
    private static String parseItalic(String text) {
        Matcher matcher = ITALIC_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String content = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            // §o = italic in Minecraft formatting
            matcher.appendReplacement(result, "§o" + content + "§r");
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Parse underline markdown (__text__)
     */
    private static String parseUnderline(String text) {
        Matcher matcher = UNDERLINE_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String content = matcher.group(1);
            // §n = underline in Minecraft formatting
            matcher.appendReplacement(result, "§n" + content + "§r");
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Parse strikethrough markdown (~~text~~)
     */
    private static String parseStrikethrough(String text) {
        Matcher matcher = STRIKETHROUGH_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String content = matcher.group(1);
            // §m = strikethrough in Minecraft formatting
            matcher.appendReplacement(result, "§m" + content + "§r");
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Parse inline code markdown (`code`)
     */
    private static String parseInlineCode(String text) {
        Matcher matcher = CODE_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String content = matcher.group(1);
            // §7 = gray, §f = white for code formatting
            matcher.appendReplacement(result, "§7" + content + "§r");
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Parse code block markdown (```code```)
     */
    private static String parseCodeBlocks(String text) {
        Matcher matcher = CODE_BLOCK_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String content = matcher.group(1);
            // Replace code blocks with gray text, preserve line breaks
            matcher.appendReplacement(result, "§7" + content.trim() + "§r");
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Remove all markdown formatting from text
     * Useful for sanitizing messages
     *
     * @param text Text with markdown
     * @return Plain text
     */
    public static String stripMarkdown(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // Remove all markdown patterns
        text = text.replaceAll("\\*\\*(.+?)\\*\\*", "$1");  // Bold
        text = text.replaceAll("\\*(.+?)\\*", "$1");         // Italic
        text = text.replaceAll("_(.+?)_", "$1");              // Italic (underscore)
        text = text.replaceAll("__(.+?)__", "$1");            // Underline
        text = text.replaceAll("~~(.+?)~~", "$1");            // Strikethrough
        text = text.replaceAll("`(.+?)`", "$1");              // Inline code
        text = text.replaceAll("```(?:[a-z]+\\n)?(.+?)```", "$1"); // Code blocks

        return text;
    }
}
