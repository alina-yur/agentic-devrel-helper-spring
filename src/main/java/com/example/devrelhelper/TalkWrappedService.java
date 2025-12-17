package com.example.devrelhelper;

import com.example.devrelhelper.model.BlogSection;
import com.example.devrelhelper.model.Talk;
import com.example.devrelhelper.model.TalkWrapped;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RegisterReflectionForBinding({
        TalkWrapped.class,
        Talk.class,
        BlogSection.class
})
public class TalkWrappedService {

    private static final String PROMPT_TEMPLATE = """
            You are a concise DevRel helper.

            Inputs:
            - Talk title: ${title}
            - Conference: ${conference}
            - Location: ${location}
            - Demo links: ${demos}

            Generate JSON with:
            - tweets: 2-3 items, each ≤280 chars
            - blogTitle: concise and punchy
            - blogOverview: 2-3 sentences, friendly and practical
            - blogSections: 3-5 sections; each has 3-5 short bullets

            Do not invent features beyond the demo links.
            Return only JSON for com.example.devrelhelper.model.TalkWrapped.
            """;

    private final ChatClient chatClient;

    public TalkWrappedService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public TalkWrapped generate(Talk in) {
        String prompt = PROMPT_TEMPLATE
                .replace("${title}", orEmpty(in.title()))
                .replace("${conference}", orEmpty(in.conf()))
                .replace("${location}", orEmpty(in.location()))
                .replace("${demos}", formatDemos(in.demos()));

        return chatClient
                .prompt()
                .user(prompt)
                .call()
                .entity(TalkWrapped.class);
    }

    public String toText(Talk in, TalkWrapped t) {
        var b = new StringBuilder();

        b.append("TITLE: ").append(orEmpty(in.title())).append('\n');
        b.append("CONFERENCE: ").append(orEmpty(in.conf())).append('\n');
        b.append("LOCATION: ").append(orEmpty(in.location())).append("\n\n");

        b.append("TWEETS:\n");
        if (t.tweets() != null) {
            t.tweets().forEach(s -> b.append("- ").append(s).append('\n'));
        }
        b.append('\n');

        b.append("BLOG TITLE:\n")
                .append(orEmpty(t.blogTitle()))
                .append("\n\n");

        b.append("BLOG OVERVIEW:\n")
                .append(orEmpty(t.blogOverview()))
                .append("\n\n");

        b.append("BLOG SECTIONS:\n");
        if (t.blogSections() != null) {
            for (var section : t.blogSections()) {
                b.append(orEmpty(section.heading())).append('\n');
                if (section.bullets() != null) {
                    for (var bullet : section.bullets()) {
                        b.append("- ").append(bullet).append('\n');
                    }
                }
                b.append('\n');
            }
        }

        if (in.demos() != null && !in.demos().isEmpty()) {
            b.append("DEMOS:\n");
            in.demos().forEach(d -> b.append(d.trim()).append('\n'));
        }

        return b.toString();
    }

    private static String orEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String formatDemos(List<String> demos) {
        if (demos == null || demos.isEmpty()) {
            return "";
        }
        return String.join(", ", demos);
    }
}
