package main;

import scroogeCoin.TxHandler;
import scroogeCoin.UTXOPool;

public class Main {

    public static void main(String[] args) {
        UTXOPool utxoPool = new UTXOPool();
        // TODO fill the pool

        TxHandler txHandler = new TxHandler(utxoPool);
        // TODO run some example transactions
    }
}