//DEPS com.fasterxml.jackson.core:jackson-databind:2.15.2

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

public class BTCChart {
    public static void main(String[] args) {
        String symbol = "BTCUSDT";
        String interval = "1d";
        int limit = 10;
        
        if (args.length > 0) {
            try {
                limit = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Fatal Error: Invalid limit argument provided.");
                System.exit(1);
            }
        }
        
        String apiUrl = String.format("https://api.binance.com/api/v3/klines?symbol=%s&interval=%s&limit=%d", symbol, interval, limit);

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", "Java/HttpClient-JbangScript")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("Fatal Error: Failed to fetch BTC prices. HTTP Status Code: " + response.statusCode());
                System.err.println("Response Body: " + response.body());
                System.exit(1);
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(response.body());

            if (!rootNode.isArray() || rootNode.size() < limit) {
                System.err.println("Fatal Error: Unexpected JSON response format or insufficient data.");
                System.exit(1);
            }

            double[] prices = new double[limit];
            for (int i = 0; i < limit; i++) {
                // In Binance klines API, index 4 is the Close price
                JsonNode kline = rootNode.get(i);
                prices[i] = kline.get(4).asDouble();
            }

            drawAsciiChart(prices, limit);

        } catch (InterruptedException e) {
            System.err.println("Fatal Error: Request was interrupted.");
            e.printStackTrace();
            Thread.currentThread().interrupt();
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Fatal Error: An exception occurred while fetching/parsing the BTC data.");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void drawAsciiChart(double[] prices, int limit) {
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;

        for (double p : prices) {
            if (p < min) min = p;
            if (p > max) max = p;
        }

        // Handle edge case where all prices are exactly the same
        if (max == min) {
            max = min + 1.0;
            min = min - 1.0;
        }

        System.out.printf("BTC/USDT - Last %d Days Close Prices%n", limit);
        System.out.println("===================================");
        for (int i = 0; i < prices.length; i++) {
            System.out.printf("Day %d: $%.2f%n", (prices.length - i - 1), prices[i]);
        }
        System.out.println();

        int height = 10;
        for (int r = height; r >= 0; r--) {
            double rowPrice = min + (max - min) * r / height;
            System.out.printf("%10.2f |", rowPrice);
            
            for (int i = 0; i < prices.length; i++) {
                int mappedRow = (int) Math.round((prices[i] - min) / (max - min) * height);
                if (mappedRow == r) {
                    System.out.print("  *  ");
                } else if (mappedRow > r) {
                    System.out.print("  |  ");
                } else {
                    System.out.print("     ");
                }
            }
            System.out.println();
        }

        // Draw X-axis
        System.out.print("-----------+");
        for (int i = 0; i < prices.length; i++) {
            System.out.print("-----");
        }
        System.out.println();

        // Draw X-axis Labels
        System.out.print("           |");
        for (int i = 0; i < prices.length; i++) {
            if (i == prices.length - 1) {
                System.out.print(" Tdy ");
            } else {
                int daysAgo = prices.length - i - 1;
                String label = "D-" + daysAgo;
                // Pad to exactly 5 characters to maintain grid alignment
                if (label.length() == 3) {
                    System.out.print(" " + label + " ");
                } else if (label.length() == 4) {
                    System.out.print(" " + label);
                } else {
                    System.out.print(label);
                }
            }
        }
        System.out.println();
    }
}