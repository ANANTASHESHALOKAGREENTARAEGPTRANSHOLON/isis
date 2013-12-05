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

package org.apache.isis.core.integtestsupport;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import org.apache.isis.applib.DomainObjectContainer;
import org.apache.isis.applib.fixtures.InstallableFixture;
import org.apache.isis.core.commons.authentication.AuthenticationSession;
import org.apache.isis.core.commons.config.IsisConfiguration;
import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.metamodel.adapter.mgr.AdapterManager;
import org.apache.isis.core.metamodel.adapter.oid.RootOid;
import org.apache.isis.core.metamodel.progmodel.ProgrammingModel;
import org.apache.isis.core.metamodel.spec.ObjectSpecification;
import org.apache.isis.core.metamodel.specloader.validator.MetaModelValidator;
import org.apache.isis.core.objectstore.InMemoryPersistenceMechanismInstaller;
import org.apache.isis.core.runtime.authentication.AuthenticationManager;
import org.apache.isis.core.runtime.authentication.AuthenticationRequest;
import org.apache.isis.core.runtime.fixtures.FixturesInstallerDelegate;
import org.apache.isis.core.runtime.installerregistry.installerapi.PersistenceMechanismInstaller;
import org.apache.isis.core.runtime.logging.IsisLoggingConfigurer;
import org.apache.isis.core.runtime.persistence.objectstore.ObjectStoreSpi;
import org.apache.isis.core.runtime.system.DeploymentType;
import org.apache.isis.core.runtime.system.context.IsisContext;
import org.apache.isis.core.runtime.system.persistence.PersistenceSession;
import org.apache.isis.core.runtime.system.persistence.Persistor;
import org.apache.isis.core.runtime.system.transaction.IsisTransaction;
import org.apache.isis.core.runtime.system.transaction.IsisTransaction.State;
import org.apache.isis.core.runtime.system.transaction.IsisTransactionManager;
import org.apache.isis.core.security.authentication.AuthenticationRequestNameOnly;
import org.apache.isis.core.specsupport.scenarios.DomainServiceProvider;

/**
 * Wraps a plain {@link IsisSystemDefault}, and provides a number of features to assist with testing.
 */
public class IsisSystemForTest implements org.junit.rules.TestRule, DomainServiceProvider {

    public interface Listener {

        void init(IsisConfiguration configuration) throws Exception;
        
        void preSetupSystem(boolean firstTime) throws Exception;
        void postSetupSystem(boolean firstTime) throws Exception;
        
        void preBounceSystem() throws Exception;
        void postBounceSystem() throws Exception;

        void preTeardownSystem() throws Exception;
        void postTeardownSystem() throws Exception;
        
    }
    
    public static abstract class ListenerAdapter implements Listener {
        
        private IsisConfiguration configuration;

        public void init(IsisConfiguration configuration) throws Exception {
            this.configuration = configuration;
        }
        
        protected IsisConfiguration getConfiguration() {
            return configuration;
        }

        @Override
        public void preSetupSystem(boolean firstTime) throws Exception {
        }

        @Override
        public void postSetupSystem(boolean firstTime) throws Exception {
        }

        @Override
        public void preBounceSystem() throws Exception {
        }

        @Override
        public void postBounceSystem() throws Exception {
        }

        @Override
        public void preTeardownSystem() throws Exception {
        }

        @Override
        public void postTeardownSystem() throws Exception {
        }

    }

    // //////////////////////////////////////

    private static ThreadLocal<IsisSystemForTest> ISFT = new ThreadLocal<IsisSystemForTest>();

    public static IsisSystemForTest getElseNull() {
        return ISFT.get();
    }
    
    public static IsisSystemForTest get() {
        final IsisSystemForTest isft = ISFT.get();
        if(isft == null) {
            throw new IllegalStateException("No IsisSystemForTest available on thread; call #set(IsisSystemForTest) first");
        }
        return isft;
    }

    public static void set(IsisSystemForTest isft) {
        ISFT.set(isft);
    }

    // //////////////////////////////////////


    private IsisSystemDefault isisSystem;
    private AuthenticationSession authenticationSession;

    private DomainObjectContainer container;
    
    private final IsisConfiguration configuration;
    private final PersistenceMechanismInstaller persistenceMechanismInstaller;
    private final AuthenticationRequest authenticationRequest;
    private final List<Object> services;
    private final List<InstallableFixture> fixtures;
    private List <Listener> listeners;
    
    private org.apache.log4j.Level level = org.apache.log4j.Level.INFO;
    
    private final MetaModelValidator metaModelValidator;
    private final ProgrammingModel programmingModel;

    
    ////////////////////////////////////////////////////////////
    // constructor
    ////////////////////////////////////////////////////////////

    public static class Builder {

        private AuthenticationRequest authenticationRequest = new AuthenticationRequestNameOnly("tester");
        
        private IsisConfiguration configuration;
        private PersistenceMechanismInstaller persistenceMechanismInstaller = new InMemoryPersistenceMechanismInstaller();
        private MetaModelValidator metaModelValidator;
        private ProgrammingModel programmingModel;

        private Object[] services;
        private InstallableFixture[] fixtures;
        
        private final List <Listener> listeners = Lists.newArrayList();

        private org.apache.log4j.Level level = null;
        
        public Builder with(IsisConfiguration configuration) {
            this.configuration = configuration;
            return this;
        }
        
        public Builder with(PersistenceMechanismInstaller persistenceMechanismInstaller) {
            this.persistenceMechanismInstaller = persistenceMechanismInstaller;
            return this;
        }
        
        public Builder with(AuthenticationRequest authenticationRequest) {
            this.authenticationRequest = authenticationRequest;
            return this;
        }

        public Builder withServices(Object... services) {
            this.services = services;
            return this;
        }

        public Builder withFixtures(InstallableFixture... fixtures) {
            this.fixtures = fixtures;
            return this;
        }
        
        public Builder withLoggingAt(org.apache.log4j.Level level) {
            this.level = level;
            return this;
        }

        public IsisSystemForTest build() {
            final List<Object> servicesIfAny = asList(services);
            final List<InstallableFixture> fixturesIfAny = asList(fixtures);
            return new IsisSystemForTest(configuration, programmingModel, metaModelValidator, persistenceMechanismInstaller, authenticationRequest, servicesIfAny, fixturesIfAny, listeners);
        }

        private static <T> List<T> asList(T[] objects) {
            return objects != null? Arrays.asList(objects): Collections.<T>emptyList();
        }

        public Builder with(Listener listener) {
            if(listener != null) {
                listeners.add(listener);
            }
            return this;
        }

        public Builder with(MetaModelValidator metaModelValidator) {
            this.metaModelValidator = metaModelValidator;
            return this;
        }

        public Builder with(ProgrammingModel programmingModel) {
            this.programmingModel = programmingModel;
            return this;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private IsisSystemForTest(IsisConfiguration configuration, ProgrammingModel programmingModel, MetaModelValidator metaModelValidator, PersistenceMechanismInstaller persistenceMechanismInstaller, AuthenticationRequest authenticationRequest, List<Object> services, List<InstallableFixture> fixtures, List<Listener> listeners) {
        this.configuration = configuration;
        this.programmingModel = programmingModel;
        this.metaModelValidator = metaModelValidator;
        this.persistenceMechanismInstaller = persistenceMechanismInstaller;
        this.authenticationRequest = authenticationRequest;
        this.services = services;
        this.fixtures = fixtures;
        this.listeners = listeners;
    }

    ////////////////////////////////////////////////////////////
    // level
    ////////////////////////////////////////////////////////////

    /**
     * The level to use for the root logger if fallback (ie a <tt>logging.properties</tt> file cannot be found).
     */
    public org.apache.log4j.Level getLevel() {
        return level;
    }
    
    public void setLevel(org.apache.log4j.Level level) {
        this.level = level;
    }

    ////////////////////////////////////////////////////////////
    // setup, teardown
    ////////////////////////////////////////////////////////////
    

    /**
     * Intended to be called from a test's {@link Before} method.
     */
    public IsisSystemForTest setUpSystem() throws RuntimeException {
        try {
            setUpSystem(FireListeners.FIRE);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    private void setUpSystem(FireListeners fireListeners) throws Exception {
        

        boolean firstTime = isisSystem == null;
        if(fireListeners.shouldFire()) {
            fireInitAndPreSetupSystem(firstTime);
        }
        
        if(firstTime) {
            IsisLoggingConfigurer isisLoggingConfigurer = new IsisLoggingConfigurer(getLevel());
            isisLoggingConfigurer.configureLogging(".", new String[]{});

            isisSystem = createIsisSystem(services);
            isisSystem.init();
            IsisContext.closeSession();
        }

        final AuthenticationManager authenticationManager = isisSystem.getSessionFactory().getAuthenticationManager();
        authenticationSession = authenticationManager.authenticate(authenticationRequest);

        IsisContext.openSession(authenticationSession);
        setContainer(getContainer());
        
        wireAndInstallFixtures();
        if(fireListeners.shouldFire()) {
            firePostSetupSystem(firstTime);
        }
    }

    private void wireAndInstallFixtures() {
        FixturesInstallerDelegate fid = new FixturesInstallerDelegate(getPersistenceSession());
        fid.addFixture(fixtures);
        fid.installFixtures();
    }

    private enum FireListeners {
        FIRE,
        DONT_FIRE;
        public boolean shouldFire() {
            return this == FIRE;
        }
    }
    
    public DomainObjectContainer getContainer() {
        return getPersistenceSession().getServicesInjector().getContainer();
    }

    /**
     * Intended to be called from a test's {@link After} method.
     */
    public void tearDownSystem() throws Exception {
        tearDownSystem(FireListeners.FIRE);
    }

    private void tearDownSystem(final FireListeners fireListeners) throws Exception {
        if(fireListeners.shouldFire()) {
            firePreTeardownSystem();
        }
        IsisContext.closeSession();
        if(fireListeners.shouldFire()) {
            firePostTeardownSystem();
        }
    }

    public void bounceSystem() throws Exception {
        firePreBounceSystem();
        tearDownSystem(FireListeners.DONT_FIRE);
        setUpSystem(FireListeners.DONT_FIRE);
        firePostBounceSystem();
    }


    private IsisSystemDefault createIsisSystem(List<Object> services) {
        final IsisSystemDefault system = new IsisSystemDefault(DeploymentType.UNIT_TESTING, services) {
            @Override
            public IsisConfiguration getConfiguration() {
                if(IsisSystemForTest.this.configuration != null) {
                    return IsisSystemForTest.this.configuration;
                } else {
                    return super.getConfiguration();
                }
            }
            @Override
            protected ProgrammingModel obtainReflectorProgrammingModel() {
                if(IsisSystemForTest.this.programmingModel != null) {
                    return IsisSystemForTest.this.programmingModel;
                } else {
                    return super.obtainReflectorProgrammingModel();
                }
            }
            @Override
            protected MetaModelValidator obtainReflectorMetaModelValidator() {
                if(IsisSystemForTest.this.metaModelValidator != null) {
                    return IsisSystemForTest.this.metaModelValidator;
                } else {
                    return super.obtainReflectorMetaModelValidator();
                }
            }
            @Override
            protected PersistenceMechanismInstaller obtainPersistenceMechanismInstaller(IsisConfiguration configuration) {
                final PersistenceMechanismInstaller installer = IsisSystemForTest.this.persistenceMechanismInstaller;
                configuration.injectInto(installer);
                return installer;
            }
        };
        return system;
    }



    ////////////////////////////////////////////////////////////
    // listeners
    ////////////////////////////////////////////////////////////

    private void fireInitAndPreSetupSystem(boolean firstTime) throws Exception {
        if(firstTime) {
            for(Listener listener: listeners) {
                listener.init(configuration);
            }
        }
        for(Listener listener: listeners) {
            listener.preSetupSystem(firstTime);
        }
    }

    private void firePostSetupSystem(boolean firstTime) throws Exception {
        for(Listener listener: listeners) {
            listener.postSetupSystem(firstTime);
        }
    }

    private void firePreTeardownSystem() throws Exception {
        for(Listener listener: listeners) {
            listener.preTeardownSystem();
        }
    }

    private void firePostTeardownSystem() throws Exception {
        for(Listener listener: listeners) {
            listener.postTeardownSystem();
        }
    }

    private void firePreBounceSystem() throws Exception {
        for(Listener listener: listeners) {
            listener.preBounceSystem();
        }
    }

    private void firePostBounceSystem() throws Exception {
        for(Listener listener: listeners) {
            listener.postBounceSystem();
        }
    }

    
    ////////////////////////////////////////////////////////////
    // properties
    ////////////////////////////////////////////////////////////

    /**
     * The {@link IsisSystemDefault} created during {@link #setUpSystem()}.
     * 
     * <p>
     * Can fine-tune the actual implementation using the hook {@link #createIsisSystem()}.
     */
    public IsisSystemDefault getIsisSystem() {
        return isisSystem;
    }

    /**
     * The {@link AuthenticationSession} created during {@link #setUpSystem()}.
     * 
     * <p>
     * Can fine-tune before hand using {@link #createAuthenticationRequest()}.
     */
    public AuthenticationSession getAuthenticationSession() {
        return authenticationSession;
    }



    ////////////////////////////////////////////////////////////
    // Convenience for tests
    ////////////////////////////////////////////////////////////

    public ObjectSpecification loadSpecification(Class<?> cls) {
        return getIsisSystem().getSessionFactory().getSpecificationLoader().loadSpecification(cls);
    }

    public ObjectAdapter persist(Object domainObject) {
        ensureSessionInProgress();
        ensureObjectIsNotPersistent(domainObject);
        getContainer().persist(domainObject);
        return adapterFor(domainObject);
    }

    public ObjectAdapter destroy(Object domainObject ) {
        ensureSessionInProgress();
        ensureObjectIsPersistent(domainObject);
        getContainer().remove(domainObject);
        return adapterFor(domainObject);
    }

    public ObjectAdapter adapterFor(Object domainObject) {
        ensureSessionInProgress();
        return getAdapterManager().adapterFor(domainObject);
    }

    public ObjectAdapter reload(RootOid oid) {
        ensureSessionInProgress();
        final Persistor persistenceSession = getPersistenceSession();
        return persistenceSession.loadObject(oid);
    }

    public ObjectAdapter recreateAdapter(RootOid oid) {
        ensureSessionInProgress();
        return getAdapterManager().adapterFor(oid);
    }

    public ObjectAdapter remapAsPersistent(Object pojo, RootOid persistentOid) {
        ensureSessionInProgress();
        ensureObjectIsNotPersistent(pojo);
        final ObjectAdapter adapter = adapterFor(pojo);
        getPersistenceSession().remapAsPersistent(adapter, persistentOid);
        return adapter;
    }

    @SuppressWarnings("unchecked")
    public <T extends ObjectStoreSpi> T getObjectStore(Class<T> cls) {
        final PersistenceSession persistenceSession = getPersistenceSession();
        return (T) persistenceSession.getObjectStore();
    }

    private static void ensureSessionInProgress() {
        if(!IsisContext.inSession()) {
            throw new IllegalStateException("Session must be in progress");
        }
    }

    private void ensureObjectIsNotPersistent(Object domainObject) {
        if(getContainer().isPersistent(domainObject)) {
            throw new IllegalArgumentException("domain object is already persistent");
        }
    }

    private void ensureObjectIsPersistent(Object domainObject) {
        if(!getContainer().isPersistent(domainObject)) {
            throw new IllegalArgumentException("domain object is not persistent");
        }
    }

    ////////////////////////////////////////////////////////////
    // JUnit @Rule integration
    ////////////////////////////////////////////////////////////

    @Override
    public Statement apply(final Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                setUpSystem();
                try {
                    base.evaluate();
                    tearDownSystem();
                } catch(Throwable ex) {
                    try {
                        tearDownSystem();
                    } catch(Exception ex2) {
                        // ignore, since already one pending
                    }
                    throw ex;
                }
            }
        };
    }


    
    public void beginTran() {
        final IsisTransactionManager transactionManager = getTransactionManager();
        final IsisTransaction transaction = transactionManager.getTransaction();

        if(transaction == null) {
            transactionManager.startTransaction();
            return;
        } 

        final State state = transaction.getState();
        switch(state) {
            case COMMITTED:
            case ABORTED:
                transactionManager.startTransaction();
                break;
            case IN_PROGRESS:
                // nothing to do
                break;
            case MUST_ABORT:
                Assert.fail("Transaction is in state of '" + state + "'");
                break;
            default:
                Assert.fail("Unknown transaction state '" + state + "'");
        }
        
    }

    public void commitTran() {
        final IsisTransactionManager transactionManager = getTransactionManager();
        final IsisTransaction transaction = transactionManager.getTransaction();
        if(transaction == null) {
            Assert.fail("No transaction exists");
            return;
        } 
        final State state = transaction.getState();
        switch(state) {
            case COMMITTED:
            case ABORTED:
            case MUST_ABORT:
                Assert.fail("Transaction is in state of '" + state + "'");
                break;
            case IN_PROGRESS:
                transactionManager.endTransaction();
                break;
            default:
                Assert.fail("Unknown transaction state '" + state + "'");
        }
    }

    public void abortTran() {
        final IsisTransactionManager transactionManager = getTransactionManager();
        final IsisTransaction transaction = transactionManager.getTransaction();
        if(transaction == null) {
            Assert.fail("No transaction exists");
            return;
        } 
        final State state = transaction.getState();
        switch(state) {
            case ABORTED:
                break;
            case COMMITTED:
                Assert.fail("Transaction is in state of '" + state + "'");
                break;
            case MUST_ABORT:
            case IN_PROGRESS:
                transactionManager.abortTransaction();
                break;
            default:
                Assert.fail("Unknown transaction state '" + state + "'");
        }
    }


    /* (non-Javadoc)
     * @see org.apache.isis.core.integtestsupport.ServiceProvider#getService(java.lang.Class)
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> serviceClass) {
        if(serviceClass == DomainObjectContainer.class) {
            return (T) getContainer();
        }
        List<ObjectAdapter> servicesAdapters = getPersistenceSession().getServices();
        for(ObjectAdapter serviceAdapter: servicesAdapters) {
            Object servicePojo = serviceAdapter.getObject();
            if(serviceClass.isAssignableFrom(servicePojo.getClass())) {
                return (T) servicePojo;
            }
        }
        throw new RuntimeException("Could not find a service of type: " + serviceClass.getName());
    }

    
    ////////////////////////////////////////////////////////////
    // Fixture management 
    // (for each test, rather than at bootstrap)
    ////////////////////////////////////////////////////////////

    
    public void installFixtures(final InstallableFixture... fixtures) {
        final FixturesInstallerDelegate fid = new FixturesInstallerDelegate(getPersistenceSession());
        for (InstallableFixture fixture : fixtures) {
            fid.addFixture(fixture);
        }
        fid.installFixtures();
    }


    ////////////////////////////////////////////////////////////
    // Dependencies
    ////////////////////////////////////////////////////////////
    
    protected IsisTransactionManager getTransactionManager() {
        return getPersistenceSession().getTransactionManager();
    }
    
    public Persistor getPersistor() {
    	return getPersistenceSession();
    }
    
    public AdapterManager getAdapterManager() {
        return getPersistor().getAdapterManager();
    }

    protected PersistenceSession getPersistenceSession() {
        return IsisContext.getPersistenceSession();
    }

    /**
     * @param container the container to set
     */
    public void setContainer(DomainObjectContainer container) {
        this.container = container;
        
    }

    
}
