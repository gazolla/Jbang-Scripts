//JAVA 26+

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

void main() throws Exception {
    var uri = URI.create("https://api.github.com/zen");
    var request = HttpRequest.newBuilder(uri).GET().build();
    var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    System.out.println(response.body());
}