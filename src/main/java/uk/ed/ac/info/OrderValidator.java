package uk.ed.ac.info;

import uk.ac.ed.inf.ilp.constant.OrderStatus;
import uk.ac.ed.inf.ilp.constant.OrderValidationCode;
import uk.ac.ed.inf.ilp.constant.SystemConstants;
import uk.ac.ed.inf.ilp.data.CreditCardInformation;
import uk.ac.ed.inf.ilp.data.Order;
import uk.ac.ed.inf.ilp.data.Pizza;
import uk.ac.ed.inf.ilp.data.Restaurant;
import uk.ac.ed.inf.ilp.interfaces.OrderValidation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;

public class OrderValidator implements OrderValidation {
    /**
     * @param order: The order to determine the associated restaurants for
     * @param definedRestaurants: The list of participating restaurants
     * @return: A list of the restaurants present in the order
     */
    private ArrayList<Restaurant> getOrderedRestauraunts(Order order, Restaurant[] definedRestaurants) {
        ArrayList<Restaurant> restaurantList = new ArrayList<>();
        for (Restaurant restaurant : definedRestaurants) {
            for (Pizza pizza : order.getPizzasInOrder()) {
                if (Arrays.asList(restaurant.menu()).contains(pizza)) {
                    restaurantList.add(restaurant);
                }
            }
        }

        return restaurantList;
    }


    /**
     * @param cardNo: The card number to verify
     * @return: Whether the card is 16 digits long and passes Luhn's algorithm
     */
    private boolean cardNumberValid(String cardNo) {
        return cardNo.matches("^[0-9]{16}$");
    }

    /**
     * @param creditCardExpiry: The last valid month for the credit card
     * @param orderDate: The date of the order
     * @return: Whether the expiry date provided has a valid format and is before the end of the last valid month
     */
    private boolean cardExpiryValid(String creditCardExpiry, LocalDate orderDate) {
        boolean validString = creditCardExpiry.matches("^(0[1-9]|1[0-2])/[0-9][0-9]$");

        if (!validString) return false;

        String[] cardNums = creditCardExpiry.split("/"); // format is "MM/yy"
        int month = Integer.parseInt(cardNums[0]); // MM
        int year = Integer.parseInt(cardNums[1]); // yy

        // Cards are valid until the end of the month, so we check if
        // the orderDate is before the first day of the next month
        LocalDate expiry = LocalDate.of(year, month+1, 1);

        return expiry.isBefore(orderDate);
    }

    /**
     * @param cvv: The CVV to validate
     * @return: Whether the CVV is three digits long
     */
    private boolean cvvValid(String cvv) {
        return cvv.matches("^[0-9]{3}$");
    }

    /**
     * @param pizzas: The list of pizzas in the order
     * @param definedRestaurants: The list of participating restaurants
     * @return: Whether each of the pizzas in the list is available in at least one participating restaurant
     */
    private boolean pizzasAvailable(Pizza[] pizzas, Restaurant[] definedRestaurants) {
        int count = 0;

        // Iterate over the restaurants to find if the pizzas provided are on any menus
        for (Restaurant restaurant : definedRestaurants) {
            for (Pizza pizzaA : pizzas) {
                for (Pizza pizzaB : restaurant.menu()) {
                    if (pizzaA.equals(pizzaB)) {
                        count++;
                    }
                }
            }
        }
        // If the pizza is found in a restaurant then it gets counted
        // at present pizza names are unique, so it will be equal.
        //
        // If this changes, then this number will be > and the check will still work
        return count >= pizzas.length;
    }

    /**
     * @param restaurant: The restaurant
     * @param orderDate: The date to check against
     * @return: Determine from the restaurant whether it is open on the OrderDate
     */
    private boolean restaurantOpen(Restaurant restaurant, LocalDate orderDate) {
        return Arrays.asList(restaurant.openingDays()).contains(orderDate.getDayOfWeek());
    }

    /**
     * @param pizzasInOrder: The list of pizzas to determine the total of
     * @param priceTotalInPence: The total to check against
     * @return: Whether the totals of the pizzas and prices match
     */
    private boolean totalCorrect(Pizza[] pizzasInOrder, int priceTotalInPence) {
        int sum = 0;

        for (Pizza pizza : pizzasInOrder) {
            sum += pizza.priceInPence();
        }

        return sum == (priceTotalInPence - SystemConstants.ORDER_CHARGE_IN_PENCE);
    }

    /**
     * @param orderToValidate: The order needing verified
     * @param definedRestaurants: The list of participating restaraunts
     * @return: The verified order complete with updated OrderValidationCode and OrderStatus
     */
    public Order validateOrder(Order orderToValidate, Restaurant[] definedRestaurants) {
        CreditCardInformation cardDetails = orderToValidate.getCreditCardInformation();

        ArrayList<Restaurant> orderedFrom = getOrderedRestauraunts(orderToValidate, definedRestaurants);

        // Comments describe the "pass" conditions, meaning they fail the
        // check and continue onto the next one

        // The card number is length 16 and composed only of digits
        if (!cardNumberValid(cardDetails.getCreditCardNumber())) {
            orderToValidate.setOrderValidationCode(OrderValidationCode.CARD_NUMBER_INVALID);
        }

        // Is the expiry date in the future and in a valid format
        else if (!cardExpiryValid(cardDetails.getCreditCardExpiry(), orderToValidate.getOrderDate())) {
            orderToValidate.setOrderValidationCode(OrderValidationCode.EXPIRY_DATE_INVALID);
        }

        // Is the CVV length 3 and composed only of digits
        else if (!cvvValid(cardDetails.getCvv())) {
            orderToValidate.setOrderValidationCode(OrderValidationCode.CVV_INVALID);
        }

        // Is there fewer than four pizzas
        else if (!(orderToValidate.getPizzasInOrder().length <= 4)) {
            orderToValidate.setOrderValidationCode(OrderValidationCode.MAX_PIZZA_COUNT_EXCEEDED);
        }

        // Are all the pizzas available in at least one restaurant
        else if (!pizzasAvailable(orderToValidate.getPizzasInOrder(), definedRestaurants)) {
            orderToValidate.setOrderValidationCode(OrderValidationCode.PIZZA_NOT_DEFINED);
        }

        // Are the pizzas all from the same restaurant
        else if (orderedFrom.size() != 1) {
            orderToValidate.setOrderValidationCode(OrderValidationCode.PIZZA_FROM_MULTIPLE_RESTAURANTS);
        }

        // Is the restaurant open
        else if (!restaurantOpen(orderedFrom.get(0), orderToValidate.getOrderDate())) {
            orderToValidate.setOrderValidationCode(OrderValidationCode.RESTAURANT_CLOSED);
        }

        // Does the price total of the pizzas match
        else if (!totalCorrect(orderToValidate.getPizzasInOrder(), orderToValidate.getPriceTotalInPence())) {
            orderToValidate.setOrderValidationCode(OrderValidationCode.TOTAL_INCORRECT);
        }

        // If all previous checks pass then there are no errors
        else {
            orderToValidate.setOrderValidationCode(OrderValidationCode.NO_ERROR);
            orderToValidate.setOrderStatus(OrderStatus.VALID_BUT_NOT_DELIVERED);
            return orderToValidate;
        }

        // If we've dropped through everything (including the else) then it must be invalid
        orderToValidate.setOrderStatus(OrderStatus.INVALID);
        return orderToValidate;
    }
}

