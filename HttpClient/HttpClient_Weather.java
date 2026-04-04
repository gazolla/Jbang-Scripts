///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 26+
//DEPS com.google.code.gson:gson:2.10.1

import com.google.gson.Gson;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

void main() throws Exception {

    record CurrentWeather(double temperature, double windspeed, int weathercode) {
    }

    record WeatherResponse(double latitude, double longitude, CurrentWeather current_weather) {
    }

    var gson = new Gson();
    var client = HttpClient.newHttpClient();

    var lat = -15.8;
    var lon = -47.9;
    var url = String.format(java.util.Locale.US,
            "https://api.open-meteo.com/v1/forecast?latitude=%.2f&longitude=%.2f&current_weather=true",
            lat, lon);

    var request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET()
            .build();

    var response = client.send(request, HttpResponse.BodyHandlers.ofString());
    var data = gson.fromJson(response.body(), WeatherResponse.class);
    var cur = data.current_weather();

    System.out.println("------------------------------------");
    System.out.printf("☀️  Clima em Brasília (%.2f, %.2f)%n", data.latitude(), data.longitude());
    System.out.printf("🌡️  Temperatura: %.1f°C%n", cur.temperature());
    System.out.printf("💨 Vento: %.1f km/h%n", cur.windspeed());
    System.out.println("------------------------------------");
}
