# D Power_blockchain
Java blockchain platform, based on the blockchain platform developed by Springboot.
### Cause

Platforms such as Ethereum and Hyperledger are all sharing ledgers, with modules such as tokens and mining. What we need is several companies to form an alliance to jointly witness and record some non-tamperable interactive information. For example, if company A sends a xxx request to company B, what does company B respond to? In fact, what is needed is a distributed database, and the performance is good. It cannot generate a block in 10 minutes like Bitcoin. What we want is more the performance of the database, and some characteristics of the blockchain.

### Passing

The project started research and development in early March 20, and the first version was released in January. Mainly made storage module, encryption module, network communication, PBFT consensus algorithm, public key and private key, block content analysis and storage, etc. The basic characteristics of the blockchain have been initially possessed.

Now, it is suitable for more blockchain scenarios, not just ledgers and various useless tokens.


### project instruction
Mainly include storage module, network module, PBFT consensus algorithm, encryption module, block analysis and storage, etc.


### Storage module
Sql-like statements are stored in the block. The alliances pre-set the database table structure that meets the needs of the business scenario, and then set the operation permissions (ADD, UPDATE, DELETE) of each node on the table. In the future, each node can perform Sql statements according to their allowed permissions Compile and package it in the Block, and then broadcast it on the whole network, and wait for the whole network to verify the legality of the signature, authority and other information. If the Block is legal, it enters the PBFT consensus algorithm mechanism, and each node starts to execute in sequence according to the states of PrePrepare, Prepare, and Commit. After 2f+1 commits, it starts to generate new blocks locally. After the new block is generated, each node analyzes the content of the block and puts it into the database.

The scenarios are more extensive, and different table structures or multiple tables can be set to complete the storage of respective types of information. For example, product traceability, from manufacturers, transportation, distributors, consumers, etc., each link can perform ADD information operations on a product.

The storage uses the key-value database rocksDB. Those who understand Bitcoin know that Bitcoin uses levelDB, which are similar things. You can dynamically switch which database to use by modifying db.levelDB in yml to true and db.RocksDB to false.

The structure is similar to SQL statements, such as ADD (addition, deletion, modification), tableName (table name), ID (primary key), JSON (json of the record). The logic of the rollback is set here, that is, when you do an ADD operation, a Delete statement will be stored at the same time for possible future rollback operations.


### Network Module
At the network layer, each node is connected to each other for a long time, disconnected and reconnected, and then maintains the heartbeat packet. The network framework uses t-io, which is also a well-known open source project of oschina. t-io adopts the AIO method, which has excellent performance in the case of a large number of long connections, with little resource usage, and has the group function, which is particularly suitable for being a SaaS platform with multiple alliance chains. It also contains excellent functions such as heartbeat packet, disconnected reconnection, and retry.

In the project, each node is a server and a client. As a server, it is connected by other N-1 nodes, and as a client, it connects to the servers of other N-1 nodes. For the same alliance, set a Group, and call the sendGroup method directly every time you send a message.

However, it should be noted that since the project adopts the pbft consensus algorithm, in the process of reaching consensus, there will be network communication of the third power of N. When the number of nodes is large, if it has reached 100, the consensus will be Will bring a heavy burden to the network. This is a limitation of the algorithm itself.

### Consensus module PBFT

Distributed consensus algorithm is the core of distributed system. Common ones include Paxos, pbft, bft, raft, pow, etc. The common ones in blockchain are POW, POS, DPOS, pbft, etc.

Bitcoin uses POW proof of work, which requires a lot of resources to be hashed (mining), and miners complete the right to generate blocks. Others mostly use election voting to determine who will generate Block. The common feature is that only specific nodes can generate blocks and then broadcast to others.

Blockchain is divided into the following three categories:

Private chain: This refers to the blockchain application deployed within the enterprise. All nodes can be trusted, and there are no malicious nodes;

Consortium chain: a semi-closed ecological transaction network, where there are nodes with unequal trust, and there may be malicious nodes;

Public chain: an open ecological transaction network, providing a global transaction network for alliance chains and private chains.

Since the private chain is a closed ecological storage system, the Paxos consensus algorithm (more than half of the agreement) can achieve the best performance; the consortium chain has semi-open and semi-open characteristics, so Byzantine fault tolerance is one of the suitable choices, such as the IBM Hyperledger project ; For the public chain, the requirements of this consensus algorithm have exceeded the scope of ordinary distributed system construction, coupled with the characteristics of transactions, so more security considerations need to be introduced. So Bitcoin's POW is a very good choice.

What we can choose here are raft and pbft, which are private chain and consortium chain respectively. In the project, I used the modified pbft consensus algorithm.

Let's first understand pbft briefly:

(1) A leader is elected from the nodes of the entire network, and the new block is generated by the leader.

(2) Each node broadcasts the transaction sent by the client to the entire network. The master node will collect multiple transactions that need to be placed in the new block from the network and store them in a list, and broadcast the list to the entire network.

(3) After each node receives the transaction list, it executes these transactions according to the sorting simulation. After all transactions are executed, the hash digest of the new block is calculated based on the transaction result and broadcast to the entire network.

(4) If a node receives 2f (f is the tolerable number of Byzantine nodes) and the digests sent by other nodes are equal to itself, it will broadcast a commit message to the entire network.

(5) If a node receives 2f+1 (including itself) commit messages, it can submit a new block to the local blockchain and state database.

(6) The client receives a return of f + 1 success (even if there are f failures and then f maliciously returned error messages, f + 1 correct is also a majority) return, it can consider that the write request is successful.

It can be seen that the traditional pbft needs to elect a leader first, and then the leader collects transactions, packs them, and then broadcasts them. Then each node starts to verify, vote, accumulate the number of commits on the new block, and finally land.

And I made a modification to pbft here. This is an alliance, each node is equal, and the performance is high. So I don't want each node to generate an instruction and send it to other nodes, and then everyone elects a node to collect the instruction combination on the network and then generate a block. It is too complicated, and there is a potential failure of the leader node.

My modification to pbft is that there is no need to select a leader, any node can build a block, and then broadcast it across the network. When other nodes receive the Block request, they will enter the Pre-Prepare state, verify the format, hash, signature, and table permissions. After the verification is passed, they enter the Prepare state and broadcast the state across the network. When the number of Prepare for each node accumulated by itself is greater than 2f+1, it enters the commit state and broadcasts this state throughout the network. When the number of commits accumulated by each node is greater than 2f+1, it is considered that a consensus has been reached, the block is added to the blockchain, and the sql statement in the block is executed.

Obviously, compared with the leader, the concept of order is missing. When there is a leader, the order of the blocks can be guaranteed, and when there is a demand to become a block, the leader can broadcast in order. For example, everyone has reached the number=5 block, and then two more blocks need to be generated. When there is a leader, it will be generated in the order of 6, 7. When there is no leader, multiple nodes may generate 6 at the same time. In order to avoid the fork, I did some processing, and you can see the implementation logic in the code.

### Block information query

Each node implements a synchronized sqlite database (or other relational databases such as mysql) by executing the same SQL. In the future, all queries on the data will directly query sqlite, which has higher performance than traditional blockchain projects.

Since each node can generate blocks, inconsistent blocks will occur under high concurrency. If the chain is forked due to some reasons, a rollback mechanism is also provided, and SQL can be rolled back. The principle is also very simple. When you ADD a piece of data, I will record two instructions in the block at the same time, one is ADD and the other is DELETE for rollback. In the same way, the original old data will also be saved during UPDATE. The SQL in the block is landed, for example, 1-10 instructions are executed sequentially, and the rollback instruction is executed from 10-1 when rolling back.

Each node will record the value of the block that it has synchronized so that SQL can be stored in the database at any time.

The query of blockchain information is simple, just do database query directly. Compared with Bitcoin, which needs to retrieve the index tree of the entire blockchain, the speed and convenience are quite different.


In addition, in the case of high concurrency, each node generates Blocks at the same time, and the system processes consensus to ensure that the blockchain does not fork.

The interface of this generated block is written for testing. The normal process is to call the instuction interface, first produce instructions that meet your needs, and then combine multiple instructions to call the generated block interface in BlockController.