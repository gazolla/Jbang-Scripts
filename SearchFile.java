///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 26

import java.nio.file.*;
import java.util.stream.*;
import java.util.concurrent.atomic.LongAdder;

void main(String[] args) throws Exception {
    if (args.length < 1) {
        System.out.println("❌ Uso: jbang SearchFile.java <diretorio> [padrao]");
        return;
    }

    var dir = Path.of(args[0]);
    var pattern = args.length > 1 ? args[1] : "*";
    var matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);

    var count = new LongAdder(); // Contador performático para concorrência
    System.out.println("Iniciando busca em: " + dir.toAbsolutePath());
    System.out.println("---------------------------------------------------");

    try (var stream = Files.walk(dir)) {
        stream.forEach(p -> {
            if (Files.isDirectory(p)) {
                String status = "Lendo: " + p.getFileName();
                System.out.print("\r" + " ".repeat(80) + "\r" +
                        (status.length() > 75 ? status.substring(0, 72) + "..." : status));
            }

            if (Files.isRegularFile(p) && matcher.matches(p.getFileName())) {
                count.increment();
                System.out.print("\r" + " ".repeat(80) + "\r");
                System.out.println(p);
            }
        });
    }

    System.out.print("\r" + " ".repeat(80) + "\r"); // Limpa o último status de pasta
    System.out.println("---------------------------------------------------");
    System.out.println("Busca finalizada. Arquivos encontrados: " + count.sum());
}