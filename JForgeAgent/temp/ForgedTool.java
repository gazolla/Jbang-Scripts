///usr/bin/env jbang
//DEPS com.google.code.gson:gson:2.10.1

import java.net.URI;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import com.google.gson.JsonParser;
import com.google.gson.JsonObject;

public class ForgedTool {
    public static void main(String[] args) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .proxy(ProxySelector.getDefault())
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://viacep.com.br/ws/01001000/json/"))
                .header("User-Agent", "Mozilla/5.0 (compatible; JForge/1.0)")
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        JsonObject jsonObject = JsonParser.parseString(response.body()).getAsJsonObject();
        String logradouro = jsonObject.get("logradouro").getAsString();
        String bairro = jsonObject.get("bairro").getAsString();

        System.out.println("Rua: " + logradouro);
        System.out.println("Bairro: " + bairro);
    }
}