///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 26+
//DEPS com.google.adk:google-adk:1.0.0-rc.1
//DEPS org.slf4j:slf4j-simple:2.0.12
//DEPS info.picocli:picocli:4.7.5

import com.google.adk.agents.LlmAgent;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.SessionKey;
import com.google.genai.types.Content;
import com.google.genai.types.Part;

import java.io.Console;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.concurrent.Callable;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import static picocli.CommandLine.Help.Ansi.AUTO;

@Command(name = "jforge", mixinStandardHelpOptions = true, version = "JForge V3.1",
         description = "MCP Metadata Tool Orchestrator - Autonomous Java Agent",
         headerHeading = "@|bold,underline Usage|@:%n%n",
         descriptionHeading = "%n@|bold,underline Description|@:%n%n",
         optionListHeading = "%n@|bold,underline Options|@:%n")
public class JForgeAgent implements Callable<Integer> {

    private static final Path TOOLS_DIR = Path.of("tools");
    private static final Path LOGS_DIR = Path.of("logs");
    
    private Path currentSessionLog;
    private final java.util.Deque<String> conversationMemory = new java.util.ArrayDeque<>();

    public static void main(String[] args) {
        // Força suporte para cores ANSI no terminal do Windows / JBang
        System.setProperty("picocli.ansi", "true");
        int exitCode = new CommandLine(new JForgeAgent()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println(AUTO.string("@|bold,red \u274C Please set the GEMINI_API_KEY environment variable.|@"));
            return 1;
        }
        System.setProperty("GOOGLE_API_KEY", apiKey);

        Files.createDirectories(TOOLS_DIR);
        Files.createDirectories(LOGS_DIR);

        initLogging();
        startChatMenu();
        
        return 0;
    }

    
    private void initLogging() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        this.currentSessionLog = LOGS_DIR.resolve("session_" + timestamp + ".log");
        logToFile("==== JForgeAgent Orchestration Lifecycle Started ====");
        rotateLogs();
    }

    private void logToFile(String message) {
        if (this.currentSessionLog == null) return;
        try {
            Files.writeString(this.currentSessionLog, message + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {}
    }

    private void addToMemory(String entry) {
        if (conversationMemory.size() >= 4) {
            conversationMemory.pollFirst();
        }
        conversationMemory.addLast(entry);
    }

    private void rotateLogs() {
        try (Stream<Path> stream = Files.list(LOGS_DIR)) {
            List<Path> allLogs = stream.filter(p -> p.toString().endsWith(".log"))
                                       .sorted(Comparator.reverseOrder())
                                       .collect(Collectors.toList());
            for (int i = 3; i < allLogs.size(); i++) {
                Files.deleteIfExists(allLogs.get(i));
            }
        } catch (IOException ignored) {}
    }

    private LlmAgent buildRouterAgent() {
        String routerInstruction = """
            You are a Logical CLI Tool Orchestrator System AND a highly intelligent Conversational Assistant. 
            Core Rule: Read the 'Cached Tools' list and the 'Recent Chat History'.
            If an existing tool matches the user's programmatic goal, USE IT. Example: 'EXECUTE: ToolName.java <arg1>'.
            If a tool exists but needs a fix or new feature requested by the user, USE EDIT. Example: 'EDIT: ToolName.java "Change details"'.
            If the user needs to automate a task, fetch data, or run code and the tool doesn't exist, command creation. Example: 'CREATE: Write pure Java HttpClient...'.
            CRITICAL: If the user is just talking, asking conceptual questions, or a script is definitively NOT necessary, reply directly! Example: 'CHAT: Hello! I am JForge...'.
            YOUR RESPONSE MUST ONLY BE ONE OF THESE FOUR PATTERNS: 'EXECUTE: ...', 'CREATE: ...', 'EDIT: ...', or 'CHAT: ...'.
            """;

        return LlmAgent.builder()
                .name("router")
                .model("gemini-3-pro-preview")
                .instruction(routerInstruction)
                .build();
    }

    private LlmAgent buildCoderAgent() {
        String coderInstruction = """
            You are a Master Java Programmer working with jbang. 
            Critical Rule:
            Your output MUST contain two sections:
            
            //METADATA_START
            {
              "name": "ToolName.java",
              "description": "Short explanation of the script",
              "args": ["description of arg1"]
            }
            //METADATA_END
            
            //FILE: ToolName.java
            //DEPS ...
            ... java code ...
            
            All generated scripts MUST be robust and extract input variables dynamically from the 'args' array.
            DO NOT WRITE MARKDOWN (such as ```java). RETURN EXECUTABLE TEXT AND STRICT METADATA BLOCK ONLY.
            CRITICAL ERROR HANDLING: Do not swallow exceptions in empty try-catch blocks. If a fatal failure occurs (e.g. network error, bad API), the script MUST crash explicitly or call System.exit(1) so the orchestrator can detect the failure.
            """;

        return LlmAgent.builder()
                .name("coder")
                .model("gemini-3-pro-preview")
                .instruction(coderInstruction)
                .build();
    }

    public void startChatMenu() throws Exception {
        Console console = System.console();
        if (console == null) {
            System.err.println(AUTO.string("@|bold,red \u274C Interactive console is not supported in this environment. Exiting.|@"));
            return;
        }

        System.out.println(AUTO.string("@|bold,cyan Welcome to JForge V3.1 - MCP Metadata Tool Orchestrator.|@"));
        System.out.println(AUTO.string("Available tools are cached in: @|yellow " + TOOLS_DIR.toAbsolutePath() + "|@"));
        System.out.println(AUTO.string("Logs are recorded in: @|yellow " + LOGS_DIR.toAbsolutePath() + "|@\n"));
        
        var routerRunner = new InMemoryRunner(buildRouterAgent(), "router-app");
        var coderRunner = new InMemoryRunner(buildCoderAgent(), "coder-app");

        var sessionRouter = routerRunner.sessionService().createSession("router-app", "user").blockingGet();
        var sessionCoder = coderRunner.sessionService().createSession("coder-app", "user").blockingGet();

        SessionKey rKey = sessionRouter.sessionKey();
        SessionKey cKey = sessionCoder.sessionKey();

        String inputPrompt = AUTO.string("@|bold,green \n\uD83E\uDD16 What would you like to achieve? (or 'exit'/'quit'): |@");

        while (true) {
            String userPrompt = console.readLine(inputPrompt);
            
            if (userPrompt == null || userPrompt.isBlank() || userPrompt.equalsIgnoreCase("exit") || userPrompt.equalsIgnoreCase("quit")) {
                System.out.println(AUTO.string("@|bold,yellow Shutting down the forge...|@"));
                logToFile("[SYSTEM] Shutting down.");
                break;
            }
            
            logToFile("[USER] " + userPrompt);
            processDemand(userPrompt, routerRunner, rKey, coderRunner, cKey);
        }
    }

    private void processDemand(String userPrompt, InMemoryRunner rRunner, SessionKey rKey, InMemoryRunner cRunner, SessionKey cKey) throws Exception {
        boolean taskResolved = false;
        String lastError = null;
        int crashRetries = 0;

        while (!taskResolved) {
            List<String> cachedTools = getCachedTools();
            String cacheList = cachedTools.isEmpty() ? "Empty" : String.join(",\n", cachedTools);
            String fallbackText = lastError == null ? "No previous errors." 
                    : "A FAILURE OCCURRED IN THE LAST EXECUTION WITH THE FOLLOWING TRACE. REQUIRED FIX: " + lastError;
            String historyList = conversationMemory.isEmpty() ? "No previous context." : String.join("\n", conversationMemory);
            
            String statePrompt = String.format("""
                [Recent Chat History]
                %s
                
                [System State]
                Cached Tools (JSON format): 
                [%s]
                
                %s
                Original User Request: %s
                Decide next action: EXECUTE, CREATE, EDIT, or CHAT.
                """, historyList, cacheList, fallbackText, userPrompt);

            System.out.println(AUTO.string("@|bold,blue [ROUTER] Analyzing Intent and Metadata Schemas...|@"));
            String routerAction = invokeLlm(rRunner, rKey, statePrompt);
            logToFile("[ROUTER ACTION] " + routerAction);

            if (routerAction.startsWith("CHAT:")) {
                String chatMessage = routerAction.substring(5).trim();
                System.out.println(AUTO.string("\n@|bold,yellow [ASSISTANT]|@ " + chatMessage + "\n"));
                logToFile("[CHAT RESULT]\n" + chatMessage);
                
                addToMemory("USER: " + userPrompt);
                addToMemory("SYSTEM (CHAT): " + chatMessage);
                taskResolved = true;
                
            } else if (routerAction.startsWith("EDIT:")) {
                String editPayload = routerAction.substring(5).trim();
                System.out.println(AUTO.string("@|bold,magenta [CODER] Modifying existing tool -> |@" + editPayload));
                
                int firstSpace = editPayload.indexOf(' ');
                String targetTool = firstSpace == -1 ? editPayload : editPayload.substring(0, firstSpace).trim();
                String changes = firstSpace == -1 ? "Fix or update according to user prompt" : editPayload.substring(firstSpace).trim();
                
                String existingCode = "";
                try {
                    existingCode = Files.readString(TOOLS_DIR.resolve(targetTool));
                } catch(Exception e) {
                    existingCode = "Tool code unreadable/missing.";
                }

                String createInstruction = "Rewrite the following tool applying these changes: " + changes + "\n\n[EXISTING CODE]\n" + existingCode;
                
                if (lastError != null) {
                    createInstruction += "\nImportant: Last logic crashed. CORRECT the architecture constraints: \n" + lastError;
                }

                String generatedCode = invokeLlm(cRunner, cKey, createInstruction);
                handleCodeGeneration(generatedCode);
                lastError = null; 
                System.out.println(AUTO.string("@|bold,yellow Returning control to [ROUTER] to invoke the produced tool...|@"));
                
            } else if (routerAction.startsWith("CREATE:")) {
                String createInstruction = routerAction.substring(7).trim();
                System.out.println(AUTO.string("@|bold,magenta [CODER] Tool missing (or corrupted). Developing new Tool -> |@" + createInstruction));

                if (lastError != null) {
                    createInstruction += "\nImportant: Last logic crashed. CORRECT the architecture constraints: \n" + lastError;
                }

                String generatedCode = invokeLlm(cRunner, cKey, createInstruction);
                handleCodeGeneration(generatedCode);
                lastError = null; 
                System.out.println(AUTO.string("@|bold,yellow Returning control to [ROUTER] to invoke the produced tool...|@"));
                
            } else if (routerAction.startsWith("EXECUTE:")) {
                System.out.println(AUTO.string("@|bold,cyan [EXECUTE] |@" + routerAction));
                
                ProcessResult result = executeToolProcess(routerAction);
                logToFile("[EXECUTION RESULT]\n" + result.output());
                
                if (result.success()) {
                    System.out.println(AUTO.string("@|bold,green Demand successfully fulfilled via native JBang tool.|@"));
                    
                    String outLog = result.output();
                    if (outLog.length() > 300) outLog = outLog.substring(0, 300) + "...";
                    addToMemory("USER: " + userPrompt);
                    addToMemory("SYSTEM (EXECUTED): " + routerAction + "\nResult Preview: " + outLog.trim());
                    
                    taskResolved = true;
                } else {
                    System.out.println(AUTO.string("@|bold,red Tool Execution Failed (Exit non-zero). Returning trace for Systemic Auto-Healing...|@"));
                    crashRetries++;
                    if (crashRetries > 2) {
                        System.out.println(AUTO.string("@|bold,red Maximum retry limit reached (" + crashRetries + "). Architecture failed to heal!|@"));
                        logToFile("[SYSTEM] Auto-heal limits exceeded.");
                        break;
                    }
                    lastError = result.output(); 
                }
            } else {
                System.out.println(AUTO.string("@|bold,red Unknown Router response. Halting logic. Response: |@" + routerAction));
                logToFile("[SYSTEM] Halting Loop due to LLM Hallucinated Output: " + routerAction);
                taskResolved = true;
            }
        }
    }

    private String invokeLlm(InMemoryRunner runner, SessionKey sessionKey, String prompt) {
        StringBuilder responseBuilder = new StringBuilder();
        runner.runAsync(sessionKey, Content.builder().role("user").parts(List.of(Part.fromText(prompt))).build())
            .blockingForEach(event -> {
                if (event.finalResponse() && event.content() != null) {
                    responseBuilder.append(event.stringifyContent());
                }
            });
        return responseBuilder.toString().replace("```", "").trim();
    }

    private ProcessResult executeToolProcess(String routerAction) throws IOException, InterruptedException {
        String execCommand = routerAction.substring(8).trim(); 
        String[] parts = execCommand.split("\\s+"); 
        
        List<String> procArgs = new ArrayList<>();
        procArgs.add("jbang");
        procArgs.addAll(List.of(parts));

        Process process = new ProcessBuilder(procArgs)
                .directory(TOOLS_DIR.toFile())
                .redirectErrorStream(true)
                .start();
                
        String executionOutput = new String(process.getInputStream().readAllBytes());
        int exitCode = process.waitFor();
        
        System.out.println("----------------[ RESULT ]----------------");
        System.out.print(executionOutput);
        System.out.println("------------------------------------------");
        
        boolean success = exitCode == 0;
        // Fallback: se o exitCode foi 0 mas a String tem vazamento explícito de StackTrace
        if (success && (executionOutput.contains("Exception in thread") || executionOutput.contains("Caused by: ") || executionOutput.toLowerCase().contains("an error occurred while"))) {
            success = false;
        }
        
        return new ProcessResult(success, executionOutput);
    }

    private void handleCodeGeneration(String generatedCode) throws IOException {
        String code = generatedCode.replace("```java", "").replace("```json", "").replace("```", "").trim(); 
        
        String metadataContent = "";
        int metaStart = code.indexOf("//METADATA_START");
        int metaEnd = code.indexOf("//METADATA_END");
        
        if (metaStart != -1 && metaEnd != -1) {
            metadataContent = code.substring(metaStart + 16, metaEnd).trim();
            code = code.substring(metaEnd + 14).trim();
        }

        String fileName = "ForgedTool.java"; 
        if (code.startsWith("//FILE:")) {
            int index = code.indexOf('\n');
            if (index != -1) {
                fileName = code.substring(7, index).trim();
                code = code.substring(index).trim();
            }
        }
        
        Files.writeString(TOOLS_DIR.resolve(fileName), code);
        logToFile("[SYSTEM] Forge saved script: " + fileName);
        System.out.println(AUTO.string("@|bold,green [Operation Successful] Script saved as: |@" + fileName));
        
        if (!metadataContent.isBlank()) {
            String metaFileName = fileName.replace(".java", ".meta.json");
            Files.writeString(TOOLS_DIR.resolve(metaFileName), metadataContent);
            System.out.println(AUTO.string("@|bold,green [Metadata] Schema generated and attached: |@" + metaFileName));
            logToFile("[SYSTEM] Metadata attached: " + metadataContent);
        }
    }

    private List<String> getCachedTools() {
        List<String> cacheEntries = new ArrayList<>();
        try (Stream<Path> stream = Files.list(TOOLS_DIR)) {
            List<Path> metaFiles = stream.filter(p -> p.toString().endsWith(".meta.json")).collect(Collectors.toList());
            if (metaFiles.isEmpty()) {
                // Retro-compatibility: if no JSON is found, fallback to Java source filenames
                try (Stream<Path> fallStream = Files.list(TOOLS_DIR)) {
                    fallStream.filter(p -> p.toString().endsWith(".java"))
                              .forEach(p -> cacheEntries.add(p.getFileName().toString()));
                }
            } else {
                for (Path metaPath : metaFiles) {
                    try {
                        String json = Files.readString(metaPath);
                        cacheEntries.add(json);
                    } catch (IOException ignored) {}
                }
            }
        } catch (IOException ignored) {}
        
        return cacheEntries;
    }

    private record ProcessResult(boolean success, String output) {}
}
