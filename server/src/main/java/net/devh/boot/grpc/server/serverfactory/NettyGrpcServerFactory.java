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

package net.devh.boot.grpc.server.serverfactory;

import static java.util.Objects.requireNonNull;
import static net.devh.boot.grpc.common.util.GrpcUtils.DOMAIN_SOCKET_ADDRESS_PREFIX;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;

import com.google.common.net.InetAddresses;

import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyServerBuilder;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerDomainSocketChannel;
import io.netty.channel.unix.DomainSocketAddress;
import io.netty.handler.ssl.SslContextBuilder;
import net.devh.boot.grpc.common.security.KeyStoreUtils;
import net.devh.boot.grpc.common.util.GrpcUtils;
import net.devh.boot.grpc.server.config.ClientAuth;
import net.devh.boot.grpc.server.config.SimpleGrpcServerProperties;
import net.devh.boot.grpc.server.config.SimpleGrpcServerProperties.Security;

/**
 * Factory for netty based grpc servers.
 *
 * @author Michael (yidongnan@gmail.com)
 */
public class NettyGrpcServerFactory extends AbstractGrpcServerFactory<NettyServerBuilder> {

    /**
     * Creates a new netty server factory with the given properties.
     *
     * @param properties The properties used to configure the server.
     * @param serverConfigurers The server configurers to use. Can be empty.
     */
    public NettyGrpcServerFactory(final SimpleGrpcServerProperties properties,
            final List<GrpcServerConfigurer> serverConfigurers) {
        super(properties, serverConfigurers);
    }

    @Override
    protected NettyServerBuilder newServerBuilder() {
        final String address = getAddress();
        final int port = getPort();
        if (address.startsWith(DOMAIN_SOCKET_ADDRESS_PREFIX)) {
            final String path = GrpcUtils.extractDomainSocketAddressPath(address);
            return NettyServerBuilder.forAddress(new DomainSocketAddress(path))
                    .channelType(EpollServerDomainSocketChannel.class)
                    .bossEventLoopGroup(new EpollEventLoopGroup(1))
                    .workerEventLoopGroup(new EpollEventLoopGroup());
        } else if (SimpleGrpcServerProperties.ANY_IP_ADDRESS.equals(address)) {
            return NettyServerBuilder.forPort(port);
        } else {
            return NettyServerBuilder.forAddress(new InetSocketAddress(InetAddresses.forString(address), port));
        }
    }

    @Override
    // Keep this in sync with ShadedNettyGrpcServerFactory#configureConnectionLimits
    protected void configureConnectionLimits(final NettyServerBuilder builder) {
        if (this.properties.getMaxConnectionIdle() != null) {
            builder.maxConnectionIdle(this.properties.getMaxConnectionIdle().toNanos(), TimeUnit.NANOSECONDS);
        }
        if (this.properties.getMaxConnectionAge() != null) {
            builder.maxConnectionAge(this.properties.getMaxConnectionAge().toNanos(), TimeUnit.NANOSECONDS);
        }
        if (this.properties.getMaxConnectionAgeGrace() != null) {
            builder.maxConnectionAgeGrace(this.properties.getMaxConnectionAgeGrace().toNanos(), TimeUnit.NANOSECONDS);
        }
    }

    @Override
    // Keep this in sync with ShadedNettyGrpcServerFactory#configureKeepAlive
    protected void configureKeepAlive(final NettyServerBuilder builder) {
        if (this.properties.isEnableKeepAlive()) {
            builder.keepAliveTime(this.properties.getKeepAliveTime().toNanos(), TimeUnit.NANOSECONDS)
                    .keepAliveTimeout(this.properties.getKeepAliveTimeout().toNanos(), TimeUnit.NANOSECONDS);
        }
        builder.permitKeepAliveTime(this.properties.getPermitKeepAliveTime().toNanos(), TimeUnit.NANOSECONDS)
                .permitKeepAliveWithoutCalls(this.properties.isPermitKeepAliveWithoutCalls());
    }


    @Override
    // Keep this in sync with ShadedNettyGrpcServerFactory#configureSecurity
    protected void configureSecurity(final NettyServerBuilder builder) {
        final Security security = this.properties.getSecurity();
        if (security.isEnabled()) {
            // Provided server certificates
            final SslContextBuilder sslContextBuilder = newServerSslContextBuilder(security);

            // Accepted client certificates
            configureAcceptedClientCertificates(security, sslContextBuilder);

            // Other configuration
            if (security.getCiphers() != null && !security.getCiphers().isEmpty()) {
                sslContextBuilder.ciphers(security.getCiphers());
            }

            if (security.getProtocols() != null && security.getProtocols().length > 0) {
                sslContextBuilder.protocols(security.getProtocols());
            }

            try {
                builder.sslContext(sslContextBuilder.build());
            } catch (final SSLException e) {
                throw new IllegalStateException("Failed to create ssl context for grpc server", e);
            }
        }
    }

    /**
     * Creates a new server ssl context builder.
     *
     * @param security The security configuration to use.
     * @return The newly created SslContextBuilder.
     */
    // Keep this in sync with ShadedNettyGrpcServerFactory#newServerSslContextBuilder
    protected static SslContextBuilder newServerSslContextBuilder(final Security security) {
        try {
            final File privateKey = security.getPrivateKey();
            final File keyStore = security.getKeyStore();

            if (privateKey != null) {
                final File certificateChain =
                        requireNonNull(security.getCertificateChain(), "certificateChain");
                final String privateKeyPassword = security.getPrivateKeyPassword();
                try (InputStream certificateChainStream = new FileInputStream(certificateChain);
                        InputStream privateKeyStream = new FileInputStream(privateKey)) {
                    return GrpcSslContexts.forServer(certificateChainStream, privateKeyStream, privateKeyPassword);
                }

            } else if (keyStore != null) {
                final KeyManagerFactory keyManagerFactory = KeyStoreUtils.loadKeyManagerFactory(
                        security.getKeyStoreFormat(), keyStore, security.getKeyStorePassword());
                return GrpcSslContexts.configure(SslContextBuilder.forServer(keyManagerFactory));

            } else {
                throw new IllegalStateException("Neither privateKey nor keyStore configured");
            }
        } catch (final Exception e) {
            throw new IllegalArgumentException("Failed to create SSLContext (PK/Cert)", e);
        }
    }

    /**
     * Configures the client certificates accepted by the ssl context.
     *
     * @param security The security configuration to use.
     * @param sslContextBuilder The ssl context builder to configure.
     */
    // Keep this in sync with ShadedNettyGrpcServerFactory#configureAcceptedClientCertificates
    protected static void configureAcceptedClientCertificates(
            final Security security,
            final SslContextBuilder sslContextBuilder) {

        if (security.getClientAuth() != ClientAuth.NONE) {
            sslContextBuilder.clientAuth(of(security.getClientAuth()));

            try {
                final File trustCertCollection = security.getTrustCertCollection();
                final File trustStore = security.getTrustStore();

                if (trustCertCollection != null) {
                    try (InputStream trustCertCollectionStream = new FileInputStream(trustCertCollection)) {
                        sslContextBuilder.trustManager(trustCertCollectionStream);
                    }

                } else if (trustStore != null) {
                    final TrustManagerFactory trustManagerFactory = KeyStoreUtils.loadTrustManagerFactory(
                            security.getTrustStoreFormat(), trustStore, security.getTrustStorePassword());
                    sslContextBuilder.trustManager(trustManagerFactory);

                } else {
                    // Use system default
                }
            } catch (final Exception e) {
                throw new IllegalArgumentException("Failed to create SSLContext (TrustStore)", e);
            }
        }
    }

    /**
     * Converts the given client auth option to netty's client auth.
     *
     * @param clientAuth The client auth option to convert.
     * @return The converted client auth option.
     */
    protected static io.netty.handler.ssl.ClientAuth of(final ClientAuth clientAuth) {
        switch (clientAuth) {
            case NONE:
                return io.netty.handler.ssl.ClientAuth.NONE;
            case OPTIONAL:
                return io.netty.handler.ssl.ClientAuth.OPTIONAL;
            case REQUIRE:
                return io.netty.handler.ssl.ClientAuth.REQUIRE;
            default:
                throw new IllegalArgumentException("Unsupported ClientAuth: " + clientAuth);
        }
    }

}
