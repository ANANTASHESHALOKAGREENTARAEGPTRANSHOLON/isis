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

package org.apache.isis.core.metamodel.facets.object.viewmodel.iface;

import org.apache.isis.applib.ViewModel;
import org.apache.isis.core.metamodel.facetapi.FacetHolder;
import org.apache.isis.core.metamodel.facets.object.viewmodel.ViewModelFacetAbstract;

public class ViewModelFacetInterface extends ViewModelFacetAbstract {

    public ViewModelFacetInterface(final FacetHolder holder) {
        super(holder);
    }

    @Override
    public void initialize(Object pojo, String memento) {
        final ViewModel viewModel = (ViewModel)pojo;
        viewModel.viewModelInit(memento);
    }
    
    @Override
    public String memento(Object pojo) {
        final ViewModel viewModel = (ViewModel)pojo;
        return viewModel.viewModelMemento();
    }

    @Override
    public boolean isCloneable(Object pojo) {
        return pojo != null && pojo instanceof ViewModel.Cloneable;
    }

    @Override
    public Object clone(Object pojo) {
        ViewModel.Cloneable viewModelCloneable = (ViewModel.Cloneable) pojo;
        return viewModelCloneable.clone();
    }

}
