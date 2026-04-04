///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 26+
//DEPS com.github.mizosoft.methanol:methanol:1.8.1
//DEPS com.github.mizosoft.methanol:methanol-gson:1.8.1

import com.github.mizosoft.methanol.Methanol;
import com.github.mizosoft.methanol.MutableRequest;
import com.github.mizosoft.methanol.adapter.gson.GsonAdapterFactory;
import com.github.mizosoft.methanol.AdapterCodec;

void main() throws Exception {

        record CurrentWeather(double temperature, double windspeed, int weathercode) {
        }

        record WeatherResponse(double latitude, double longitude, CurrentWeather current_weather) {
        }

        var codec = AdapterCodec.newBuilder().basic()
                        .decoder(GsonAdapterFactory.createDecoder())
                        .build();

        var client = Methanol.newBuilder().adapterCodec(codec).build();
        var lat = -15.8;
        var lon = -47.9;
        var url = String.format(java.util.Locale.US,
                        "https://api.open-meteo.com/v1/forecast?latitude=%.2f&longitude=%.2f&current_weather=true",
                        lat, lon);

        var request = MutableRequest.GET(url);
        var response = client.send(request, WeatherResponse.class);
        var data = response.body();
        var cur = data.current_weather();

        System.out.println("------------------------------------");
        System.out.printf("☀️  Clima em Brasília (%.2f, %.2f)%n", data.latitude(), data.longitude());
        System.out.printf("🌡️  Temperatura: %.1f°C%n", cur.temperature());
        System.out.printf("💨 Vento: %.1f km/h%n", cur.windspeed());
        System.out.println("------------------------------------");

}
