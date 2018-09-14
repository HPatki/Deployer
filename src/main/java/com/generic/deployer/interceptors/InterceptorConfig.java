package com.generic.deployer.interceptors;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

/**
 * Created by Hpatki on 2/27/2018.
 */

@Configuration
public class InterceptorConfig extends DelegatingWebMvcConfiguration
{
    RequestInterceptor inst = new RequestInterceptor();

    @Override
    public void addInterceptors(InterceptorRegistry registry)
    {
        registry.addInterceptor(inst);
    }
}

