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

    public SandboxResult execute(String code, String driverCode, String inputData, int timeLimitMs, int memoryLimitMb) {
        String className = extractClassName(code);
        Path tempDir = null;
        try {
            // Create a temp directory
            tempDir = Files.createTempDirectory("umdane-sandbox-" + UUID.randomUUID());
            String hostPath = tempDir.toAbsolutePath().toString().replace("\\", "/");

            // Write the user's Java code file
            File sourceFile = new File(tempDir.toFile(), className + ".java");
            try (FileWriter writer = new FileWriter(sourceFile, StandardCharsets.UTF_8)) {
                writer.write(code);
            }

            // Write the driver code if present
            String mainClassName = className;
            if (driverCode != null && !driverCode.trim().isEmpty()) {
                String driverClassName = extractClassName(driverCode);
                if (driverClassName == null || driverClassName.isEmpty()) {
                    driverClassName = "Main";
                }
                mainClassName = driverClassName;
                File driverFile = new File(tempDir.toFile(), driverClassName + ".java");
                try (FileWriter writer = new FileWriter(driverFile, StandardCharsets.UTF_8)) {
                    writer.write(driverCode);
                }
            }

            // 1. COMPILE CODE using JDK Container
            SandboxResult compileResult = compile(hostPath, mainClassName);
            if (compileResult.getStatus() == SubmissionStatus.COMPILE_ERROR) {
                return compileResult;
            }

            // 2. RUN CODE using JRE Container with constraints
            return run(hostPath, mainClassName, inputData, timeLimitMs, memoryLimitMb);

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

    private SandboxResult compile(String localPath, String mainClassName) throws IOException, InterruptedException {
        String containerName = "umdane-compile-" + UUID.randomUUID();

        // 1. Create a JDK container to run javac on the main class
        String[] createCmd = {
                "docker", "create",
                "--name", containerName,
                "--network", "none",
                "-w", "/app",
                "eclipse-temurin:21-alpine",
                "javac", mainClassName + ".java"
        };
        runCmdSynchronously(createCmd);

        try {
            // 2. Copy all java files from localPath into the container
            File dir = new File(localPath);
            File[] files = dir.listFiles((d, name) -> name.endsWith(".java"));
            if (files != null) {
                for (File f : files) {
                    String[] cpCmd = {
                            "docker", "cp",
                            f.getAbsolutePath(),
                            containerName + ":/app/" + f.getName()
                    };
                    runCmdSynchronously(cpCmd);
                }
            }

            // 3. Start compile container and capture stdout/stderr
            String[] startCmd = {
                    "docker", "start", "-a",
                    containerName
            };

            ProcessBuilder pb = new ProcessBuilder(startCmd);
            Process process = pb.start();

            // Read errors if any
            StringBuilder stderr = new StringBuilder();
            StringBuilder stdout = new StringBuilder();

            Thread outThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stdout.append(line).append("\n");
                    }
                } catch (IOException e) {}
            });

            Thread errThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stderr.append(line).append("\n");
                    }
                } catch (IOException e) {}
            });

            outThread.start();
            errThread.start();

            boolean finished = process.waitFor(10, TimeUnit.SECONDS); // Compilation timeout: 10s
            if (!finished) {
                process.destroyForcibly();
                return SandboxResult.builder()
                        .status(SubmissionStatus.COMPILE_ERROR)
                        .errorOutput("Compilation timed out.")
                        .runtimeMs(0)
                        .build();
            }

            outThread.join(200);
            errThread.join(200);

            if (process.exitValue() != 0) {
                String errorMsg = stderr.toString().trim();
                if (errorMsg.isEmpty()) {
                    errorMsg = stdout.toString().trim();
                }
                return SandboxResult.builder()
                        .status(SubmissionStatus.COMPILE_ERROR)
                        .errorOutput(errorMsg)
                        .runtimeMs(0)
                        .build();
            }

            // 4. Copy all compiled files (.class) back to localPath
            String[] cpBackCmd = {
                    "docker", "cp",
                    containerName + ":/app/.",
                    localPath
            };
            runCmdSynchronously(cpBackCmd);

            return SandboxResult.builder().status(SubmissionStatus.ACCEPTED).build();

        } finally {
            // Delete container
            String[] rmCmd = {"docker", "rm", "-f", containerName};
            runCmdSynchronously(rmCmd);
        }
    }

    private SandboxResult run(String localPath, String className, String inputData, int timeLimitMs, int memoryLimitMb) throws IOException, InterruptedException {
        String containerName = "umdane-run-" + UUID.randomUUID();
        String memoryLimitStr = memoryLimitMb + "m";

        // 1. Create a JRE container to execute the java code
        String[] createCmd = {
                "docker", "create",
                "--name", containerName,
                "-i",
                "--network", "none",
                "--memory", memoryLimitStr,
                "--cpus", "0.5",
                "-w", "/app",
                "eclipse-temurin:21-jre-alpine",
                "java", className
        };
        runCmdSynchronously(createCmd);

        try {
            // 2. Copy compiled class files from localPath into the container
            String[] cpCmd = {
                    "docker", "cp",
                    localPath + "/.",
                    containerName + ":/app"
            };
            runCmdSynchronously(cpCmd);

            // 3. Start running container
            String[] startCmd = {
                    "docker", "start", "-a", "-i",
                    containerName
            };

            ProcessBuilder pb = new ProcessBuilder(startCmd);
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

        } finally {
            // Delete running container
            String[] rmCmd = {"docker", "rm", "-f", containerName};
            runCmdSynchronously(rmCmd);
        }
    }

    private void runCmdSynchronously(String[] cmd) {
        try {
            Process p = new ProcessBuilder(cmd).start();
            p.waitFor();
        } catch (Exception e) {
            log.error("Lỗi khi chạy lệnh đồng bộ: " + String.join(" ", cmd), e);
        }
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
