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

package net.devh.boot.grpc.client.config;

import java.io.File;
import java.net.URI;
import java.security.KeyStore;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.grpc.LoadBalancerRegistry;
import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.devh.boot.grpc.common.security.KeyStoreUtils;

/**
 * The channel properties for a single named gRPC channel or service reference.
 *
 * @author Michael (yidongnan@gmail.com)
 * @author Daniel Theuke (daniel.theuke@heuboe.de)
 * @since 5/17/16
 */
@ToString
@EqualsAndHashCode
public class SimpleGrpcChannelProperties {

    // --------------------------------------------------
    // Target Address
    // --------------------------------------------------

    private static final String DEFAULT_DEFAULT_LOAD_BALANCING_POLICY = "round_robin";
    private static final boolean DEFAULT_ENABLE_KEEP_ALIVE = false;

    // --------------------------------------------------
    // defaultLoadBalancingPolicy
    // --------------------------------------------------
    private static final Duration DEFAULT_KEEP_ALIVE_TIME = Duration.of(5, ChronoUnit.MINUTES);
    private static final Duration DEFAULT_KEEP_ALIVE_TIMEOUT = Duration.of(20, ChronoUnit.SECONDS);
    private static final boolean DEFAULT_KEEP_ALIVE_WITHOUT_CALLS = false;
    private static final Duration DEFAULT_SHUTDOWN_GRACE_PERIOD = Duration.ofSeconds(30);

    // --------------------------------------------------
    // KeepAlive
    // --------------------------------------------------
    private static final NegotiationType DEFAULT_NEGOTIATION_TYPE = NegotiationType.TLS;
    private static final Duration DEFAULT_IMMEDIATE_CONNECT = Duration.ZERO;
    private final Security security = new Security();
    @Getter
    @Setter
    private URI address = null;

    // --------------------------------------------------
    private String defaultLoadBalancingPolicy;
    private Boolean enableKeepAlive;
    private Duration keepAliveTime;
    private Duration keepAliveTimeout;

    // --------------------------------------------------
    private Boolean keepAliveWithoutCalls;
    private Duration shutdownGracePeriod;
    @Getter
    @Setter
    private Integer maxInboundMessageSize;
    @Getter
    @Setter
    private Integer maxInboundMetadataSize;

    // --------------------------------------------------
    private NegotiationType negotiationType;
    private Duration immediateConnectTimeout;
    private String userAgent = null;

    public void setAddress(final String address) {
        this.address = address == null ? null : URI.create(address);
    }

    // --------------------------------------------------

    /**
     * Gets the default load balancing policy this channel should use.
     *
     * @return The default load balancing policy.
     * @see ManagedChannelBuilder#defaultLoadBalancingPolicy(String)
     */
    public String getDefaultLoadBalancingPolicy() {
        return this.defaultLoadBalancingPolicy == null ? DEFAULT_DEFAULT_LOAD_BALANCING_POLICY
                : this.defaultLoadBalancingPolicy;
    }

    /**
     * Sets the default load balancing policy for this channel. This config might be overwritten by the service config
     * received from the target address. The names have to be resolvable from the {@link LoadBalancerRegistry}. By
     * default this the {@code round_robin} policy. Please note that this policy is different from the normal grpc-java
     * default policy {@code pick_first}.
     *
     * @param defaultLoadBalancingPolicy The default load balancing policy to use or null to use the fallback.
     */
    public void setDefaultLoadBalancingPolicy(final String defaultLoadBalancingPolicy) {
        this.defaultLoadBalancingPolicy = defaultLoadBalancingPolicy;
    }

    /**
     * Gets whether keepAlive is enabled.
     *
     * @return True, if keep alive should be enabled. False otherwise.
     * @see #setEnableKeepAlive(Boolean)
     */
    public boolean isEnableKeepAlive() {
        return this.enableKeepAlive == null ? DEFAULT_ENABLE_KEEP_ALIVE : this.enableKeepAlive;
    }

    /**
     * Sets whether keepAlive should be enabled. Defaults to false.
     *
     * @param enableKeepAlive True, to enable. False, to disable. Null, to use the fallback.
     */
    public void setEnableKeepAlive(final Boolean enableKeepAlive) {
        this.enableKeepAlive = enableKeepAlive;
    }

    // --------------------------------------------------
    // Message Transfer
    // --------------------------------------------------

    /**
     * Gets the default delay before we send a keepAlive.
     *
     * @return The default delay before sending keepAlives.
     * @see #setKeepAliveTime(Duration)
     */
    public Duration getKeepAliveTime() {
        return this.keepAliveTime == null ? DEFAULT_KEEP_ALIVE_TIME : this.keepAliveTime;
    }

    /**
     * The default delay before we send a keepAlives. Defaults to {@code 5min}. Default unit {@link ChronoUnit#SECONDS
     * SECONDS}. Please note that shorter intervals increase the network burden for the server. Cannot be lower than
     * permitKeepAliveTime on server (default 5min).
     *
     * @param keepAliveTime The new default delay before sending keepAlives, or null to use the fallback.
     * @see #setEnableKeepAlive(Boolean)
     * @see NettyChannelBuilder#keepAliveTime(long, TimeUnit)
     */
    public void setKeepAliveTime(final Duration keepAliveTime) {
        this.keepAliveTime = keepAliveTime;
    }

    /**
     * The default timeout for a keepAlives ping request.
     *
     * @return The default timeout for a keepAlives ping request.
     * @see #setKeepAliveTimeout(Duration)
     */
    public Duration getKeepAliveTimeout() {
        return this.keepAliveTimeout == null ? DEFAULT_KEEP_ALIVE_TIMEOUT : this.keepAliveTimeout;
    }

    /**
     * The default timeout for a keepAlives ping request. Defaults to {@code 20s}. Default unit
     * {@link ChronoUnit#SECONDS SECONDS}.
     *
     * @param keepAliveTimeout The default timeout for a keepAlives ping request.
     * @see #setEnableKeepAlive(Boolean)
     * @see NettyChannelBuilder#keepAliveTimeout(long, TimeUnit)
     */
    public void setKeepAliveTimeout(final Duration keepAliveTimeout) {
        this.keepAliveTimeout = keepAliveTimeout;
    }
    // --------------------------------------------------

    /**
     * Gets whether keepAlive will be performed when there are no outstanding RPC on a connection.
     *
     * @return True, if keepAlives should be performed even when there are no RPCs. False otherwise.
     * @see #setKeepAliveWithoutCalls(Boolean)
     */
    public boolean isKeepAliveWithoutCalls() {
        return this.keepAliveWithoutCalls == null ? DEFAULT_KEEP_ALIVE_WITHOUT_CALLS : this.keepAliveWithoutCalls;
    }

    /**
     * Sets whether keepAlive will be performed when there are no outstanding RPC on a connection. Defaults to
     * {@code false}.
     *
     * @param keepAliveWithoutCalls whether keepAlive will be performed when there are no outstanding RPC on a
     *        connection.
     * @see #setEnableKeepAlive(Boolean)
     * @see NettyChannelBuilder#keepAliveWithoutCalls(boolean)
     */
    public void setKeepAliveWithoutCalls(final Boolean keepAliveWithoutCalls) {
        this.keepAliveWithoutCalls = keepAliveWithoutCalls;
    }

    /**
     * Gets the time to wait for the channel to gracefully shutdown. If set to a negative value, the channel waits
     * forever. If set to {@code 0} the channel will force shutdown immediately. Defaults to {@code 30s}.
     *
     * @return The time to wait for a graceful shutdown.
     */
    public Duration getShutdownGracePeriod() {
        return this.shutdownGracePeriod == null ? DEFAULT_SHUTDOWN_GRACE_PERIOD : this.shutdownGracePeriod;
    }

    /**
     * Sets the time to wait for the channel to gracefully shutdown (completing all requests). If set to a negative
     * value, the channel waits forever. If set to {@code 0} the channel will force shutdown immediately. Defaults to
     * {@code 30s}.
     *
     * @param shutdownGracePeriod The time to wait for a graceful shutdown.
     */
    public void setShutdownGracePeriod(final Duration shutdownGracePeriod) {
        this.shutdownGracePeriod = shutdownGracePeriod;
    }



    /**
     * Gets the negotiation type to use on the connection.
     *
     * @return The negotiation type that the channel will use.
     * @see #setNegotiationType(NegotiationType)
     */
    public NegotiationType getNegotiationType() {
        return this.negotiationType == null ? DEFAULT_NEGOTIATION_TYPE : this.negotiationType;
    }

    /**
     * Sets the negotiation type to use on the connection. Either of {@link NegotiationType#TLS TLS} (recommended),
     * {@link NegotiationType#PLAINTEXT_UPGRADE PLAINTEXT_UPGRADE} or {@link NegotiationType#PLAINTEXT PLAINTEXT}.
     * Defaults to TLS.
     *
     * @param negotiationType The negotiation type to use or null to use the fallback.
     */
    public void setNegotiationType(final NegotiationType negotiationType) {
        this.negotiationType = negotiationType;
    }

    // --------------------------------------------------

    /**
     * Get the connection timeout at application startup.
     *
     * @return connection timeout at application startup.
     * @see #setImmediateConnectTimeout(Duration)
     */
    public Duration getImmediateConnectTimeout() {
        return this.immediateConnectTimeout == null ? DEFAULT_IMMEDIATE_CONNECT : this.immediateConnectTimeout;
    }

    /**
     * If set to a positive duration instructs the client to connect to the gRPC endpoint when the GRPC stub is created.
     * As a result the application startup will be slightly slower due to connection process being executed
     * synchronously up to the maximum to connection timeout. If the connection fails, the stub will fail to create with
     * an exception which in turn causes the application context startup to fail. Defaults to {@code 0}.
     *
     * @param immediateConnectTimeout Connection timeout at application startup.
     */
    public void setImmediateConnectTimeout(final Duration immediateConnectTimeout) {
        if (immediateConnectTimeout.isNegative()) {
            throw new IllegalArgumentException("Timeout can't be negative");
        }
        this.immediateConnectTimeout = immediateConnectTimeout;
    }

    /**
     * Get custom User-Agent for the channel.
     *
     * @return custom User-Agent for the channel.
     * @see #setUserAgent(String)
     */
    public String getUserAgent() {
        return this.userAgent;
    }

    // --------------------------------------------------

    /**
     * Sets custom User-Agent HTTP header.
     *
     * @param userAgent Custom User-Agent.
     * @see ManagedChannelBuilder#userAgent(String)
     */
    public void setUserAgent(final String userAgent) {
        this.userAgent = userAgent;
    }

    /**
     * Gets the options for transport security.
     *
     * @return The options for transport security.
     */
    public Security getSecurity() {
        return this.security;
    }

    /**
     * Copies the defaults from the given configuration. Values are considered "default" if they are null. Please note
     * that the getters might return fallback values instead.
     *
     * @param config The config to copy the defaults from.
     */
    public void copyDefaultsFrom(final SimpleGrpcChannelProperties config) {
        if (this == config) {
            return;
        }
        if (this.address == null) {
            this.address = config.address;
        }
        if (this.defaultLoadBalancingPolicy == null) {
            this.defaultLoadBalancingPolicy = config.defaultLoadBalancingPolicy;
        }
        if (this.enableKeepAlive == null) {
            this.enableKeepAlive = config.enableKeepAlive;
        }
        if (this.keepAliveTime == null) {
            this.keepAliveTime = config.keepAliveTime;
        }
        if (this.keepAliveTimeout == null) {
            this.keepAliveTimeout = config.keepAliveTimeout;
        }
        if (this.keepAliveWithoutCalls == null) {
            this.keepAliveWithoutCalls = config.keepAliveWithoutCalls;
        }
        if (this.shutdownGracePeriod == null) {
            this.shutdownGracePeriod = config.shutdownGracePeriod;
        }
        if (this.maxInboundMessageSize == null) {
            this.maxInboundMessageSize = config.maxInboundMessageSize;
        }
        if (this.maxInboundMetadataSize == null) {
            this.maxInboundMetadataSize = config.maxInboundMetadataSize;
        }
        if (this.negotiationType == null) {
            this.negotiationType = config.negotiationType;
        }
        if (this.immediateConnectTimeout == null) {
            this.immediateConnectTimeout = config.immediateConnectTimeout;
        }
        if (this.userAgent == null) {
            this.userAgent = config.userAgent;
        }
        this.security.copyDefaultsFrom(config.security);
    }

    /**
     * A container with options for the channel's transport security.
     */
    @ToString
    @EqualsAndHashCode
    public static class Security {

        private static final boolean DEFAULT_CLIENT_AUTH_ENABLED = false;
        private static final String DEFAULT_KEY_STORE_FORMAT = KeyStoreUtils.FORMAT_AUTODETECT;
        private Boolean clientAuthEnabled;
        @Getter
        @Setter
        private File certificateChain = null;

        // --------------------------------------------------
        @Getter
        @Setter
        private File privateKey = null;
        private String privateKeyPassword = null;
        private String keyStoreFormat = null;

        // --------------------------------------------------
        @Getter
        @Setter
        private File keyStore = null;
        private String keyStorePassword = null;
        @Getter
        @Setter
        private File trustCertCollection = null;

        // --------------------------------------------------
        private String trustStoreFormat = null;
        @Getter
        @Setter
        private File trustStore = null;
        private String trustStorePassword = null;

        // --------------------------------------------------
        private String authorityOverride = null;
        private List<String> ciphers = null;
        private String[] protocols = null;

        /**
         * Gets whether client can authenticate using certificates.
         *
         * @return True, if the client can authenticate itself using certificates.
         * @see #setClientAuthEnabled(Boolean)
         */
        public boolean isClientAuthEnabled() {
            return this.clientAuthEnabled == null ? DEFAULT_CLIENT_AUTH_ENABLED : this.clientAuthEnabled;
        }

        // --------------------------------------------------

        /**
         * Set whether client can authenticate using certificates. Defaults to {@code false}.
         *
         * @param clientAuthEnabled Whether the client can authenticate itself using certificates.
         */
        public void setClientAuthEnabled(final Boolean clientAuthEnabled) {
            this.clientAuthEnabled = clientAuthEnabled;
        }



        // --------------------------------------------------



        /**
         * Gets the password for the private key.
         *
         * @return The password for the private key or null, if the private key is not set or not encrypted.
         * @see #setPrivateKeyPassword(String)
         */
        public String getPrivateKeyPassword() {
            return this.privateKeyPassword;
        }

        // --------------------------------------------------

        /**
         * Sets the password for the private key.
         *
         * @param privateKeyPassword The password for the private key.
         * @see SslContextBuilder#keyManager(File, File, String)
         */
        public void setPrivateKeyPassword(final String privateKeyPassword) {
            this.privateKeyPassword = privateKeyPassword;
        }

        /**
         * The format of the {@link #keyStore}.
         *
         * <p>
         * Possible values includes:
         * </p>
         * <ul>
         * <li>{@link KeyStoreUtils#FORMAT_AUTODETECT AUTODETECT} (default)</li>
         * <li>{@code JKS} ({@code .jks})</li>
         * <li>{@code PKCS12} ({@code .p12})</li>
         * <li>any supported {@link KeyStore} format</li>
         * <li>Fallback to {@code KeyStore#getDefaultType()}</li>
         * </ul>
         *
         * @return The key store format to use.
         */
        public String getKeyStoreFormat() {
            return this.keyStoreFormat == null ? DEFAULT_KEY_STORE_FORMAT : this.keyStoreFormat;
        }

        /**
         * The format of the {@link #keyStore}.
         *
         * <p>
         * Possible values includes:
         * </p>
         * <ul>
         * <li>{@link KeyStoreUtils#FORMAT_AUTODETECT AUTODETECT} (default)</li>
         * <li>{@code JKS} ({@code .jks})</li>
         * <li>{@code PKCS12} ({@code .p12})</li>
         * <li>any supported {@link KeyStore} format</li>
         * <li>Fallback to {@code KeyStore#getDefaultType()}</li>
         * </ul>
         *
         * @param keyStoreFormat The key store format to use
         */
        public void setKeyStoreFormat(final String keyStoreFormat) {
            this.keyStoreFormat = keyStoreFormat;
        }

        // --------------------------------------------------


        /**
         * Password for the key store. Use is combination with {@link #keyStore}.
         *
         * @return The password for the key store or null.
         */
        public String getKeyStorePassword() {
            return this.keyStorePassword;
        }

        // --------------------------------------------------

        /**
         * Password for the key store. Use is combination with {@link #keyStore}.
         *
         * @param keyStorePassword The password for the key store.
         */
        public void setKeyStorePassword(final String keyStorePassword) {
            this.keyStorePassword = keyStorePassword;
        }



        // --------------------------------------------------

        /**
         * The format of the {@link #trustStore}.
         *
         * <p>
         * Possible values includes:
         * </p>
         * <ul>
         * <li>{@link KeyStoreUtils#FORMAT_AUTODETECT AUTODETECT} (default)</li>
         * <li>{@code JKS} ({@code .jks})</li>
         * <li>{@code PKCS12} ({@code .p12})</li>
         * <li>any supported {@link KeyStore} format</li>
         * <li>Fallback to {@code KeyStore#getDefaultType()}</li>
         * </ul>
         *
         * @return The trust store format to use.
         */
        public String getTrustStoreFormat() {
            return this.trustStoreFormat == null ? DEFAULT_KEY_STORE_FORMAT : this.trustStoreFormat;
        }

        /**
         * The format of the {@link #trustStore}.
         *
         * <p>
         * Possible values includes:
         * </p>
         * <ul>
         * <li>{@link KeyStoreUtils#FORMAT_AUTODETECT AUTODETECT} (default)</li>
         * <li>{@code JKS} ({@code .jks})</li>
         * <li>{@code PKCS12} ({@code .p12})</li>
         * <li>any supported {@link KeyStore} format</li>
         * <li>Fallback to {@code KeyStore#getDefaultType()}</li>
         * </ul>
         *
         * @param trustStoreFormat The trust store format to use.
         */
        public void setTrustStoreFormat(final String trustStoreFormat) {
            this.trustStoreFormat = trustStoreFormat;
        }



        /**
         * Password for the trust store. Use is combination with {@link #trustStore}.
         *
         * @return The password for the trust store or null.
         */
        public String getTrustStorePassword() {
            return this.trustStorePassword;
        }

        /**
         * Password for the trust store. Use is combination with {@link #trustStore}.
         *
         * @param trustStorePassword The password for the trust store.
         */
        public void setTrustStorePassword(final String trustStorePassword) {
            this.trustStorePassword = trustStorePassword;
        }

        // --------------------------------------------------

        /**
         * Gets the authority to check for during server certificate verification.
         *
         * @return The override for the authority to check for or null, there is no override configured.
         * @see #setAuthorityOverride(String)
         */
        public String getAuthorityOverride() {
            return this.authorityOverride;
        }

        /**
         * Sets the authority to check for during server certificate verification. By default the clients will use the
         * name of the client to check the server certificate's common + alternative names.
         *
         * @param authorityOverride The authority to check for in the certificate, or null to use the default checks.
         * @see NettyChannelBuilder#overrideAuthority(String)
         */
        public void setAuthorityOverride(final String authorityOverride) {
            this.authorityOverride = authorityOverride;
        }

        /**
         * Gets the cipher suite accepted for secure connections (in the order of preference).
         *
         * @return The cipher suite accepted for secure connections or null.
         */
        public List<String> getCiphers() {
            return this.ciphers;
        }

        // --------------------------------------------------

        /**
         * Sets the cipher suite accepted for secure connections (in the order of preference). If not specified (null),
         * then the default suites should be used.
         *
         * @param ciphers Cipher suite consisting of one or more cipher strings separated by colons, commas or spaces
         * @see SslContextBuilder#ciphers(Iterable)
         */
        public void setCiphers(final String ciphers) {
            if (ciphers == null) {
                this.ciphers = null;
            } else {
                this.ciphers = Arrays.asList(ciphers.split("[ :,]"));
            }
        }

        /**
         * Gets the TLS protocols accepted for secure connections
         *
         * @return The protocols accepted for secure connections or null.
         */
        public String[] getProtocols() {
            return this.protocols;
        }

        /**
         * Sets the TLS protocols accepted for secure connections. If not specified (null), then the default ones will
         * be used.
         *
         * @param protocols Protocol list consisting of one or more protocols separated by colons, commas or spaces.
         * @see SslContextBuilder#protocols(String...)
         */
        public void setProtocols(final String protocols) {
            if (protocols == null) {
                this.protocols = null;
            } else {
                this.protocols = protocols.split("[ :,]");
            }
        }

        // --------------------------------------------------

        /**
         * Copies the defaults from the given configuration. Values are considered "default" if they are null. Please
         * note that the getters might return fallback values instead.
         *
         * @param config The config to copy the defaults from.
         */
        public void copyDefaultsFrom(final Security config) {
            if (this == config) {
                return;
            }
            if (this.clientAuthEnabled == null) {
                this.clientAuthEnabled = config.clientAuthEnabled;
            }
            if (this.certificateChain == null) {
                this.certificateChain = config.certificateChain;
            }
            if (this.privateKey == null) {
                this.privateKey = config.privateKey;
            }
            if (this.privateKeyPassword == null) {
                this.privateKeyPassword = config.privateKeyPassword;
            }
            if (this.keyStoreFormat == null) {
                this.keyStoreFormat = config.keyStoreFormat;
            }
            if (this.keyStore == null) {
                this.keyStore = config.keyStore;
            }
            if (this.keyStorePassword == null) {
                this.keyStorePassword = config.keyStorePassword;
            }
            if (this.trustCertCollection == null) {
                this.trustCertCollection = config.trustCertCollection;
            }
            if (this.trustStoreFormat == null) {
                this.trustStoreFormat = config.trustStoreFormat;
            }
            if (this.trustStore == null) {
                this.trustStore = config.trustStore;
            }
            if (this.trustStorePassword == null) {
                this.trustStorePassword = config.trustStorePassword;
            }
            if (this.authorityOverride == null) {
                this.authorityOverride = config.authorityOverride;
            }
            if (this.ciphers == null) {
                this.ciphers = config.ciphers;
            }
            if (this.protocols == null) {
                this.protocols = config.protocols;
            }
        }

    }

}
