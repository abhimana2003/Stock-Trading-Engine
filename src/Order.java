import java.util.concurrent.atomic.*;

/**
 * Represents a stock order
 */
public class Order {
    // defining the different types of order a user could have
    public enum Type {BUY, SELL}

    Type orderType;
    String tickerSymbol;
    AtomicInteger quantity;
    float price;
    long timestamp;
    AtomicReference<Order> next;

    /** Constructs a stock order with the specified parameters
    * @param orderType the type of order (could be BUY or SELL)
    * @param tickerSymbol the ticker symbol of the Stock
    * @param quantity the number of shares
    * @param price the price for each share
    * @param timestamp timestamp of when the order came in
    */
    public Order(Type orderType, String tickerSymbol, int quantity, float price){
        this.orderType = orderType;
        this.tickerSymbol = tickerSymbol;
        this.quantity = new AtomicInteger(quantity);
        this.price = price;
        this.timestamp =  System.currentTimeMillis(); ;
        this.next = new AtomicReference<>(null);
    }

    @Override
    public String toString() {
        return this.orderType + " " + this.tickerSymbol + " " + this.quantity.get() + " at " + this.price;
    }

}
