package com.lsj.tingshu.user.config;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.impl.WxMaServiceImpl;
import cn.binarywang.wx.miniapp.config.impl.WxMaDefaultConfigImpl;
import com.lsj.tingshu.user.config.properties.WxLoginProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.jwt.crypto.sign.RsaSigner;
import org.springframework.security.rsa.crypto.KeyStoreKeyFactory;

import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;

@Configuration
@EnableConfigurationProperties(WxLoginProperties.class)
public class AppConfiguration {
    @Bean
    public WxMaService wxMaService(WxLoginProperties wxLoginProperties) {
        WxMaService wxMaService = new WxMaServiceImpl();

        WxMaDefaultConfigImpl wxMaDefaultConfig = new WxMaDefaultConfigImpl();
        wxMaDefaultConfig.setAppid(wxLoginProperties.getAppId());
        wxMaDefaultConfig.setSecret(wxLoginProperties.getSecret());

        wxMaService.setWxMaConfig(wxMaDefaultConfig);
        return wxMaService;
    }

    @Bean
    public RsaSigner rsaSigner() {
        //1.从类路径获取jks文件
        ClassPathResource classPathResource = new ClassPathResource("tingshu.jks");
        //2.获取密钥库对象
        KeyStoreKeyFactory keyStoreKeyFactory = new KeyStoreKeyFactory(classPathResource, "tingshu".toCharArray());
        //3.从密钥库对象中获取密钥对
        KeyPair keyPair = keyStoreKeyFactory.getKeyPair("tingshu", "tingshu".toCharArray());
        //4.获取私钥对象
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        //5.得到非对称加密的RSA签名器
        RsaSigner rsaSigner = new RsaSigner(privateKey);
        //6.返回签名器
        return rsaSigner;
    }
}
