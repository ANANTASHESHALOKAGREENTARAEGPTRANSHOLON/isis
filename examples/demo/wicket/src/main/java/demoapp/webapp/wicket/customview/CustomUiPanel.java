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

package demoapp.webapp.wicket.customview;

import java.io.IOException;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.request.resource.ByteArrayResource;

import org.apache.isis.core.metamodel.spec.ManagedObject;
import org.apache.isis.core.metamodel.spec.feature.OneToOneAssociation;
import org.apache.isis.viewer.common.model.object.ObjectUiModel;
import org.apache.isis.viewer.wicket.model.hints.UiHintContainer;
import org.apache.isis.viewer.wicket.model.mementos.PropertyMemento;
import org.apache.isis.viewer.wicket.model.models.EntityModel;
import org.apache.isis.viewer.wicket.ui.ComponentFactory;
import org.apache.isis.viewer.wicket.ui.ComponentType;
import org.apache.isis.viewer.wicket.ui.panels.PanelAbstract;

import lombok.SneakyThrows;
import lombok.val;

import demoapp.dom.ui.custom.geocoding.GeoapifyClient;
import demoapp.dom.ui.custom.vm.CustomUiVm;

public class CustomUiPanel extends PanelAbstract<EntityModel>  {

    private static final long serialVersionUID = 1L;

    private final GeoapifyClient geoapifyClient;

    public CustomUiPanel(
            final String id,
            final EntityModel model,
            final ComponentFactory componentFactory,
            final GeoapifyClient geoapifyClient) {
        super(id, model);
        this.geoapifyClient = geoapifyClient;
    }


    @Override
    public UiHintContainer getUiHintContainer() {
        // disables hinting by this component
        return null;
    }

    /**
     * Build UI only after added to parent.
     */
    @Override
    public void onInitialize() {
        super.onInitialize();
        buildGui();
    }


    @SneakyThrows
    private void buildGui() {
        val managedObject = (ManagedObject) getModelObject();
        val customUiVm = (CustomUiVm) managedObject.getPojo();

        val latitude = new Label("latitude", customUiVm.getLatitude());
        val longitude = new Label("longitude", customUiVm.getLongitude());
        val address = new Label("address", customUiVm.getAddress());
        val map = createMapComponent("map", customUiVm);
        val sourcesComponent = createPropertyComponent(managedObject, "sources");
        val descriptionComponent = createPropertyComponent(managedObject, "description");

        addOrReplace(latitude, longitude, address, map, sourcesComponent, descriptionComponent);
    }

    private Image createMapComponent(String id, CustomUiVm customUiVm) throws IOException {
        val bytes = geoapifyClient.toJpeg(customUiVm.getLatitude(), customUiVm.getLongitude(), customUiVm.getZoom());
        val map = new Image(id, new ByteArrayResource("image/jpeg", bytes));
        return map;
    }

    private Component createPropertyComponent(ManagedObject managedObject, String propertyId) {
        val spec = managedObject.getSpecification();
        val descriptionAssoc = (OneToOneAssociation) spec.getAssociationElseFail(propertyId);
        val descriptionPm = new PropertyMemento(descriptionAssoc);

        val entityModel = EntityModel.ofAdapter(getCommonContext(), managedObject);
        val descriptionModel = entityModel.getPropertyModel(descriptionPm, ObjectUiModel.Mode.VIEW, ObjectUiModel.RenderingHint.REGULAR);
        return getComponentFactoryRegistry().createComponent(ComponentType.SCALAR_NAME_AND_VALUE, propertyId, descriptionModel);
    }


}
