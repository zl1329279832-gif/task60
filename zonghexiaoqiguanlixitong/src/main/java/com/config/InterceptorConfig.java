package com.config;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import com.interceptor.AuthorizationInterceptor;

@Configuration
public class InterceptorConfig extends WebMvcConfigurationSupport{

	@Value("${app.upload-dir:./uploads}")
	private String uploadDir;

	@PostConstruct
	public void initUploadDir() {
		java.io.File dir = new java.io.File(uploadDir);
		if (!dir.exists()) {
			dir.mkdirs();
		}
	}

	@Bean
    public AuthorizationInterceptor getAuthorizationInterceptor() {
        return new AuthorizationInterceptor();
    }

	@Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(getAuthorizationInterceptor()).addPathPatterns("/**").excludePathPatterns("/static/**");
        super.addInterceptors(registry);
    }

	/**
	 * springboot 2.0配置WebMvcConfigurationSupport之后，会导致默认配置被覆盖，要访问静态资源需要重写addResourceHandlers方法
	 */
	@Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/**")
        .addResourceLocations("classpath:/resources/")
        .addResourceLocations("classpath:/static/")
        .addResourceLocations("classpath:/admin/")
        .addResourceLocations("classpath:/img/")
        .addResourceLocations("classpath:/front/")
        .addResourceLocations("classpath:/public/");

		// 上传文件: 从外部可挂载目录读取 (prod 用 Docker volume 持久化)
		String uploadLocation = "file:" + uploadDir + "/";
		registry.addResourceHandler("/upload/**")
			.addResourceLocations(uploadLocation);

		super.addResourceHandlers(registry);
    }
}
