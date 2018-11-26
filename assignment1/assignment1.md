# Cryptocurrencies and Blockchain
## Sheet 1, submission by Konstantin Bork

### Assignment 1: Bitcoin Various Topics
1. A malicious ISP can launch such an attack, it just has to send a transaction to the blockchain at the same time the
user sends one. To succeed with the attack, the ISP can try to bribe the next node, which can belong to the ISP, to
extend the block containing the double spend.

2.
    a) To determine which block will end up on the consensus branch,  
    b) Blubb  
    c) B  
    d) She has not wasted her effort. In fact, she can confirm Minnies block and so tells other participants to trust this
    block.
      
3. The probability to find a block in the next 10 minutes is: Pr(Find block in next 10 minutes) = 1 - e^-1

4. f

### Assignment 2: Validation of transactions
#### scroogeCoin.TxHandler.java

    package scroogeCoin;
    
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
            Transaction[] validTransactions = (Transaction[]) Arrays.asList(possibleTxs).parallelStream()
                    .filter(this::isValidTx) // Only get all valid transactions
                    .toArray(); // Return the filtered transactions as an array
    
            // Update the UTXOPool
            Arrays.stream(validTransactions).forEach(tx -> {
                List<Transaction.Output> outputs = tx.getOutputs(); // get all outputs of the transaction
                outputs.forEach(output -> utxoPool.removeUTXO(new UTXO(tx.getHash(), outputs.indexOf(output)))); // Remove the transaction from the pool
            });
    
            // Return the valid transactions
            return validTransactions;
        }
    }
    
You can find the source code of the whole program in the src folder.