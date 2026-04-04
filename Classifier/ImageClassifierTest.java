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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Teste funcional para validar a integração ADK + Gemini Vision
 */
public class ImageClassifierTest {

    public static void main(String[] args) throws IOException {
        // 1. Verificação de ambiente
        var apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("❌ Erro: Configure a variável de ambiente GEMINI_API_KEY");
            System.exit(1);
        }
        System.setProperty("GOOGLE_API_KEY", apiKey);

        if (args.length == 0) {
            System.err.println("❌ Uso: jbang ImageClassifierTest.java <caminho_para_uma_imagem_de_teste>");
            return;
        }
        var testImagePath = Path.of(args[0]);

        // 2. Configuração do Agente
        var agent = LlmAgent.builder()
                .name("test-agent")
                .model("gemini-3-pro-preview")
                .instruction("Você é um assistente de teste. Identifique o que há na imagem de forma breve.")
                .build();

        // 3. Inicialização do Runner
        var runner = new InMemoryRunner(agent, "test-app");

        System.out.println("🚀 Iniciando teste de integração...");

        // 4. Execução com Inferência de Tipo (Evitando o erro de import do SessionKey)
        // O segredo está em usar o 'var' e manter a chamada no mesmo escopo
        try {
            var session = runner.sessionService()
                    .createSession("test-app", "test-user")
                    .blockingGet();

            var key = session.sessionKey(); // O tipo SessionKey é inferido aqui

            byte[] imageBytes = Files.readAllBytes(testImagePath);
            String mimeType = Files.probeContentType(testImagePath);

            var userContent = Content.builder()
                    .role("user")
                    .parts(List.of(
                            Part.fromText("O que você vê nesta imagem? Responda em uma frase."),
                            Part.builder().inlineData(
                                    Blob.builder()
                                            .data(imageBytes)
                                            .mimeType(mimeType != null ? mimeType : "image/jpeg")
                                            .build())
                                    .build()))
                    .build();

            System.out.println("📤 Enviando imagem para o Gemini...");

            StringBuilder responseText = new StringBuilder();

            // Chamada direta onde o compilador aceita o 'key' inferido pelo var
            runner.runAsync(key, userContent).blockingForEach(event -> {
                if (event.finalResponse()) {
                    responseText.append(event.stringifyContent());
                }
            });

            System.out.println("\n✅ Teste Concluído com Sucesso!");
            System.out.println("🤖 Resposta da IA: " + responseText.toString().trim());

        } catch (Exception e) {
            System.err.println("\n❌ Falha no teste:");
            e.printStackTrace();
        }
    }
}