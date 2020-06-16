package blockchain;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

public class DBConnection {
	private static String DB_NAME_MYSQL = "org.mariadb.jdbc.Driver";
	private String DEFAULT_URL = "jdbc:mysql://localhost:3306/blockchain"; //"jdbc:hsqldb:file:db/testnet; ifexists=false; shutdown=true;";
	public String dbUrl = DEFAULT_URL;
	
	String uid, pwd, bcName;
	boolean isDbOpen;
	private Connection con = null;

	private static String ADDRESS = Blockchain.ADDRESS;
	private static String PREVIOUS_HASH = Blockchain.PREVIOUS_HASH;
	private static String TRANSACTIONS = Blockchain.TRANSACTIONS;
	private static String TREE_INDEX = Blockchain.TREE_INDEX;
	private static String INDEX_NUMBER = Blockchain.INDEX_NUMBER;
	
	public static String WRONG_RETURN = "-1";
	
	
	public DBConnection(String uid, String pwd, String bcName) {
		this.isDbOpen = false;
		this.bcName = bcName;
		initDBConnector(uid, pwd);
	}
	
	public void setDatabaseUrl(String url) {
		this.dbUrl = url;
	}
	
	private void initDBConnector(String uid, String pwd) {
		String dbname = DB_NAME_MYSQL;

		try {
			Class.forName(dbname);
			this.uid = uid;
			this.pwd = pwd;
			if(!isDbOpen) {
				con = DriverManager.getConnection(dbUrl, uid, pwd);
				con.setHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT);
			}

			if(con != null) {
				isDbOpen = true;
			} else {
				isDbOpen = false;
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
	}
	
	public boolean sql(PreparedStatement[] pstmts){
		if(!isDbOpen) return false;
		
		try {
			for(PreparedStatement pstmt : pstmts) {
				pstmt.execute();
				pstmt.close();
			}
			con.commit();
			return true;
		} catch (SQLException e) { 
			e.printStackTrace();
			return false;
		} 
		
	}
	
	public ResultSet sqlResponseString(PreparedStatement pstmt) {
		if(!isDbOpen) return null;
		ResultSet rs = null;
		try {
			rs = pstmt.executeQuery();
			pstmt.close();
			con.commit();
			return rs;
		} catch (SQLException e) { 
			e.printStackTrace();
			return null;
		}	
	}
	
	public boolean initializeBlockchainDatabase() {
		boolean result = false;

//		System.out.println("init");
		try {
			String statement1 = "create table IF NOT EXISTS " + bcName + " ("
					+ ADDRESS + " CHAR(36) UNIQUE NOT NULL, "
					+ PREVIOUS_HASH + " CHAR(64) DEFAULT '-', "
					+ TRANSACTIONS + " VARCHAR(2048) NOT NULL, "
					+ TREE_INDEX + " CHAR(64), "
					+ INDEX_NUMBER + " INT UNIQUE NOT NULL, "
					+ "PRIMARY KEY (" + ADDRESS + ") "
					+ ");";
			String statement2 = "INSERT INTO " + bcName
					+ " (" + ADDRESS + ", " + TRANSACTIONS + ", " + TREE_INDEX + ", " + INDEX_NUMBER + ") SELECT UUID(), '-', '-', '0' FROM DUAL"
					+ " WHERE NOT EXISTS  (SELECT * FROM " + bcName + " WHERE " + INDEX_NUMBER + "=0) LIMIT 1;";
			PreparedStatement pstmt1 = con.prepareStatement(statement1);
			PreparedStatement pstmt2 = con.prepareStatement(statement2);
			result = sql(new PreparedStatement[] {pstmt1, pstmt2});
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	public boolean updateTreeIndex(String treeIndex, int indexNumber) {
		boolean result = false;
		String statement;

		try {
			statement = "UPDATE " + bcName + " SET " + TREE_INDEX + "=? WHERE " + INDEX_NUMBER + "=?;";
			PreparedStatement pstmt = con.prepareStatement(statement);
			pstmt.setString(1, treeIndex);
			pstmt.setInt(2, indexNumber);
			result = sql(new PreparedStatement[] {pstmt});
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	public boolean addBlock(String transactions, String treeIndex, int indexNumber) {
		boolean result = false;
		try {
			String concat = concatBlock(indexNumber - 1);
//			System.out.println("concat: " + concat);
			String statement = "INSERT INTO " + bcName
				+ " (" + ADDRESS + ", " + TRANSACTIONS + ", " + TREE_INDEX + "," + INDEX_NUMBER + "," + PREVIOUS_HASH + ") "
				+ "VALUES ("
				+ "UUID(), ?, ?, ?, SHA2(?, 256));";
			PreparedStatement pstmt = con.prepareStatement(statement);
			pstmt.setString(1, transactions);
			pstmt.setString(2, treeIndex);
			pstmt.setInt(3, indexNumber);
			pstmt.setString(4, concat);
			result = sql(new PreparedStatement[] {pstmt});
			con.commit();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		
		return result;
	}
	
	public String concatBlock(int indexNumber) {
		String result = WRONG_RETURN;
		try {
//	    	SET hashed = SHA2(CONCAT(pPassword, pLastLogin), 256);
			String statement = "SELECT GROUP_CONCAT(DISTINCT " + ADDRESS + ", " + TRANSACTIONS + "," + INDEX_NUMBER + "," + PREVIOUS_HASH + " SEPARATOR '') AS res"
					+ " FROM " + bcName + " WHERE " + INDEX_NUMBER + "=?;";
			PreparedStatement pstmt = con.prepareStatement(statement);
			pstmt.setInt(1, indexNumber);
			ResultSet rs = sqlResponseString(pstmt);
			if(rs.next()) {
				result = rs.getString("res");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		
		return result;
	}
	
	public int getLastBlockIndexNumber() {
		String statement = "SELECT MAX(" + INDEX_NUMBER + ") FROM " + bcName +" ;";
		int result = -1;
		try {
			PreparedStatement pstmt = con.prepareStatement(statement);
			ResultSet resSet = sqlResponseString(pstmt);
			while(resSet.next()) {
				result = resSet.getInt(1);
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	public HashMap<String, Object> getBlockByTreeIndex(String treeIndex) {
		String statement = "SELECT * FROM " + bcName +" WHERE " + TREE_INDEX + "=? ;";
//		System.out.println(statement);
		HashMap<String, Object> result = new HashMap<>();
		try {
			PreparedStatement pstmt = con.prepareStatement(statement);
			pstmt.setString(1, treeIndex);
			ResultSet resSet = sqlResponseString(pstmt);
			while(resSet.next()) {
				result.put(ADDRESS, resSet.getString(ADDRESS));
				result.put(TRANSACTIONS, resSet.getString(TRANSACTIONS));
				result.put(TREE_INDEX, resSet.getString(TREE_INDEX));
				result.put(INDEX_NUMBER, resSet.getInt(INDEX_NUMBER));
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	public HashMap<String, Object> getBlockByIndexNumber(int indexNumber) {
		String statement = "SELECT * FROM " + bcName +" WHERE " + INDEX_NUMBER + "=? ;";
		HashMap<String, Object> result = new HashMap<>();
		try {
			PreparedStatement pstmt = con.prepareStatement(statement);
			pstmt.setInt(1, indexNumber);
			ResultSet resSet = sqlResponseString(pstmt);
			while(resSet.next()) {
				result.put(ADDRESS, resSet.getString(ADDRESS));
				result.put(PREVIOUS_HASH, resSet.getString(PREVIOUS_HASH));
				result.put(TRANSACTIONS, resSet.getString(TRANSACTIONS));
				result.put(TREE_INDEX, resSet.getString(TREE_INDEX));
				result.put(INDEX_NUMBER, resSet.getInt(INDEX_NUMBER));
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	public HashMap<String, Object> getBlockByHash(String hash) {
		String statement = "SELECT * FROM " + bcName + " "
				+ "WHERE SHA2(CONCAT(address, transactions, index_number, previous_hash), 256)=?;";
		HashMap<String, Object> result = new HashMap<>();
		try {
			PreparedStatement pstmt = con.prepareStatement(statement);
			pstmt.setString(1, hash);
			ResultSet resSet = sqlResponseString(pstmt);
			while(resSet.next()) {
				result.put(ADDRESS, resSet.getString(ADDRESS));
				result.put(PREVIOUS_HASH, resSet.getString(PREVIOUS_HASH));
				result.put(TRANSACTIONS, resSet.getString(TRANSACTIONS));
				result.put(TREE_INDEX, resSet.getString(TREE_INDEX));
				result.put(INDEX_NUMBER, resSet.getInt(INDEX_NUMBER));
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	public ArrayList<HashMap<String, Object>> getTreeBlock(int mult, int currentIndexNumber, int acc) {
		String statement = "SELECT * FROM " + bcName +" WHERE " + INDEX_NUMBER + " < ? AND " + INDEX_NUMBER + " >= ?"
				+ " AND MOD(" + Blockchain.INDEX_NUMBER + "+1, ?)=0" + ";";
		
		ArrayList<HashMap<String, Object>> result = new ArrayList<>();
		try {
			PreparedStatement pstmt = con.prepareStatement(statement);
			pstmt.setInt(1, currentIndexNumber);
			pstmt.setInt(2, acc);
			pstmt.setInt(3, mult);
			ResultSet resSet = sqlResponseString(pstmt);
			if(resSet.next()) {
				HashMap<String, Object> map = new HashMap<>();
				map.put(ADDRESS, resSet.getString(ADDRESS));
				map.put(TRANSACTIONS, resSet.getString(TRANSACTIONS));
				map.put(TREE_INDEX, resSet.getString(TREE_INDEX));
				map.put(INDEX_NUMBER, resSet.getInt(INDEX_NUMBER));
				result.add(map);
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return result;
	}

}
