import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

public class Main {

    public static void main(String[] args) {
        // Create two users
        PublicKey pk1 = generatePublicKey();
        if (pk1 == null) {
            System.exit(1);
        }
        PublicKey pk2 = generatePublicKey();
        if (pk2 == null) {
            System.exit(1);
        }

        // Set up the blockchain
        Block genesisBlock = new Block(null, pk1);
        genesisBlock.finalize();
        BlockChain blockChain = new BlockChain(genesisBlock);
        BlockHandler blockHandler = new BlockHandler(blockChain);

        // Create transactions and run the blockchain
        Transaction tx1 = new Transaction(10, pk2);
        tx1.addInput(genesisBlock.getHash(), 0);
        blockChain.addTransaction(tx1);
        blockHandler.createBlock(pk1);

        Transaction tx2 = new Transaction(10, pk2);
        tx2.addInput(blockChain.getMaxHeightBlock().getTransaction(0).getHash(), 0);
        blockChain.addTransaction(tx2);
        blockHandler.createBlock(pk1);

        Transaction tx3 = new Transaction(15, pk1);
        tx3.addInput(blockChain.getMaxHeightBlock().getTransaction(0).getHash(), 0);
        blockChain.addTransaction(tx3);
        blockHandler.createBlock(pk2);

        Transaction tx4 = new Transaction(5, pk2);
        tx4.addInput(blockChain.getMaxHeightBlock().getTransaction(0).getHash(), 0);
        blockChain.addTransaction(tx4);
        blockHandler.createBlock(pk1);

        Transaction tx5 = new Transaction(1, pk1);
        tx5.addInput(blockChain.getMaxHeightBlock().getTransaction(0).getHash(), 0);
        blockChain.addTransaction(tx5);
        blockHandler.createBlock(pk2);
    }

    private static PublicKey generatePublicKey() {
        KeyPairGenerator keyPairGen;
        try {
            keyPairGen = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
        keyPairGen.initialize(2048);
        KeyPair keyPair = keyPairGen.genKeyPair();
        return keyPair.getPublic();
    }
}