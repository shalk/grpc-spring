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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * A container for named channel properties. Each channel has its own configuration. If you try to get a channel that
 * does not have a configuration yet, it will be created. If something is not configured in the channel properties, it
 * will be copied from the global config during the first retrieval. If some property is configured in neither the
 * channel properties nor the global properties then a default value will be used.
 *
 * @author Michael (yidongnan@gmail.com)
 * @author Daniel Theuke (daniel.theuke@heuboe.de)
 * @since 5/17/16
 */
@ToString
@EqualsAndHashCode
public class SimpleGrpcChannelsProperties {

    /**
     * The key that will be used for the {@code GLOBAL} properties.
     */
    public static final String GLOBAL_PROPERTIES_KEY = "GLOBAL";

    private final Map<String, SimpleGrpcChannelProperties> client = new ConcurrentHashMap<>();

    /**
     * Gets the configuration mapping for each client.
     *
     * @return The client configuration mappings.
     */
    public final Map<String, SimpleGrpcChannelProperties> getClient() {
        return this.client;
    }

    /**
     * Gets the properties for the given channel. If the properties for the specified channel name do not yet exist,
     * they are created automatically. Before the instance is returned, the unset values are filled with values from the
     * global properties.
     *
     * @param name The name of the channel to get the properties for.
     * @return The properties for the given channel name.
     */
    public SimpleGrpcChannelProperties getChannel(final String name) {
        final SimpleGrpcChannelProperties properties = getRawChannel(name);
        properties.copyDefaultsFrom(getGlobalChannel());
        return properties;
    }

    /**
     * Gets the global channel properties. Global properties are used, if the channel properties don't overwrite them.
     * If neither the global nor the per client properties are set then default values will be used.
     *
     * @return The global channel properties.
     */
    public final SimpleGrpcChannelProperties getGlobalChannel() {
        // This cannot be moved to its own field,
        // as Spring replaces the instance in the map and inconsistencies would occur.
        return getRawChannel(GLOBAL_PROPERTIES_KEY);
    }

    /**
     * Gets or creates the channel properties for the given client.
     *
     * @param name The name of the channel to get the properties for.
     * @return The properties for the given channel name.
     */
    private SimpleGrpcChannelProperties getRawChannel(final String name) {
        return this.client.computeIfAbsent(name, key -> new SimpleGrpcChannelProperties());
    }

    private String defaultScheme;

    /**
     * Get the default scheme that should be used, if the client doesn't specify a scheme/address.
     *
     * @return The default scheme to use or null.
     * @see #setDefaultScheme(String)
     */
    public String getDefaultScheme() {
        return this.defaultScheme;
    }

    /**
     * Sets the default scheme to use, if the client doesn't specify a scheme/address. If not specified it will default
     * to the default scheme of the {@link io.grpc.NameResolver.Factory}. Examples: {@code dns}, {@code discovery}.
     *
     * @param defaultScheme The default scheme to use or null.
     */
    public void setDefaultScheme(String defaultScheme) {
        this.defaultScheme = defaultScheme;
    }

}
