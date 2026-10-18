package com.ilia.gikashvili.archmain.ai;
import org.springframework.stereotype.Component; import org.springframework.beans.factory.annotation.Value;
import javax.crypto.*; import javax.crypto.spec.*; import java.security.*; import java.nio.charset.StandardCharsets; import java.util.*;
@Component public class AiKeyCipher {
 private final SecretKeySpec key; private final SecureRandom random=new SecureRandom();
 public AiKeyCipher(@Value("${app.ai.key-encryption-secret:archmain-development-only-change-this-secret}") String secret) throws Exception { key=new SecretKeySpec(MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8)),"AES"); }
 public String encrypt(String value) { try {byte[] iv=new byte[12];random.nextBytes(iv);Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key,new GCMParameterSpec(128,iv));byte[] data=c.doFinal(value.getBytes(StandardCharsets.UTF_8));return Base64.getEncoder().encodeToString(java.nio.ByteBuffer.allocate(iv.length+data.length).put(iv).put(data).array());}catch(Exception e){throw new IllegalStateException("Key encryption failed");} }
 public String decrypt(String value) { try {byte[] data=Base64.getDecoder().decode(value);Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,key,new GCMParameterSpec(128,Arrays.copyOf(data,12)));return new String(c.doFinal(Arrays.copyOfRange(data,12,data.length)),StandardCharsets.UTF_8);}catch(Exception e){throw new IllegalStateException("Key decryption failed");} }
}
