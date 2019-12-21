package com.dt.module.wx.service;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson.JSONObject;
import com.dt.core.cache.CacheConfig;
import com.dt.core.common.base.BaseService;
import com.dt.core.common.base.R;

/**
 * @author: jinjie
 * @date: 2018年3月22日 上午8:42:07
 * @Description: TODO
 */
@Service
@Configuration
@PropertySource(value = "classpath:application.properties")
public class WxConfigService extends BaseService {
	@Autowired
	private WxService wxService;

	@Value("${wx.appId}")
	public String appIdconf;

	@Value("${wx.secret}")
	public String secretconf;
	private static Logger _log = LoggerFactory.getLogger(WxConfigService.class);

	@Cacheable(value = CacheConfig.CACHE_WX_CONF_300_180, key = "'wx_config_'+#url")
	public R queryWxConfig(String url) {
		return queryWxConfig(appIdconf, secretconf, url);
	}

	/**
	 * @Description:微信公众号获取配置
	 */
	public R queryWxConfig(String appId, String secret, String locurl) {
		System.out.println("tt");
		_log.info("url:" + locurl);
		_log.info("appId:" + appId);
		Map<String, Object> ret = new HashMap<String, Object>();
		String requestUrl = locurl;
		String access_token = "";
		String jsapi_ticket = "";
		String timestamp = Long.toString(System.currentTimeMillis() / 1000); // 必填，生成签名的时间戳
		String nonceStr = UUID.randomUUID().toString(); // 必填，生成签名的随机串
		// 获取token
		R tokenr = wxService.queryWxToken(appId, secret, false);
		if (tokenr.isSuccess()) {
			// 获取ticket
			access_token = tokenr.queryDataToJSONObject().getString("access_token");
			R ticketr = wxService.queryWxTicket(access_token, false);
			if (ticketr.isSuccess()) {
				jsapi_ticket = ticketr.queryDataToJSONObject().getString("access_ticket");
			} else {
				return ticketr;
			}
		} else {
			return tokenr;
		}

		// 注意这里参数名必须全部小写，且必须有序,做签名
		String signature = "";
		_log.info("jsapi_ticket:" + jsapi_ticket);
		String sign = "jsapi_ticket=" + jsapi_ticket + "&noncestr=" + nonceStr + "&timestamp=" + timestamp + "&url="
				+ requestUrl;
		try {
			MessageDigest crypt = MessageDigest.getInstance("SHA-1");
			crypt.reset();
			crypt.update(sign.getBytes("UTF-8"));
			signature = WxService.byteToHex(crypt.digest());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		ret.put("appId", appId);
		ret.put("timestamp", timestamp);
		ret.put("nonceStr", nonceStr);
		ret.put("signature", signature);
		JSONObject r = new JSONObject();
		r.put("signature", ret.get("signature"));
		r.put("appId", ret.get("appId"));
		r.put("nonceStr", ret.get("nonceStr"));
		r.put("timestamp", ret.get("timestamp"));
		_log.info(r.toJSONString());
		return R.SUCCESS_OPER(r);

	}

}
