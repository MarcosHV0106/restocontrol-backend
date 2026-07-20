package com.utp.RestoControl.Config;

import com.utp.RestoControl.Logging.AuditoriaOperacionInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuditoriaOperacionInterceptor auditoriaOperacionInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(auditoriaOperacionInterceptor).addPathPatterns("/api/**");
    }
}
