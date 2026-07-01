package com.deployforge.deployment.service;

import com.deployforge.deployment.entity.ProjectType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
@Slf4j
public class ProjectTypeDetector {

    public ProjectType detectProjectType(File workspaceDir) {
        if (workspaceDir == null || !workspaceDir.exists() || !workspaceDir.isDirectory()) {
            log.warn("Invalid workspace folder for project detection");
            return ProjectType.UNKNOWN;
        }

        File pomXml = new File(workspaceDir, "pom.xml");
        File packageJson = new File(workspaceDir, "package.json");

        if (pomXml.exists() && pomXml.isFile()) {
            log.info("Detected SPRING_BOOT framework (found pom.xml)");
            return ProjectType.SPRING_BOOT;
        }

        if (packageJson.exists() && packageJson.isFile()) {
            // Check for any vite.config.* files
            boolean hasVite = false;
            File[] files = workspaceDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().startsWith("vite.config.")) {
                        hasVite = true;
                        break;
                    }
                }
            }

            if (hasVite) {
                log.info("Detected REACT framework (found package.json and vite.config)");
                return ProjectType.REACT;
            } else {
                log.info("Detected NODE framework (found package.json without vite)");
                return ProjectType.NODE;
            }
        }

        File indexHtml = new File(workspaceDir, "index.html");
        if (indexHtml.exists() && indexHtml.isFile()) {
            log.info("Detected STATIC_HTML framework (found index.html)");
            return ProjectType.STATIC_HTML;
        }

        log.warn("Could not match any supported framework. Mapping to UNKNOWN");
        return ProjectType.UNKNOWN;
    }
}
