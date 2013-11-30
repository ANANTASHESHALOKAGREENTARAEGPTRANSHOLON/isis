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

package org.apache.isis.viewer.wicket.ui.components.actions;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;

import java.util.List;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseAtInterceptPageException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;

import org.apache.isis.core.commons.ensure.Ensure;
import org.apache.isis.core.commons.lang.ObjectExtensions;
import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.metamodel.adapter.mgr.AdapterManager.ConcurrencyChecking;
import org.apache.isis.core.metamodel.adapter.version.ConcurrencyException;
import org.apache.isis.core.metamodel.spec.feature.ObjectAction;
import org.apache.isis.core.metamodel.spec.feature.ObjectActionParameter;
import org.apache.isis.viewer.wicket.model.mementos.ActionParameterMemento;
import org.apache.isis.viewer.wicket.model.mementos.ObjectAdapterMemento;
import org.apache.isis.viewer.wicket.model.models.ActionExecutor;
import org.apache.isis.viewer.wicket.model.models.ActionModel;
import org.apache.isis.viewer.wicket.model.models.ScalarModel;
import org.apache.isis.viewer.wicket.model.util.MementoFunctions;
import org.apache.isis.viewer.wicket.ui.ComponentType;
import org.apache.isis.viewer.wicket.ui.components.actions.ActionPanel.ResultType;
import org.apache.isis.viewer.wicket.ui.components.scalars.ScalarModelSubscriber;
import org.apache.isis.viewer.wicket.ui.components.scalars.ScalarPanelAbstract;
import org.apache.isis.viewer.wicket.ui.components.scalars.TextFieldValueModel.ScalarModelProvider;
import org.apache.isis.viewer.wicket.ui.components.widgets.formcomponent.FormFeedbackPanel;
import org.apache.isis.viewer.wicket.ui.pages.PageAbstract;
import org.apache.isis.viewer.wicket.ui.pages.entity.EntityPage;
import org.apache.isis.viewer.wicket.ui.panels.PanelAbstract;
import org.apache.isis.viewer.wicket.ui.util.CssClassAppender;

/**
 * {@link PanelAbstract Panel} to capture the arguments for an action
 * invocation.
 */
public class ActionParametersFormPanel extends PanelAbstract<ActionModel> {

    private static final long serialVersionUID = 1L;

    private static final String ID_OK_BUTTON = "okButton";
    private static final String ID_CANCEL_BUTTON = "cancelButton";
    private static final String ID_ACTION_PARAMETERS = "parameters";

    private final ActionExecutor actionExecutor;

    public ActionParametersFormPanel(final String id, final ActionModel model) {
        super(id, model);

        Ensure.ensureThatArg(model.getExecutor(), is(not(nullValue())));

        this.actionExecutor = model.getExecutor();
        buildGui();
    }

    private void buildGui() {
        add(new ActionParameterForm("inputForm", getModel()));
    }

    class ActionParameterForm extends Form<ObjectAdapter> implements ScalarModelSubscriber  {

        private static final long serialVersionUID = 1L;

        private static final String ID_FEEDBACK = "feedback";
        
        private final List<ScalarPanelAbstract> paramPanels = Lists.newArrayList();

        private boolean renderedFirstField;

        public ActionParameterForm(final String id, final ActionModel actionModel) {
            super(id, actionModel);

            setOutputMarkupId(true); // for ajax button
            
            addParameters();

            FormFeedbackPanel formFeedback = new FormFeedbackPanel(ID_FEEDBACK);
            formFeedback.setEscapeModelStrings(false);
            addOrReplace(formFeedback);
            addButtons();
        }

        private ActionModel getActionModel() {
            return (ActionModel) super.getModel();
        }

        private void addParameters() {
            final ActionModel actionModel = getActionModel();
            final ObjectAction objectAction = actionModel.getActionMemento().getAction();
            
            final List<ObjectActionParameter> parameters = objectAction.getParameters();
            
            final List<ActionParameterMemento> mementos = buildParameterMementos(parameters);
            for (final ActionParameterMemento apm1 : mementos) {
                actionModel.getArgumentModel(apm1);
            }
            
            final RepeatingView rv = new RepeatingView(ID_ACTION_PARAMETERS);
            add(rv);
            
            paramPanels.clear();
            for (final ActionParameterMemento apm : mementos) {
                final WebMarkupContainer container = new WebMarkupContainer(rv.newChildId());
                rv.add(container);

                final ScalarModel argumentModel = actionModel.getArgumentModel(apm);
                argumentModel.setActionArgsHint(actionModel.getArgumentsAsArray());
                final Component component = getComponentFactoryRegistry().addOrReplaceComponent(container, ComponentType.SCALAR_NAME_AND_VALUE, argumentModel);
                final ScalarPanelAbstract paramPanel = component instanceof ScalarPanelAbstract ? (ScalarPanelAbstract) component : null;
                paramPanels.add(paramPanel);
                if(paramPanel != null) {
                    paramPanel.setOutputMarkupId(true);
                    paramPanel.notifyOnChange(this);
                }
                
                if(!renderedFirstField) {
                    component.add(new CssClassAppender("first-field"));
                    renderedFirstField = true;
                }
            }
        }


        private void addButtons() {
            AjaxButton okButton = new AjaxButton(ID_OK_BUTTON) {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                    actionExecutor.executeActionAndProcessResults(target, form);
                };
            };
            add(okButton);
            Button cancelButton = new Button(ID_CANCEL_BUTTON) {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    // no-op works fine for prompt modal dialog, but need to do something else if modal dialog disabled
                };
            };
            add(cancelButton);
            
            // TODO: hide cancel button if dialogs disabled, as not yet implemented.
            if(!PageAbstract.isActionPromptModalDialogEnabled()) {
                cancelButton.setVisible(false);
            }
        }


        private List<ActionParameterMemento> buildParameterMementos(final List<ObjectActionParameter> parameters) {
            final List<ActionParameterMemento> parameterMementoList = Lists.transform(parameters, MementoFunctions.fromActionParameter());
            // we copy into a new array list otherwise we get lazy evaluation =
            // reference to a non-serializable object
            return Lists.newArrayList(parameterMementoList);
        }

        @Override
        public void onUpdate(AjaxRequestTarget target, ScalarModelProvider provider) {

            final ActionModel actionModel = getActionModel();
            
            final ObjectAdapter[] pendingArguments = actionModel.getArgumentsAsArray();
            
            try {
                final ObjectAction action = actionModel.getActionMemento().getAction();
                final int numParams = action.getParameterCount();
                for (int i = 0; i < numParams; i++) {
                    final ScalarPanelAbstract paramPanel = paramPanels.get(i);
                    if(paramPanel != null) {
                        // this could throw a ConcurrencyException as we may have to reload the 
                        // object adapter of the action in order to compute the choices
                        // (and that object adapter might have changed)
                        if(paramPanel.updateChoices(pendingArguments)) {
                            target.add(paramPanel);
                        }
                    }
                }
            } catch(ConcurrencyException ex) {
                
                // second attempt should succeed, because the Oid would have
                // been updated in the attempt
                ObjectAdapter targetAdapter = getActionModel().getTargetAdapter();

                // forward onto the target page with the concurrency exception
                final EntityPage entityPage = new EntityPage(targetAdapter, ex);
                
                ActionParametersFormPanel.this.setResponsePage(entityPage);
                
                getAuthenticationSession().getMessageBroker().addWarning(ex.getMessage());
                return;
            }
            
        }
    }
}
