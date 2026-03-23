package com.swpu.equipment.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
//web配置
/**
 * Web配置：配置静态资源映射
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${upload.base-dir:src/main/resources/static/uploads}")
    //上传文件的目录
    private String uploadBaseDir;

    //配置静态资源映射，如上传的二维码图片
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")  //处理前端上传的文件
                .addResourceLocations("file:" + uploadBaseDir + "/");  //映射到后端上传的文件目录  
    }
}
