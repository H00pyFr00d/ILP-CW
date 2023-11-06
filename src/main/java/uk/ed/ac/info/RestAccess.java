package uk.ed.ac.info;

import com.google.gson.GsonBuilder;
import uk.ac.ed.inf.ilp.data.NamedRegion;
import uk.ac.ed.inf.ilp.data.Order;
import uk.ac.ed.inf.ilp.data.Restaurant;
import uk.ac.ed.inf.ilp.gsonUtils.LocalDateDeserializer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Objects;

/**
 * A class for allowing access to up-to-date information from the REST-API
 */
public class RestAccess {
    private final String url;

    public RestAccess(String url) {
        this.url = url;
    }

    /**
     * Attempts to access the isAlive endpoint to determine the API status
     *
     * @return: If the API can be accessed (no error, status code 200, and returning the expected result)
     */
    public boolean apiAlive() {
        try {
            // Create a new HTTP Client to access the API endpoint
            HttpClient client = HttpClient.newBuilder().build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(this.url + "isAlive"))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // If the status code is 200 and the body contains the string "true", the api is alive and well
            return Objects.equals(response.body(), "true") && response.statusCode() == 200;
        }
        // If at any point we encounter an error, there is something wrong with the API
        // We report the error and return false
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Attempts to access the restaurants endpoint to determine what restaurants are open
     *
     * @return: If the API is alive and there is no error, returns the list of restaurants open on the
     *          date specified on creation of the RestAccess object, otherwise gives null
     */
    public Restaurant[] getOpenRestaurants(LocalDate date) {
        // Check the API is alive, if it isn't throw an error and return null
        if (this.apiAlive()) {
            try {
                // Create a new HTTP Client to access the restaurants endpoint
                HttpClient client = HttpClient.newBuilder().build();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(this.url + "restaurants"))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                // Deserialize the response body as a list of Restaurant objects
                Restaurant[] allRestaurants = new GsonBuilder().create().fromJson(response.body(), Restaurant[].class);

                // Open restaurants are ones which contain the current day of the week in their openingDays list
                // We can filter the restaurants down to those which contain the day of the week
                return Arrays.stream(allRestaurants)
                        .filter(r -> Arrays.asList(r.openingDays()).contains(date.getDayOfWeek()))
                        .toArray(Restaurant[]::new);

            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        } else {
            System.err.println("API Dead, try again later");
            return null;
        }
    }

    /**
     * Attempts to access the restaurants endpoint to get the list of orders for the objects current date
     *
     * @return: If the API is alive and there is no error, returns the list obtained from the API filtered by date,
     *          otherwise throws an error and returns null
     */
    public Order[] getOrdersForDate(LocalDate date) {
        // Check the API is alive, if it isn't throw an error and return null
        if (this.apiAlive()) {
            try {
                String dateString = date.toString();

                // Create a new HTTP Client to access the orders endpoint
                HttpClient client = HttpClient.newBuilder().build();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(this.url + "orders/" + dateString))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                // Deserialize the response body as a list of Order objects, utilises LocalDateTypeAdapter
                return new GsonBuilder().registerTypeAdapter(LocalDate.class, new LocalDateDeserializer()).create().fromJson(response.body(), Order[].class);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        } else {
            System.err.println("API Dead, try again later");
            return null;
        }
    }

    /**
     * Attempts to access the restaurants endpoint to get the centralArea
     *
     * @return: If the API is alive and there is no error, return the centralArea obtained from the API,
     *          otherwise throws an error and returns null
     */
    public NamedRegion getCentralArea() {
        // Check the API is alive, if it isn't throw an error and return null
        if (this.apiAlive()) {
            try {
                 // Create a new HTTP Client to access the orders endpoint
                HttpClient client = HttpClient.newBuilder().build();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(this.url + "centralArea"))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                // Deserialize the response body as a NamedRegion
                return new GsonBuilder().create().fromJson(response.body(), NamedRegion.class);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        } else {
            System.err.println("API Dead, try again later");
            return null;
        }
    }

    /**
     * Attempts to access the restaurants endpoint to get the no-fly zones
     *
     * @return: If the API is alive and there is no error, return the list of no-fly zones obtained from the API,
     *          otherwise throws an error and returns null
     */
    public NamedRegion[] getNoFlyZones() {
        // Check the API is alive, if it isn't throw an error and return null
        if (this.apiAlive()) {
            try {
                // Create a new HTTP Client to access the orders endpoint
                HttpClient client = HttpClient.newBuilder().build();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(this.url + "noFlyZones"))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                // Deserialize the response body as a NamedRegion
                return new GsonBuilder().create().fromJson(response.body(), NamedRegion[].class);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        } else {
            System.err.println("API Dead, try again later");
            return null;
        }
    }
}
