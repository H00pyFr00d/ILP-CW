package uk.ed.ac.inf;

import uk.ac.ed.inf.ilp.data.Order;
import uk.ac.ed.inf.ilp.data.Restaurant;
import uk.ac.ed.inf.ilp.interfaces.OrderValidation;

import java.util.ArrayList;

public class OrderValidator implements OrderValidation {
    /**
     * @param orderToValidate 
     * @param definedRestaurants
     * @return
     */
    public Order validateOrder(Order orderToValidate, Restaurant[] definedRestaurants) {

        return null;
    }

    private static int luhnAlgorithm(int cardNumber) {
        // Convert cardNumber into an array of strings containing the digits
        ArrayList<Integer> cardList = new ArrayList<>();
        for (char c : String.valueOf(cardNumber).toCharArray()) {
            cardList.add(Integer.parseInt(String.valueOf(c)));
        }

        

        return 0;
    }
}

