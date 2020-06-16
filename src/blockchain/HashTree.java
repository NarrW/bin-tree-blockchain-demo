package blockchain;

import java.util.ArrayList;
import java.util.HashMap;

public class HashTree {
	private DBConnection dbcon;
	private int numberOfLeaf;
	private int leafSquare;
	private int coefficient = -1;
	private int criteria = 0;

	
	public HashTree(DBConnection dbcon, int numberOfLeaf) {
		this.dbcon = dbcon;
		this.numberOfLeaf = numberOfLeaf;
		leafSquare = checkPowerOfTwo(numberOfLeaf);
	}

	public HashMap<String, Object> search(String treeIndex) {
		return dbcon.getBlockByTreeIndex(treeIndex);
	}

//	나누다가 2의 제곱수가 되면 그거의 1/2
	public HashMap<String, Object> getLeftSibling(String parentHash) {
		try {
//			System.out.println(parentHash);
			HashMap<String, Object> parentBlock = search(parentHash);
			int parentIndexNumber = (int)parentBlock.get(Blockchain.INDEX_NUMBER);
			
			int var = parentIndexNumber;
			
			while(checkPowerOfTwo(var + 1) == -1) {
				int highBin = getMostLeftHighOfBinary(var);
				double div = Math.pow(2, highBin);
				criteria += div;
				var %= div;
			}
			coefficient = var / 2; 
			int subtracted = parentIndexNumber - coefficient - 1;
//			System.out.println("subtracted: " + subtracted);
			HashMap<String, Object> leftSiblingBlock = dbcon.getBlockByIndexNumber(subtracted);
			
			return leftSiblingBlock;		
			
		} catch(NullPointerException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public long getCoefficient() {
		return coefficient;
	}
	
	public long getCriteria() {
		return criteria;
	}

	public ArrayList<String> getRootArray(int maxIndex) {
		ArrayList<String> res = new ArrayList<String>();
		
		int remainderIndex = maxIndex;
		int offset = 0;
		
		while(remainderIndex > 1) {
			int highRootBin = getMostLeftHighOfBinary(remainderIndex + 1);
			int highRoot = (int)Math.pow(2, highRootBin) - 1;
			remainderIndex %= highRoot;
			
			HashMap<String, Object> block = dbcon.getBlockByIndexNumber(offset + highRoot);
			String treeHash = (String)block.get(Blockchain.TREE_INDEX);
//			System.out.println("offset:" + offset);
//			System.out.println("highRoot:" + highRoot);
//			System.out.println("offset + highRoot:" + (offset + highRoot));
			if(treeHash.equals("-")) {
				res.add(Blockchain.makeBlockHash(block));
			}
			else {
//				System.out.println("highRoot:" + highRoot);
				offset += highRoot + 1;
				res.add(treeHash);
			}
		}
		
//		System.out.println("res.length:" + res.size());
		return res;
	}

	private int checkPowerOfTwo(int num) {
		num >>= leafSquare;
		int pow = leafSquare;
		while(true) {
			if(num == 1) break;
			else if(num % 2 == 1 && num != 1) return -1;
			else if(num == 0) return 0;
			else if(num < 0) return -1;
			num >>= 1;
			pow++;
		}
		
		return pow;
	}
	
	private int getMostLeftHighOfBinary(int num) {
		int mostLeftBinary = -1;
		while(num != 0) {
			num >>= 1;
			mostLeftBinary++;
		}
		
		return mostLeftBinary;
	}
	
	public int getMostRightTreeNode(int num) {
		int tmpNum = num;
		int offset = 0;
		while(!isPowerOfTwo(tmpNum)) {
			int mostLeft = (int) Math.pow(2, getMostLeftHighOfBinary(num));
			tmpNum -= mostLeft;
			offset += mostLeft;
		}
		return (int) Math.pow(2, getMostLeftHighOfBinary(num) + 1) - 1 + offset;
	}
	
	public int getRightRightTreeNode(int num) {
		System.out.println("num: " + num);
		int tmpNum = num;
		int offset = 0;
		while(!isPowerOfTwo(tmpNum + 1)) {
//			System.out.println("tmpNum: " + tmpNum);
			int mostLeft = (int) Math.pow(2, getMostLeftHighOfBinary(tmpNum));
			System.out.println("mostLeft: " + mostLeft);
			tmpNum -= mostLeft;
			offset += mostLeft;
		}
		System.out.println("offset: " + offset);
		System.out.println("tmpNum: " + tmpNum);
		System.out.println((int) Math.pow(2, getMostLeftHighOfBinary(tmpNum) + 1) - 1 + numberOfLeaf + offset);
		return (int) Math.pow(2, getMostLeftHighOfBinary(tmpNum) + 1) - 1 + numberOfLeaf + offset;
	}
	
	private boolean isMultipleOfLeafNumber(long num) {
		if(num % numberOfLeaf == 0) return true;
		else return false;
	}
	
	private String getLeavesHashSum(int indexNumber, String selfHash) {
		StringBuilder hashSum = new StringBuilder();
		for(int i = indexNumber - numberOfLeaf + 1; i < indexNumber; i++) {
			String hash = Blockchain.makeBlockHash(dbcon.getBlockByIndexNumber(i));
//			System.out.println(i + "/ " + hash);
			hashSum.append(hash);
		}
		hashSum.append(selfHash);
		
		SecurityManager sm = new SecurityManager();
		String res = sm.generateHash(hashSum.toString());
//		System.out.println(indexNumber + "} " + res);
		return res;
	}

	public String makeTreeHash(int currentIndexNumber, String selfHash) {
		int indexCal = currentIndexNumber + 1;
		if(!isMultipleOfLeafNumber(indexCal)) return "-";
		
		int acc = 0;
		int pow;
		do {
			pow = checkPowerOfTwo(indexCal);
			int tmp = (int)Math.pow(2, getMostLeftHighOfBinary(indexCal));

			indexCal %= tmp; 
			if(pow != -1 && pow >= leafSquare) {				
				int mult = numberOfLeaf;
				if(pow == leafSquare) {
					return getLeavesHashSum(currentIndexNumber, selfHash);
				} else {
					mult = numberOfLeaf * (int)Math.pow(2, pow - leafSquare - 1);
				}
				
				ArrayList<HashMap<String, Object>> treeBlocks = dbcon.getTreeBlock(mult, currentIndexNumber, acc);
				
				String hashSum = "";
				for(HashMap<String, Object> tb : treeBlocks) {
					String treeIndex = (String)tb.get(Blockchain.TREE_INDEX);
					if(treeIndex.equals("-")) return "-";
					hashSum += treeIndex;
				}
				hashSum += getBranchHashSum(acc + mult, currentIndexNumber, mult / 2, selfHash);

				SecurityManager sm = new SecurityManager();
//				System.out.println(currentIndexNumber + "! " + sm.generateHash(hashSum));

				return sm.generateHash(hashSum);
			}
			
			acc += tmp;
			
			if(indexCal < numberOfLeaf) return "-";
			
		} while(true);
	}
	
	public String getBranchHashSum(int fromIndex, int toIndex, int interval, String selfHash) {
		StringBuilder hashSum = new StringBuilder();
		int branch = fromIndex - 1 + interval;
		
		HashMap<String, Object> block = dbcon.getBlockByIndexNumber(branch);
		if(block != null) {
			hashSum.append(block.get(Blockchain.TREE_INDEX));
		}
		
		if(interval <= numberOfLeaf) {
			hashSum.append(getLeavesHashSum(toIndex, selfHash));
//			System.out.println(fromIndex + "] " + hashSum.toString());
		} else {
			int newInterval = interval / 2;
			int newFromIndex = fromIndex + interval;
			hashSum.append(getBranchHashSum(newFromIndex, toIndex, newInterval, selfHash));
		}
		
		SecurityManager sm = new SecurityManager();
		 
//		System.out.println(fromIndex + ": " + sm.generateHash(hashSum.toString()));
		return sm.generateHash(hashSum.toString());
	}
	
	public boolean isLeafNode(String treeIndex) {
		HashMap<String, Object> block = search(treeIndex);
		int indexNumber = (int)block.get(Blockchain.INDEX_NUMBER);
//		System.out.println("treeIndex: " + treeIndex);
//		System.out.println("indexNumber: " + indexNumber);
		int leaf;
		if(isPowerOfTwo(indexNumber + 1)) {
//			int mostLeft = (indexNumber < numberOfLeaf) ? (int)Math.pow(2, getMostLeftHighOfBinary(indexNumber)) : 0;
			int mostLeft = (int)Math.pow(2, getMostLeftHighOfBinary(indexNumber));
			leaf = indexNumber - mostLeft;
		} else {
			int mostLeft = (int)Math.pow(2, getMostLeftHighOfBinary(indexNumber));
			leaf = indexNumber - mostLeft;
		}
		
//		System.out.println("leaf: " + leaf);
		return leaf + 1 < numberOfLeaf;
	}
	
	public boolean isSemiLeafNode(String treeIndex) {
		HashMap<String, Object> block = search(treeIndex);
		int indexNumber = (int)block.get(Blockchain.INDEX_NUMBER);
		System.out.println("treeIndex: " + treeIndex);
		System.out.println("indexNumber: " + indexNumber);
		int leaf;
		if(isPowerOfTwo(indexNumber + 1)) {
//			int mostLeft = (indexNumber < numberOfLeaf) ? (int)Math.pow(2, getMostLeftHighOfBinary(indexNumber)) : 0;
			int mostLeft = (int)Math.pow(2, getMostLeftHighOfBinary(indexNumber));
			leaf = indexNumber - mostLeft;
		} else {
			int mostLeft = (int)Math.pow(2, getMostLeftHighOfBinary(indexNumber));
			leaf = indexNumber - mostLeft;
		}
		
		System.out.println("leaf: " + leaf);
		return leaf  < numberOfLeaf;
	}
	
	public int searchRightLeftSib(String treeIndex) {
		HashMap<String, Object> parent = search(treeIndex);
		int parentIndex = (int)parent.get(Blockchain.INDEX_NUMBER);
//		if(!isMultipleOfLeafNumber(parentIndex)) return -1;
		
		int criteria = 0;
		int leftSib = parentIndex;

		while(true) {
//			System.out.println("criteria: " + criteria);
//			System.out.println("leftSib: " + leftSib);
			if(leftSib + 1  <= numberOfLeaf * 2) {
				return -(criteria + leftSib) ;
			}
			if(isPowerOfTwo(leftSib + 1)) { 
				return criteria + ((leftSib + 1) / 2) + ((leftSib + 1) / 4) - 1;
			}
			
			int mostLeft = (int)Math.pow(2, getMostLeftHighOfBinary(leftSib));
			leftSib = leftSib - mostLeft;
			criteria += mostLeft;
		}
		
	}
	
	private boolean isPowerOfTwo(int number) {
		if (number % 2 != 0) {
			return false;
		} else {
			for (int i = 0; i <= number; i++) {
				if (Math.pow(2, i) == number) return true;
			}
		}
		return false;
	}
}
