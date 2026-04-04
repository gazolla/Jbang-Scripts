///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS com.github.mizosoft.methanol:methanol:1.8.1
//DEPS com.github.mizosoft.methanol:methanol-gson:1.8.1

import com.github.mizosoft.methanol.Methanol;
import com.github.mizosoft.methanol.MutableRequest;
import com.github.mizosoft.methanol.adapter.gson.GsonAdapterFactory;
import com.github.mizosoft.methanol.AdapterCodec;

public class Weather {

        // 1. Mapeamento do JSON para Records (Imutáveis e concisos)
        record CurrentWeather(double temperature, double windspeed, int weathercode) {
        }

        record WeatherResponse(double latitude, double longitude, CurrentWeather current_weather) {
        }

        public static void main(String[] args) throws Exception {
                // 2. Configuração do Cliente Methanol com Gson
                var codec = AdapterCodec.newBuilder().basic()
                                .decoder(GsonAdapterFactory.createDecoder())
                                .build();

                var client = Methanol.newBuilder().adapterCodec(codec).build();

                // 3. Montagem da URL e Request
                var lat = -15.8;
                var lon = -47.9;
                // IMPORTANTE: Locale US para os pontos decimais (evita problemas com a API)
                var url = String.format(java.util.Locale.US,
                                "https://api.open-meteo.com/v1/forecast?latitude=%.2f&longitude=%.2f&current_weather=true",
                                lat, lon);

                var request = MutableRequest.GET(url);

                // 4. Chamada tipada com mapeamento automático pelo Gson
                var response = client.send(request, WeatherResponse.class);
                var data = response.body();
                var cur = data.current_weather();

                System.out.println("------------------------------------");
                System.out.printf("☀️  Clima em Brasília (%.2f, %.2f)%n", data.latitude(), data.longitude());
                System.out.printf("🌡️  Temperatura: %.1f°C%n", cur.temperature());
                System.out.printf("💨 Vento: %.1f km/h%n", cur.windspeed());
                System.out.println("------------------------------------");

        }
}