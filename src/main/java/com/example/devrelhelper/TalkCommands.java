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
    private final ComponentFlow.Builder flowBuilder;

    public TalkCommands(TalkWrappedService service, ComponentFlow.Builder flowBuilder) {
        this.service = service;
        this.flowBuilder = flowBuilder;
    }

    @ShellMethod(key = "generate-assets", value = "Interactively generate talk assets")
    public String generateAssets() {
        ComponentFlow flow = flowBuilder.clone().reset()
                .withStringInput("title")
                .name("Talk title")
                .and()
                .withStringInput("conference")
                .name("Conference")
                .and()
                .withStringInput("shortDesc")
                .name("Short description")
                .and()
                .build();

        ComponentFlow.ComponentFlowResult result = flow.run();

        String title = result.getContext().get("title", String.class);
        String conference = result.getContext().get("conference", String.class);
        String shortDesc = result.getContext().get("shortDesc", String.class);

        Talk talk = new Talk(title, conference, shortDesc, List.of());
        TalkWrapped wrapped = service.generate(talk);
        return service.toText(talk, wrapped);
    }
}
