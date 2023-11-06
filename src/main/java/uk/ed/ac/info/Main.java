package uk.ed.ac.info;

import uk.ac.ed.inf.ilp.constant.OrderStatus;
import uk.ac.ed.inf.ilp.data.LngLat;
import uk.ac.ed.inf.ilp.data.Order;
import uk.ac.ed.inf.ilp.data.Restaurant;

import java.time.LocalDate;
import java.util.*;

import static uk.ac.ed.inf.ilp.constant.OrderStatus.VALID_BUT_NOT_DELIVERED;

public class Main {
    public static void main(String[] args) {
        String url = "https://ilp-rest.azurewebsites.net/";
        LocalDate date = LocalDate.now().minusDays(2);

        LngLat APPLETON_TOWER = new LngLat(-3.186874, 55.944494);

//        try {
//            date = LocalDate.parse(args[0]);
//            url = args[1];
//        }
//        // A NullPointerException here would be because we tried to access an argument that wasn't provided
//        // Any other exception is just thrown as usual
//        catch(NullPointerException e) {
//            throw new RuntimeException("Incorrect number of arguments provided: should have been 2, was " + args.length);
//        }

        RestAccess restAccess = new RestAccess(url);
        OrderValidator orderValidator = new OrderValidator();

        Restaurant[] openRestaurants = restAccess.getOpenRestaurants(date);
        Order[] orders = restAccess.getOrdersForDate(date);
        PathRouter router = new PathRouter(restAccess.getCentralArea(), restAccess.getNoFlyZones());

        HashMap<Restaurant, ArrayList<PathNode>> restaurantPaths = new HashMap<>();
        HashMap<String, ArrayList<PathNode>> paths = new HashMap<>();

        System.out.println("Orders for " + date + ":");

        int counter = 1;
        for (Order order : orders) {
            order = orderValidator.validateOrder(order, openRestaurants);
            if (order.getOrderStatus() == VALID_BUT_NOT_DELIVERED) {
                try {
                    Restaurant orderedRestaurant = getRestaurant(order, openRestaurants);

                    if (restaurantPaths.containsKey(orderedRestaurant)) {
                        ArrayList<PathNode> orderPath = restaurantPaths.get(orderedRestaurant);

                        paths.put(order.getOrderNo(), orderPath);
                        Collections.reverse(orderPath);
                        paths.put(order.getOrderNo(), orderPath);

                    } else {
                        assert orderedRestaurant != null;
                        ArrayList<PathNode> path = router.getRoute(APPLETON_TOWER, orderedRestaurant.location());

                        restaurantPaths.put(orderedRestaurant, path);
                        System.out.println("> Route for restaurant " + orderedRestaurant.name() + " completed");

                        if (path != null) {
                            paths.put(order.getOrderNo(), path);
                            Collections.reverse(path);
                            paths.put(order.getOrderNo(), path);
                        }

                    }

                    order.setOrderStatus(OrderStatus.DELIVERED);
                    System.out.println("    > Route for order " + order.getOrderNo() + " completed [" + (counter++) + "/" + (orders.length) + "]");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        OutputProcessor outputProcessor = new OutputProcessor();

        outputProcessor.writeDeliveries("/deliveries-" + date + ".json", orders);
        outputProcessor.writeFlightpathJson("/flightpath-" + date + ".json", paths);
        outputProcessor.writePathGeoJson("/drone-" + date + ".geojson", paths);
    }

    /**
     * Get the restaurant for the order. When this is called we know the order is valid, so must only have
     * one restaurant as part of the order
     *
     * @param order: The order whose restaurant we want to determine
     * @param openRestaurants: The list of restaurants currently open
     * @return: The restaurant ordered from
     */
    private static Restaurant getRestaurant(Order order, Restaurant[] openRestaurants) {
        // For each of the restaurants,
        for (Restaurant restaurant : openRestaurants) {
            // If the restaurant menu contains the pizza, it must be the restaurant we've ordered from
            if (Arrays.asList(restaurant.menu()).contains(order.getPizzasInOrder()[0])) {
                return restaurant;
            }
        }
        // Shouldn't ever reach here, assuming the OrderValidator is doing its job
        return null;
    }
}
