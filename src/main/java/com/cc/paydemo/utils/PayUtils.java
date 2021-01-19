package com.cc.paydemo.utils;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * pay-demo
 *
 * @author Chan
 * @since 2021/1/15 17:01
 **/
public class PayUtils {
    public static Map<String, String> requestToMap(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();
        Iterator iterator = requestParams.keySet().iterator();
        while(iterator.hasNext()) {
            String name = (String) iterator.next();
            String[] values = requestParams.get(name);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = i == values.length - 1 ? valueStr + values[i] : valueStr + values[i] + ",";
            }
            params.put(name, valueStr);
        }
        return params;
    }
}
