import java.security.*;
import java.util.Random;

public class Test {

    public static void main(String[] args) throws InvalidKeyException, NoSuchAlgorithmException, SignatureException {
        // txHandler holds all randomly generated transactions
        TxHandler txHandler;

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
        txHandler = new TxHandler(new UTXOPool(utxoPool));

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


        // Test BlockChain
        // scrooge creates the genesis block and therefore the start of the blockchain
        Block genesisBlock = new Block(null, scroogeKeyPair.getPublic());
        genesisBlock.finalize();
        BlockChain blockChain = new BlockChain(genesisBlock);
        BlockHandler blockHandler = new BlockHandler(blockChain);

        // First transaction spends more money than available
        // Block should be null
        Transaction tx1 = new Transaction();
        utxo = blockChain.getMaxHeightUTXOPool().getAllUTXO().get(0);
        tx1.addInput(utxo.getTxHash(), utxo.getIndex());
        tx1.addOutput(30.0, aliceKeyPair.getPublic());
        signatureScrooge.update(tx1.getRawDataToSign(0));
        tx1.addSignature(signatureScrooge.sign(), 0);
        tx1.finalize();
        blockChain.addTransaction(tx1);
        Block b1 = blockHandler.createBlock(aliceKeyPair.getPublic());
        if (b1 == null) {
            System.out.println("(Works) Block 1 has not been created due to too much spending.\n");
        } else {
            System.out.println("Block 1 has been created.\n");
        }

        // Second transaction is valid
        // Block should be created and added to blockchain
        Transaction tx2 = new Transaction();
        utxo = blockChain.getMaxHeightUTXOPool().getAllUTXO().get(0);
        tx2.addInput(utxo.getTxHash(), utxo.getIndex());
        tx2.addOutput(5.0, bobKeyPair.getPublic());
        signatureScrooge.update(tx2.getRawDataToSign(0));
        tx2.addSignature(signatureScrooge.sign(), 0);
        tx2.finalize();
        blockChain.addTransaction(tx2);
        Block b2 = blockHandler.createBlock(bobKeyPair.getPublic());
        if (b2 != null) {
            System.out.println("(Works) Block 2 has been created.\n");
        } else {
            System.out.println("Block 2 has not been created.\n");
        }
        if (blockChain.getMaxHeightBlock().equals(b2)) {
            System.out.println("(Works) Block 2 has been added to blockchain.\n");
        } else {
            System.out.println("Block 2 has not been added.\n");
        }

        // Block should not be created as Alice does not have coins
        Transaction tx3 = new Transaction();
        tx3.addInput(b2.getTransaction(0).getHash(), utxo.getIndex());
        tx3.addOutput(5.0, bobKeyPair.getPublic());
        Signature signatureAlice = Signature.getInstance("SHA256withRSA");
        signatureAlice.initSign(aliceKeyPair.getPrivate());
        signatureAlice.update(tx3.getRawDataToSign(0));
        tx3.addSignature(signatureAlice.sign(), 0);
        tx3.finalize();
        blockChain.addTransaction(tx3);
        Block b3 = blockHandler.createBlock(aliceKeyPair.getPublic());
        if (b3 == null) {
            System.out.println("(Works) Block 3 has not been created.\n");
        } else {
            System.out.println("Block 3 has been created.\n");
        }
        if (!blockChain.getMaxHeightBlock().equals(b3)) {
            System.out.println("(Works) Block 3 has not been added.\n");
        } else {
            System.out.println("Block 3 has been added to blockchain.\n");
        }

        Block b4 = new Block(b2.getHash(), aliceKeyPair.getPublic());
        Transaction tx4 = new Transaction();
        utxo = blockChain.getMaxHeightUTXOPool().getAllUTXO().get(0);
        tx4.addInput(utxo.getTxHash(), utxo.getIndex());
        tx4.addOutput(10.0, scroogeKeyPair.getPublic());
        Signature signatureBob = Signature.getInstance("SHA256withRSA");
        signatureBob.initSign(bobKeyPair.getPrivate());
        signatureBob.update(tx4.getRawDataToSign(0));
        tx4.addSignature(signatureBob.sign(), 0);
        tx4.finalize();
        b4.addTransaction(tx4);
        b4.finalize();
        if (blockHandler.processBlock(b4)) {
            System.out.println("(Works) Block 4 has been processed and added to blockchain.\n");
        } else {
            System.out.println("Block 4 has not been processed.\n");
        }
        if (blockChain.getMaxHeightBlock().equals(b4)) {
            System.out.println("(Works) Block 4 has been added to blockchain.\n");
        } else {
            System.out.println("Block 4 has not been added.\n");
        }

        // This should fail as more coins are spent than allowed
        Transaction tx5 = new Transaction();
        utxo = blockChain.getMaxHeightUTXOPool().getAllUTXO().get(0);
        tx5.addInput(utxo.getTxHash(), utxo.getIndex());
        tx5.addOutput(100.0, scroogeKeyPair.getPublic());
        signatureBob = Signature.getInstance("SHA256withRSA");
        signatureBob.initSign(bobKeyPair.getPrivate());
        signatureBob.update(tx5.getRawDataToSign(0));
        tx5.addSignature(signatureBob.sign(), 0);
        tx5.finalize();
        blockChain.addTransaction(tx5);
        Block b5 = blockHandler.createBlock(aliceKeyPair.getPublic());
        if (b5 == null) {
            System.out.println("(Works) Block 5 has not been created due to too much spending.\n");
        } else {
            System.out.println("Block 5 has been created.\n");
        }

        Block b = blockChain.getMaxHeightBlock();
        int height = 1;
        byte[] parentBlockHash = b.getPrevBlockHash();
        while (parentBlockHash != null) {
            height++;
            b = blockChain.getBlock(new ByteArrayWrapper(parentBlockHash));
            parentBlockHash = b.getPrevBlockHash();
        }
        if (height == 3) {
            System.out.println("(Works) Blockchain has been built as intended.\n");
        } else {
            System.out.println("Something went wrong building the blockchain.\n");
        }
    }
}