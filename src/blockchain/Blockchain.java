package blockchain;

import java.util.ArrayList;
import java.util.HashMap;

public class Blockchain {
	DBConnection dbcon;
	private static String uid = "root";
	private static String pwd = "dmdrk123";

	public static String ADDRESS = "address";
	public static String PREVIOUS_HASH = "previous_hash";
	public static String TRANSACTIONS = "transactions";
	public static String TREE_INDEX = "tree_index";
	public static String INDEX_NUMBER = "index_number";

	private HashTree hashTree;

	public Blockchain(String name) {
		dbcon = new DBConnection(uid, pwd, name);
		dbcon.initializeBlockchainDatabase();
		hashTree = new HashTree(dbcon, 8);
	}

	public int addNewBlock(String transactions) {
		int lastIndex = dbcon.getLastBlockIndexNumber();

		if(lastIndex == -1) {
			return -1;
		}

		int resIndex = lastIndex + 1;

		boolean isBlockAdded = dbcon.addBlock(transactions, "-", resIndex);
		if(!isBlockAdded) return -1;
		int lastBlock = dbcon.getLastBlockIndexNumber();
		if(lastBlock != resIndex) {
			return -2;
		}
		HashMap<String, Object> generatedBlock = dbcon.getBlockByIndexNumber(lastBlock);

		String treeIndex = hashTree.makeTreeHash(resIndex, makeBlockHash(generatedBlock));
		dbcon.updateTreeIndex(treeIndex, lastBlock);

		return resIndex;
	}

	public static String makeBlockHash(HashMap<String, Object> block) {
		StringBuilder sum = new StringBuilder();
		sum.append(block.get(ADDRESS));
		sum.append(block.get(PREVIOUS_HASH));
		sum.append(block.get(TRANSACTIONS));
		sum.append(block.get(INDEX_NUMBER));

		SecurityManager sm = new SecurityManager();
		return sm.generateHash(sum.toString());	
	}

	public HashTree getHashTree() {
		return hashTree;
	}

	public HashMap<String, Object> searchFaultBlockByBinaryTree(Blockchain trueBlockchain) {
		HashTree trueHashTree = trueBlockchain.getHashTree();
		ArrayList<String> faultStartHash = hashTree.getRootArray(dbcon.getLastBlockIndexNumber());
		ArrayList<String> trueStartHash = trueHashTree.getRootArray(dbcon.getLastBlockIndexNumber());
		for(int i = 0; i < trueStartHash.size(); i++) {
			String trueHash = trueStartHash.get(i);
			String faultHash = faultStartHash.get(i);
//			System.out.println(i + "/trueHash: " + trueHash);
//			System.out.println(i + "/faultHash: " + faultHash);
			if(trueHash.equals(faultHash)) continue;

			String trueErrorSib = trueHash;
			String faultErrorSib = faultHash;
			while(true) {
//				System.out.println("trueErrorSib: " + trueErrorSib);
//				System.out.println("faultErrorSib: " + faultErrorSib);
				String trueLeftSib = (String)trueHashTree.getLeftSibling(trueErrorSib).get(TREE_INDEX);
				String faultLeftSib = (String)hashTree.getLeftSibling(faultErrorSib).get(TREE_INDEX);

//				System.out.println("trueLeftSib: " + trueLeftSib);
//				System.out.println("faultLeftSib: " + faultLeftSib);
				boolean leftSibError = !trueLeftSib.equals(faultLeftSib);

				if(leftSibError) {
//					System.out.println("leftSibError: " + faultErrorSib);
					if(hashTree.isLeafNode(faultLeftSib)) {
						return hashTree.search(faultLeftSib);
					}
					trueErrorSib = trueLeftSib;
					faultErrorSib = faultLeftSib;
					continue;
				} else {
//					System.out.println("rightSibError: " + faultErrorSib);
					int trueSibIndexNumber = trueHashTree.searchRightLeftSib(trueErrorSib);
					int faultSibIndexNumber = hashTree.searchRightLeftSib(faultErrorSib);
					String rightRes = faultErrorSib;
					if(faultErrorSib.equals("-")) {
						for(int j = faultSibIndexNumber; j < dbcon.getLastBlockIndexNumber(); j++) {
							HashMap<String, Object> faultBlock = dbcon.getBlockByIndexNumber(j);
							String faultBlockHash = makeBlockHash(faultBlock);
							String trueBlockHash = makeBlockHash(trueBlockchain.getDbcon().getBlockByIndexNumber(j));
//							System.out.println("----j: " + j);
//							System.out.println("faultBlockHash: " + faultBlockHash);
//							System.out.println("trueBlockHash: " + trueBlockHash);
							if(!faultBlockHash.equals(trueBlockHash)) {
								return faultBlock;
							}
						}
					}

//					System.out.println("faultSibIndexNumber: " + faultSibIndexNumber);
					if(trueSibIndexNumber > 0 && faultSibIndexNumber > 0) {
						trueErrorSib = (String)trueBlockchain.getDbcon().getBlockByIndexNumber(trueSibIndexNumber).get(TREE_INDEX);
						faultErrorSib = (String)dbcon.getBlockByIndexNumber(faultSibIndexNumber).get(TREE_INDEX);
					} else {
//						System.out.println("trueSibIndexNumber: " + trueSibIndexNumber);
//						System.out.println("faultSibIndexNumber: " + faultSibIndexNumber);
						trueErrorSib = (String)trueBlockchain.getDbcon().getBlockByIndexNumber(-trueSibIndexNumber).get(TREE_INDEX);
						faultErrorSib = (String)dbcon.getBlockByIndexNumber(-faultSibIndexNumber).get(TREE_INDEX);
//						System.out.println("trueErrorSib: " + trueErrorSib);
//						System.out.println("faultErrorSib: " + faultErrorSib);
						if(!trueErrorSib.equals(faultErrorSib))
							return hashTree.search(rightRes);
						else {
//							System.out.println("right-1");
//							System.out.println("trueHashTree.getRightRightTreeNode(-faultSibIndexNumber): " + trueHashTree.getRightRightTreeNode(-faultSibIndexNumber));
							trueErrorSib = (String) trueBlockchain.getDbcon().getBlockByIndexNumber(trueHashTree.getRightRightTreeNode(-faultSibIndexNumber)).get(TREE_INDEX);
							faultErrorSib = (String) dbcon.getBlockByIndexNumber(hashTree.getRightRightTreeNode(-faultSibIndexNumber)).get(TREE_INDEX);
//							System.out.println("trueErrorSib: " + trueErrorSib);
//							System.out.println("faultErrorSib: " + faultErrorSib);
							if(!trueErrorSib.equals(faultErrorSib))
								return dbcon.getBlockByIndexNumber(hashTree.getRightRightTreeNode(-faultSibIndexNumber));
							else 
								return dbcon.getBlockByIndexNumber(hashTree.getMostRightTreeNode(-faultSibIndexNumber));
						}
					}
					if(hashTree.isSemiLeafNode(faultErrorSib)) {
						if(trueErrorSib.equals(faultErrorSib)) {
							return hashTree.search(rightRes);
						} else {
							return hashTree.search(faultErrorSib);
						}
					}
				}
			} // end while
			
		}	// end for

//		System.out.println("null return");
		return null;
	}
	
	public int checkLeaves(int start, int end, Blockchain trueChain) {
		DBConnection trueDbcon = trueChain.getDbcon();
		
		for(int i = start; i < end; i++) {
			String faultBlockHash = makeBlockHash(dbcon.getBlockByIndexNumber(i));
			String trueBlockHash = makeBlockHash(trueDbcon.getBlockByIndexNumber(i));
			if(!faultBlockHash.equals(trueBlockHash)) {
				return i;
			}
		}
		
		return -1;
	}
	
	public void renewHashTree() {
		for(int i = 0; i <= dbcon.getLastBlockIndexNumber(); i++) {
			String treeIndex = hashTree.makeTreeHash(i, makeBlockHash(dbcon.getBlockByIndexNumber(i)));
			dbcon.updateTreeIndex(treeIndex, i);
		}
	}
	
	public int nativeFaultSearch(Blockchain trueChain) {
		DBConnection trueDbcon = trueChain.getDbcon();
		HashMap<String, Object> faultBlock = dbcon.getBlockByIndexNumber(dbcon.getLastBlockIndexNumber());
		HashMap<String, Object> trueBlock = trueDbcon.getBlockByIndexNumber(trueDbcon.getLastBlockIndexNumber());

		String faultPrevHash = (String) faultBlock.get(PREVIOUS_HASH);
		String truePrevHash = (String) trueBlock.get(PREVIOUS_HASH);

		int res = dbcon.getLastBlockIndexNumber();
		while(true) {
			if(faultPrevHash == null) return res;
//			System.out.println(faultBlock.get(INDEX_NUMBER) + ": ");
//			System.out.println("[true] " + truePrevHash);
//			System.out.println("[fault] " + faultPrevHash);
			String faultBlockHash = makeBlockHash(faultBlock);
			String trueBlockHash = makeBlockHash(trueBlock);
			
			if(faultBlockHash.equals(trueBlockHash)) {
				faultBlock = dbcon.getBlockByHash(faultPrevHash);
				trueBlock = trueDbcon.getBlockByHash(truePrevHash);
//				System.out.println("[faultBlock] " + faultBlock);
//				System.out.println("[trueBlock] " + trueBlock);
				faultPrevHash = (String) faultBlock.get(PREVIOUS_HASH);
				truePrevHash = (String) trueBlock.get(PREVIOUS_HASH);
				try {
					res = (int)trueBlock.get(INDEX_NUMBER);
				} catch(NullPointerException e) {
					return res - 1;
				}
			} else {
//				System.out.println("not correspond here");
				return (int)faultBlock.get(INDEX_NUMBER);
			}
		}
	}
	
	public DBConnection getDbcon() {
		return dbcon;
	}
}
