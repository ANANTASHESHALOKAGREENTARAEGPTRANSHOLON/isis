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
package org.apache.isis.viewer.restfulobjects.tck.domainobject.oid;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.apache.isis.core.commons.matchers.IsisMatchers;
import org.apache.isis.core.webserver.WebServer;
import org.apache.isis.viewer.restfulobjects.applib.*;
import org.apache.isis.viewer.restfulobjects.applib.client.RestfulClient;
import org.apache.isis.viewer.restfulobjects.applib.client.RestfulResponse;
import org.apache.isis.viewer.restfulobjects.applib.domainobjects.DomainObjectRepresentation;
import org.apache.isis.viewer.restfulobjects.applib.errors.ErrorRepresentation;
import org.apache.isis.viewer.restfulobjects.tck.IsisWebServerRule;
import org.apache.isis.viewer.restfulobjects.tck.RestfulMatchers;
import org.apache.isis.viewer.restfulobjects.tck.Util;

import static org.apache.isis.core.commons.matchers.IsisMatchers.matches;
import static org.apache.isis.viewer.restfulobjects.tck.RestfulMatchers.isLink;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class Delete_then_405_bad {

    @Rule
    public IsisWebServerRule webServerRule = new IsisWebServerRule();

    protected RestfulClient client;

    private ErrorRepresentation errorRepresentation;

    @Before
    public void setUp() throws Exception {
        final WebServer webServer = webServerRule.getWebServer();
        client = new RestfulClient(webServer.getBase());
    }

    @Test
    public void followLink() throws Exception {

        // when
        final LinkRepresentation link = Util.domainObjectLink(client, "PrimitiveValuedEntities");
        final LinkRepresentation deleteLink = link.withMethod(RestfulHttpMethod.DELETE);
        final RestfulResponse<JsonRepresentation> restfulResponse = client.follow(deleteLink);

        assertThat(restfulResponse.getStatus(), is(RestfulResponse.HttpStatusCode.METHOD_NOT_ALLOWED));
        assertThat(restfulResponse.getHeader(RestfulResponse.Header.WARNING), is("Deleting objects is not supported."));

    }

}
