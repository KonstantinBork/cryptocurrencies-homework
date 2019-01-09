import java.util.Arrays;
import java.util.List;

public class TxHandler {

    /**
     * Current collection of unspent transaction outputs
     */
    private UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current scroogeCoin.UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the scroogeCoin.UTXOPool(scroogeCoin.UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
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
        List<Transaction.Output> allTxOutputs = tx.getOutputs();
        boolean case3Matched = allTxOutputs.stream().distinct().count() == tx.numOutputs();
        if (!case3Matched) {
            return false;
        }

        boolean case1And4Matched = allTxOutputs.stream()
                .allMatch(output ->
                        utxoPool.contains(new UTXO(tx.getHash(), allTxOutputs.indexOf(output))) // check case 1
                                && output.value >= 0.0D); // check case 4
        if (!case1And4Matched) {
            return false;
        }

        List<Transaction.Input> allTxInputs = tx.getInputs();
        boolean case2Matched = allTxInputs.stream()
                .allMatch(input -> Crypto.verifySignature(tx.getOutput(input.outputIndex).address, tx.getRawTx(), input.signature));
        if (!case2Matched) {
            return false;
        }

        // At last, check if sum of input values are at least as big as output values (see case 5)
        return allTxOutputs.stream().mapToDouble(output -> output.value).sum()
                <= allTxInputs.stream().mapToDouble(input -> tx.getOutput(input.outputIndex).value).sum();
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

    public UTXOPool getUTXOPool() {
        return this.utxoPool;
    }
}