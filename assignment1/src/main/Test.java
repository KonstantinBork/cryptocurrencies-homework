package main;

import scroogeCoin.*;

import java.security.*;
import java.util.Random;

public class Test {

    public static void main(String[] args) throws InvalidKeyException, NoSuchAlgorithmException, SignatureException {
        // txHandler holds all randomly generated transactions
        TxHandler_New txHandler;

        Transaction tx;
        Random random = new Random();
        int numBitsKeyPair = 512;
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(numBitsKeyPair);
        KeyPair scroogeKeyPair = keyPairGenerator.genKeyPair();
        KeyPair aliceKeyPair = keyPairGenerator.genKeyPair();
        KeyPair bobKeyPair = keyPairGenerator.genKeyPair();

        // Create initial UTXO-Pool with coins to be spend later
        UTXOPool utxoPool = new UTXOPool();
        int numInitialUTXOs = 20;
        int maxValueOutput = 20;

        for (int i = 0; i < numInitialUTXOs; i++) {
            tx = new Transaction();
            int numOutput = random.nextInt(maxValueOutput) + 1;

            for (int j = 0; j < numOutput; j++) {
                double value = random.nextDouble() + 1;
                tx.addOutput(value, scroogeKeyPair.getPublic());    // assign all coins to scrooge
            }
            tx.finalize();
            // add all outputs as Unspent Transaction Output
            for (int j = 0; j < numOutput; j++) {
                UTXO utxo = new UTXO(tx.getHash(), j);
                utxoPool.addUTXO(utxo, tx.getOutput(j));
            }
        }
        // txHandler is now initialized
        txHandler = new TxHandler_New(new UTXOPool(utxoPool));

        // Example Test: test isValidTx() with one valid transaction from scrooge to alice
        Transaction validTx = new Transaction();
        // choose an unspent transaction as input
        UTXO utxo = utxoPool.getAllUTXO().get(0);
        validTx.addInput(utxo.getTxHash(), utxo.getIndex());
        // assign whole input to alice
        double inputValue = utxoPool.getTxOutput(utxo).value;
        double outputValue = inputValue;
        validTx.addOutput(outputValue, aliceKeyPair.getPublic());
        // uncommenting the next line tests for doublespending within one transaction
        //validTx.addOutput(outputValue, bobKeyPair.getPublic());

        // sign transaction using scrooge's private key
        Signature signatureScrooge = Signature.getInstance("SHA256withRSA");
        signatureScrooge.initSign(scroogeKeyPair.getPrivate());
        signatureScrooge.update(validTx.getRawDataToSign(0));
        validTx.addSignature(signatureScrooge.sign(), 0);

        validTx.finalize();

        // check for validity
        if (txHandler.isValidTx(validTx)) {
            System.out.println("Transaction is valid.\n");
        } else {
            System.out.println("Transaction is invalid.\n");
        }

        Transaction[] acceptedTx = txHandler.handleTxs(new Transaction[]{validTx});
        if (acceptedTx.length == 1) {
            System.out.println("Transaction is accepted.\n");
        } else {
            System.out.println("Transaction is not accepted.\n");
        }
    }
}