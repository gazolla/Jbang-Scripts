///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS com.github.mizosoft.methanol:methanol:1.8.1
//DEPS com.github.mizosoft.methanol:methanol-gson:1.8.1
//DEPS org.slf4j:slf4j-simple:2.0.12

import com.github.mizosoft.methanol.Methanol;
import com.github.mizosoft.methanol.MutableRequest;
import com.github.mizosoft.methanol.adapter.gson.GsonAdapterFactory;
import com.github.mizosoft.methanol.AdapterCodec;

import java.io.IOException;

public class WebApi {
    public static void main(String[] args) throws IOException, InterruptedException {

        var codec = AdapterCodec.newBuilder().basic()
                .decoder(GsonAdapterFactory.createDecoder())
                .build();
        var client = Methanol.newBuilder().adapterCodec(codec).build();
        var request = MutableRequest.GET("https://api.github.com/users/octocat");
        var response = client.send(request, GitHubUser.class);

        System.out.println("Nome: " + response.body().name());
        System.out.println("Companhia: " + response.body().company());
    }

    public record GitHubUser(String name, String company, String blog) {
    }
}
