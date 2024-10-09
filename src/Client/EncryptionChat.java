package src.Client;

import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class EncryptionChat {

        // Método para encriptar
        public static String Encrypt(String texto, SecretKey claveSecreta) throws Exception {
            
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, claveSecreta);
            byte[] textoEncriptado = cipher.doFinal(texto.getBytes("UTF-8"));
            String base64Encoded = Base64.getEncoder().encodeToString(textoEncriptado);
            
            
            return base64Encoded.replace("^", "~");
        }
    
        // Método para desencriptar
        public static String Dencrypt(String textoEncriptado, SecretKey claveSecreta) throws Exception {
            System.out.println(textoEncriptado);
            System.out.println(claveSecreta);
            
            // Revertir la sustitución antes de decodificar
            String base64Decoded = textoEncriptado.replace("~", "^");
            byte[] textoDesencriptado = Base64.getDecoder().decode(base64Decoded);
            
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, claveSecreta);
            return new String(cipher.doFinal(textoDesencriptado), "UTF-8");
        }
    
        public static SecretKey keyGenerator() {
            try {
                // Generar una clave AES
                KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
                keyGenerator.init(128); // Tamaño de la clave (128, 192, 256 bits)
                SecretKey claveSecreta = keyGenerator.generateKey();
                return claveSecreta;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    
        public static String toString(SecretKey secretKey) {
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        }
    
        public static SecretKey toSecretKey(String secretKeyString) {
                // Decodificar el String en Base64 a un arreglo de bytes
                byte[] decodedKey = Base64.getDecoder().decode(secretKeyString);
    
                // Crear un nuevo objeto SecretKey a partir de los bytes
                SecretKey secretKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
                return secretKey;
        }
    
}
