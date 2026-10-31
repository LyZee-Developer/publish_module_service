package org.module.publish_service.util;

import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * @author : Ly LeangSeng
 * @email : lyleangseng712@gmail.com
 * @date : 10/29/2026 4:31 PM
 */
@Component
public class SensitiveUtil {
    private final String secretPassword = "MySuperSecretKey";
    private final String salt = "5c0742f0d9def632"; // 8-byte hex-encoded salt

    private final TextEncryptor textEncryptor = Encryptors.text(secretPassword, salt);

    //*** Convert the number to text
    //example: 200 => a60e641522353ecc02159e684cf246365093fd9d3487cf0820565539ad694fcc
    public String encryptNumber(BigDecimal number) {
        return textEncryptor.encrypt(String.valueOf(number));
    }

    //*** Convert the text to number
    //example: a60e641522353ecc02159e684cf246365093fd9d3487cf0820565539ad694fcc => 200
    public String decryptNumber(String encryptedValue) {
        return textEncryptor.decrypt(encryptedValue);
    }
}
