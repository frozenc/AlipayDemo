package com.cc.paydemo.controller;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.json.JSONUtil;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.*;
import com.alipay.api.response.*;
import com.cc.paydemo.config.AlipayConfig;
import com.cc.paydemo.utils.PayUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


/**
 * paydemo
 *
 * @author Chan
 * @since 2021/1/15 15:53
 **/
@Controller
@Slf4j
public class PayController {

    @Autowired
    private AlipayConfig alipayConfig;

    @Autowired
    private AlipayClient alipayClient;

    @Autowired
    private Snowflake snowflake;

    @RequestMapping("/test")
    @ResponseBody
    public String test() {
        return "test";
    }

    /**
     * https://opendocs.alipay.com/open/270/
     * 调用流程如下：
     * 商户系统调用 alipay.trade.page.pay（统一收单下单并支付页面接口）向支付宝发起支付请求，支付宝对商户请求参数进行校验，而后重新定向至用户登录页面。
     * 用户确认支付后，支付宝通过 get 请求 returnUrl（商户入参传入），返回同步返回参数。
     * 交易成功后，支付宝通过 post 请求 notifyUrl（商户入参传入），返回异步通知参数。
     * 若由于网络等原因，导致商户系统没有收到异步通知，商户可自行调用 alipay.trade.query（统一收单线下交易查询）接口查询交易以及支付信息（商户也可以直接调用该查询接口，不需要依赖异步通知）。
     */

    /**
     * 沙箱URL拼接规则https://openauth.alipaydev.com/oauth2/publicAppAuthorize.htm?app_id=APPID&scope=SCOPE&redirect_uri=ENCODED_URL
     * app_id：开发者应用的 APPID。
     * scope：接口权限值，获取用户信息场景暂支持 auth_user 和 auth_base 两个值。
     * redirect_uri：回调页面，是 经过转义 的url链接（url 必须以 http 或者 https 开头），比如： http%3A%2F%2Fexample.com
     * 在请求之前，开发者需要先到开发者中心对应应用内，配置授权回调地址。
     * state：商户自定义参数，用户授权后，重定向到 redirect_uri 时会原样回传给商户。 为防止 CSRF 攻击，建议开发者请求授权时传入 state 参数，
     * 该参数要做到既不可预测，又可以证明客户端和当前第三方网站的登录认证状态存在关联，并且不能有中文。
     *
     * 登录 https://opendocs.alipay.com/open/284
     */

    @RequestMapping("/login")
    public void login(HttpServletResponse httpServletResponse) throws IOException {
        //第一步：拼接URL
        String url = "https://openauth.alipaydev.com/oauth2/publicAppAuthorize.htm?app_id=" + alipayConfig.getAPP_ID() +
            "&scope=auth_user,auth_base&redirect_uri=" + alipayConfig.getAUTH_URL();
        //第二步，获取auth_code
        httpServletResponse.sendRedirect(url);
    }

    /**
     * 授权 https://opendocs.alipay.com/open/284
     */
    @RequestMapping("/auth")
    @ResponseBody
    public String auth(HttpServletRequest httpServletRequest) throws AlipayApiException {
        log.info("进入auth");
        //第二步，获取auth_code
        Map<String, String> map = PayUtils.requestToMap(httpServletRequest);
        //第三步，换取 access_token 和 userId
        AlipaySystemOauthTokenRequest request = new AlipaySystemOauthTokenRequest();
        request.setCode(map.get("auth_code"));
        request.setGrantType("authorization_code");
        AlipaySystemOauthTokenResponse response = alipayClient.execute(request);
        String accessToken = response.getAccessToken();
        //第四步，获取用户信息
        AlipayUserInfoShareRequest alipayUserInfoShareRequest = new AlipayUserInfoShareRequest();
        AlipayUserInfoShareResponse userInfoShareResponse = alipayClient.execute(alipayUserInfoShareRequest, accessToken);
        return userInfoShareResponse.getBody();
    }

    /**
     * 支付 https://docs.open.alipay.com/api_1/alipay.trade.pay/
     *
     * 1.用户点击购买
     * 2.跳转到支付宝收银台界面，用户选择付款方式：1）扫码付款 2）登录账户密码付款
     * 3.用户输入支付密码后确认付款
     *
     */
    @RequestMapping("/pay")
    @ResponseBody
    public void pay(HttpServletResponse response) throws IOException, AlipayApiException {
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        //支付成功回调
        request.setReturnUrl(alipayConfig.getRETURN_URL());
        //交易完成回调
        request.setNotifyUrl(alipayConfig.getNOTIFY_URL());
        Map<String, Object> biz = new HashMap<>();
        biz.put("out_trade_no", snowflake.nextId());
        biz.put("product_code", "FAST_INSTANT_TRADE_PAY");
        biz.put("total_amount", 88.88);
        biz.put("subject", "电脑网站支付测试");
        biz.put("body", "支付宝沙箱测试");
        log.info(JSONUtil.toJsonStr(biz));
        request.setBizContent(JSONUtil.toJsonStr(biz));
        String form= alipayClient.pageExecute(request, "GET").getBody();  //调用SDK生成表单
        response.sendRedirect(form);
        response.setContentType( "text/html;charset="  + alipayConfig.getCHARSET());
        response.getWriter().write(form); //直接将完整的表单html输出到页面
        response.getWriter().flush();
        response.getWriter().close();
    }

    /**
     * 退款 https://opendocs.alipay.com/apis/api_1/alipay.trade.refund
     */
    @RequestMapping("refund")
    @ResponseBody
    public String refund(@RequestParam(name = "outTradeNo", required = false) String outTradeNo,
                         @RequestParam(name = "tradeNo", required = false) String tradeNo) throws AlipayApiException {
        outTradeNo = "1350087821397852160";

        Map<String, Object> params = new HashMap<>();
        params.put("out_trade_no", outTradeNo);
        params.put("tradeNo", tradeNo);
        params.put("refund_amount", 88.88); // 必选

        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        request.setBizContent(JSONUtil.toJsonStr(params));
        AlipayTradeRefundResponse response = alipayClient.execute(request);
        if(response.isSuccess()){
            log.info("退款成功");
        } else {
            log.info("退款失败");
        }
        return response.getBody();
    }

    /**
     * 查询订单 https://opendocs.alipay.com/apis/api_1/alipay.trade.query/
     */
    @RequestMapping(value = "/query", method = RequestMethod.GET)
    @ResponseBody
    public String query(@RequestParam(value = "outTradeNo", required = false) String outTradeNo,
                        @RequestParam(value = "tradeNo", required = false) String tradeNo) throws AlipayApiException {
//    public String query(String outTradeNo, String tradeNo) throws AlipayApiException {
//        outTradeNo = "1350087821397852160";
        System.out.println(outTradeNo);
        Map<String, String> params = new HashMap<>();
        params.put("out_trade_no", outTradeNo);
        params.put("tradeNo", tradeNo);

        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        request.setBizContent(JSONUtil.toJsonStr(params));
        AlipayTradeQueryResponse response = alipayClient.execute(request);
        if(response.isSuccess()){
            log.info("查询订单成功");
        } else {
            log.info("查询订单失败");
        }
        return response.getBody();
    }


    /**
     * 关闭订单，用户超时未支付 https://opendocs.alipay.com/apis/api_1/alipay.trade.close
     */
    @RequestMapping("close")
    @ResponseBody
    public String close(@RequestParam(name = "outTradeNo", required = false) String outTradeNo,
                        @RequestParam(name = "tradeNo", required = false) String tradeNo) throws AlipayApiException {
        outTradeNo = "1350087821397852160";

        Map<String, String> params = new HashMap<>();
        params.put("out_trade_no", outTradeNo);
        params.put("tradeNo", tradeNo);

        AlipayTradeCloseRequest request = new AlipayTradeCloseRequest();
        request.setBizContent(JSONUtil.toJsonStr(params));
        AlipayTradeCloseResponse response = alipayClient.execute(request);
        if(response.isSuccess()){
            log.info("关闭订单成功");
        } else {
            log.info("关闭订单失败");
        }
        return response.getBody();
    }

    /**
     * 支付宝确认支付同步通知接口
     */
    @RequestMapping("/return")
    @ResponseBody
    public String Return(HttpServletRequest request) throws AlipayApiException {
        Map<String, String> paramsMap = PayUtils.requestToMap(request); //将异步通知中收到的所有参数都存放到map中
        log.info(JSONUtil.toJsonStr(paramsMap));
        boolean signVerified = AlipaySignature.rsaCheckV1(paramsMap, alipayConfig.getALIPAY_PUBLIC_KEY(),
                alipayConfig.getCHARSET(), alipayConfig.getSIGN_TYPE()); //调用SDK验证签名
        if(signVerified){
            // 验签成功后，按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure
            log.info("Return 验证成功");
            return "success";
        }else{
            // 验签失败则记录异常日志，并在response中返回failure.
            log.info("Return 验证失败");
            return "failure";
        }
    }

    /**
     * 支付宝交易成功异步通知接口
     */
    @RequestMapping("/notify")
    @ResponseBody
    public String Notify(HttpServletRequest request) throws AlipayApiException {
        Map<String, String> paramsMap = PayUtils.requestToMap(request); //将异步通知中收到的所有参数都存放到map中
        log.info(JSONUtil.toJsonStr(paramsMap));
        boolean signVerified = AlipaySignature.rsaCheckV1(paramsMap, alipayConfig.getALIPAY_PUBLIC_KEY(),
                alipayConfig.getCHARSET(), alipayConfig.getSIGN_TYPE()); //调用SDK验证签名
        if(signVerified){
            // 验签成功后，按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure
            log.info("Notify 验证成功");
            return "success";
        }else{
            // 验签失败则记录异常日志，并在response中返回failure.
            log.info("Notify 验证失败");
            return "failure";
        }
    }
}
