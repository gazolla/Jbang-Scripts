///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 26+
//JAVA_OPTIONS --enable-final-field-mutation=ALL-UNNAMED
//DEPS com.google.adk:google-adk:1.0.0-rc.1
//DEPS org.slf4j:slf4j-simple:2.0.12

import com.google.adk.agents.LlmAgent;
import com.google.adk.runner.InMemoryRunner;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import com.google.genai.types.Blob;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Stream;

public class ImageClassifier {

    public static void main(String[] args) throws IOException {
        // 1. Validação de Argumentos e API Key
        if (args.length == 0) {
            System.err.println("❌ Uso: jbang ImageClassifier.java <caminho_da_pasta>");
            return;
        }
        var inputDir = Path.of(args[0]);
        var apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("❌ Erro: Configure a variável de ambiente GEMINI_API_KEY");
            return;
        }
        System.setProperty("GOOGLE_API_KEY", apiKey);

        // 2. Configuração do Agente
        var agent = LlmAgent.builder()
                .name("classifier-agent")
                .model("gemini-3-pro-preview")
                .instruction(
                        "Você é um classificador especializado. Responda apenas com a palavra 'cão', 'gato' ou 'outro'.")
                .build();

        var runner = new InMemoryRunner(agent, "classifier-app");

        // 3. Criação de pastas de destino
        Path dogsDir = inputDir.resolve("cães");
        Path catsDir = inputDir.resolve("gatos");
        Path othersDir = inputDir.resolve("outros");
        Files.createDirectories(dogsDir);
        Files.createDirectories(catsDir);
        Files.createDirectories(othersDir);

        // 4. Sessão e Processamento (Escopo Único para evitar erros de import)
        try {
            var session = runner.sessionService().createSession("classifier-app", "user-1").blockingGet();
            var key = session.sessionKey(); // Inferência de tipo mágica aqui

            System.out.println("🚀 Iniciando classificação em: " + inputDir.toAbsolutePath());

            try (Stream<Path> stream = Files.list(inputDir)) {
                stream.filter(Files::isRegularFile)
                        .filter(p -> p.toString().toLowerCase().matches(".*\\.(jpg|jpeg|png|webp)$"))
                        .forEach(imagePath -> {
                            try {
                                System.out.print("Analisando " + imagePath.getFileName() + "... ");

                                // Prepara o conteúdo multimodal
                                byte[] bytes = Files.readAllBytes(imagePath);
                                String mime = Files.probeContentType(imagePath);

                                var content = Content.builder()
                                        .role("user")
                                        .parts(List.of(
                                                Part.fromText("Esta imagem contém um cão, um gato ou outra coisa?"),
                                                Part.builder().inlineData(
                                                        Blob.builder().data(bytes)
                                                                .mimeType(mime != null ? mime : "image/jpeg").build())
                                                        .build()))
                                        .build();

                                // Executa e coleta resposta
                                StringBuilder aiResponse = new StringBuilder();
                                runner.runAsync(key, content).blockingForEach(event -> {
                                    if (event.finalResponse()) {
                                        aiResponse.append(event.stringifyContent());
                                    }
                                });

                                String result = aiResponse.toString().toLowerCase().trim().replaceAll("[^a-zãç]", "");
                                System.out.println("-> [" + result + "]");

                                // Move o arquivo
                                Path target = result.contains("cão") ? dogsDir
                                        : result.contains("gato") ? catsDir : othersDir;

                                Files.move(imagePath, target.resolve(imagePath.getFileName()),
                                        StandardCopyOption.REPLACE_EXISTING);

                            } catch (Exception e) {
                                System.err.println("Erro no arquivo: " + e.getMessage());
                            }
                        });
            }
            System.out.println("\n✅ Processamento concluído!");

        } catch (Exception e) {
            System.err.println("❌ Erro fatal: " + e.getMessage());
            e.printStackTrace();
        }
    }
}