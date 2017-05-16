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

package org.apache.isis.viewer.wicket.ui.components.widgets.linkandlabel;

import org.apache.wicket.Application;
import org.apache.wicket.ajax.IAjaxIndicatorAware;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.AjaxIndicatorAppender;
import org.apache.wicket.markup.ComponentTag;

import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.viewer.wicket.model.isis.WicketViewerSettings;
import org.apache.isis.viewer.wicket.model.isis.WicketViewerSettingsAccessor;
import org.apache.isis.viewer.wicket.model.models.ActionModel;
import org.apache.isis.viewer.wicket.ui.panels.PanelUtil;

import de.agilecoders.wicket.core.markup.html.bootstrap.button.Buttons;

public abstract class ActionLink extends AjaxLink<ObjectAdapter> implements IAjaxIndicatorAware {

    private final AjaxIndicatorAppender indicatorAppenderIfAny;

    public ActionLink(String id, ActionModel model) {
        super(id, model);

        final boolean useIndicatorForNoArgAction = getSettings().isUseIndicatorForNoArgAction();
        this.indicatorAppenderIfAny =
                useIndicatorForNoArgAction
                        ? new AjaxIndicatorAppender()
                        : null;

        if(this.indicatorAppenderIfAny != null) {
            this.add(this.indicatorAppenderIfAny);
        }
    }

    ActionModel getActionModel() {
        return (ActionModel) getModel();
    }

    @Override
    protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
        super.updateAjaxAttributes(attributes);
        if(getSettings().isPreventDoubleClickForNoArgAction()) {
            PanelUtil.disableBeforeReenableOnComplete(attributes, this);
        }

        // allow the event to bubble so the menu is hidden after click on an item
        attributes.setEventPropagation(AjaxRequestAttributes.EventPropagation.BUBBLE);
    }

    public String getReasonDisabledIfAny() {
        final ActionModel actionModel = getActionModel();
        final String disabledReasonIfAny = actionModel.getReasonDisabledIfAny();
        return disabledReasonIfAny;
    }

    @Override
    public boolean isEnabled() {
        final String reasonDisabledIfAny = getReasonDisabledIfAny();
        return reasonDisabledIfAny == null;
    }

    @Override
    protected void onComponentTag(ComponentTag tag) {
        super.onComponentTag(tag);
        Buttons.fixDisabledState(this, tag);
    }

    public String getAjaxIndicatorMarkupId() {
        return this.indicatorAppenderIfAny != null
                ? this.indicatorAppenderIfAny.getMarkupId()
                : null;
    }

    protected WicketViewerSettings getSettings() {
        return ((WicketViewerSettingsAccessor) Application.get()).getSettings();
    }

}
