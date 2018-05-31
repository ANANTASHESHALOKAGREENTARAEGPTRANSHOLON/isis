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

import org.apache.isis.applib.AppManifest;
import org.apache.isis.applib.Module;
import org.apache.isis.core.runtime.headless.HeadlessTransactionSupport;
import org.apache.isis.core.runtime.headless.HeadlessWithBootstrappingAbstract;
import org.apache.isis.core.runtime.headless.IsisSystem;
import org.apache.isis.core.runtime.headless.logging.LogConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.slf4j.event.Level;

/**
 * Reworked base class for integration tests, uses a {@link Module} to bootstrap, rather than an {@link AppManifest}.
 */
public abstract class IntegrationTestAbstract3 extends HeadlessWithBootstrappingAbstract {

    //private static final Logger LOG = LoggerFactory.getLogger(IntegrationTestAbstract3.class);

    @Rule
    public ExpectedException expectedExceptions = ExpectedException.none();

    /**
     * this is asymmetric - handles only the teardown of the transaction afterwards, not the initial set up
     * (which is done instead by the @Before, so that can also bootstrap system the very first time)
     */
    @Rule
    public IntegrationTestAbstract3.IsisTransactionRule isisTransactionRule = new IntegrationTestAbstract3.IsisTransactionRule();

    private static class IsisTransactionRule implements MethodRule {

        @Override
        public Statement apply(final Statement base, final FrameworkMethod method, final Object target) {

            return new Statement() {
                @Override
                public void evaluate() throws Throwable {

                    // we don't set up the ISFT, because the very first time it won't be there.
                    // Instead we expect it to be bootstrapped via @Before
                    try {
                        base.evaluate();
                        final IsisSystem isft = IsisSystem.get();
                        isft.getService(HeadlessTransactionSupport.class).endTransaction();
                    } catch(final Exception e) {
                    	Util.handleTransactionContextException(e);
                    }
                }

            };
        }
    }

    protected IntegrationTestAbstract3(final Module module) {
        this(new LogConfig(Level.INFO), module);
    }

    protected IntegrationTestAbstract3(
            final LogConfig logConfig,
            final Module module) {
    	super(logConfig, 
				Util.moduleBuilder(module)
				.withHeadlessTransactionSupport()
				.withIntegrationTestConfigFallback()
				.build() );
    }

	@Override
    @Before
    public void bootstrapAndSetupIfRequired() {

        super.bootstrapAndSetupIfRequired();

        log("### TEST: " + this.getClass().getCanonicalName());
    }

    @Override
    @After
    public void tearDownAllModules() {
        super.tearDownAllModules();
    }

}