package demo;

import blockchain.Blockchain;
import blockchain.SecurityManager;

public class SimpleBlockchainLocalNet {

	public static void main(String[] args) { 
		Blockchain chainA = new Blockchain("chain_a");
		
//		for(int i = 0; i < 99; i++) {
//			chainA.addNewBlock(new SecurityManager().generateHash(i + "t"));
//		}
		
		Blockchain chainB = new Blockchain("chain_b");
		chainA.renewHashTree();
		
// LINKED LIST
		long start = System.currentTimeMillis();
		int fault = chainA.nativeFaultSearch(chainB);
		long finish = System.currentTimeMillis();
		long timeElapsed = finish - start;
		System.out.println("fault:" + fault);
		System.out.println("timeElapsed:" + timeElapsed);
		
		
// TREE
//		long start = System.currentTimeMillis();
//		try {
//			int res = (int) chainA.searchFaultBlockByBinaryTree(chainB).get(Blockchain.INDEX_NUMBER);
//
//			int fault = chainA.checkLeaves(res - 7, res, chainB);
//			
//			long finish = System.currentTimeMillis();
//			long timeElapsed = finish - start;
//			System.out.println("fault:" + fault);
//			System.out.println("timeElapsed:" + timeElapsed);
//			
//		} catch (NullPointerException e) {
//			System.out.println("correspond");
//		}
	}
}
