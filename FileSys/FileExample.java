//JAVA 26+

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

void main() throws Exception {
    // Definindo o caminho do arquivo
    var filePath = Path.of("meu_arquivo_exemplo.txt");
    
    // Escrevendo no arquivo (sobrescreve se já existir)
    Files.writeString(
        filePath, 
        "Olá! Este é um exemplo de como escrever arquivos usando JBang e Java moderno.\n", 
        StandardOpenOption.CREATE, 
        StandardOpenOption.TRUNCATE_EXISTING
    );
    
    // Adicionando mais linhas ao arquivo (append)
    var extras = List.of("Aqui temos uma linha extra.", "E mais uma linha aqui.");
    Files.write(
        filePath, 
        extras, 
        StandardOpenOption.APPEND
    );
    
    // Exibindo mensagem de sucesso e lendo o arquivo para confirmar
    System.out.println("Arquivo '" + filePath.getFileName() + "' criado e atualizado com sucesso!\n");
    System.out.println("Conteúdo do arquivo:");
    System.out.println(Files.readString(filePath));
}
