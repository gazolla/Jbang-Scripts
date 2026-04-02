//JAVA 26+

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.io.IOException;

void main() throws IOException {
    // --- Configuração Inicial ---
    var sourceFile = Path.of("arquivo_original.txt");
    var destDir = Path.of("meu_diretorio_destino");
    var copiedFile = destDir.resolve(sourceFile.getFileName());
    var movedFile = destDir.resolve("arquivo_movido.txt");

    System.out.println("Iniciando operações com arquivos...");

    try {
        // 1. Criar arquivo de origem e diretório de destino
        Files.writeString(sourceFile, "Este é o conteúdo do arquivo original.");
        if (!Files.exists(destDir)) {
            Files.createDirectory(destDir);
        }
        System.out.println("- Arquivo de origem '"+ sourceFile +"' e diretório '"+ destDir +"' preparados.");

        // 2. Copiar arquivo
        Files.copy(sourceFile, copiedFile, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("- Arquivo copiado para: " + copiedFile);
        System.out.println("  Conteúdo do arquivo copiado: '" + Files.readString(copiedFile) + "'");

        // 3. Mover/Renomear arquivo
        Files.move(copiedFile, movedFile, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("- Arquivo copiado foi movido/renomeado para: " + movedFile);

        // --- Verificação ---
        System.out.println("
--- Verificação Pós-Operações ---");
        System.out.println("Arquivo original '"+ sourceFile +"' existe? " + Files.exists(sourceFile));
        System.out.println("Arquivo copiado '"+ copiedFile +"' existe? " + Files.exists(copiedFile));
        System.out.println("Arquivo movido '"+ movedFile +"' existe? " + Files.exists(movedFile));

    } finally {
        // 4. Apagar os arquivos e o diretório para limpeza
        System.out.println("
--- Limpeza ---");
        boolean deletedSource = Files.deleteIfExists(sourceFile);
        System.out.println("- Apagando '" + sourceFile + "': " + (deletedSource ? "Sucesso" : "Não encontrado"));
        
        boolean deletedMoved = Files.deleteIfExists(movedFile);
        System.out.println("- Apagando '" + movedFile + "': " + (deletedMoved ? "Sucesso" : "Não encontrado"));

        // O diretório só pode ser apagado se estiver vazio
        boolean deletedDir = Files.deleteIfExists(destDir);
        System.out.println("- Apagando '" + destDir + "': " + (deletedDir ? "Sucesso" : "Não encontrado"));
    }

    System.out.println("
Operações com arquivos concluídas e limpeza efetuada.");
}
