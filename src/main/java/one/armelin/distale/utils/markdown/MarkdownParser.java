package one.armelin.distale.utils.markdown;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.protocol.MaybeBool;
import one.armelin.distale.DisTale;

import java.awt.Color;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Advanced Markdown parser for DisTale
 * Converts Discord markdown formatting to Hytale Message objects
 *
 * Supported formats:
 * - **bold**
 * - __underline__
 * - *italic* or _italic_
 * - `code`
 * - Nested combinations (e.g. **bold and __underlined__**)
 */
public class MarkdownParser {

    // ========== Configuration ==========

    private static final int MAX_RECURSION_DEPTH = 10;

    // ========== Markdown Patterns ==========

    private static final Pattern CODE_PATTERN = Pattern.compile("`(.+?)`");
    private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*(.+?)\\*\\*");
    private static final Pattern UNDERLINE_PATTERN = Pattern.compile("__(.+?)__");

    // Italic fix: Prevents matching math equations like "3 * 4" by enforcing boundary rules
    private static final Pattern ITALIC_ASTERISK_PATTERN = Pattern.compile("\\*(?!\\s)(.+?)(?<!\\s)\\*");
    private static final Pattern ITALIC_UNDERSCORE_PATTERN = Pattern.compile("(?<!_)_(?!\\s)([^_]+?)(?<!\\s)_(?!_)");

    private static final Pattern ESCAPE_PATTERN = Pattern.compile("\\\\([*_`\\\\])");

    // ========== Placeholders ==========

    // Shortened magic string for performance
    private static final String MAGIC = "§MD" + UUID.randomUUID().toString().substring(0, 5);

    private static final String ESCAPE_PREFIX = MAGIC + "E[";     private static final String ESCAPE_SUFFIX = "]E" + MAGIC;
    private static final String CODE_PREFIX = MAGIC + "C[";       private static final String CODE_SUFFIX = "]C" + MAGIC;
    private static final String BOLD_PREFIX = MAGIC + "B[";       private static final String BOLD_SUFFIX = "]B" + MAGIC;
    private static final String UNDERLINE_PREFIX = MAGIC + "U[";  private static final String UNDERLINE_SUFFIX = "]U" + MAGIC;
    private static final String ITALIC_PREFIX = MAGIC + "I[";     private static final String ITALIC_SUFFIX = "]I" + MAGIC;
    private static final String SHRUG_PLACEHOLDER = MAGIC + "SHRUG";

    // ========== Storage for extracted content ==========

    private static class ParseContext {
        final List<String> escapes = new ArrayList<>();
        final List<String> codeBlocks = new ArrayList<>();
        final List<String> boldBlocks = new ArrayList<>();
        final List<String> underlineBlocks = new ArrayList<>();
        final List<String> italicBlocks = new ArrayList<>();
        final Color defaultColor;
        int recursionDepth = 0;

        ParseContext(Color defaultColor) {
            this.defaultColor = defaultColor;
        }
    }

    // ========== Public API ==========

    public static Message parseMarkdown(String text) {
        return parseMarkdown(text, null);
    }

    public static Message parseMarkdown(String text, Color defaultColor) {
        if (text == null || text.isEmpty()) {
            return Message.empty();
        }

        ParseContext context = new ParseContext(defaultColor);

        try {
            String processed = preprocessText(text, context);
            Message result = Message.raw("");
            restorePlaceholders(processed, result, context);
            return result;

        } catch (Exception e) {
            DisTale.LOGGER.atSevere().withCause(e).log("[MarkdownParser] Parse error");
            return Message.raw(sanitizeText(text));
        }
    }

    public static String stripMarkdown(String text) {
        if (text == null || text.isEmpty()) return text;

        try {
            text = text.replace("¯\\_(ツ)_/¯", SHRUG_PLACEHOLDER);
            text = ESCAPE_PATTERN.matcher(text).replaceAll("$1");

            text = CODE_PATTERN.matcher(text).replaceAll("$1");
            text = BOLD_PATTERN.matcher(text).replaceAll("$1");
            text = UNDERLINE_PATTERN.matcher(text).replaceAll("$1");
            text = ITALIC_ASTERISK_PATTERN.matcher(text).replaceAll("$1");
            text = ITALIC_UNDERSCORE_PATTERN.matcher(text).replaceAll("$1");

            text = text.replace(SHRUG_PLACEHOLDER, "¯\\_(ツ)_/¯");
            return text;
        } catch (Exception e) {
            DisTale.LOGGER.atSevere().withCause(e).log("[MarkdownParser] Strip markdown error");
            return text;
        }
    }

    // ========== Preprocessing ==========

    private static String preprocessText(String text, ParseContext context) {
        text = protectSpecialSequences(text, context);
        text = extractEscapes(text, context);

        // 1. Extract Code (Highest priority, no nesting inside)
        text = extractCode(text, context);

        // 2. Extract Bold (Can contain Underline, Italic, Code)
        text = extractBold(text, context);

        // 3. Extract Underline (Can contain Bold, Italic, Code)
        text = extractUnderline(text, context);

        // 4. Extract Italic (Lowest priority)
        text = extractItalic(text, context);

        return text;
    }

    private static String protectSpecialSequences(String text, ParseContext context) {
        return text.replace("¯\\_(ツ)_/¯", SHRUG_PLACEHOLDER);
    }

    private static String extractEscapes(String text, ParseContext context) {
        StringBuilder result = new StringBuilder(text.length());
        Matcher matcher = ESCAPE_PATTERN.matcher(text);
        int lastEnd = 0;

        while (matcher.find()) {
            result.append(text, lastEnd, matcher.start());
            int index = context.escapes.size();
            context.escapes.add(matcher.group(1));
            result.append(ESCAPE_PREFIX).append(index).append(ESCAPE_SUFFIX);
            lastEnd = matcher.end();
        }
        result.append(text.substring(lastEnd));
        return result.toString();
    }

    private static String extractCode(String text, ParseContext context) {
        // Code blocks do NOT process nested content
        return extractPattern(text, CODE_PATTERN, context.codeBlocks, CODE_PREFIX, CODE_SUFFIX, null);
    }

    private static String extractBold(String text, ParseContext context) {
        return extractPattern(text, BOLD_PATTERN, context.boldBlocks, BOLD_PREFIX, BOLD_SUFFIX, (content) -> {
            // Define what can be INSIDE bold
            content = extractCode(content, context);
            content = extractUnderline(content, context); // FIX: Allow underline inside bold
            content = extractItalic(content, context);
            return content;
        });
    }

    private static String extractUnderline(String text, ParseContext context) {
        return extractPattern(text, UNDERLINE_PATTERN, context.underlineBlocks, UNDERLINE_PREFIX, UNDERLINE_SUFFIX, (content) -> {
            // Define what can be INSIDE underline
            content = extractCode(content, context);
            content = extractBold(content, context); // FIX: Allow bold inside underline
            content = extractItalic(content, context);
            return content;
        });
    }

    private static String extractItalic(String text, ParseContext context) {
        // Italics usually don't contain other structural formatting in simple parsers,
        // but we ensure code is preserved if somehow nested
        UnaryOperator<String> italicProcessor = (content) -> extractCode(content, context);

        text = extractPattern(text, ITALIC_ASTERISK_PATTERN, context.italicBlocks, ITALIC_PREFIX, ITALIC_SUFFIX, italicProcessor);
        text = extractPattern(text, ITALIC_UNDERSCORE_PATTERN, context.italicBlocks, ITALIC_PREFIX, ITALIC_SUFFIX, italicProcessor);
        return text;
    }

    /**
     * Generic extractor that uses a UnaryOperator to handle nested content processing
     */
    private static String extractPattern(String text, Pattern pattern, List<String> storage,
                                         String prefix, String suffix,
                                         UnaryOperator<String> nestedProcessor) {
        StringBuilder result = new StringBuilder(text.length());
        Matcher matcher = pattern.matcher(text);
        int lastEnd = 0;

        while (matcher.find()) {
            result.append(text, lastEnd, matcher.start());

            String content = matcher.group(1);

            // Apply the specific nested processing rules for this type
            if (nestedProcessor != null) {
                content = nestedProcessor.apply(content);
            }

            int index = storage.size();
            storage.add(content);
            result.append(prefix).append(index).append(suffix);

            lastEnd = matcher.end();
        }

        result.append(text.substring(lastEnd));
        return result.toString();
    }

    // ========== Restoration ==========

    private static void restorePlaceholders(String text, Message message, ParseContext context) {
        if (context.recursionDepth++ > MAX_RECURSION_DEPTH) {
            message.insert(Message.raw(sanitizeText(text)));
            return;
        }

        // Unified regex to find ANY placeholder (B=Bold, U=Underline, I=Italic, C=Code, E=Escape)
        Pattern placeholderPattern = Pattern.compile(
                Pattern.quote(MAGIC) + "([BUICE])\\[(\\d+)\\]([BUICE])" + Pattern.quote(MAGIC)
        );

        Matcher matcher = placeholderPattern.matcher(text);
        int lastEnd = 0;

        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                String plainText = sanitizeText(text.substring(lastEnd, matcher.start()));
                if (!plainText.isEmpty()) {
                    Message part = Message.raw(plainText);
                    applyDefaultColor(part, context.defaultColor);
                    message.insert(part);
                }
            }

            String type = matcher.group(1);
            int index = Integer.parseInt(matcher.group(2));

            processPlaceholderByType(type, index, message, context);

            lastEnd = matcher.end();
        }

        if (lastEnd < text.length()) {
            String remainingText = sanitizeText(text.substring(lastEnd));
            if (!remainingText.isEmpty()) {
                Message part = Message.raw(remainingText);
                applyDefaultColor(part, context.defaultColor);
                message.insert(part);
            }
        }
    }

    private static void processPlaceholderByType(String type, int index, Message message, ParseContext context) {
        switch (type) {
            case "B": // Bold
                restoreAndFormat(index, context.boldBlocks, message, context, (m) -> m.getFormattedMessage().bold = MaybeBool.True);
                break;
            case "U": // Underline
                restoreAndFormat(index, context.underlineBlocks, message, context, (m) -> m.getFormattedMessage().underlined = MaybeBool.True);
                break;
            case "I": // Italic
                restoreAndFormat(index, context.italicBlocks, message, context, (m) -> m.getFormattedMessage().italic = MaybeBool.True);
                break;
            case "C": // Code
                if (index >= 0 && index < context.codeBlocks.size()) {
                    Message part = Message.raw(sanitizeText(context.codeBlocks.get(index))).monospace(true);
                    applyDefaultColor(part, context.defaultColor);
                    message.insert(part);
                }
                break;
            case "E": // Escape
                if (index >= 0 && index < context.escapes.size()) {
                    Message part = Message.raw(context.escapes.get(index));
                    applyDefaultColor(part, context.defaultColor);
                    message.insert(part);
                }
                break;
        }
    }

    // Functional interface for formatting
    @FunctionalInterface
    interface MessageFormatter {
        void apply(Message m);
    }

    private static void restoreAndFormat(int index, List<String> blocks, Message parent, ParseContext context, MessageFormatter formatter) {
        if (index < 0 || index >= blocks.size()) return;

        String content = blocks.get(index);
        Message tempContainer = Message.raw("");

        // Recursively restore content
        restorePlaceholders(content, tempContainer, context);

        // Apply formatting to children and add to parent
        List<Message> children = tempContainer.getChildren();
        if (children.isEmpty()) return;

        for (Message child : children) {
            formatter.apply(child);
        }
        parent.insertAll(children);
    }

    // ========== Utility Methods ==========

    private static void applyDefaultColor(Message part, Color color) {
        if (color != null) {
            part.color(color);
        }
    }

    private static String sanitizeText(String text) {
        return text.replace(SHRUG_PLACEHOLDER, "¯\\_(ツ)_/¯");
    }
}