# Cryptocurrencies and Blockchain
## Sheet 2, submission by Konstantin Bork

### Assignment 1: Bitcoin Various Topics
1. Step 4, 8, 9, 10, 11, 12  

2.  

### Extra question: more forking
1. The fork lasted for 24 blocks when one branch finally pulled ahead. Therefore, the 24 blocks of the other branch have
been abandoned. Source: https://bitcoinmagazine.com/articles/bitcoin-network-shaken-by-blockchain-fork-1363144448/

2. 94 blocks have been orphaned because of this soft-fork as they include an unconfirmed transaction which might include
an incorrect pay-to-script-hash. Source: https://bitcoin.stackexchange.com/questions/9678/what-is-script-hash-address-exactly-and-how-does-it-work

### Assignment 2: Transaction fees
1.  

2.  

3.  

### Assignment 3: Multi-signature wallet
1. It is one solution to create a new multi-signature wallet with both non-compromised keys and a new key. Then, all Bitcoins
are transfered from the old wallet to the new one. This transaction is signed by both old non-compromised keys. When the
transaction succeeds, the old wallet can be abandoned and is practically unusable for Mallory.
It is even more secure to create new private keys when creating the new wallet and revoke all old private keys. This
step minimises the risk that Mallory signs transactions from the old wallet in case of gaining the other keys, too.

2. At first, they should backup the seeds of both keys and store them in a secure place. The compromised and potentially
deleted key can then be restored from this seed. After restoring the keys, they should create a new wallet with new keys
and transfer all Bitcoins to the new wallet.

### Assignment 4: Node in a blockchain