
///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 26
//DEPS org.jsoup:jsoup:1.18.3
import org.jsoup.Jsoup;
import java.util.concurrent.*;
import java.util.List;
import java.time.Duration;
import java.time.Instant;

record ScrapeResult(String url, String title, List<String> topHeadlines) {
}

void main(String[] args) throws Exception {
    System.out.println("Iniciando raspagem de dados com Threads Virtuais...\n");
    var start = Instant.now();

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
        var sites = List.of(
                "https://news.ycombinator.com",
                "https://dev.to",
                "https://github.com/trending");

        var futures = sites.stream()
                .map(url -> executor.submit(() -> scrapeSite(url)))
                .toList();

        for (var f : futures) {
            var result = f.get();
            System.out.println("Site: " + result.title() + " (" + result.url() + ")");
            System.out.println("Top 3 Assuntos:");
            result.topHeadlines().forEach(h -> System.out.println("  - " + h));
            System.out.println();
        }
    }

    var end = Instant.now();
    System.out.println("Tempo total: " + Duration.between(start, end).toMillis() + " ms.");
}

// Extrai as melhores manchetes usando seletores CSS (Jsoup)
ScrapeResult scrapeSite(String url) throws Exception {
    var doc = Jsoup.connect(url)
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)") // Finge ser um navegador real
            .timeout(5000)
            .get();

    List<String> headlines;

    // Lógica customizada de CSS dependendo do site
    if (url.contains("ycombinator")) {
        headlines = doc.select(".titleline > a").stream().limit(3).map(e -> e.text()).toList();
    } else if (url.contains("dev.to")) {
        headlines = doc.select(".crayons-story__title > a").stream().limit(3).map(e -> e.text()).toList();
    } else if (url.contains("github.com")) {
        headlines = doc.select("h2.h3 a").stream().limit(3).map(e -> e.text().replaceAll("\\s+", " ").trim()).toList();
    } else {
        headlines = List.of();
    }

    return new ScrapeResult(url, doc.title(), headlines);
}