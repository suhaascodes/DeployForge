package com.deployforge.project.service;

import com.deployforge.common.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
@Slf4j
public class GitValidationService {

    private static final Pattern GIT_URL_PATTERN = Pattern.compile(
            "^(https?://|git@|ssh://)([^/:]+)(/|:)(.+)/(.+)$"
    );

    public void validateRepository(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new BadRequestException("Repository URL cannot be empty");
        }

        if (!GIT_URL_PATTERN.matcher(url).matches()) {
            throw new BadRequestException("Invalid repository URL format. Must be a valid Git URL (HTTPS or SSH)");
        }

        // Only ping remote for HTTP/HTTPS since SSH checks require SSH keys configured in environment
        if (url.startsWith("http://") || url.startsWith("https://")) {
            try {
                log.info("Pinging git repository using JGit: {}", url);
                Git.lsRemoteRepository()
                        .setRemote(url)
                        .setTimeout(5) // 5 seconds timeout
                        .call();
                log.info("Git repository {} is reachable", url);
            } catch (GitAPIException e) {
                log.warn("Git validation failed for URL: {}. Error: {}", url, e.getMessage());
                throw new BadRequestException("Git repository is unreachable. Make sure the URL is correct and the repository is public. Details: " + e.getMessage());
            } catch (Exception e) {
                log.error("Unexpected error validating repository: {}", e.getMessage());
                throw new BadRequestException("Failed to validate remote repository: " + e.getMessage());
            }
        } else {
            log.info("SSH repository URL detected: {}. Skipping remote check to prevent key errors.", url);
        }
    }
}
