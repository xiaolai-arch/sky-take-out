package com.sky.controller.notify;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class SearchHttpAK {

    private static final String MATRIX_URL = "https://api.map.baidu.com/routematrix/v2/driving?";

    private static final String GEOCODING_URL = "https://api.map.baidu.com/geocoding/v3/?";

    private static final String AK = "FqDYB7N02pWPsFddTtCo6DHhgGkuQf8v";

    /**
     * 地址转经纬度，返回 "lat,lng" 格式
     */
    public String getCode(String address) throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();

        String url = GEOCODING_URL + "address=" + UriUtils.encode(address, "UTF-8") + "&output=json&ak=" + AK;

        HttpGet httpGet = new HttpGet(url);

        CloseableHttpResponse response = httpClient.execute(httpGet);

        int statusCode = response.getStatusLine().getStatusCode();
        System.out.println(statusCode);

        HttpEntity entity = response.getEntity();
        String result = EntityUtils.toString(entity, "UTF-8");
        System.out.println("服务端返回的数据" + result);

        response.close();
        httpClient.close();

        // 解析json：先 parse 响应字符串，再逐层取 result → location
        JSONObject json = JSON.parseObject(result);
        JSONObject location = json.getJSONObject("result").getJSONObject("location");

        double lat = location.getDoubleValue("lat");
        double lng = location.getDoubleValue("lng");
        return lat + "," + lng;
    }

    /**
     * 两坐标之间的骑行的距离，返回米
     */
    public Long getDistance(String origin, String destination) throws Exception {
        origin = getCode(origin);
        destination = getCode(destination);
        Map<String, String> params = new LinkedHashMap<>();
        params.put("output", "json");
        params.put("origins", origin);
        params.put("destinations", destination);
        params.put("ak", AK);

        String json = httpGet(params);
        JSONObject jsonObj = JSON.parseObject(json);
        JSONArray resultArr = jsonObj.getJSONArray("result");
        if (resultArr != null && !resultArr.isEmpty()) {
            return resultArr.getJSONObject(0).getJSONObject("distance").getLong("value");
        }
        return null;
    }

    // ... existing code ...

    /**
     * 发送HTTP GET请求并获取响应结果
     * @param param 请求参数Map集合，键为参数名，值为参数值
     * @return API返回的响应字符串（通常为JSON格式）
     * @throws IOException 当网络请求失败或IO异常时抛出
     */
    private String httpGet(Map<String, String> param) throws IOException {
        // 构建带参数的完整URL
//        https://api.map.baidu.com/routematrix/v2/driving?origins=40.45,116.34&destinations=40.34,116.45&ak=您的AK
        String urlStr = MATRIX_URL + "origins=" + param.get("origins") + "&destinations=" + param.get("destinations") + "&ak=" + AK;

        // 创建HttpClient并执行GET请求
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(urlStr);
        CloseableHttpResponse response = httpClient.execute(httpGet);

        // 获取响应内容并转换为字符串
        HttpEntity entity = response.getEntity();
        String result = EntityUtils.toString(entity, "UTF-8");
        System.out.println("Response: " + result);

        // 关闭资源
        response.close();
        httpClient.close();

        return result;
    }

}