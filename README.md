### README.md

[Hashed Binary Tree Generation and Fast Fraud Detection on Simple Blockchain Network]

- Demo Program Development
The demo program was developed using native Java. It has a minimal structure for this experiment and records blocks in a MySQL database. Each block has the structure of a linked list connected by hash, but numeric indexing was written in the order of creation to increase the search speed. Each block contains the information of Previous Hash, Address, Transactions, Tree Index, and Index Number as shown in Figure 1. The Previous Hash stores the value of the address of the previous block. The Address contains random string value of all information of the current block. Transactions store the transaction history of cryptocurrency. Tree Index stores hash value of tree structure. Its detailed definition method is described in section B. Index Number is defined as an integer that increases by 1 in the order of block creation.