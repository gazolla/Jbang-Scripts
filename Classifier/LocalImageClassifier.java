///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 26+
//DEPS dev.langchain4j:langchain4j-ollama:0.35.0
//DEPS org.slf4j:slf4j-simple:2.0.12

import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.ollama.OllamaChatModel;
import java.io.IOException;
import java.nio.file.*;
import java.util.Base64;
import java.util.List;
import java.util.stream.Stream;

public class LocalImageClassifier {

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.err.println("❌ Uso: jbang LocalImageClassifier.java <pasta_imagens>");
            return;
        }

        Path inputDir = Path.of(args[0]);

        // 1. Configurar o Modelo Local (Ollama + Moondream)
        var model = OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("moondream")
                .temperature(0.0) // Respostas determinísticas
                .build();

        // Preparar pastas
        Path dogs = inputDir.resolve("cães");
        Path cats = inputDir.resolve("gatos");
        Path others = inputDir.resolve("outros");
        Files.createDirectories(dogs);
        Files.createDirectories(cats);
        Files.createDirectories(others);

        System.out.println("🏠 Iniciando CLASSIFICAÇÃO LOCAL (Moondream)...");

        try (Stream<Path> stream = Files.list(inputDir)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> p.toString().toLowerCase().matches(".*\\.(jpg|jpeg|png)$"))
                    .forEach(path -> {
                        try {
                            System.out.print("🖼️ " + path.getFileName() + " -> ");

                            // Ler imagem e converter para Base64 (padrão Ollama)
                            byte[] bytes = Files.readAllBytes(path);
                            String base64 = Base64.getEncoder().encodeToString(bytes);

                            // Criar mensagem multimodal
                            var message = UserMessage.from(
                                    TextContent.from(
                                            "Identify if this is a 'dog', 'cat', or 'other'. Answer with only one word."),
                                    ImageContent.from(base64, "image/jpeg"));

                            // Inferência Local
                            String res = model.generate(message).content().text().toLowerCase();
                            System.out.println("[" + res + "]");

                            Path target = res.contains("dog") ? dogs : res.contains("cat") ? cats : others;

                            Files.move(path, target.resolve(path.getFileName()), StandardCopyOption.REPLACE_EXISTING);

                        } catch (Exception e) {
                            System.err.println("Erro: " + e.getMessage());
                        }
                    });
        }
    }
}