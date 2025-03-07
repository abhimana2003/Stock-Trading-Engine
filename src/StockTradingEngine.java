import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.*;



public class StockTradingEngine {
    private static final int MAX_NUM_OF_TICKERS = 1024;
    private final AtomicReferenceArray<AtomicReference<Order>> buyingOrders;
    private final AtomicReferenceArray<AtomicReference<Order>> sellingOrders;
    /** 
    * Constructs a stock trading engine
    */
    public StockTradingEngine(){
        this.buyingOrders = new AtomicReferenceArray<>(MAX_NUM_OF_TICKERS);
        this.sellingOrders = new AtomicReferenceArray<>(MAX_NUM_OF_TICKERS);
        
        for (int i = 0; i < MAX_NUM_OF_TICKERS; i++) {
            buyingOrders.set(i, new AtomicReference<>(null));
            sellingOrders.set(i, new AtomicReference<>(null));
        }
    }

    /** converts the ticker/stock symbol into an index using hashing
    * @param tickerSymbol the ticker symbol of the Stock
    * @return the index that the ticker symbol hashs into in the orders arrays
    */
    public int getStockIndex(String tickerSymbol){
        return (Math.abs(tickerSymbol.hashCode())) % MAX_NUM_OF_TICKERS;
    }

    /** adds new order to respective array and linked list based on order type and ticker symbol
    * @param orderType the type of order (could be BUY or SELL)
    * @param tickerSymbol the ticker symbol of the Stock
    * @param quantity the number of shares
    * @param price the price for each share
    */
    public void addOrder(Order.Type orderType, String tickerSymbol, int quantity, float price){
        int stockIndex = getStockIndex(tickerSymbol);
        Order newOrder = new Order(orderType, tickerSymbol,quantity, price);

        if (orderType == Order.Type.BUY){
            addBuyOrder(stockIndex,newOrder);
        }
        else{
            addSellOrder(stockIndex,newOrder);
        }
        // After adding the order, try to match buy and sell orders
        matchOrders(stockIndex, tickerSymbol); 
    }

    /** adds new buy order in order to the buy order linked list for its ticker based on price and then time
    * @param index index that the ticker symbol hashs into 
    * @param order the buy order we are adding
    */
    private void addBuyOrder(int index, Order order){
        // keeps looping until it can succesfully and safely add order (to address race conditions)
        while (true){
            AtomicReference<Order> currentRef = buyingOrders.get(index);
            Order currentHead = currentRef.get();
            // if no buying orders for that ticker exist or if the order to be added should be placed before the currentOrder
            // when the order to be added should be at the head of the linked list for the buying orders
            if ((currentHead == null) || ((order.price > currentHead.price) || (order.price == currentHead.price && order.timestamp < currentHead.timestamp))){
                order.next.set(currentHead);
                if (currentRef.compareAndSet(currentHead, order)) {
                    break;
                }
            }
            else{
                Order previous = currentHead;
                Order currentOrder = currentHead.next.get();
                // traverse linked list for this ticker for the appropriate position
                while ( (currentOrder != null) && (order.price < currentOrder.price || (order.price == currentOrder.price && order.timestamp >= currentOrder.timestamp))){
                    previous = currentOrder;
                    currentOrder = currentOrder.next.get();
                }
                // insert the new order at that position
                order.next.set(currentOrder);
                if (previous.next.compareAndSet(currentOrder, order)) {
                    break;
                }

            }
        }
    }

    /** adds new sell order in order to the sell order linked list for its ticker based on price and then time
    * @param index index that the ticker symbol hashs into 
    * @param order the sell order we are adding
    */
    private void addSellOrder(int index, Order order){
        // keeps looping until it can succesfully and safely add order (to address race conditions)
        while (true){
            AtomicReference<Order> currentRef = sellingOrders.get(index);
            Order currentHead = currentRef.get();
            // if no selling orders for that ticker exist or if the order to be added should be placed before the currentOrder
            // when the order to be added should be at the head of the linked list for the selling orders
            if ((currentHead == null) || ((order.price < currentHead.price) || (order.price == currentHead.price && order.timestamp < currentHead.timestamp))){
                order.next.set(currentHead);
                if (currentRef.compareAndSet(currentHead, order)) {
                    break;
                }
            }
            else{
                Order previous = currentHead;
                Order currentOrder = currentHead.next.get();

                // traverse linked list for this ticker for the appropriate position
                while ( (currentOrder != null) && (order.price > currentOrder.price || (order.price == currentOrder.price && order.timestamp >= currentOrder.timestamp))){
                    previous = currentOrder;
                    currentOrder = currentOrder.next.get();
                }
                // insert the new order at that position
                order.next.set(currentOrder); 
                if (previous.next.compareAndSet(currentOrder, order)) {
                    break;
                }
            }
        }
    }

    /** checks for buy and sell matches for a specific ticker symbol
    * @param index index that the ticker symbol hashs into 
    * @param tickerSymbol ticker symbol
    */
    private void matchOrders(int index, String tickerSymbol) {
        while (true) {
            // Get current head atomic references of buy and sell order linked lists
            AtomicReference<Order> buyingHeadRef = buyingOrders.get(index);
            AtomicReference<Order> sellingHeadRef = sellingOrders.get(index);
            
            Order buyingHead = buyingHeadRef.get();
            Order sellingHead = sellingHeadRef.get();
            
            // If either list is empty no matches can be made so just break
            if (buyingHead == null || sellingHead == null) {
                break;
            }
            
            // buying price at the head is less than the selling price at the head so no match can be made
            else if (buyingHead.price < sellingHead.price) {
                break;
            }
            
            // if none of the above conditions are met match can be made
            int buyQuantity = buyingHead.quantity.get();
            int sellQuantity = sellingHead.quantity.get();
            
            if (buyQuantity <= 0 || sellQuantity <= 0) {
                // Order might have been matched by another thread so just remove the heads
                if (buyQuantity <= 0) {
                    if (buyingHeadRef.compareAndSet(buyingHead, buyingHead.next.get())){
                        continue;
                    }
                }
                if (sellQuantity <= 0) {
                    if (sellingHeadRef.compareAndSet(sellingHead, sellingHead.next.get())){
                        continue;
                    }  
                }
                // If removing the heads doesn't work just retry
                continue;
            }
            // calculate how much quantity can actually be matched
            int matchedQuantity = Math.min(buyQuantity, sellQuantity);
            
            // Try to update buy order quantity atomically
            int newBuyQuantity = buyQuantity - matchedQuantity;
            if (!buyingHead.quantity.compareAndSet(buyQuantity, newBuyQuantity)) {
                // if compare and set fails then another thread must have made changes so reset and retry 
                continue;
            }
            
            // Try to update sell order quantity atomically
            int newSellQuantity = sellQuantity - matchedQuantity;
            if (!sellingHead.quantity.compareAndSet(sellQuantity, newSellQuantity)) {
                // if compare and set fails then another thread must have made changes so reset and retry 
                buyingHead.quantity.compareAndSet(newBuyQuantity, buyQuantity);
                continue;
            }
            
            // Match successful, print the match
            System.out.println("Matched " + matchedQuantity + " shares of " + tickerSymbol + " at price " + sellingHead.price);

            // if the buy order at the head is fullfilled try to remove it atomically
            if (newBuyQuantity == 0) {
                buyingHeadRef.compareAndSet(buyingHead, buyingHead.next.get());
        
            }

            // if the sell order at the head is fullfilled try to remove it atomically
            if (newSellQuantity == 0) {
                sellingHeadRef.compareAndSet(sellingHead, sellingHead.next.get());
            }
        }
    }
    
    
    /** simulates concurrent trading using threads
    * @param maxNumOrders max number of orders each thread would be able to make 
    * @param numThreads number of threads to generate
    */
    public void simulateTrading(int maxNumOrders, int numThreads) {
        Random random = new Random();
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        String[] sampleTickers = {"AAPL", "GOOG", "TSLA", "AMZN", "MSFT"};
        
        // Create numThreads number of threads, each running concurrently
        for (int i = 0; i < numThreads; i++) {
            executor.execute(() -> {
                int tradesToPerform = random.nextInt(maxNumOrders) + 1;
    
                for (int j = 0; j < tradesToPerform; j++) {
                    Order.Type orderType = random.nextBoolean() ? Order.Type.BUY : Order.Type.SELL;
                    String ticker = sampleTickers[random.nextInt(sampleTickers.length)];
                    int quantity = random.nextInt(100) + 1;
                    float price = 100 + random.nextFloat() * 50;
    
                    System.out.println("Placing Order: " + orderType + " " + quantity + " shares of " + ticker + " at $" + price);
                    addOrder(orderType, ticker, quantity, price);
                }
            });
        }
        // make sure the executor shuts down 
        executor.shutdown();  
    }




}
