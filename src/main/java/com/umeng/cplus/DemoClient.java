package com.umeng.cplus;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.RandomBasedGenerator;
import com.google.common.base.Joiner;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

public class DemoClient {
    private static final RandomBasedGenerator rug = Generators.randomBasedGenerator(
            new Random(System.currentTimeMillis()));

    private static String readKeyFile(String filename) throws Exception {
        BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(filename)));
        String line;
        StringBuilder sb = new StringBuilder();
        while ((line = br.readLine()) != null) {
            if (line.charAt(0) != '-') {
                sb.append(line).append(System.getProperty("line.separator"));
            }
        }
        br.close();
        return sb.toString();
    }

    private static PrivateKey loadPrivateKey(String privateKey) throws Exception {
        byte[] keyBytes = Base64.decodeBase64(privateKey);
        PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(pkcs8KeySpec);
    }

    private static String rsaSign(PrivateKey key, String text) throws Exception {
        Signature sig = Signature.getInstance("SHA1WithRSA");
        sig.initSign(key);
        byte[] data = text.getBytes("UTF-8");
        sig.update(data);
        byte[] signature = sig.sign();
        return Base64.encodeBase64String(signature);
    }

    public static void main(String[] args) throws Exception {
        // openssl genrsa | openssl pkcs8 -topk8 -nocrypt -out rsa_private_key.pem
        // openssl rsa -in rsa_private_key.pem -pubout -out rsa_public_key.pem
        String keyFile = args[0];
        String privateKeyStr = readKeyFile(keyFile);
        PrivateKey privateKey = loadPrivateKey(privateKeyStr);

        // Parameters
        Map<String, String> parameters = new TreeMap<String, String>() {{ // Sorted by key
            put("app_id", "123456");
            put("id_type", "mobile");
            put("id_value", "13456789012");
            put("req_id", rug.generate().toString());
        }};

        // Sign
        Joiner.MapJoiner mapJoiner = Joiner.on("&").withKeyValueSeparator("=");
        String signText = mapJoiner.join(parameters);
        String sign = rsaSign(privateKey, signText);
        parameters.put("sign", sign);

        // Build the URL
        URIBuilder ub = new URIBuilder("https://open.umeng.com/cplus/credit");
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            ub.addParameter(entry.getKey(), entry.getValue());
        }
        String url = ub.toString();
        System.out.println("Getting:\n" + url + "\n");

        // Response handler
        ResponseHandler<String> responseHandler = response -> {
            int status = response.getStatusLine().getStatusCode();
            if (status == HttpStatus.SC_OK) {
                HttpEntity entity = response.getEntity();
                return entity != null ? EntityUtils.toString(entity) : null;
            } else {
                throw new ClientProtocolException("Unexpected response status: " + status);
            }
        };

        // Get
        try (CloseableHttpClient client = HttpClients.createMinimal()) {
            HttpGet get = new HttpGet(url);
            String response = client.execute(get, responseHandler);
            System.out.println("Got response:\n" + response);
        }
    }
}

