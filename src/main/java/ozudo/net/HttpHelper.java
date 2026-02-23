package ozudo.net;


import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import ozudo.base.helper.GsonUtil;
import ozudo.base.log.Logs;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * One for all http everything
 */
public class HttpHelper {

    static final CloseableHttpClient httpClient;

    static {
        // Filter certificate verification
        SSLConnectionSocketFactory scsf = null;
        PoolingHttpClientConnectionManager connectionManager = null;
        try {

            scsf = new SSLConnectionSocketFactory(
                    SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build(), SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

            connectionManager = new PoolingHttpClientConnectionManager(RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", PlainConnectionSocketFactory.getSocketFactory())
                    .register("https", scsf)
                    .build());
            connectionManager.setMaxTotal(100);
            connectionManager.setDefaultMaxPerRoute(50);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        httpClient = HttpClients.custom().setConnectionManager(connectionManager).setSSLSocketFactory(scsf).build();
    }

    public static String getRealIp(HttpRequest request, SocketAddress address) {
        return "";
    }

    public static String getContent(String url) {
        return getContent(url, null);
    }

    public static String getPostContent(String url, Map<String, String> params) {
        return getContent(url, params, true);
    }

    public static String getContent(String url, Map<String, String> params, boolean... isPost) {
        try {
            String result = "";
            if (isPost.length > 0) {
                HttpPost httpPost = new HttpPost(url);
                List<NameValuePair> values = new ArrayList<>(params.size());
                params.forEach((k, v) -> values.add(new BasicNameValuePair(k, v)));
                httpPost.setEntity(new UrlEncodedFormEntity(values, "UTF-8"));
                CloseableHttpResponse response = httpClient.execute(httpPost);
                HttpEntity entity = response.getEntity();
                result = EntityUtils.toString(entity);
                response.close();
            } else {
                HttpGet httpGet = new HttpGet(url);
                CloseableHttpResponse response = httpClient.execute(httpGet);
                HttpEntity entity = response.getEntity();
                result = EntityUtils.toString(entity);

                EntityUtils.consume(entity);
                response.close();
            }
            return result;
        } catch (Exception ex) {
            Logs.error(ex);
        }
        return null;
    }


    public static byte[] getByteContent(String url, byte[] data) {
        try {
            HttpPost httpPost = new HttpPost(url);
            httpPost.setEntity(new ByteArrayEntity(data));
            CloseableHttpResponse response = httpClient.execute(httpPost);
            HttpEntity entity = response.getEntity();
            byte[] responseData = EntityUtils.toByteArray(entity);
            response.close();
            return responseData;
        } catch (Exception ex) {
            Logs.error(ex);
        }
        return null;
    }

    public static String graphQL(String url, String query) {
        try {
            HttpPost request = new HttpPost(url);
            request.addHeader("content-type", "application/json");
            request.addHeader("accept", "application/json");
            request.setEntity(new StringEntity(query));
            HttpResponse response = httpClient.execute(request);
            return EntityUtils.toString(response.getEntity());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

}
