import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BitcoinPriceClient {

    public static void main(String[] args) {
        // Extract symbol from args, default to BTCUSDT if not provided
        String symbol = args.length > 0 ? args[0].toUpperCase().trim() : "BTCUSDT";
        
        System.out.println("Fetching current price for: " + symbol + "...\n");

        try {
            // Using Binance public API for simple, fast, and accurate ticker quotes
            URI uri = URI.create("https://api.binance.com/api/v3/ticker/price?symbol=" + symbol);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                String responseBody = response.body();
                
                // Pure Java parsing using Regex to avoid external JSON dependencies
                Pattern pricePattern = Pattern.compile("\"price\"\\s*:\\s*\"([^\"]+)\"");
                Matcher matcher = pricePattern.matcher(responseBody);
                
                if (matcher.find()) {
                    String price = matcher.group(1);
                    // Format the output for readability
                    System.out.println("=========================================");
                    System.out.println(" Symbol : " + symbol);
                    System.out.println(" Price  : " + price);
                    System.out.println("=========================================");
                } else {
                    System.err.println("Could not parse the price from the response.");
                    System.err.println("Raw Response: " + responseBody);
                }
            } else {
                System.err.println("Failed to fetch price. HTTP Status Code: " + response.statusCode());
                // Handle common Binance API error (e.g., invalid symbol)
                if (response.statusCode() == 400) {
                    System.err.println("Hint: Make sure the trading pair symbol is correct (e.g., BTCUSDT, ETHUSDT).");
                }
                System.err.println("Response: " + response.body());
                System.exit(1);
            }

        } catch (InterruptedException e) {
            System.err.println("Request was interrupted.");
            Thread.currentThread().interrupt();
            System.exit(1);
        } catch (Exception e) {
            System.err.println("An error occurred while communicating with the API:");
            e.printStackTrace();
            System.exit(1);
        }
    }
}