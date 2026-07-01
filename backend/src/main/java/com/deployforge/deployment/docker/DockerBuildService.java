package com.deployforge.deployment.docker;

import com.deployforge.deployment.entity.ProjectType;
import com.deployforge.logging.entity.LogCategory;
import com.deployforge.logging.service.LogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class DockerBuildService {

    private final LogService logService;

    public DockerBuildService(LogService logService) {
        this.logService = logService;
    }

    public void buildImage(String deploymentId, String projectId, File workspaceDir, ProjectType projectType) throws Exception {
        logService.info(deploymentId, "Generating Dockerfile in workspace...", LogCategory.DEPLOYMENT);

        File dockerfile = new File(workspaceDir, "Dockerfile");
        writeDockerfile(dockerfile, projectType);

        String imageName = "deployforge-" + projectId.toLowerCase();
        String imageTag = "dep-" + deploymentId.toLowerCase();

        logService.info(deploymentId, "Initiating Docker image compile. Image name: " + imageName + ":" + imageTag, LogCategory.DEPLOYMENT);
        runDockerBuild(deploymentId, workspaceDir, imageName, imageTag);
        logService.info(deploymentId, "Docker image created successfully: " + imageName + ":" + imageTag, LogCategory.DEPLOYMENT);
    }

    private void writeDockerfile(File file, ProjectType projectType) throws IOException {
        String content;
        switch (projectType) {
            case REACT:
                content = "FROM node:20 AS build\n" +
                          "WORKDIR /app\n" +
                          "COPY . .\n" +
                          "RUN npm install\n" +
                          "RUN npm run build\n" +
                          "FROM nginx:alpine\n" +
                          "COPY --from=build /app/dist /usr/share/nginx/html\n";
                break;
            case SPRING_BOOT:
                content = "FROM eclipse-temurin:21-jre\n" +
                          "COPY target/*.jar app.jar\n" +
                          "ENTRYPOINT [\"java\",\"-jar\",\"/app.jar\"]\n";
                break;
            case NODE:
                content = "FROM node:20\n" +
                          "WORKDIR /app\n" +
                          "COPY . .\n" +
                          "RUN npm install\n" +
                          "CMD [\"npm\",\"start\"]\n";
                break;
            case STATIC_HTML:
                content = "FROM nginx:alpine\n" +
                          "COPY . /usr/share/nginx/html\n";
                break;
            default:
                throw new IllegalArgumentException("Unsupported project framework: " + projectType);
        }

        Files.writeString(file.toPath(), content);
    }

    private void runDockerBuild(String deploymentId, File directory, String imageName, String imageTag) throws Exception {
        String fullTag = imageName + ":" + imageTag;
        // docker build -t imageName:imageTag .
        ProcessBuilder pb = new ProcessBuilder("docker", "build", "-t", fullTag, ".");
        pb.directory(directory);
        pb.redirectErrorStream(true);

        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logService.info(deploymentId, line, LogCategory.DEPLOYMENT);
            }
        }

        boolean completed = process.waitFor(10, TimeUnit.MINUTES);
        if (!completed) {
            process.destroyForcibly();
            logService.error(deploymentId, "Docker build process timed out.", LogCategory.DEPLOYMENT);
            throw new RuntimeException("Docker build command timed out");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            logService.error(deploymentId, "Docker build failed with exit code: " + exitCode, LogCategory.DEPLOYMENT);
            throw new RuntimeException("Docker build failed with exit code: " + exitCode);
        }
    }
}
