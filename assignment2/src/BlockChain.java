// Block Chain should maintain only limited block nodes to satisfy the functions
// You should not have all the blocks added to the block chain in memory 
// as it would cause a memory overflow.

import java.util.*;

public class BlockChain {

    public static final int CUT_OFF_AGE = 10;

    private Map<ByteArrayWrapper, Block> blockMap; // Stores all blocks of the blockchain
    private Map<ByteArrayWrapper, UTXOPool> unprocessedTransactionOutputs; // Stores all UTXOPools
    private TransactionPool globalTransactionPool;
    private List<ByteArrayWrapper> highestBlocks;

    /**
     * create an empty block chain with just a genesis block. Assume {@code genesisBlock} is a valid
     * block
     */
    public BlockChain(Block genesisBlock) {
        blockMap = new HashMap<>();
        ByteArrayWrapper genesisBlockHash = new ByteArrayWrapper(genesisBlock.getHash());
        blockMap.put(genesisBlockHash, genesisBlock);

        highestBlocks = new ArrayList<>();
        highestBlocks.add(genesisBlockHash);

        unprocessedTransactionOutputs = new HashMap<>();
        unprocessedTransactionOutputs.put(genesisBlockHash, new UTXOPool());

        globalTransactionPool = new TransactionPool();
        highestBlocks = new ArrayList<>();
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
        ByteArrayWrapper blockHash = new ByteArrayWrapper(block.getHash());
        UTXOPool utxoPool = unprocessedTransactionOutputs.get(blockHash);
        TxHandler txHandler = new TxHandler(utxoPool);
        List<Transaction> blockTransactions = block.getTransactions();
        Transaction[] validTXs = txHandler.handleTxs(blockTransactions.toArray(new Transaction[0]));
        if (validTXs.length != blockTransactions.size()) {
            return false;
        }

        // Add the block to the chain
        blockMap.put(blockHash, block);

        // Remove block transactions from pool
        block.getTransactions().forEach(
                transaction -> globalTransactionPool.removeTransaction(transaction.getHash())
        );

        // If the parent block is on the highest level, remove it and add the new block to the list
        if (highestBlocks.contains(prevBlockHash)) {
            if (highestBlocks.size() > 1) {
                highestBlocks = new ArrayList<>();
            } else {
                highestBlocks.remove(prevBlockHash);
            }
            highestBlocks.add(blockHash);
        }

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
     * Removes all block which are above @code{CUT_OFF_AGE}.
     */
    private void removeOldBlocks() {
        int blockNumber = 0;
        for (ByteArrayWrapper blockHash : highestBlocks) {
            int i = 0;
            Block block = blockMap.get(blockHash); // Used to go down the blockchain
            while (block != null) {
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
}