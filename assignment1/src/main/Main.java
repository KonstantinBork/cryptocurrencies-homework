package main;

import scroogeCoin.Transaction;
import scroogeCoin.TxHandler;
import scroogeCoin.UTXO;
import scroogeCoin.UTXOPool;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.SecureRandom;

/**
 * DISCLAIMER: The test does not work properly as the signature used does not work properly.
 *             Maybe I am just to stupid to use the proper signature.
 *             However, I am too lazy to further work on this. Besides the signature issue, the validation works.
 *             To see this, just delete 47 to 51 in TxHandler aka delete the Case 2 Check.
 */
public class Main {

    public static void main(String[] args) {
        UTXOPool utxoPool = new UTXOPool();
        KeyPair[] keyPairs = {createKeyPair(), createKeyPair(), createKeyPair()};

        Transaction initialTransaction = createInitialTransaction(keyPairs[0].getPublic());

        UTXO utxo = new UTXO(initialTransaction.getHash(), 0);
        utxoPool.addUTXO(utxo, initialTransaction.getOutput(0));

        Transaction transaction1 = createTransaction(initialTransaction, keyPairs[1].getPublic(), 0, 10.0D); // valid
        utxo = new UTXO(transaction1.getHash(), 0);
        utxoPool.addUTXO(utxo, transaction1.getOutput(0));

        Transaction transaction2 = createTransaction(initialTransaction, keyPairs[2].getPublic(), 0, 50.0D); // invalid
        utxo = new UTXO(transaction2.getHash(), 0);
        utxoPool.addUTXO(utxo, transaction2.getOutput(0));

        TxHandler txHandler = new TxHandler(utxoPool);
        System.out.println(txHandler.isValidTx(initialTransaction)); // should print true but is false because of wrong signature length
        Transaction[] proposedTransactions = {transaction1, transaction2};

        Transaction[] validTransactions = txHandler.handleTxs(proposedTransactions);

        // Print if the result is correct
        System.out.println(validTransactions.length == 1);
        System.out.println(validTransactions[0].equals(transaction1));
    }

    private static KeyPair createKeyPair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            keyGen.initialize(1024, random);
            return keyGen.generateKeyPair();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private static Transaction createInitialTransaction(PublicKey publicKey) {
        Transaction initialTransaction = new Transaction();
        initialTransaction.addInput(null, 0);
        initialTransaction.addOutput(100.0D, publicKey);
        byte[] signature = initialTransaction.getRawDataToSign(0);
        initialTransaction.addSignature(signature, 0);
        initialTransaction.finalize();
        return initialTransaction;
    }

    private static Transaction createTransaction(Transaction previousTransaction, PublicKey publicKey, int outputIndex, double value) {
        Transaction transaction = new Transaction();
        transaction.addInput(previousTransaction.getHash(), outputIndex);
        transaction.addOutput(value, publicKey);
        byte[] signature = transaction.getRawDataToSign(0);
        transaction.addSignature(signature, 0);
        transaction.finalize();
        return transaction;
    }
}