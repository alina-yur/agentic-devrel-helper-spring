package com.example.devrelhelper;

import com.example.devrelhelper.model.Talk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;

@Service
@RegisterReflectionForBinding({
    GitHubPRService.GitHubContent.class,
    GitHubPRService.GitHubRef.class,
    GitHubPRService.GitHubObject.class,
    GitHubPRService.GitHubPrResponse.class
})
public class GitHubPRService {

    private static final Logger log = LoggerFactory.getLogger(GitHubPRService.class);
    private static final String REPO_OWNER = "alina-yur";
    private static final String REPO_NAME = "public-speaking";
    private static final String FILE_PATH = "README.md";
    private static final String BASE_BRANCH = "main";

    public record GitHubContent(String content, String sha) {
    }

    public record GitHubRef(GitHubObject object) {
    }

    public record GitHubObject(String sha) {
    }

    public record GitHubPrResponse(String html_url) {
    }

    private final RestClient restClient;

    public GitHubPRService(RestClient.Builder builder, @Value("${github.token}") String githubToken) {
        this.restClient = builder
                .baseUrl("https://api.github.com")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + githubToken)
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .build();
    }

    public String createPR(Talk talk) {
        log.info("Processing talk: {}", talk.title());
        try {
            GitHubContent fileInfo = getFileContent(BASE_BRANCH);
            String baseSha = getBranchSha(BASE_BRANCH);

            String currentContent = decodeContent(fileInfo.content());
            String updatedContent = insertTalk(currentContent, talk);

            String branchName = "feat/talk-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"));
            createBranch(branchName, baseSha);

            commitFile(branchName, updatedContent, fileInfo.sha(), talk);

            return createPullRequest(branchName, talk);

        } catch (Exception e) {
            log.error("PR creation failed", e);
            throw new RuntimeException("Workflow failed: " + e.getMessage(), e);
        }
    }

    private String decodeContent(String encoded) {
        String clean = encoded.replaceAll("\\s", "");
        return new String(Base64.getDecoder().decode(clean), StandardCharsets.UTF_8);
    }

    private String insertTalk(String content, Talk talk) {
        String entry = String.format("* [%s]() (%s, %s)\n", talk.title(), talk.conf(), talk.location());
        String targetHeader = "## Upcoming talks";

        int headerIndex = content.indexOf(targetHeader);
        if (headerIndex != -1) {
            int lineEnd = content.indexOf('\n', headerIndex);

            if (lineEnd != -1) {
                int insertPos = lineEnd + 1;

                while (insertPos < content.length() && content.charAt(insertPos) == '\n') {
                    insertPos++;
                }

                return content.substring(0, insertPos) + entry + content.substring(insertPos);
            }
        }

        return targetHeader + "\n\n" + entry + "\n" + content;
    }

    private GitHubContent getFileContent(String ref) {
        return restClient.get()
                .uri("/repos/{owner}/{repo}/contents/{path}?ref={ref}", REPO_OWNER, REPO_NAME, FILE_PATH, ref)
                .retrieve()
                .body(GitHubContent.class);
    }

    private String getBranchSha(String branch) {
        GitHubRef res = restClient.get()
                .uri("/repos/{owner}/{repo}/git/ref/heads/{branch}", REPO_OWNER, REPO_NAME, branch)
                .retrieve()
                .body(GitHubRef.class);
        if (res == null || res.object() == null) {
            throw new IllegalStateException("Failed to retrieve branch SHA for: " + branch);
        }
        return res.object().sha();
    }

    private void createBranch(String branchName, String sha) {
        restClient.post()
                .uri("/repos/{owner}/{repo}/git/refs", REPO_OWNER, REPO_NAME)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("ref", "refs/heads/" + branchName, "sha", sha))
                .retrieve()
                .toBodilessEntity();
    }

    private void commitFile(String branch, String content, String originalSha, Talk talk) {
        String encoded = Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));

        restClient.put()
                .uri("/repos/{owner}/{repo}/contents/{path}", REPO_OWNER, REPO_NAME, FILE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "message", "Add talk: " + talk.title(),
                        "content", encoded,
                        "sha", originalSha,
                        "branch", branch))
                .retrieve()
                .toBodilessEntity();
    }

    private String createPullRequest(String branch, Talk talk) {
        GitHubPrResponse response = restClient.post()
                .uri("/repos/{owner}/{repo}/pulls", REPO_OWNER, REPO_NAME)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "title", "Add upcoming talk: " + talk.title(),
                        "body", String.format("Adding upcoming talk:\n\n**%s** at %s", talk.title(), talk.conf()),
                        "head", branch,
                        "base", BASE_BRANCH))
                .retrieve()
                .body(GitHubPrResponse.class);

        if (response == null) {
            throw new IllegalStateException("Failed to create pull request");
        }
        return response.html_url();
    }
}