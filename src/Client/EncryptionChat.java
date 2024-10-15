package src.Client;

import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptionChat {

        // Método para encriptar

        public static String Encrypt(String texto, SecretKey claveSecreta) throws Exception {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

            // Generar un nuevo IV para cada encriptación
            byte[] iv = new byte[16]; // AES usa un bloque de 16 bytes para el IV
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);
            IvParameterSpec ivParams = new IvParameterSpec(iv);

            // Inicializar el cifrado con el IV
            cipher.init(Cipher.ENCRYPT_MODE, claveSecreta, ivParams);
            byte[] textoEncriptado = cipher.doFinal(texto.getBytes("UTF-8"));
            String base64Encoded = Base64.getEncoder().encodeToString(textoEncriptado);

            // Devuelve el IV concatenado con el mensaje encriptado (ambos en Base64)
            String ivBase64 = Base64.getEncoder().encodeToString(iv);
            return ivBase64 + ":" + base64Encoded;
        }

    
        // Método para desencriptar
        public static String Dencrypt(String textoEncriptado, SecretKey claveSecreta) throws Exception {
            System.out.println(textoEncriptado);
            System.out.println(claveSecreta);
        
            // Separar el IV del texto encriptado
            String[] parts = textoEncriptado.split(":");
            String ivBase64 = parts[0];  // La primera parte es el IV
            String base64Decoded = parts[1];  // La segunda parte es el mensaje encriptado
        
            byte[] iv = Base64.getDecoder().decode(ivBase64);  // Decodificar el IV desde Base64
            byte[] textoDesencriptado = Base64.getDecoder().decode(base64Decoded);  // Decodificar el mensaje encriptado
        
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            IvParameterSpec ivParams = new IvParameterSpec(iv);  // Usar el IV para la desencriptación
            cipher.init(Cipher.DECRYPT_MODE, claveSecreta, ivParams);
        
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
