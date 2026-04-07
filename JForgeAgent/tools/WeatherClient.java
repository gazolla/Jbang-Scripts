import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class WeatherClient {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Error: Please provide a city name.");
            System.err.println("Usage: jbang WeatherClient.java \"City Name\"");
            System.exit(1);
        }

        String city = args[0];
        System.out.println("Fetching weather for: " + city + "...\n");

        try {
            // URL Encode the city name to handle spaces and special characters safely
            String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8)
                    .replace("+", "%20"); // Preferred encoding for spaces in URI paths

            // Using wttr.in which provides an excellent no-auth weather service
            // The "?0" limits the output to current weather. Remove it for a 3-day forecast.
            URI uri = URI.create("https://wttr.in/" + encodedCity);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            // We mock the User-Agent as curl. 
            // wttr.in responds with beautifully formatted ANSI terminal output when it detects curl.
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("User-Agent", "curl/7.88.1")
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                System.out.println(response.body());
            } else {
                System.err.println("Failed to fetch weather. HTTP Status Code: " + response.statusCode());
                System.err.println("Response: " + response.body());
                System.exit(1);
            }

        } catch (InterruptedException e) {
            System.err.println("Request was interrupted.");
            Thread.currentThread().interrupt();
            System.exit(1);
        } catch (Exception e) {
            System.err.println("An error occurred while communicating with the weather service:");
            e.printStackTrace();
            System.exit(1);
        }
    }
}