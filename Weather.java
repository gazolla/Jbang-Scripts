///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 26+
//DEPS io.github.openfeign:feign-core:13.1
//DEPS io.github.openfeign:feign-jackson:13.1
//DEPS com.fasterxml.jackson.core:jackson-databind:2.16.1

import feign.Feign;
import feign.Param;
import feign.RequestLine;
import feign.jackson.JacksonDecoder;

// 1. Mapeamento do JSON para Records (Imutáveis e concisos)
record CurrentWeather(double temperature, double windspeed, int weathercode) {
}

record WeatherResponse(double latitude, double longitude, CurrentWeather current_weather) {
}

// 2. Interface de contrato com a API
interface WeatherClient {
        @RequestLine("GET /v1/forecast?latitude={lat}&longitude={lon}&current_weather=true")
        WeatherResponse getForecast(@Param("lat") double lat, @Param("lon") double lon);
}

void main() {
        // 3. Configuração do Cliente
        WeatherClient weatherApi = Feign.builder()
                        .decoder(new JacksonDecoder())
                        .target(WeatherClient.class, "https://api.open-meteo.com");

        try {
                // 4. Chamada tipada
                var lat = -15.8;
                var lon = -47.9;
                var data = weatherApi.getForecast(lat, lon);

                var cur = data.current_weather();

                System.out.println("------------------------------------");
                System.out.printf("☀️  Clima em Brasília (%.2f, %.2f)%n", data.latitude(), data.longitude());
                System.out.printf("🌡️  Temperatura: %.1f°C%n", cur.temperature());
                System.out.printf("💨 Vento: %.1f km/h%n", cur.windspeed());
                System.out.println("------------------------------------");

        } catch (Exception e) {
                System.err.println("Erro ao buscar clima: " + e.getMessage());
        }
}