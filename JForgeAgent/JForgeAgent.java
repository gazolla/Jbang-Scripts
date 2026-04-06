///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 26+
//DEPS com.google.adk:google-adk:1.0.0-rc.1
//DEPS org.slf4j:slf4j-simple:2.0.12

import com.google.adk.agents.LlmAgent;
import com.google.adk.runner.InMemoryRunner;
import com.google.genai.types.Content;
import com.google.genai.types.Part;

import java.io.Console;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JForgeAgent {

    public static void main(String[] args) throws IOException, InterruptedException {
        // Validação da Chave
        var apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("❌ Configure a variável de ambiente GEMINI_API_KEY");
            return;
        }
        System.setProperty("GOOGLE_API_KEY", apiKey);

        Path tempDir = Path.of("temp");
        Files.createDirectories(tempDir);

        // Agente 1: Diretor das Ferramentas
        String routerInstruction = """
            Você é um Sistema Orquestrador Lógico de Ferramentas CLI. 
            Regra Fundamental: Se a ferramenta existir e resolver o problema, mande EXECUTÁ-LA com os argumentos necessários. Se não existir ou se ela falhou na última tentativa, mande CRIAR uma nova (ou re-escrever).
            O SEU RETORNO DEVE SER APENAS UM DESTOS DOIS PADRÕES:
            1. Para Usar: 'EXECUTE: NomeDaFerramenta.java <arg1> <arg2>' (Não use vírgulas nos argumentos).
            2. Para Criar: 'CREATE: <Instrução técnica de código para o Coder Agent, informando que os parâmetros vindos do usuário devem usar o array Java args[] em vez de hardcode>'.
            Exemplo de CREATE: 'CREATE: Escreva um script chamado WeatherClient.java usando HttpClient puro que extrai a cidade (args[0]) e consome open-meteo API.'
            """;

        // Agente 2: Forjador
        String coderInstruction = """
            Você é um Programador Java Mestre trabalhando com jbang. 
            Regra Crítica:
            Sua PRIMEIRA LINHA de retorno deve ser ESTRITAMENTE: //FILE: NomeDescritivoSemEspacos.java
            O código JBang inicia na linha seguinte obrigatoriamente com as assinaturas //DEPS se aplicável.
            Todos os scripts gerados PRECISAM ser generalistas, extraindo variáveis dependentes do array 'args'.
            NÃO asse argumentos de usuário diretamente no código se eles puderem vir por args[0].
            NÃO ESCREVA MARKDOWN (como ```java). RETORNE APENAS TEXTO EXECUTÁVEL E O //FILE:.
            """;

        var routerAgent = LlmAgent.builder().name("router").model("gemini-3-pro-preview").instruction(routerInstruction).build();
        var coderAgent = LlmAgent.builder().name("coder").model("gemini-3-pro-preview").instruction(coderInstruction).build();

        var routerRunner = new InMemoryRunner(routerAgent, "router-app");
        var coderRunner = new InMemoryRunner(coderAgent, "coder-app");

        // Sessões Assíncronas Persistentes (Isso evita importação de variáveis dinâmicas erradas)
        var sessionRouter = routerRunner.sessionService().createSession("router-app", "user").blockingGet();
        var sessionCoder = coderRunner.sessionService().createSession("coder-app", "user").blockingGet();

        Console console = System.console();
        if (console == null) {
            System.err.println("❌ Ambiente não suporta console interativo. Finalizando.");
            return;
        }

        System.out.println("🔨 Bem-vindo ao Sistema JForge V2 - Orquestrador de Ferramentas Persistentes.");
        System.out.println("Ferramentas disponíveis cacheadas em: " + tempDir.toAbsolutePath() + "\n");

        while (true) {
            String problemStatement = console.readLine("\n🤖 Sua demanda (ou 'sair'): ");
            
            if (problemStatement == null || problemStatement.isBlank() || problemStatement.equalsIgnoreCase("sair") || problemStatement.equalsIgnoreCase("exit")) {
                System.out.println("Encerrando a forja...");
                break;
            }

            boolean taskResolved = false;
            String lastError = null;
            int crashRetries = 0;

            while (!taskResolved) {
                // Listar catálogo local
                List<String> tools = new ArrayList<>();
                try (Stream<Path> stream = Files.list(tempDir)) {
                    tools = stream.filter(p -> p.toString().endsWith(".java")).map(p -> p.getFileName().toString()).collect(Collectors.toList());
                }
                
                String cacheList = tools.isEmpty() ? "Vazia" : String.join(", ", tools);
                String fallbackText = lastError == null ? "Nenhum erro anterior. Fluxo Limpo." : "FALHA E CÓDIGO DO ERRO OCORRIDO NA ÚLTIMA EXECUÇÃO PARA CORREÇÃO: " + lastError;
                
                // Prompt que o Diretor Lê a cada ciclo
                String statePrompt = String.format("""
                    [Estado do Sistema]
                    Ferramentas no Cache: %s
                    %s
                    Pedido Original: %s
                    Decida a próxima ação: EXECUTE (para usar ou re-usar as ferramentas do cache compatíveis com o formato EXECUTE: Tool.java argumento) ou CREATE (para escrever um novo script arrumando um erro caso haja, ou criando uma tool do zero).
                    """, cacheList, fallbackText, problemStatement);

                System.out.println("▶️  [ROUTER] Analisando Intent de uso e Banco da Ferramentas...");
                
                // Roda O Diretor
                StringBuilder routerResponseBuilder = new StringBuilder();
                routerRunner.runAsync(sessionRouter.sessionKey(), Content.builder().role("user").parts(List.of(Part.fromText(statePrompt))).build())
                    .blockingForEach(event -> {
                        if (event.finalResponse() && event.content() != null) {
                            routerResponseBuilder.append(event.stringifyContent());
                        }
                    });
                    
                String routerAction = routerResponseBuilder.toString().replace("```", "").trim();

                if (routerAction.startsWith("CREATE:")) {
                    String createInstruction = routerAction.substring(7).trim();
                    System.out.println("🔨 [CODER] Ferramenta ausente (ou código corrompido). Desenvolvendo nova Tool -> " + createInstruction);

                    StringBuilder coderResponseBuilder = new StringBuilder();
                    coderRunner.runAsync(sessionCoder.sessionKey(), Content.builder().role("user").parts(List.of(Part.fromText(createInstruction))).build())
                        .blockingForEach(event -> {
                            if (event.finalResponse() && event.content() != null) {
                                coderResponseBuilder.append(event.stringifyContent());
                            }
                        });
                        
                    String generatedCode = coderResponseBuilder.toString();
                    generatedCode = generatedCode.replace("```java", "").replace("```", "").trim(); // Markdown cleanup
                    
                    // Extração flexível da Assinatura do Arquivo (Ex: "//FILE: FetchTool.java")
                    String fileName = "ForgedTool.java"; 
                    if (generatedCode.startsWith("//FILE:")) {
                        int index = generatedCode.indexOf('\n');
                        if (index != -1) {
                            fileName = generatedCode.substring(7, index).trim();
                            // Optional: Remove a linha //FILE para o JBang interpretar a //DEPS perfeitamente na primiera linha.
                            generatedCode = generatedCode.substring(index).trim();
                        }
                    }
                    
                    Files.writeString(tempDir.resolve(fileName), generatedCode);
                    System.out.println("💾 [Operação Efetuada] Script guardado como: " + fileName);
                    lastError = null; 
                    System.out.println("🔄 Retornando o controle ao [ROUTER] para invocar a ferramenta produzida...");
                    continue; // Pula o resto. O diretor refaz o Prompt e agora verá a nova Tool e responderá EXECUTE
                    
                } else if (routerAction.startsWith("EXECUTE:")) {
                    System.out.println("🚀 [EXECUTE] " + routerAction);
                    
                    // Quebra "EXECUTE: Weather.java Brasilia" 
                    String execCommand = routerAction.substring(8).trim(); 
                    String[] parts = execCommand.split("\\s+"); 
                    
                    List<String> procArgs = new ArrayList<>();
                    procArgs.add("jbang");
                    for (String p : parts) {
                        procArgs.add(p);
                    }

                    Process p = new ProcessBuilder(procArgs)
                            .directory(tempDir.toFile())
                            .redirectErrorStream(true)
                            .start();
                            
                    String executionOutput = new String(p.getInputStream().readAllBytes());
                    int exitCode = p.waitFor();
                    
                    System.out.println("----------------[ RESULTADO ]----------------");
                    System.out.print(executionOutput);
                    System.out.println("---------------------------------------------");
                    
                    if (exitCode == 0) {
                        System.out.println("✨ Demanda concluída com sucesso usando JBang nativo.");
                        taskResolved = true;
                    } else {
                        System.out.println("🔥 Execução da Ferramenta Falhou (Exit " + exitCode + ")");
                        crashRetries++;
                        if (crashRetries > 2) {
                            System.out.println("❌ Limite de Retentativas Estourado. A Arquitetura falhou em se Curar.");
                            break;
                        }
                        lastError = executionOutput; // O Router verá que o script que enviamos ao ProcessBuilder falhou e pedirá um CREATE pra substituir
                    }
                } else {
                    System.out.println("⚠️ Resposta do Router confusa: " + routerAction);
                    taskResolved = true; // Quebra para evitar laço infinito por alucinação pura
                }
            }
        }
    }
}
