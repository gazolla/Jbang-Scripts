///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 26+
//JAVA_OPTIONS --enable-final-field-mutation=ALL-UNNAMED
//DEPS com.google.adk:google-adk:1.0.0-rc.1
//DEPS org.slf4j:slf4j-nop:2.0.12

import com.google.adk.agents.LlmAgent;
import com.google.adk.runner.InMemoryRunner;
import com.google.genai.types.Content;
import com.google.genai.types.Part;

import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.List;

void main(String[] args) {
    var apiKey = System.getenv("GEMINI_API_KEY");

    if (apiKey == null || apiKey.isBlank()) {
        System.err.println("❌ Erro: GEMINI_API_KEY não configurada.");
        return;
    }

    // O SDK por baixo (Google Gen AI SDK) usa a variável GOOGLE_API_KEY ou
    // GEMINI_API_KEY.
    // Se ela não existir configurada nas vars de ambiente, o property ajuda!
    System.setProperty("GEMINI_API_KEY", apiKey);
    System.setProperty("GOOGLE_API_KEY", apiKey);

    var agent = LlmAgent.builder()
            .name("gemini-chat")
            .model("gemini-3.1-flash-lite-preview")
            .build();

    var runner = new InMemoryRunner(agent, "chat-app");
    var session = runner.sessionService().createSession("chat-app", "user-1").blockingGet();

    System.out.println("🤖 Gemini Pronto! (Java 26 + Google ADK)");

    try (var scanner = new Scanner(System.in)) {
        while (true) {
            System.out.print("\nVocê: ");
            if (!scanner.hasNextLine())
                break;

            var input = scanner.nextLine();
            if (input.equalsIgnoreCase("sair"))
                break;
            if (input.isBlank())
                continue;

            var processing = new AtomicBoolean(true);

            try {
                Thread.startVirtualThread(() -> {
                    while (processing.get()) {
                        System.out.print("\rGemini: Processando...");
                        try {
                            Thread.sleep(80);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                });

                // Cria o objeto Content para passar pro ADK Runner
                Content userContent = Content.builder()
                        .parts(List.of(Part.builder().text(input).build()))
                        .role("user")
                        .build();

                StringBuilder responseBuilder = new StringBuilder();

                // Executa o agent
                runner.runAsync(session.sessionKey(), userContent)
                        .blockingForEach(event -> {
                            // Se é a resposta final do agente, a gente concatena
                            if (event.finalResponse()) {
                                responseBuilder.append(event.stringifyContent());
                            }
                        });

                processing.set(false);

                System.out.print("\rGemini: " + " ".repeat(25) + "\rGemini: ");
                System.out.println(responseBuilder.toString());

            } catch (Exception e) {
                processing.set(false);
                System.err.println("\n❌ Erro na API: " + e.getMessage());
                if (e.getMessage().contains("API key expired") || e.getMessage().contains("API_KEY_INVALID")) {
                    System.err.println("👉 Sua chave expirou ou é inválida. Gere uma nova no Google AI Studio.");
                    break;
                }
            }
        }
    }
}
