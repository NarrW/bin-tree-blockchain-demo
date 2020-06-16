package blockchain;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class SecurityManager {
	
    private static final int DEFAULT_KEY_SIZE = 1024;
    private static final String KEY_FACTORY_ALGORITHM = "RSA";
    private static final String ASYMMETRIC_KEY_PADDING_ALGORITHM = "RSA/ECB/PKCS1Padding";
    private static final String SESSION_KEY_PADDING_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final String SESSION_KEY_ALGORITHM = "AES";
    private static final int RSA_CRYPTO_SIZE_ENC = (DEFAULT_KEY_SIZE / 8) - 11;
    private static final int RSA_CRYPTO_SIZE_DEC = 172;//128;
    
	public static final int ENCRYPTION_KEY_LENGTH = 16;
    
	public SecurityManager() {
	}
	
	private PrivateKey getPrivateKey(byte[] key) throws NoSuchAlgorithmException, InvalidKeySpecException {
		KeyFactory kf;
		kf = KeyFactory.getInstance(KEY_FACTORY_ALGORITHM);
		
		byte[] keyBytes = Base64.getDecoder().decode(key);
		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
		PrivateKey priv = kf.generatePrivate(keySpec);
		return priv;
	}

	private PublicKey getPublicKey(byte[] key) throws NoSuchAlgorithmException, InvalidKeySpecException {
		KeyFactory kf;
		kf = KeyFactory.getInstance(KEY_FACTORY_ALGORITHM);
		byte[] keyBytes = Base64.getDecoder().decode(key);
		X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
		PublicKey pub = kf.generatePublic(spec);
		return pub;
	}
	
    public String encryptWithPublicKey(String plainText, byte[] pubKey) {
    	byte[] pBytes = plainText.getBytes(); 
    	StringBuilder result = new StringBuilder();
        try {
            int length = pBytes.length;
        	for(int i = 0; i < length; i += RSA_CRYPTO_SIZE_ENC) {
        		PublicKey publicKey = getPublicKey(pubKey);
        		Cipher cipher = Cipher.getInstance(ASYMMETRIC_KEY_PADDING_ALGORITHM);
                cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        		
        		int left = length - i;
        		int limitIndex = left > RSA_CRYPTO_SIZE_ENC ? RSA_CRYPTO_SIZE_ENC : left;
        		byte[] cBytes = new byte[limitIndex];
        		System.arraycopy(pBytes, i, cBytes, 0, limitIndex);
        		
        		byte[] resBytes = cipher.doFinal(cBytes);
        		
        		result.append(Base64.getEncoder().encodeToString(resBytes));
        	}
        } catch (NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException | InvalidKeySpecException e) {
        	e.printStackTrace();
        }
        
        return result.toString();
    }
    
    public String encryptWithPublicKey(byte[] plainText, byte[] pubKey) {
    	byte[] pBytes = plainText; 
    	StringBuilder result = new StringBuilder();
        try {
            int length = pBytes.length;
        	for(int i = 0; i < length; i += RSA_CRYPTO_SIZE_ENC) {
        		PublicKey publicKey = getPublicKey(pubKey);
        		Cipher cipher = Cipher.getInstance(ASYMMETRIC_KEY_PADDING_ALGORITHM);
                cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        		
        		int left = length - i;
        		int limitIndex = left > RSA_CRYPTO_SIZE_ENC ? RSA_CRYPTO_SIZE_ENC : left;
        		byte[] cBytes = new byte[limitIndex];
        		System.arraycopy(pBytes, i, cBytes, 0, limitIndex);
        		
        		byte[] resBytes = cipher.doFinal(cBytes);
        		
        		result.append(Base64.getEncoder().encodeToString(resBytes));
        	}
        } catch (NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException | InvalidKeySpecException e) {
        	e.printStackTrace();
        }
        
        return result.toString();
    }
    
    public String encryptWithPrivateKey(String plainText, byte[] privKey) {
    	byte[] pBytes = plainText.getBytes();
    	StringBuilder result = new StringBuilder();
        try {
        	int length = pBytes.length;
        	for(int i = 0; i < length; i += RSA_CRYPTO_SIZE_ENC) {
        		PrivateKey privateKey = getPrivateKey(privKey);
        		Cipher cipher = Cipher.getInstance(ASYMMETRIC_KEY_PADDING_ALGORITHM);
        		cipher.init(Cipher.ENCRYPT_MODE, privateKey);
        		
        		int left = length - i;
        		int limitIndex = left > RSA_CRYPTO_SIZE_ENC ? RSA_CRYPTO_SIZE_ENC : left;
        		byte[] cBytes = new byte[limitIndex];
        		System.arraycopy(pBytes, i, cBytes, 0, limitIndex);
        		
        		byte[] resBytes = cipher.doFinal(cBytes);
        		
        		result.append(Base64.getEncoder().encodeToString(resBytes));
        	}
            
        } catch (NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException | InvalidKeySpecException e) {
        	e.printStackTrace();
        }

        return result.toString();
    }
    
    public String encryptWithSessionKey(String plainText, byte[] sessionKey) {
        try {
            SecretKey secretKey = new SecretKeySpec(sessionKey, SESSION_KEY_ALGORITHM);
            Cipher cipher = Cipher.getInstance(SESSION_KEY_PADDING_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(new byte[16]));
            byte[] bytes = cipher.doFinal(plainText.getBytes());
            
            return Base64.getEncoder().encodeToString(bytes);
        } catch (NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
        	e.printStackTrace();
            return "";
        }
    }
	
    public byte[] decryptWithPrivateKey(String cipherText, byte[] privKey) {
    	byte[] byteBuffer = new byte[2048];
    	int length = cipherText.length();
		int accumulated = 0;
		try {
			PrivateKey privateKey = getPrivateKey(privKey);
			for(int i = 0; i < length; i += RSA_CRYPTO_SIZE_DEC) {
        		int left = length - i;
        		int limitIndex = left > RSA_CRYPTO_SIZE_DEC ? RSA_CRYPTO_SIZE_DEC : left;
        		
        		String cipherSub = cipherText.substring(i, i + limitIndex - 1);
				byte[] cBytes = Base64.getDecoder().decode(cipherSub);
				Cipher cipher = Cipher.getInstance(ASYMMETRIC_KEY_PADDING_ALGORITHM);
				cipher.init(Cipher.DECRYPT_MODE, privateKey);

				byte[] byteRes = cipher.doFinal(cBytes);
				
				System.arraycopy(byteRes, 0, byteBuffer, accumulated, byteRes.length);
				accumulated += byteRes.length;
			}
		} catch (NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException | InvalidKeySpecException | IllegalArgumentException e) {

		} 
		return Arrays.copyOfRange(byteBuffer, 0, accumulated);
	}
    
    public String decryptWithPublicKey(String cipherText, byte[] pubKey) {
    	StringBuilder result = new StringBuilder();
    	int length = cipherText.length();
    	try {
			PublicKey publicKey = getPublicKey(pubKey);
			
			for(int i = 0; i < length; i += RSA_CRYPTO_SIZE_DEC) {
        		int left = length - i;
        		int limitIndex = left > RSA_CRYPTO_SIZE_DEC ? RSA_CRYPTO_SIZE_DEC : left;
        		
        		String cipherSub = cipherText.substring(i, i + limitIndex - 1);
				byte[] cBytes = Base64.getDecoder().decode(cipherSub);

				Cipher cipher = Cipher.getInstance(ASYMMETRIC_KEY_PADDING_ALGORITHM);
				cipher.init(Cipher.DECRYPT_MODE, publicKey);
				
				String tmp = new String(cipher.doFinal(cBytes));
				result.append(tmp);
			}
		} catch (NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException | InvalidKeySpecException e) {

		} 
    	
    	return result.toString();
    }
    
    public String decryptWithSessionKey(String cipherText, byte[] sessionKey) {
    	try {
			SecretKey secretKey = new SecretKeySpec(sessionKey, SESSION_KEY_ALGORITHM);
			byte[] bytes = Base64.getDecoder().decode(cipherText);
			Cipher cipher = Cipher.getInstance(SESSION_KEY_PADDING_ALGORITHM);
			cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(new byte[16]));

			return new String(cipher.doFinal(bytes));
		} catch (NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException | InvalidAlgorithmParameterException | IllegalArgumentException e) {
			
			return "";
		} 
    }
	
    public String generateHash(String plainText) {
		byte[] byteData;
		StringBuffer sb = new StringBuffer(); 
		
		try {
			MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
			md.update(plainText.getBytes());
			byteData = md.digest();

			for(int i = 0 ; i < byteData.length ; i++){
				sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
			}

		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		
		return sb.toString();
	}
    
    public String generateQuatHash(String plainText) {
		byte[] byteData;
		StringBuffer sb = new StringBuffer(); 
		
		try {
			MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
			md.update(plainText.getBytes());
			byteData = md.digest();

			for(int i = 0 ; i < byteData.length / 4 ; i++){
				sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
			}

		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		
		return sb.toString();
	}
    
    public byte[] generateTemporarySessionKey() {
    	SecureRandom r = new SecureRandom();
    	byte[] aesKey = new byte[16];
    	r.nextBytes(aesKey);
    	
    	return aesKey;
    }
		
    private KeyPair generateKeyPair(String seed) throws NoSuchAlgorithmException {
    	SecureRandom rand = new SecureRandom(seed.getBytes());
        KeyPairGenerator generator = KeyPairGenerator.getInstance(KEY_FACTORY_ALGORITHM);
        generator.initialize(DEFAULT_KEY_SIZE, rand);
        KeyPair pair = generator.generateKeyPair();
        return pair;
    }
    
    // first one is the private, the other is the public.
    public String[] generateAsymmetricKeys() {
    	String privKeyAsString;
		String pubKeyAsString;
    	try {
			KeyPair keyPair = generateKeyPair(TimeChecker.getCurrentTime());
			PrivateKey privKey = keyPair.getPrivate();
			PublicKey pubKey = keyPair.getPublic();

//			byte[] t = pubKey.getEncoded();
//			byte[] pri = privKey.getEncoded();
			
			privKeyAsString = Base64.getEncoder().encodeToString(privKey.getEncoded());
			pubKeyAsString = Base64.getEncoder().encodeToString(pubKey.getEncoded());
			
		} catch (NoSuchAlgorithmException e) {
			pubKeyAsString = privKeyAsString = "";
			e.printStackTrace();
		}
    	
		String[] res = {privKeyAsString, pubKeyAsString};
    	
    	return res;
    }
    
//    public static void main(String[] args) {
//    	SecurityManager sm = new SecurityManager();
//    	String[] kp = sm.generateAsymmetricKeys();
//    	byte[] session = sm.generateTemporarySessionKey();
//    	String plainText = "14c63643643hfhtgdfvgdfgdfgfdhafhaf4643643634gdfgaggagasdgasgadsgsadgasdggahafhgfhgfahgadhdsafhdshfdshfdhdhdfhgshrtvw34tqt43t4wrh45yb34tversgfty34wt3w4t43a";
//    	System.out.println("plain length " + plainText.length());
//    	
////    	String sessionEnc = sm.encryptWithSessionKey(plainText, session);
//    	String hash = sm.generateQuatHash(plainText);
//    	System.out.println(hash.toString());
//    	System.out.println("length " + hash.length());
////    	String encrypted = sm.encryptWithPublicKey(session, kp[1].getBytes());
////    	byte[] decrypted = sm.decryptWithPrivateKey(encrypted, kp[0].getBytes());
////    	String replain = sm.decryptWithSessionKey(hash, session);
//    	
////    	System.out.println(replain.toString());
////    	System.out.println("length " + replain.length());
//    }

}
