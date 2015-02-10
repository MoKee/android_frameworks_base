/*
 * Copyright (C) 2014-2015 The MoKee OpenSource Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mokee.services.assist.utils;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;

import com.google.common.io.BaseEncoding;

public class RSAEncryption {
    private static String pub_key = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCvlIvTfzmkZFq5wCT2kztGbxYFzHnYrynuLZFbcUAk4weIIDlPs95YbesaAUMolaJOWq6VZ7hoydnpUGmRqb6Ygy+de1xQaPzIKXn+8f1GwRo2JvipHm/T/SPc/OcJommeI7KZNGjH88z7dqFbuGIPZIZh/lyJO1CZcM0uddgCnQIDAQAB";

    private static final String RSA_KEY_ALGORITHM = "RSA";

    private static final String SIGNATURE_ALGORITHM = "MD5withRSA";

    public boolean verify(byte[] data, byte[] sign) throws Exception {
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_KEY_ALGORITHM);
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(BaseEncoding.base64().decode(
                pub_key));
        PublicKey pubKey = keyFactory.generatePublic(x509KeySpec);
        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
        signature.initVerify(pubKey);
        signature.update(data);
        return signature.verify(sign);
    }

    public static boolean verify(String signature) throws Exception {
        RSAEncryption das = new RSAEncryption();
        String comment = "Prevent abuse of our interface.";
        return das.verify(comment.getBytes(), BaseEncoding.base64().decode(signature));
    }
}
