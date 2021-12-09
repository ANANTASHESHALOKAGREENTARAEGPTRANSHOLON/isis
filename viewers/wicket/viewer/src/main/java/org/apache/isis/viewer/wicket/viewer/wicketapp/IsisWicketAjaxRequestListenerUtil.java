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
package org.apache.isis.viewer.wicket.viewer.wicketapp;

import org.apache.wicket.SystemMapper;
import org.apache.wicket.core.request.handler.ListenerRequestHandler;
import org.apache.wicket.core.request.mapper.PageInstanceMapper;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.IRequestMapper;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.http.WebRequest;

import org.apache.isis.commons.internal.debug._Debug;
import org.apache.isis.core.metamodel.facets.object.viewmodel.ViewModelFacet;
import org.apache.isis.core.runtime.context.IsisAppCommonContext;
import org.apache.isis.viewer.wicket.model.models.EntityModel;
import org.apache.isis.viewer.wicket.model.util.PageParameterUtils;
import org.apache.isis.viewer.wicket.ui.pages.entity.EntityPage;

import lombok.val;
import lombok.experimental.UtilityClass;

/**
 * Could maybe instead utilize
 * {@link org.apache.wicket.ajax.AjaxRequestTarget.IListener#onBeforeRespond(java.util.Map, org.apache.wicket.ajax.AjaxRequestTarget)}
 */
@UtilityClass
public final class IsisWicketAjaxRequestListenerUtil {

    public void setRootRequestMapper(
            final WebApplication app,
            final IsisAppCommonContext commonContext) {
        app.setRootRequestMapper(new SystemMapper(app) {
            @Override
            protected IRequestMapper newPageInstanceMapper() {
                return new PageInstanceMapper() {
                    @Override
                    public IRequestHandler mapRequest(final Request request) {
                        var handler = super.mapRequest(request);
                        final boolean isAjax = ((WebRequest)request).isAjax();
                        return isAjax
                                ? entityRefetchingHandler(commonContext, handler)
                                : handler;
                    }
                };
            }
        });
    }

    // -- HELPER

    private IRequestHandler entityRefetchingHandler(
            final IsisAppCommonContext commonContext, final IRequestHandler handler) {
        if(handler instanceof ListenerRequestHandler) {
            val lirqh = (ListenerRequestHandler)handler;
            val page = lirqh.getPage();
            if(!(page instanceof EntityPage)) {
                return lirqh;
            }

            val entityPage = (EntityPage) page;
            val entityModel = (EntityModel)entityPage.getUiHintContainerIfAny();
            val spec = entityModel.getObject().getSpecification();

            if(!spec.isViewModel()) {
                return lirqh;
            };

            val viewModelFacet = spec.getFacet(ViewModelFacet.class);
            if(!viewModelFacet.containsEntities()) {
                return lirqh;
            }

            _Debug.log("[IRequestHandler] recreate model %s", spec.getCorrespondingClass().getSimpleName());

            val viewmodel = entityModel.getBookmarkedOwner();
            if(viewmodel.isBookmarkMemoized()) {
                viewmodel.reloadViewmodelFromMemoizedBookmark();
            } else {
                val bookmark = PageParameterUtils.toBookmark(entityPage.getPageParameters()).orElseThrow();
                viewmodel.reloadViewmodelFromBookmark(bookmark);
            }

            return lirqh;

        }
        return handler;
    }

}
