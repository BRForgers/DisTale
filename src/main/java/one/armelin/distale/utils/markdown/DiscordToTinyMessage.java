package one.armelin.distale.utils.markdown;

import com.vladsch.flexmark.html.renderer.*;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.ast.*;

import java.util.*;

public class DiscordToTinyMessage {
    static class TinyMessageRenderer implements NodeRenderer {

        public static class Factory implements NodeRendererFactory {
            @Override
            public NodeRenderer apply(DataHolder options) {
                return new TinyMessageRenderer();
            }
        }

        @Override
        public Set<NodeRenderingHandler<?>> getNodeRenderingHandlers() {
            Set<NodeRenderingHandler<?>> handlers = new HashSet<>();

            handlers.add(new NodeRenderingHandler<>(StrongEmphasis.class,
                    (node, context, html) -> {
                        StrongEmphasis strong = (StrongEmphasis) node;
                        if (strong.getOpeningMarker().equals("__")) {
                            html.raw("<u>");
                            context.renderChildren(node);
                            html.raw("</u>");
                        } else {
                            html.raw("<b>");
                            context.renderChildren(node);
                            html.raw("</b>");
                        }
                    }));

            handlers.add(new NodeRenderingHandler<>(Emphasis.class,
                    (node, context, html) -> {
                        html.raw("<i>");
                        context.renderChildren(node);
                        html.raw("</i>");
                    }));

            handlers.add(new NodeRenderingHandler<>(Code.class,
                    (node, context, html) -> {
                        html.raw("<mono>");
                        html.text(node.getText().toString());
                        html.raw("</mono>");
                    }));

            handlers.add(new NodeRenderingHandler<>(FencedCodeBlock.class,
                    (node, context, html) -> {
                        html.raw("<mono>");
                        html.text(node.getContentChars().toString());
                        html.raw("</mono>");
                    }));

            handlers.add(new NodeRenderingHandler<>(Paragraph.class,
                    (node, context, html) -> {
                        context.renderChildren(node);
                        html.raw("\n");
                    }));

            handlers.add(new NodeRenderingHandler<>(SoftLineBreak.class,
                    (node, context, html) -> {
                        html.text(" ");
                    }));

            handlers.add(new NodeRenderingHandler<>(HardLineBreak.class,
                    (node, context, html) -> {
                        html.raw("\n");
                    }));

            handlers.add(new NodeRenderingHandler<>(Link.class,
                    (node, context, html) -> {
                        String url = node.getUrl().toString();

                        html.raw("<u><color:#80a1d8><link:" + url + ">");
                        context.renderChildren(node);
                        html.raw("</link></color></u>");
                    }));

            handlers.add(new NodeRenderingHandler<>(AutoLink.class,
                    (node, context, html) -> {
                        String url = node.getUrl().toString();

                        html.raw("<u><color:#80a1d8><link:" + url + ">");
                        context.renderChildren(node);
                        html.raw("</link></color></u>");
                    }));

            handlers.add(new NodeRenderingHandler<>(HtmlInline.class,
                    (node, context, html) -> {
                        html.raw(node.getChars().toString());
                    }));

            handlers.add(new NodeRenderingHandler<>(HtmlBlock.class,
                    (node, context, html) -> {
                        html.raw(node.getChars().toString());
                    }));

            return handlers;
        }
    }
}