package one.armelin.distale.utils.markdown;

import com.hypixel.hytale.server.core.Message;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.escaped.character.EscapedCharacterExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.vladsch.flexmark.util.misc.Extension;
import one.armelin.distale.utils.TinyMsg;

import java.util.List;

public class MarkdownConverter {
    public static String convert(String markdown) {

        List<Extension> extensions = List.of(
                AutolinkExtension.create(),
                EscapedCharacterExtension.create()
        );

        MutableDataSet options = new MutableDataSet();

        options.set(Parser.EXTENSIONS, extensions);
        options.set(Parser.LIST_BLOCK_PARSER, false);
        options.set(Parser.BLOCK_QUOTE_PARSER, false);
        options.set(HtmlRenderer.ESCAPE_HTML, false);
        options.set(HtmlRenderer.ESCAPE_INLINE_HTML, false);
        options.set(HtmlRenderer.ESCAPE_HTML_BLOCKS, false);

        Parser parser = Parser.builder(options).build();
        HtmlRenderer renderer = HtmlRenderer.builder(options).escapeHtml(false)
                .nodeRendererFactory(new DiscordToTinyMessage.TinyMessageRenderer.Factory())
                .build();
        Node document = parser.parse(markdown.trim().replaceAll("¯\\\\_\\(ツ\\)_/¯", "¯\\\\\\\\_(ツ)\\\\_/¯"));
        return renderer.render(document).trim();
    }

    public static Message toMessage(String input) {
        return TinyMsg.parse(convert(input));
    }

}
