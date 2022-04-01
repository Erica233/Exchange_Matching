package ece568.awsome_exchange_matching;

import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class Transaction {
    private int transaction_id;
    private long time;
    private String symbol;
    private double amount;
    private String account_id;
    private double price;
    private String status;

    // constructor for insert
    public Transaction(String _symbol, double _amount, double _price, int _transaction_id) {
        symbol = _symbol;
        transaction_id = _transaction_id;
        amount = _amount;
        price = _price;
    }

    // constructor for query and cancel
    public Transaction(int _transaction_id, String _status, double _amount, Timestamp _time, double _price) {
        transaction_id = _transaction_id;
        status = _status;
        amount = _amount;
        String time_string = _time.toString();
        if (time_string.length() < 26){
            int i = 26 - time_string.length();
            while(i != 0){
                time_string += "0";
                i--;
            }
        }
        time_string = _time.toString()+"+0400";
        DateTimeFormatter dtf  = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSZ");
        ZonedDateTime zdt  = ZonedDateTime.parse(time_string,dtf);
        time = zdt.toInstant().toEpochMilli();
        price = _price;
    }

    public int getTransaction_id() {
        return transaction_id;
    }

    public String getStatus() {
        return status;
    }

    public double getAmount() {
        return amount;
    }

    public long getTime() {
        return time;
    }

    public double getPrice() {
        return price;
    }
}
