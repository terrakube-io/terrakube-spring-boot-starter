package io.terrakube.client.spring.autoconfigure;

import feign.Feign;
import feign.http2client.Http2Client;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.slf4j.Slf4jLogger;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import io.terrakube.client.TerrakubeClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import io.terrakube.client.dex.DexCredentialAuthentication;

@AutoConfiguration
@EnableConfigurationProperties(RestClientProperties.class)
@ConditionalOnMissingBean(TerrakubeClient.class)
public class RestClientAutoConfiguration {

    @Autowired
    private RestClientProperties restClientProperties;

    @Bean
    public TerrakubeClient restClient(RestClientProperties restClientProperties) {
        TerrakubeClient restClient = null;
        if (restClientProperties.isEnableSecurity()) {

            DexCredentialAuthentication clientCredentialAuthentication = new DexCredentialAuthentication(
                    restClientProperties.getSecretKey(),
                    restClientProperties.getCredentialType()
            );

            restClient = Feign.builder()
                    .encoder(new JacksonEncoder())
                    .decoder(new JacksonDecoder())
                    .client(new Http2Client())
                    .requestInterceptor(clientCredentialAuthentication)
                    .target(TerrakubeClient.class, restClientProperties.getUrl());
        }else{
            restClient = Feign.builder()
                    .encoder(new JacksonEncoder())
                    .decoder(new JacksonDecoder())
                    .logger(new Slf4jLogger())
                    .client(new Http2Client())
                    .target(TerrakubeClient.class, restClientProperties.getUrl());
        }


        return restClient;
    }
}
