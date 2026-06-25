package com.Dane.UMDane.service;

import com.Dane.UMDane.dto.SandboxResult;
import com.Dane.UMDane.entity.SubmissionStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class DockerSandboxService {

    public SandboxResult execute(String code, String inputData, int timeLimitMs, int memoryLimitMb) {
        String className = extractClassName(code);
        Path tempDir = null;
        try {
            // Create a temp directory
            tempDir = Files.createTempDirectory("umdane-sandbox-" + UUID.randomUUID());
            String hostPath = tempDir.toAbsolutePath().toString().replace("\\", "/");

            // Write the Java code file
            File sourceFile = new File(tempDir.toFile(), className + ".java");
            try (FileWriter writer = new FileWriter(sourceFile, StandardCharsets.UTF_8)) {
                writer.write(code);
            }

            // 1. COMPILE CODE using JDK Container
            SandboxResult compileResult = compile(hostPath, className);
            if (compileResult.getStatus() == SubmissionStatus.COMPILE_ERROR) {
                return compileResult;
            }

            // 2. RUN CODE using JRE Container with constraints
            return run(hostPath, className, inputData, timeLimitMs, memoryLimitMb);

        } catch (Exception e) {
            log.error("Lỗi trong quá trình chạy sandbox", e);
            return SandboxResult.builder()
                    .status(SubmissionStatus.COMPILE_ERROR)
                    .errorOutput("Hệ thống sandbox gặp lỗi: " + e.getMessage())
                    .runtimeMs(0)
                    .build();
        } finally {
            // Cleanup temp directory
            if (tempDir != null) {
                cleanup(tempDir.toFile());
            }
        }
    }

    private SandboxResult compile(String hostPath, String className) throws IOException, InterruptedException {
        String[] compileCmd = {
                "docker", "run", "--rm",
                "--network", "none",
                "-v", hostPath + ":/app",
                "-w", "/app",
                "eclipse-temurin:21-alpine",
                "javac", className + ".java"
        };

        ProcessBuilder pb = new ProcessBuilder(compileCmd);
        Process process = pb.start();

        // Read errors if any
        StringBuilder stderr = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stderr.append(line).append("\n");
            }
        }

        boolean finished = process.waitFor(10, TimeUnit.SECONDS); // Compilation timeout: 10s
        if (!finished) {
            process.destroyForcibly();
            return SandboxResult.builder()
                    .status(SubmissionStatus.COMPILE_ERROR)
                    .errorOutput("Compilation timed out.")
                    .runtimeMs(0)
                    .build();
        }

        if (process.exitValue() != 0) {
            return SandboxResult.builder()
                    .status(SubmissionStatus.COMPILE_ERROR)
                    .errorOutput(stderr.toString())
                    .runtimeMs(0)
                    .build();
        }

        return SandboxResult.builder().status(SubmissionStatus.ACCEPTED).build();
    }

    private SandboxResult run(String hostPath, String className, String inputData, int timeLimitMs, int memoryLimitMb) throws IOException, InterruptedException {
        String containerName = "umdane-run-" + UUID.randomUUID();
        String memoryLimitStr = memoryLimitMb + "m";

        String[] runCmd = {
                "docker", "run", "--rm", "-i",
                "--name", containerName,
                "--network", "none",
                "--memory", memoryLimitStr,
                "--cpus", "0.5",
                "-v", hostPath + ":/app",
                "-w", "/app",
                "eclipse-temurin:21-jre-alpine",
                "java", className
        };

        ProcessBuilder pb = new ProcessBuilder(runCmd);
        Process process = pb.start();

        // Feed input to stdin
        if (inputData != null && !inputData.isEmpty()) {
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8))) {
                writer.write(inputData);
                if (!inputData.endsWith("\n")) {
                    writer.write("\n");
                }
                writer.flush();
            }
        } else {
            // Close stdin if no input
            process.getOutputStream().close();
        }

        // Read stdout and stderr asynchronously
        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();

        Thread outThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stdout.append(line).append("\n");
                }
            } catch (IOException e) {
                // stream closed
            }
        });

        Thread errThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stderr.append(line).append("\n");
                }
            } catch (IOException e) {
                // stream closed
            }
        });

        outThread.start();
        errThread.start();

        long startTime = System.currentTimeMillis();
        boolean finished = process.waitFor(timeLimitMs, TimeUnit.MILLISECONDS);
        long endTime = System.currentTimeMillis();
        int runtimeMs = (int) (endTime - startTime);

        // Wait a short time for streams to flush
        outThread.join(200);
        errThread.join(200);

        if (!finished) {
            // Time Limit Exceeded
            // Kill the container forcefully
            try {
                new ProcessBuilder("docker", "kill", containerName).start().waitFor(500, TimeUnit.MILLISECONDS);
            } catch (Exception ex) {
                // ignore
            }
            process.destroyForcibly();

            return SandboxResult.builder()
                    .status(SubmissionStatus.TIME_LIMIT_EXCEEDED)
                    .errorOutput("Time Limit Exceeded!")
                    .runtimeMs(runtimeMs)
                    .build();
        }

        int exitValue = process.exitValue();
        if (exitValue != 0) {
            // Check if it failed due to memory limit (out of memory exit code is typically 137)
            if (exitValue == 137) {
                return SandboxResult.builder()
                        .status(SubmissionStatus.TIME_LIMIT_EXCEEDED)
                        .errorOutput("Memory Limit Exceeded (Container OOM Killed)")
                        .runtimeMs(runtimeMs)
                        .build();
            }

            return SandboxResult.builder()
                    .status(SubmissionStatus.RUNTIME_ERROR)
                    .errorOutput(stderr.toString().isEmpty() ? "Runtime Error (Exit Code: " + exitValue + ")" : stderr.toString())
                    .runtimeMs(runtimeMs)
                    .build();
        }

        return SandboxResult.builder()
                .status(SubmissionStatus.ACCEPTED)
                .output(stdout.toString().trim())
                .runtimeMs(runtimeMs)
                .build();
    }

    private String extractClassName(String code) {
        Pattern pattern = Pattern.compile("class\\s+([A-Za-z0-9_]+)");
        Matcher matcher = pattern.matcher(code);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "Solution";
    }

    private void cleanup(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    cleanup(f);
                }
            }
        }
        file.delete();
    }
}
