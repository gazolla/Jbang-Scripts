///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 26+
//JAVA_OPTIONS --enable-final-field-mutation=ALL-UNNAMED
//DEPS dev.langchain4j:langchain4j:0.35.0
//DEPS dev.langchain4j:langchain4j-google-ai-gemini:0.35.0
//DEPS org.slf4j:slf4j-nop:2.0.12

import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.AiServices;
import java.util.Scanner;

interface Assistant {
    String chat(String message);
}

void main() {
    var apiKey = System.getenv("GEMINI_API_KEY");

    if (apiKey == null || apiKey.isBlank()) {
        System.err.println("❌ Erro: GEMINI_API_KEY não configurada.");
        return;
    }

    var model = GoogleAiGeminiChatModel.builder()
            .apiKey(apiKey)
            .modelName("gemini-3.1-flash-lite-preview")
            .temperature(0.7)
            .build();

    var assistant = AiServices.create(Assistant.class, model);

    System.out.println("🤖 Gemini Pronto! (Java 26)");

    try (var scanner = new Scanner(System.in)) {
        while (true) {
            System.out.print("\nVocê: ");
            if (!scanner.hasNextLine())
                break; // Evita o erro NoSuchElementException

            var input = scanner.nextLine();
            if (input.equalsIgnoreCase("sair"))
                break;
            if (input.isBlank())
                continue;

            var processing = new java.util.concurrent.atomic.AtomicBoolean(true);

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
                var response = assistant.chat(input);

                processing.set(false);

                System.out.print("\rGemini: " + " ".repeat(25) + "\rGemini: ");
                System.out.println(response);

            } catch (Exception e) {
                System.err.println("\n❌ Erro na API: " + e.getMessage());
                if (e.getMessage().contains("API key expired")) {
                    System.err.println("👉 Sua chave expirou. Gere uma nova no Google AI Studio.");
                    break;
                }
            }
        }
    }
}