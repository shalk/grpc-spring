/*
 * Copyright (c) 2016-2024 The gRPC-Spring Authors
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

package net.devh.boot.grpc.server.config;

import java.io.File;
import java.io.IOException;

import org.springframework.core.io.Resource;
import org.springframework.util.unit.DataSize;

public class ServerPropertiesConverter {

    public static SimpleGrpcServerProperties toSimple(GrpcServerProperties properties) {
        SimpleGrpcServerProperties simpleGrpcServerProperties = getSimpleGrpcServerProperties(properties);
        SimpleGrpcServerProperties.Security simpleSecurity = simpleGrpcServerProperties.getSecurity();

        GrpcServerProperties.Security security = properties.getSecurity();
        simpleSecurity.setCiphers(security.getCiphers());
        simpleSecurity.setProtocols(security.getProtocols());
        simpleSecurity.setEnabled(security.isEnabled());
        simpleSecurity.setCertificateChain(getFileFromResource(security.getCertificateChain()));
        simpleSecurity.setPrivateKey(getFileFromResource(security.getPrivateKey()));
        simpleSecurity.setPrivateKeyPassword(security.getPrivateKeyPassword());
        simpleSecurity.setKeyStoreFormat(security.getKeyStoreFormat());
        simpleSecurity.setKeyStore(getFileFromResource(security.getKeyStore()));
        simpleSecurity.setKeyStorePassword(security.getKeyStorePassword());
        simpleSecurity.setClientAuth(security.getClientAuth());
        simpleSecurity.setTrustCertCollection(getFileFromResource(security.getTrustCertCollection()));
        simpleSecurity.setTrustStoreFormat(security.getTrustStoreFormat());
        simpleSecurity.setTrustStore(getFileFromResource(security.getTrustStore()));
        simpleSecurity.setTrustStorePassword(security.getTrustStorePassword());

        return simpleGrpcServerProperties;
    }

    private static SimpleGrpcServerProperties getSimpleGrpcServerProperties(GrpcServerProperties properties) {
        SimpleGrpcServerProperties simpleGrpcServerProperties = new SimpleGrpcServerProperties();
        simpleGrpcServerProperties.setMaxInboundMessageSize(toInteger(properties.getMaxInboundMessageSize()));
        simpleGrpcServerProperties.setMaxInboundMetadataSize(toInteger(properties.getMaxInboundMetadataSize()));
        simpleGrpcServerProperties.setAddress(properties.getAddress());
        simpleGrpcServerProperties.setPort(properties.getPort());
        simpleGrpcServerProperties.setInProcessName(properties.getInProcessName());
        simpleGrpcServerProperties.setShutdownGracePeriod(properties.getShutdownGracePeriod());
        simpleGrpcServerProperties.setEnableKeepAlive(properties.isEnableKeepAlive());
        simpleGrpcServerProperties.setKeepAliveTime(properties.getKeepAliveTime());
        simpleGrpcServerProperties.setKeepAliveTimeout(properties.getKeepAliveTimeout());
        simpleGrpcServerProperties.setPermitKeepAliveTime(properties.getPermitKeepAliveTime());
        simpleGrpcServerProperties.setPermitKeepAliveWithoutCalls(properties.isPermitKeepAliveWithoutCalls());
        simpleGrpcServerProperties.setMaxConnectionIdle(properties.getMaxConnectionIdle());
        simpleGrpcServerProperties.setMaxConnectionAge(properties.getMaxConnectionAge());
        simpleGrpcServerProperties.setMaxConnectionAgeGrace(properties.getMaxConnectionAgeGrace());
        simpleGrpcServerProperties.setHealthServiceEnabled(properties.isHealthServiceEnabled());
        simpleGrpcServerProperties.setReflectionServiceEnabled(properties.isReflectionServiceEnabled());
        return simpleGrpcServerProperties;
    }

    public static Integer toInteger(DataSize dataSize) {
        if (dataSize == null)
            return null;
        return (int) dataSize.toBytes();
    }

    public static File getFileFromResource(Resource resource) {
        if (resource == null)
            return null;
        try {
            return resource.getFile();
        } catch (IOException e) {
            // ignore
        }
        return null;
    }
}
