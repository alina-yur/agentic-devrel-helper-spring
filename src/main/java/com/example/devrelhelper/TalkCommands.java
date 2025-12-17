package com.example.devrelhelper;

import com.example.devrelhelper.model.Talk;
import com.example.devrelhelper.model.TalkWrapped;
import org.springframework.shell.component.flow.ComponentFlow;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import java.util.List;

@ShellComponent
public class TalkCommands {

    private final TalkWrappedService service;
    private final GitHubPRService githubPRService;
    private final ComponentFlow.Builder flowBuilder;

    public TalkCommands(TalkWrappedService service, GitHubPRService githubPRService, ComponentFlow.Builder flowBuilder) {
        this.service = service;
        this.githubPRService = githubPRService;
        this.flowBuilder = flowBuilder;
    }

    @ShellMethod(key = "generate-assets", value = "Interactively generate talk assets")
    public String generateAssets() {
        Talk talk = collectTalkInfo();
        TalkWrapped wrapped = service.generate(talk);
        return service.toText(talk, wrapped);
    }

    @ShellMethod(key = "submit-talk-pr", value = "Submit an upcoming talk as a PR to public-speaking repo")
    public String submitTalkPR() {
        Talk talk = collectTalkInfo();

        try {
            return githubPRService.createPR(talk);
        } catch (Exception e) {
            return "Failed to create PR: " + e.getMessage();
        }
    }

    private Talk collectTalkInfo() {
        ComponentFlow flow = flowBuilder.clone().reset()
                .withStringInput("title")
                .name("Talk title")
                .and()
                .withStringInput("conference")
                .name("Conference")
                .and()
                .withStringInput("location")
                .name("Location")
                .and()
                .build();

        ComponentFlow.ComponentFlowResult result = flow.run();

        String title = result.getContext().get("title", String.class);
        String conference = result.getContext().get("conference", String.class);
        String location = result.getContext().get("location", String.class);

        return new Talk(title, conference, location, List.of());
    }
}
