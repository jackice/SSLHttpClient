import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MIME;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.URISyntaxException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;


/**
 * Modification History:
 * =============================================================================
 * Author         Date          Description
 * ------------ ---------- ---------------------------------------------------
 * JackIce       2017/9/4
 * =============================================================================
 */
public class SSLHttpClient {
    public static HttpClientConnectionManager CONNECTION_MANAGER = null;

    /**
     * 初始化 connection manager.
     * @param keyStoreFile
     * @param keyStorePass
     * @param trustStoreFile
     * @param trustStorePass
     * @throws Exception
     */
    public void init(String keyStoreFile, String keyStorePass,
                     String trustStoreFile, String trustStorePass) throws Exception {
        System.out.println("init conection pool...");

        InputStream ksis = new FileInputStream(new File(keyStoreFile));
        InputStream tsis = new FileInputStream(new File(trustStoreFile));

        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(ksis, keyStorePass.toCharArray());

        KeyStore ts = KeyStore.getInstance("JKS");
        ts.load(tsis, trustStorePass.toCharArray());

        SSLContext sslContext = SSLContexts.custom()
                .loadKeyMaterial(ks, keyStorePass.toCharArray())
                // 如果有 服务器证书
                .loadTrustMaterial(ts, new TrustSelfSignedStrategy())
                // 如果没有服务器证书，可以采用自定义 信任机制
                // .loadTrustMaterial(null, new TrustStrategy() {
                //
                // // 信任所有
                // public boolean isTrusted(X509Certificate[] arg0,
                // String arg1) throws CertificateException {
                // return true;
                // }
                //
                // })
                .build();
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
                sslContext, new String[] { "TLSv1" }, null,
                SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

        Registry<ConnectionSocketFactory> registry = RegistryBuilder
                .<ConnectionSocketFactory> create()
                .register("http", PlainConnectionSocketFactory.INSTANCE)
                .register("https", sslsf).build();
        ksis.close();
        tsis.close();
        CONNECTION_MANAGER = new PoolingHttpClientConnectionManager(registry);

    }
    public void MultiPost(String url,String filePath){
        /**
         * method_name: post
         * param: [url, params, filePath]
         * describe: 上传文件
         * creat_user: JackIce
         * creat_date: 2017/9/6
         * creat_time: 11:31
         **/
        if (CONNECTION_MANAGER == null) {
            return;
        }
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(CONNECTION_MANAGER).build();
        HttpPost httpPost = new HttpPost(url);
        String auth = "jackice:123456";
        Base64 base64 = new Base64();
        byte[] encode = base64.encode(auth.getBytes());
        String authorization = new String(encode);
        httpPost.setHeader("Authorization", "Basic " + authorization);
        MultipartEntityBuilder builderMaster = MultipartEntityBuilder.create()
                .setCharset(MIME.UTF8_CHARSET);
        File file=new File(filePath);
        FileBody fileBody = new FileBody(file);
        builderMaster.addPart("jpg", fileBody);
        builderMaster.addTextBody("FLW_CODE","20170906004");
        try {
            KeyStore keyStore = Certifacate.getKeyStore("1234567890", "D:/public/client.jks");
            X509Certificate clientCertificate = Certifacate.getCertificateByKeystore(keyStore, "client");
            PrivateKey privateKey = Certifacate.getPrivateKey(keyStore, "client", "1234567890");
            byte[] sign = Certifacate.sign(clientCertificate, privateKey, File2byte(filePath));
            System.out.println("KEY-CID:"+clientCertificate.getSerialNumber());
            builderMaster.addTextBody("CID",clientCertificate.getSerialNumber().toString());
            System.out.println(util.Base64.encode(sign));
            builderMaster.addTextBody("signature", util.Base64.encode(sign));
            Security.addProvider(new BouncyCastleProvider());
            X509Certificate certificateByCertPath = Certifacate.getCertificateByCertPath("D:/public/client.cer", "X509");
            boolean verify = Certifacate.verify(certificateByCertPath, File2byte(filePath), sign);
            System.out.println("CER-CID:"+certificateByCertPath.getSerialNumber());
            System.out.println("Verify result:"+verify);
        } catch (IOException e) {
            e.printStackTrace();
        }
        builderMaster.addTextBody("name","5555");
        builderMaster.addTextBody("DESCRIPTION","JackIce test");
        HttpEntity entity = builderMaster.build();
        httpPost.setEntity(entity);
        HttpResponse httpResponse;
        try {
            httpResponse = httpClient.execute(httpPost);
            HttpEntity httpEntity = httpResponse.getEntity();
            //TODO httpResponse
        } catch (IOException e) {
            // TODO Auto-generated catch block
        }


    }
    public static byte[] File2byte(String filePath)
    {
        byte[] buffer = null;
        try
        {
            File file = new File(filePath);
            FileInputStream fis = new FileInputStream(file);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] b = new byte[1024];
            int n;
            while ((n = fis.read(b)) != -1)
            {
                bos.write(b, 0, n);
            }
            fis.close();
            bos.close();
            buffer = bos.toByteArray();
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return buffer;
    }

    /**
     * do post
     * @param url
     * @param params
     * @throws Exception
     */
    public void post(String url, String params) throws Exception {
        if (CONNECTION_MANAGER == null) {
            return;
        }
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(CONNECTION_MANAGER).build();
        HttpPost httpPost = new HttpPost(url);

        httpPost.setEntity(new StringEntity(params,
                ContentType.APPLICATION_JSON));

        CloseableHttpResponse resp = httpClient.execute(httpPost);
        System.out.println(resp.getStatusLine());
        InputStream respIs = resp.getEntity().getContent();
        String content = convertStreamToString(respIs);
        System.out.println(content);
        EntityUtils.consume(resp.getEntity());
    }


    public static String convertStreamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        // 服务地址
        String url = "https://localhost:8443/ssl/jackice_test";

        // 私钥证书
        String keyStoreFile = "D:\\github_ucontent\\NoCAAndUserKeytool\\client.p12";
        String keyStorePass = "1234567890";

        // 配置信任证书库及密码
        String trustStoreFile = "D:\\github_ucontent\\NoCAAndUserKeytool\\client.truststore";
        String trustStorePass = "1234567890";

        String filePath = "D:/test/3.jpg";

        SSLHttpClient obj = new SSLHttpClient();
        try {
            obj.init(keyStoreFile, keyStorePass, trustStoreFile, trustStorePass);
            obj.MultiPost(url,filePath);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
