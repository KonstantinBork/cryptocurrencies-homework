// Block Chain should maintain only limited block nodes to satisfy the functions
// You should not have all the blocks added to the block chain in memory 
// as it would cause a memory overflow.

import java.util.*;

public class BlockChain {

    public static final int CUT_OFF_AGE = 10;

    private Map<ByteArrayWrapper, Block> blockMap; // Stores all blocks of the blockchain
    private Map<ByteArrayWrapper, UTXOPool> unprocessedTransactionOutputs; // Stores all UTXOPools
    private TransactionPool globalTransactionPool; // TransactionPool available in the whole application
    private List<ByteArrayWrapper> highestBlocks; // List of all blocks which are on the top level

    /**
     * create an empty block chain with just a genesis block. Assume {@code genesisBlock} is a valid block
     */
    public BlockChain(Block genesisBlock) {
        // Generate a map in which all blocks are stored, and store the genesis block there
        blockMap = new HashMap<>();
        ByteArrayWrapper genesisBlockHash = new ByteArrayWrapper(genesisBlock.getHash());
        blockMap.put(genesisBlockHash, genesisBlock);

        // Initialise a list in which the hashes of all highest blocks are stored
        highestBlocks = new ArrayList<>();
        highestBlocks.add(genesisBlockHash);

        // Initialise a map which stores all unprocessed transaction outputs
        unprocessedTransactionOutputs = new HashMap<>();
        UTXO utxo = new UTXO(genesisBlock.getCoinbase().getHash(), 0);
        UTXOPool utxoPool = new UTXOPool();
        utxoPool.addUTXO(utxo, genesisBlock.getCoinbase().getOutput(0));
        unprocessedTransactionOutputs.put(genesisBlockHash, utxoPool);

        // Initialise the global transaction pool
        globalTransactionPool = new TransactionPool();
    }

    /**
     * Get the maximum height block
     */
    public Block getMaxHeightBlock() {
        return blockMap.get(getHighestBlockHash());
    }

    /**
     * Get the UTXOPool for mining a new block on top of max height block
     */
    public UTXOPool getMaxHeightUTXOPool() {
        return unprocessedTransactionOutputs.get(getHighestBlockHash());
    }

    /**
     * Gets the hash of the highest block in the blockchain.
     *
     * @return The first element in the list of highest blocks as it is the oldest one.
     */
    private ByteArrayWrapper getHighestBlockHash() {
        return highestBlocks.get(0);
    }

    /**
     * Get the transaction pool to mine a new block
     */
    public TransactionPool getTransactionPool() {
        return globalTransactionPool;
    }

    /**
     * Add {@code block} to the block chain if it is valid. For validity, all transactions should be
     * valid and block should be at {@code height > (maxHeight - CUT_OFF_AGE)}.
     *
     * <p>
     * For example, you can try creating a new block over the genesis block (block height 2) if the
     * block chain height is {@code <=
     * CUT_OFF_AGE + 1}. As soon as {@code height > CUT_OFF_AGE + 1}, you cannot create a new block
     * at height 2.
     *
     * @return true if block is successfully added
     */
    public boolean addBlock(Block block) {
        if (block.getPrevBlockHash() == null) {
            return false;
        }

        // Check if the previous block still is available
        ByteArrayWrapper prevBlockHash = new ByteArrayWrapper(block.getPrevBlockHash());
        Block previousBlock = blockMap.get(prevBlockHash);
        if (previousBlock == null) {
            return false;
        }

        // Validate the block transactions
        if (!validateBlockTransactions(block)) {
            return false;
        }

        // Add the block to the chain
        ByteArrayWrapper blockHash = new ByteArrayWrapper(block.getHash());
        blockMap.put(blockHash, block);

        // Remove block transactions from pool
        block.getTransactions().forEach(
                transaction -> globalTransactionPool.removeTransaction(transaction.getHash())
        );

        // Add a new UTXOPool to the added block
        addUtxoPoolToBlock(block);

        // If the parent block is on the highest level, remove it and add the new block to the list
        changeHighestBlock(blockHash, prevBlockHash);

        removeOldBlocks();

        return true;
    }

    /**
     * Add a transaction to the transaction pool
     */
    public void addTransaction(Transaction tx) {
        globalTransactionPool.addTransaction(tx);
    }

    /**
     * Validates the transactions of the given block
     * @param block The block with the transactions which should be validated
     * @return true if all transactions are valid, otherwise false
     */
    private boolean validateBlockTransactions(Block block) {
        UTXOPool utxoPool = unprocessedTransactionOutputs.get(new ByteArrayWrapper(block.getPrevBlockHash()));
        TxHandler txHandler = new TxHandler(utxoPool);
        List<Transaction> blockTransactions = block.getTransactions();
        Transaction[] validTXs = txHandler.handleTxs(blockTransactions.toArray(new Transaction[0]));

        if (validTXs.length == 0 || validTXs.length != blockTransactions.size()) {
            // Remove all invalid transactions from the pool so they are not used in future blocks
            List<Transaction> invalidTxs = new ArrayList<>(blockTransactions);
            invalidTxs.removeAll(Arrays.asList(validTXs));
            invalidTxs.forEach(
                    transaction -> globalTransactionPool.removeTransaction(transaction.getHash())
            );
            return false;
        }

        return true;
    }

    /**
     * Adds a UTXOPool to the added block
     * @param block The block to which the new UTXOPool should be added
     */
    private void addUtxoPoolToBlock(Block block) {
        // Add UTXOPool for the block
        UTXOPool utxoPool = new UTXOPool();

        // First, check if coinbase exists and add its output to the pool
        Transaction coinBase = block.getCoinbase();
        if (coinBase != null) {
            UTXO utxo = new UTXO(coinBase.getHash(), 0);
            utxoPool.addUTXO(utxo, coinBase.getOutput(0));
        }
        // Then, add the outputs from all transactions
        for (Transaction tx : block.getTransactions()) {
            for (int i = 0; i < tx.numOutputs(); i++) {
                UTXO utxo = new UTXO(tx.getHash(), i);
                utxoPool.addUTXO(utxo, tx.getOutput(i));
            }
        }
        unprocessedTransactionOutputs.put(new ByteArrayWrapper(block.getHash()), utxoPool);
    }

    /**
     * Changes the list of highest blocks
     *
     * @param blockHash     Hash of the current block
     * @param prevBlockHash Hash of the parent block
     */
    private void changeHighestBlock(ByteArrayWrapper blockHash, ByteArrayWrapper prevBlockHash) {
        if (highestBlocks.contains(prevBlockHash)) {
            if (highestBlocks.size() > 1) {
                highestBlocks = new ArrayList<>();
            } else {
                highestBlocks.remove(prevBlockHash);
            }
            highestBlocks.add(blockHash);
        }
    }

    /**
     * Removes all block which are above @code{CUT_OFF_AGE}.
     */
    private void removeOldBlocks() {
        int blockNumber = 0;
        for (ByteArrayWrapper blockHash : highestBlocks) {
            int i = 0;
            Block block = blockMap.get(blockHash); // Used to go down the blockchain
            while (block != null && block.getPrevBlockHash() != null) {
                ByteArrayWrapper prevBlockHash = new ByteArrayWrapper(block.getPrevBlockHash());
                block = blockMap.get(prevBlockHash);
                i++;

                // If the block is above the cut off age, remove it from the blockchain
                if (i > CUT_OFF_AGE) {
                    blockMap.remove(prevBlockHash);
                }
            }

            // Remove all UTXOPools for all previous maxHeightBlocks
            if (blockNumber > 0) {
                unprocessedTransactionOutputs.remove(blockHash);
            }
            blockNumber++;
        }
    }

    /**
     * Function for test purposes to access single blocks in the blockchain.
     * @param blockHash The hash of the requested block
     * @return The block with the given hash
     */
    public Block getBlock(ByteArrayWrapper blockHash) {
        return blockMap.get(blockHash);
    }
}