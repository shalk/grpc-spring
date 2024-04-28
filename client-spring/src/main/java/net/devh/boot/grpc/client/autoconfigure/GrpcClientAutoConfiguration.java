/*
 * Copyright (c) 2016-2023 The gRPC-Spring Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.devh.boot.grpc.client.autoconfigure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import io.grpc.ClientInterceptor;
import net.devh.boot.grpc.client.config.ConfigConverter;
import net.devh.boot.grpc.client.interceptor.GlobalClientInterceptorConfigurer;
import net.devh.boot.grpc.client.interceptor.GrpcGlobalClientInterceptor;
import net.devh.boot.grpc.common.util.InterceptorOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import io.grpc.CompressorRegistry;
import io.grpc.DecompressorRegistry;
import io.grpc.NameResolverProvider;
import io.grpc.NameResolverRegistry;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.channelfactory.GrpcChannelConfigurer;
import net.devh.boot.grpc.client.channelfactory.GrpcChannelFactory;
import net.devh.boot.grpc.client.channelfactory.InProcessChannelFactory;
import net.devh.boot.grpc.client.channelfactory.InProcessOrAlternativeChannelFactory;
import net.devh.boot.grpc.client.channelfactory.NettyChannelFactory;
import net.devh.boot.grpc.client.channelfactory.ShadedNettyChannelFactory;
import net.devh.boot.grpc.client.config.GrpcChannelsProperties;
import net.devh.boot.grpc.client.inject.GrpcClientBeanPostProcessor;
import net.devh.boot.grpc.client.inject.GrpcClientConstructorInjectionBeanFactoryPostProcessor;
import net.devh.boot.grpc.client.interceptor.AnnotationGlobalClientInterceptorConfigurer;
import net.devh.boot.grpc.client.interceptor.GlobalClientInterceptorRegistry;
import net.devh.boot.grpc.client.nameresolver.NameResolverRegistration;
import net.devh.boot.grpc.client.stubfactory.AsyncStubFactory;
import net.devh.boot.grpc.client.stubfactory.BlockingStubFactory;
import net.devh.boot.grpc.client.stubfactory.FutureStubFactory;
import net.devh.boot.grpc.common.autoconfigure.GrpcCommonCodecAutoConfiguration;

import static com.google.common.collect.Maps.transformValues;

/**
 * The auto configuration used by Spring-Boot that contains all beans to create and inject grpc clients into beans.
 *
 * @author Michael (yidongnan@gmail.com)
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties
@AutoConfigureAfter(name = "org.springframework.cloud.client.CommonsClientAutoConfiguration",
        value = GrpcCommonCodecAutoConfiguration.class)
public class GrpcClientAutoConfiguration {

    @Bean
    static GrpcClientBeanPostProcessor grpcClientBeanPostProcessor(final ApplicationContext applicationContext) {
        return new GrpcClientBeanPostProcessor(applicationContext);
    }

    @Bean
    static GrpcClientConstructorInjectionBeanFactoryPostProcessor grpcClientConstructorInjectBeanFactoryPostProcessor() {
        return new GrpcClientConstructorInjectionBeanFactoryPostProcessor();
    }

    @Bean
    AsyncStubFactory asyncStubFactory() {
        return new AsyncStubFactory();
    }

    @Bean
    BlockingStubFactory blockingStubFactory() {
        return new BlockingStubFactory();
    }

    @Bean
    FutureStubFactory futureStubFactory() {
        return new FutureStubFactory();
    }

    @ConditionalOnMissingBean
    @Bean
    GrpcChannelsProperties grpcChannelsProperties() {
        return new GrpcChannelsProperties();
    }

    @ConditionalOnMissingBean
    @Bean
    GlobalClientInterceptorRegistry globalClientInterceptorRegistry(final ApplicationContext applicationContext) {
        Supplier<List<GlobalClientInterceptorConfigurer>> supplier = new Supplier<List<GlobalClientInterceptorConfigurer>>() {
            @Override
            public List<GlobalClientInterceptorConfigurer> get() {
                List<GlobalClientInterceptorConfigurer> list = new ArrayList<>();
                 list.addAll(applicationContext.getBeansOfType(GlobalClientInterceptorConfigurer.class).values());
                 return list;
            }
        };
        Supplier<Comparator<Object> supplier1 = new Supplier<>() {

            @Override
            public Comparator<Object> get() {
                return InterceptorOrder.beanFactoryAwareOrderComparator(applicationContext, ClientInterceptor.class);
            }
        };

        return new GlobalClientInterceptorRegistry(supplier,supplier1);
    }

    @Bean
    @Lazy
    AnnotationGlobalClientInterceptorConfigurer annotationGlobalClientInterceptorConfigurer(
            final ApplicationContext applicationContext) {
        Supplier<Map<String, ClientInterceptor>> supplier = new Supplier<>() {

            @Override
            public Map<String, ClientInterceptor> get() {
                return transformValues(applicationContext.getBeansWithAnnotation(GrpcGlobalClientInterceptor.class),
                    ClientInterceptor.class::cast);
            }
        };
        return new AnnotationGlobalClientInterceptorConfigurer(supplier);
    }

    /**
     * Creates a new NameResolverRegistration. This ensures that the NameResolverProvider's get unregistered when spring
     * shuts down. This is mostly required for tests/when running multiple application contexts within the same JVM.
     *
     * @param nameResolverProviders The spring managed providers to manage.
     * @return The newly created NameResolverRegistration bean.
     */
    @ConditionalOnMissingBean
    @Lazy
    @Bean(destroyMethod="destroy")
    NameResolverRegistration grpcNameResolverRegistration(
            @Autowired(required = false) final List<NameResolverProvider> nameResolverProviders) {
        final NameResolverRegistration nameResolverRegistration = new NameResolverRegistration(nameResolverProviders);
        nameResolverRegistration.register(NameResolverRegistry.getDefaultRegistry());
        return nameResolverRegistration;
    }

    @ConditionalOnBean(CompressorRegistry.class)
    @Bean
    GrpcChannelConfigurer compressionChannelConfigurer(final CompressorRegistry registry) {
        return (builder, name) -> builder.compressorRegistry(registry);
    }

    @ConditionalOnBean(DecompressorRegistry.class)
    @Bean
    GrpcChannelConfigurer decompressionChannelConfigurer(final DecompressorRegistry registry) {
        return (builder, name) -> builder.decompressorRegistry(registry);
    }

    @ConditionalOnMissingBean(GrpcChannelConfigurer.class)
    @Bean
    List<GrpcChannelConfigurer> defaultChannelConfigurers() {
        return Collections.emptyList();
    }

    // First try the shaded netty channel factory
    @ConditionalOnMissingBean(GrpcChannelFactory.class)
    @ConditionalOnClass(name = {"io.grpc.netty.shaded.io.netty.channel.Channel",
            "io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder"})
    @Bean
    @Lazy
    GrpcChannelFactory shadedNettyGrpcChannelFactory(
            final GrpcChannelsProperties properties,
            final GlobalClientInterceptorRegistry globalClientInterceptorRegistry,
            final List<GrpcChannelConfigurer> channelConfigurers) {

        log.info("Detected grpc-netty-shaded: Creating ShadedNettyChannelFactory + InProcessChannelFactory");
        final ShadedNettyChannelFactory channelFactory =
            new ShadedNettyChannelFactory(ConfigConverter.toSimples(properties), globalClientInterceptorRegistry, channelConfigurers);
        final InProcessChannelFactory inProcessChannelFactory =
                new InProcessChannelFactory(ConfigConverter.toSimples(properties), globalClientInterceptorRegistry, channelConfigurers);
        return new InProcessOrAlternativeChannelFactory(ConfigConverter.toSimples(properties), inProcessChannelFactory, channelFactory);
    }

    // Then try the normal netty channel factory
    @ConditionalOnMissingBean(GrpcChannelFactory.class)
    @ConditionalOnClass(name = {"io.netty.channel.Channel", "io.grpc.netty.NettyChannelBuilder"})
    @Bean
    @Lazy
    GrpcChannelFactory nettyGrpcChannelFactory(
            final GrpcChannelsProperties properties,
            final GlobalClientInterceptorRegistry globalClientInterceptorRegistry,
            final List<GrpcChannelConfigurer> channelConfigurers) {

        log.info("Detected grpc-netty: Creating NettyChannelFactory + InProcessChannelFactory");
        final NettyChannelFactory channelFactory =
                new NettyChannelFactory(ConfigConverter.toSimples(properties), globalClientInterceptorRegistry, channelConfigurers);
        final InProcessChannelFactory inProcessChannelFactory =
                new InProcessChannelFactory(ConfigConverter.toSimples(properties), globalClientInterceptorRegistry, channelConfigurers);
        return new InProcessOrAlternativeChannelFactory(ConfigConverter.toSimples(properties), inProcessChannelFactory, channelFactory);
    }

    // Finally try the in process channel factory
    @ConditionalOnMissingBean(GrpcChannelFactory.class)
    @Bean
    @Lazy
    GrpcChannelFactory inProcessGrpcChannelFactory(
            final GrpcChannelsProperties properties,
            final GlobalClientInterceptorRegistry globalClientInterceptorRegistry,
            final List<GrpcChannelConfigurer> channelConfigurers) {

        log.warn("Could not find a GrpcChannelFactory on the classpath: Creating InProcessChannelFactory as fallback");
        return new InProcessChannelFactory(ConfigConverter.toSimples(properties), globalClientInterceptorRegistry, channelConfigurers);
    }

}
