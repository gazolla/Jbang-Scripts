//DEPS com.fasterxml.jackson.core:jackson-databind:2.16.1

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ISSTracker {
    public static void main(String[] args) {
        String formatStr = args.length > 0 ? args[0] : "Posicao atual da ISS -> Latitude: %s, Longitude: %s";
        String apiUrl = args.length > 1 ? args[1] : "http://api.open-notify.org/iss-now.json";
        
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(15))
                    .build();
                    
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
                    
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(response.body());
                
                if (root.has("iss_position")) {
                    JsonNode position = root.get("iss_position");
                    String latitude = position.get("latitude").asText();
                    String longitude = position.get("longitude").asText();
                    
                    System.out.printf(formatStr + "%n", latitude, longitude);
                } else {
                    System.err.println("Resposta JSON invalida: 'iss_position' nao encontrado.");
                }
            } else {
                System.err.println("Falha ao buscar dados. HTTP Status Code: " + response.statusCode());
            }
        } catch (Exception e) {
            System.err.println("Ocorreu um erro ao rastrear a ISS: " + e.getMessage());
            e.printStackTrace();
        }
    }
}