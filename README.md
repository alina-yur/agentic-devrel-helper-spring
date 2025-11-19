# Developer Relations Helper - GraalVM & Spring Boot CLI

## Purpose
A Spring Boot command-line tool that uses AI (OpenAI) to automatically generate developer relations content (tweets, blog posts) from technical talk information.

## Tech Stack
* Java 21 + Spring Boot 3.4.1
* Spring AI 1.1.0 (OpenAI integration)
* Spring Shell 3.4.1 (interactive CLI)
* GraalVM Native Image

## How It Works
* User runs generate-assets command in interactive shell
* Shell prompts for: talk title, conference, short description
* Service loads prompt template from prompts/talk_wrapped.txt
* Calls OpenAI API via Spring AI ChatClient
* AI returns structured JSON → deserialized to TalkWrapped record
* Formatted output displays tweets, blog title, overview, and sections