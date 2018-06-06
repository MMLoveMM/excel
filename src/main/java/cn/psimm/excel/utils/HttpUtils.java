package cn.psimm.excel.utils;

import com.google.common.collect.Lists;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author jamkong
 */
public class HttpUtils {
    private static PoolingHttpClientConnectionManager connMgr;
    private static RequestConfig requestConfig;
    private static final int MAX_TIMEOUT = 7000;
    // 字符集编码
    public static final String CHARSET_CODE = "UTF-8";
    private static final Logger logger = LoggerFactory.getLogger(HttpUtils.class);

    public interface CallBack {
        void onRequestComplete(String result);
    }


    /**
     * 异步的Get请求
     *
     * @param urlStr
     * @param callBack
     */
    public static void doGetAsyn(final String url, final Map<String, Object> params, final Map<String, String> propertys, final CallBack callBack) {
        new Thread() {
            @Override
            public void run() {
                try {
                    String result = doGet(url, params, propertys);
                    if (callBack != null) {
                        callBack.onRequestComplete(result);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    logger.error(e.getMessage());
                }

            }
        }.start();
    }

    /**
     * 异步的Post请求
     *
     * @param urlStr
     * @param params
     * @param callBack
     * @throws Exception
     */
    public static void doPostAsyn(final String url, final Map<String, Object> params, final Map<String, String> propertys, final CallBack callBack) {
        new Thread() {
            @Override
            public void run() {
                try {
                    String result = doPost(url, params, propertys);
                    if (callBack != null) {
                        callBack.onRequestComplete(result);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }.start();

    }


    static {
        // 设置连接池
        connMgr = new PoolingHttpClientConnectionManager();
        // 设置连接池大小
        connMgr.setMaxTotal(100);
        connMgr.setDefaultMaxPerRoute(connMgr.getMaxTotal());
        // Validate connections after 1 sec of inactivity
        connMgr.setValidateAfterInactivity(1000);
        RequestConfig.Builder configBuilder = RequestConfig.custom();
        // 设置连接超时
        configBuilder.setConnectTimeout(MAX_TIMEOUT);
        // 设置读取超时
        configBuilder.setSocketTimeout(MAX_TIMEOUT);
        // 设置从连接池获取连接实例的超时
        configBuilder.setConnectionRequestTimeout(MAX_TIMEOUT);

        requestConfig = configBuilder.build();
    }

    /**
     * 发送 GET 请求（HTTP），K-V形式
     *
     * @param url
     * @param params
     * @return
     */
    public static String doGet(String url, Map<String, Object> params, Map<String, String> propertys) {
        logger.info("GET url:{} params:{} propertys:{}", url, params, propertys);
        HttpResponse response = null;
        InputStream in = null;
        String body = "";
        try {
            response = send(url, HttpMethod.GET.toString(), params, propertys);
            in = response.getEntity().getContent();
            body = IOUtils.toString(in, CHARSET_CODE);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(in);
        }
        return body;
    }


    /**
     * 发送 POST 请求（HTTP），K-V形式
     *
     * @param url
     * @param params
     * @return
     */
    public static String doPost(String url, Map<String, Object> params, Map<String, String> propertys) {
        logger.info("POST url:{} params:{} propertys:{}", url, params, propertys);
        HttpResponse response = null;
        InputStream in = null;
        String body = "";
        try {
            response = send(url, HttpMethod.POST.toString(), params, propertys);
            in = response.getEntity().getContent();
            body = IOUtils.toString(in, CHARSET_CODE);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(in);
        }
        return body;
    }

    /**
     * 创建SSL安全连接
     *
     * @return
     */
    private static SSLConnectionSocketFactory createSSLConnSocketFactory() {
        SSLConnectionSocketFactory sslsf = null;
        try {
            SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
                @Override
                public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    return true;
                }
            }).build();
            sslsf = new SSLConnectionSocketFactory(sslContext, new HostnameVerifier() {
                @Override
                public boolean verify(String arg0, SSLSession arg1) {
                    return true;
                }
            });
        } catch (GeneralSecurityException e) {
            logger.debug(e.getMessage());
        }
        return sslsf;
    }


    /**
     * 发送HTTP请求
     *
     * @param urlString  地址
     * @param method     get/post
     * @param parameters 添加由键值对指定的请求参数
     * @param propertys  添加由键值对指定的一般请求属性
     * @return 响映对象
     * @throws IOException
     */
    private static HttpResponse send(String urlString, String method, Map<String, Object> parameters,
                                     Map<String, String> propertys) throws IOException {

        CloseableHttpClient client = null;
        if (urlString.startsWith("https")) {
            client = HttpClients.custom().setSSLSocketFactory(createSSLConnSocketFactory())
                    .setConnectionManager(connMgr).setDefaultRequestConfig(requestConfig).build();
        } else {
            client = HttpClients.createDefault();
        }

        // 设置请求头
        List<Header> headerList = Lists.newArrayList();
        if (propertys != null) {
            for (String key : propertys.keySet()) {
                headerList.add(new BasicHeader(key, propertys.get(key)));
            }
        }

        CloseableHttpResponse response = null;
        if (method.equalsIgnoreCase("GET") && parameters != null) {
            StringBuffer param = new StringBuffer();
            int i = 0;
            for (String key : parameters.keySet()) {
                if (i == 0) {
                    param.append("?");
                } else {
                    param.append("&");
                }
                param.append(key).append("=").append(parameters.get(key));
                i++;
            }
            urlString += param;
            HttpGet request = new HttpGet(urlString);
            if (headerList != null) {
                for (Header header : headerList) {
                    request.setHeader(header);
                }
            }
            response = client.execute(request);
        } else if (method.equalsIgnoreCase("POST") && parameters != null) {
            HttpPost request = new HttpPost(urlString);
            String body = JsonMapper.getInstance().toJson(parameters);
            HttpEntity entity = new StringEntity(body, Charset.forName(CHARSET_CODE));
            request.setEntity(entity);
            if (headerList != null) {
                for (Header header : headerList) {
                    request.setHeader(header);
                }
            }
            response = client.execute(request);
        }

        return response;
    }


    public static void main(String[] args) {
        Map<String, Object> parameters = new HashMap<>();
        Map<String, String> propertys = new HashMap<>();
        propertys.put("Content-Type", "application/json");
        propertys.put("Authorization", "Basic eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJuYW1lIjoiZGRkIERvZSJ9.TXxw_307JASo6uM_ATko9pbaE10xwekSaPMfOv-2bQQ");
        parameters.put("search", "紫杉醇");
        InputStream in = null;
//        try {
//            HttpResponse response = send(InterfaceURL.getTechNavSearchApi(), "POST", parameters, propertys);
//            in = response.getEntity().getContent();
//            String str = IOUtils.toString(in, "utf-8");
//        String str = doPost(InterfaceURL.getTechNavSearchApi(), parameters, propertys);
//        System.out.println(str);
//        System.out.println(JsonMapper.getInstance().toJson(str));
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            IOUtils.closeQuietly(in);
//        }

    }
}
