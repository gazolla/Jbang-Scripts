//DEPS com.fasterxml.jackson.core:jackson-databind:2.16.1

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BitcoinPricer {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Nenhuma moeda informada nos argumentos. Usando o padrao: USD");
        }
        
        String currency = args.length > 0 ? args[0].toLowerCase() : "usd";
        String apiUrl = "https://api.coingecko.com/api/v3/simple/price?ids=bitcoin&vs_currencies=" + currency;
        
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(15))
                    .build();
                    
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Accept", "application/json")
                    .header("User-Agent", "JBang-BitcoinPricer/1.0")
                    .GET()
                    .build();
                    
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(response.body());
                
                if (root.has("bitcoin") && root.get("bitcoin").has(currency)) {
                    String price = root.get("bitcoin").get(currency).asText();
                    System.out.printf("Cotacao atual do Bitcoin: %s %s%n", price, currency.toUpperCase());
                } else {
                    System.err.println("Nao foi possivel encontrar a cotacao para a moeda especificada: " + currency.toUpperCase());
                    System.err.println("Resposta da API: " + response.body());
                }
            } else {
                System.err.println("Falha ao buscar dados. HTTP Status Code: " + response.statusCode());
                System.err.println("Detalhes: " + response.body());
            }
        } catch (Exception e) {
            System.err.println("Ocorreu um erro ao obter o preco do Bitcoin: " + e.getMessage());
            e.printStackTrace();
        }
    }
}