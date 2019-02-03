package scroogeCoin;

import java.util.Arrays;
import java.util.List;

public class TxHandler_New {

    /**
     * Current collection of unspent transaction outputs
     */
    private UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current scroogeCoin.UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the scroogeCoin.UTXOPool(scroogeCoin.UTXOPool uPool)
     * constructor.
     */
    public TxHandler_New(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current scroogeCoin.UTXO pool,
     * (2) the signatures on each input of {@code tx} are valid,
     * (3) no scroogeCoin.UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     * values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        List<Transaction.Input> allTxInputs = tx.getInputs();
        List<Transaction.Output> allTxOutputs = tx.getOutputs();

        UTXOPool alreadySeenUTXOs = new UTXOPool();
        boolean case1Matched = allTxInputs.stream().allMatch(
                input -> {
                    UTXO utxo = new UTXO(input.prevTxHash, allTxInputs.indexOf(input));

                    // I did not think about already seen UTXOs
                    if (alreadySeenUTXOs.contains(utxo)) {
                        return false;
                    }
                    alreadySeenUTXOs.addUTXO(utxo, utxoPool.getTxOutput(utxo));

                    return utxoPool.contains(utxo);
                }
        );
        if (!case1Matched) {
            return false;
        }

        boolean case2Matched = allTxInputs.stream().allMatch(
                input -> {
                    UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
                    int inputIndex = allTxInputs.indexOf(input);
                    return input.signature != null
                            && Crypto.verifySignature(utxoPool.getTxOutput(utxo).address, tx.getRawDataToSign(inputIndex), input.signature);
                }
        );

        if (!case2Matched) {
            return false;
        }

        //boolean case3Matched = allTxOutputs.stream().distinct().count() == tx.numOutputs();
        //if (!case3Matched) {
        //    return false;
        //}

        boolean case4Matched = allTxOutputs.stream().allMatch(output -> output.value >= 0.0D); // check case 4
        if (!case4Matched) {
            return false;
        }

        // At last, check if sum of input values are at least as big as output values (see case 5)
        return allTxOutputs.stream().mapToDouble(output -> output.value).sum() <=
                allTxInputs.stream().mapToDouble(input -> {
                    UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
                    return utxoPool.getTxOutput(utxo).value;
                }).sum();
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current scroogeCoin.UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        Transaction[] validTransactions = Arrays.stream(possibleTxs)
                .filter(this::isValidTx) // Only get all valid transactions
                .toArray(Transaction[]::new); // Return the filtered transactions as an array

        // Update the UTXOPool
        Arrays.stream(validTransactions).forEach(tx -> {
            List<Transaction.Output> outputs = tx.getOutputs(); // get all outputs of the transaction
            outputs.forEach(output -> utxoPool.removeUTXO(new UTXO(tx.getHash(), outputs.indexOf(output)))); // Remove the transaction from the pool
        });

        // Return the valid transactions
        return validTransactions;
    }
}