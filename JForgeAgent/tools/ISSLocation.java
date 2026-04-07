//DEPS com.fasterxml.jackson.core:jackson-databind:2.15.2

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

public class ISSLocation {
    public static void main(String[] args) {
        String apiUrl = "http://api.open-notify.org/iss-now.json";

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("Fatal Error: Failed to fetch ISS location. HTTP Status Code: " + response.statusCode());
                System.err.println("Response Body: " + response.body());
                System.exit(1);
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(response.body());

            String message = rootNode.path("message").asText();
            if (!"success".equalsIgnoreCase(message)) {
                System.err.println("Fatal Error: API did not return a success message. Message: " + message);
                System.exit(1);
            }

            JsonNode positionNode = rootNode.path("iss_position");
            if (positionNode.isMissingNode()) {
                System.err.println("Fatal Error: 'iss_position' node is missing from the JSON response.");
                System.exit(1);
            }

            String latitude = positionNode.path("latitude").asText();
            String longitude = positionNode.path("longitude").asText();

            System.out.println("The International Space Station (ISS) is currently at:");
            System.out.println("Latitude:  " + latitude);
            System.out.println("Longitude: " + longitude);

        } catch (InterruptedException e) {
            System.err.println("Fatal Error: Request was interrupted.");
            e.printStackTrace();
            Thread.currentThread().interrupt();
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Fatal Error: An exception occurred while fetching/parsing the ISS location.");
            e.printStackTrace();
            System.exit(1);
        }
    }
}