
///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 26

import java.nio.file.*;

void main(String[] args) throws Exception {
    var dir = Path.of(args[0]);
    var watcher = FileSystems.getDefault().newWatchService();

    dir.register(watcher,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_DELETE,
            StandardWatchEventKinds.ENTRY_MODIFY);

    System.out.println("Monitorando pasta " + dir + "...");

    while (true) {
        var key = watcher.take();
        for (var ev : key.pollEvents()) {
            var kind = ev.kind();
            var file = dir.resolve((Path) ev.context());

            System.out.print("Evento: " + kind.name() + " -> " + file.getFileName());

            switch (kind.name()) {
                case "ENTRY_CREATE" -> {
                    if (Files.isRegularFile(file)) {
                        System.out.println(" (Processando novo arquivo...)");
                    }
                }
                case "ENTRY_DELETE" -> System.out.println(" (Arquivo foi removido ou renomeado)");
                case "ENTRY_MODIFY" -> System.out.println(" (Arquivo modificado)");
                default -> System.out.println();
            }

        }
        key.reset();
    }
}