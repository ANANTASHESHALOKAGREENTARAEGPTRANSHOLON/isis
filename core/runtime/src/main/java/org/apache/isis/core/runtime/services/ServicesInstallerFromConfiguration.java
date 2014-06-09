/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.isis.core.runtime.services;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.isis.core.commons.config.ConfigurationConstants;
import org.apache.isis.core.commons.config.InstallerAbstract;
import org.apache.isis.core.runtime.fixturedomainservice.ObjectFixtureService;
import org.apache.isis.core.runtime.system.DeploymentType;
import org.apache.isis.core.runtime.system.SystemConstants;

public class ServicesInstallerFromConfiguration extends InstallerAbstract implements ServicesInstaller {

    private static final Logger LOG = LoggerFactory.getLogger(ServicesInstallerFromConfiguration.class);

    private static final String SERVICES = "services";
    private static final String EXPLORATION_OBJECTS = "exploration-objects";
    private static final String SERVICES_PREFIX = "services.prefix";

    private final static Pattern regex = Pattern.compile("((\\d+):)(.*)");

    private final ServiceInstantiator serviceInstantiator;

    public ServicesInstallerFromConfiguration() {
        this(new ServiceInstantiator());
    }

    ServicesInstallerFromConfiguration(final ServiceInstantiator serviceInstantiator) {
        super(ServicesInstaller.TYPE, "configuration");
        this.serviceInstantiator = serviceInstantiator;
    }

    // //////////////////////////////////////

    private Map<DeploymentType, List<Object>> servicesByDeploymentType = Maps.newHashMap();

    @Override
    public List<Object> getServices(final DeploymentType deploymentType) {

        LOG.info("installing " + this.getClass().getName());

        List<Object> serviceList = servicesByDeploymentType.get(deploymentType);
        if(serviceList == null) {

            final SortedMap<String, SortedSet<String>> positionedServices = Maps.newTreeMap(new DeweyOrderComparator());
            appendServices(deploymentType, positionedServices);

            serviceList = ServicesInstallerUtils.instantiateServicesFrom(positionedServices, serviceInstantiator);

            servicesByDeploymentType.put(deploymentType, serviceList);
        }
        return serviceList;
    }

    // //////////////////////////////////////

    public void appendServices(DeploymentType deploymentType, final SortedMap<String, SortedSet<String>> positionedServices) {

        appendConfiguredServices(null, positionedServices);
        appendConfiguredServices(deploymentType, positionedServices);

        appendObjectFixtureService(null, positionedServices);
    }

    private void appendConfiguredServices(final DeploymentType deploymentType, final SortedMap<String, SortedSet<String>> positionedServices) {
        String group = deploymentType != null? deploymentType.name(): null;
        final String root = ConfigurationConstants.ROOT + (group == null ? "" : group.toLowerCase() + ".");

        String servicePrefix = getConfiguration().getString(root + SERVICES_PREFIX);
        if (group != null && servicePrefix == null) {
            servicePrefix = getConfiguration().getString(ConfigurationConstants.ROOT + SERVICES_PREFIX);
        }

        final String configuredServices = getConfiguration().getString(root + SERVICES);
        if (configuredServices == null) {
            return;
        }

        final StringTokenizer services = new StringTokenizer(configuredServices, ConfigurationConstants.LIST_SEPARATOR);
        while (services.hasMoreTokens()) {
            String serviceName = services.nextToken().trim();
            if (serviceName.equals("")) {
                continue;
            }
            final Matcher matcher = regex.matcher(serviceName);
            Integer order = Integer.MAX_VALUE;
            if(matcher.matches()) {
                order = Integer.parseInt(matcher.group(2));
                serviceName = matcher.group(3);
            }

            ServicesInstallerUtils.appendInPosition(positionedServices, "" + order, ServicesInstallerUtils.fullyQualifiedServiceName(servicePrefix, serviceName));
        }
    }

    private void appendObjectFixtureService(DeploymentType deploymentType, final SortedMap<String, SortedSet<String>> positionedServices) {

        final String group = deploymentType != null? deploymentType.name(): null;
        final String root = ConfigurationConstants.ROOT + (group == null ? "" : group.toLowerCase() + ".");

        if (getConfiguration().getBoolean(root + EXPLORATION_OBJECTS)) {
            final DeploymentType explorationDeploymentType = DeploymentType.lookup(getConfiguration().getString(SystemConstants.DEPLOYMENT_TYPE_KEY));
            if (explorationDeploymentType.isExploring()) {
                ServicesInstallerUtils.appendInPosition(positionedServices, "" + Integer.MAX_VALUE, ObjectFixtureService.class.getName());
            }
        }
    }

    // unused, I think....?
    private static String servicePrefix(final String servicePrefix) {
        String prefix = servicePrefix == null ? "" : servicePrefix.trim();
        if (prefix.length() > 0 && !prefix.endsWith(ConfigurationConstants.DELIMITER)) {
            prefix = prefix + ConfigurationConstants.DELIMITER;
        }
        return prefix;
    }

    @Override
    public List<Class<?>> getTypes() {
        return listOf(List.class); // ie List<Object.class>, of services
    }
}
