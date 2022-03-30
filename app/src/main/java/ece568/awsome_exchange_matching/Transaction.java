package ece568.awsome_exchange_matching;

import java.sql.Timestamp;

public class Transaction {
    private int transaction_id;
    private String status;
    private double amount;
    private Timestamp time;
    private double price;
    public Transaction(int _transaction_id, String _status, double _amount, Timestamp _time, double _price) {
        transaction_id = _transaction_id;
        status = _status;
        amount = _amount;
        time = _time;
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

    public Timestamp getTime() {
        return time;
    }

    public double getPrice() {
        return price;
    }
}

