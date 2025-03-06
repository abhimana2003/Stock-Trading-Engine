
/**
 * Represents a stock order
 */
public class Order {
    // defining the different types of order a user could have
    public enum Type {BUY, SELL}

    public Type orderType;
    public String tickerSymbol;
    public int quantity;
    public int price;

    /** Constructs a stock order with the specified parameters
    * @param orderType the type of order (could be BUY or SELL)
    * @param tickerSymbol the ticker symbol of the Stock
    * @param quantity the number of shares
    * @param price the price for each share
    */
    public Order(Type orderType, String tickerSymbol, int quantity, int price){
        this.orderType = orderType;
        this.tickerSymbol = tickerSymbol;
        this.quantity = quantity;
        this.price = price;
    }
}
