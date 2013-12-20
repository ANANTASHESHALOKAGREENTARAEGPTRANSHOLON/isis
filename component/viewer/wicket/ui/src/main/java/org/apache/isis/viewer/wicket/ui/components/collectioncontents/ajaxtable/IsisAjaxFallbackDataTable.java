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
package org.apache.isis.viewer.wicket.ui.components.collectioncontents.ajaxtable;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxNavigationToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.data.table.NoRecordsToolbar;
import org.apache.wicket.markup.repeater.IItemFactory;
import org.apache.wicket.markup.repeater.IItemReuseStrategy;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.OddEvenItem;
import org.apache.wicket.markup.repeater.ReuseIfModelsEqualStrategy;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.lang.Generics;

import org.apache.isis.viewer.wicket.model.hints.UiHintPathSignificant;

public class IsisAjaxFallbackDataTable<T, S> extends DataTable<T, S> implements UiHintPathSignificant {
    private static final long serialVersionUID = 1L;
    private final ISortableDataProvider<T, S> dataProvider;

    public IsisAjaxFallbackDataTable(final String id, final List<? extends IColumn<T, S>> columns,
        final ISortableDataProvider<T, S> dataProvider, final int rowsPerPage)
    {
        super(id, columns, dataProvider, rowsPerPage);
        this.dataProvider = dataProvider;
        setOutputMarkupId(true);
        setVersioned(false);
        setItemReuseStrategy(new PreserveModelReuseStrategy());
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        buildGui();
    }
    
    private void buildGui() {
        addTopToolbar(new IsisAjaxFallbackHeadersToolbar<S>(this, this.dataProvider));
        addBottomToolbar(new IsisAjaxNavigationToolbar(this));
        addBottomToolbar(new NoRecordsToolbar(this));
    }
    
    @Override
    protected Item<T> newRowItem(final String id, final int index, final IModel<T> model)
    {
        return new OddEvenItem<T>(id, index, model);
    }

    static class PreserveModelReuseStrategy implements IItemReuseStrategy {
        private static final long serialVersionUID = 1L;

        private static IItemReuseStrategy instance = new PreserveModelReuseStrategy();

        /**
         * @return static instance
         */
        public static IItemReuseStrategy getInstance()
        {
            return instance;
        }

        /**
         * @see org.apache.wicket.markup.repeater.IItemReuseStrategy#getItems(org.apache.wicket.markup.repeater.IItemFactory,
         *      java.util.Iterator, java.util.Iterator)
         */
        @Override
        public <T> Iterator<Item<T>> getItems(final IItemFactory<T> factory,
            final Iterator<IModel<T>> newModels, Iterator<Item<T>> existingItems)
        {
            final Map<IModel<T>, Item<T>> modelToItem = Generics.newHashMap();
            while (existingItems.hasNext())
            {
                final Item<T> item = existingItems.next();
                modelToItem.put(item.getModel(), item);
            }

            return new Iterator<Item<T>>()
            {
                private int index = 0;

                @Override
                public boolean hasNext()
                {
                    return newModels.hasNext();
                }

                @Override
                public Item<T> next()
                {
                    final IModel<T> model = newModels.next();
                    final Item<T> oldItem = modelToItem.get(model);

                    final IModel<T> model2;
                    if (oldItem == null) {
                        model2 = model;
                    } else {
                        model2 = oldItem.getModel();
                    }
                    return factory.newItem(index++, 
                            model2);
                }

                @Override
                public void remove()
                {
                    throw new UnsupportedOperationException();
                }

            };
        }

    }

    
}