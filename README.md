# Stock-Trading-Engine
Anjali Bhimanadham

## Project Folders Structure
```
/Stock-Trading-Engine
  /src
    Main.java
    Order.java
    StockTradingEngine.java
  /bin
  /.vscode
  README.md
```

## Compilation and Execution

### **Compiling the Project**
* Need to be at project's root folder
To compile all the Java files in the src file, run this command:
```sh
javac -d bin src/*.java
```

### **Running the Simulation**
After compiling the project, you can run the stock trading engine simulator using:
```sh
java -cp bin Main
```

## File Descriptions
- **`Main.java`**: The main entry point for the simulation.
- **`Order.java`**: defines the Order class for buying and selling stock orders
- **`StockTradingEngine.java`**: defines the Stock Trading Engine class and all the methods for it, including addOrder and matchOrder



