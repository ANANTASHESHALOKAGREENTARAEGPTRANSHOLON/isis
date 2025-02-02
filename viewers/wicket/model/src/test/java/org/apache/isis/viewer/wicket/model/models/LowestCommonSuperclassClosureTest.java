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
package org.apache.isis.viewer.wicket.model.models;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.isis.core.metamodel.commons.ClassExtensions;

import lombok.val;

public class LowestCommonSuperclassClosureTest {

    static class Animal {}
    static class Mineral {}
    static class Vegetable {}
    static class Mammal extends Animal {}
    static class Lion extends Mammal {}

    @Test
    public void nothingInCommon() {
        assertCommonOfListIs(Arrays.asList(new Animal(), new Mineral(), new Vegetable()), Object.class);
    }

    @Test
    public void superclassInCommon() {
        assertCommonOfListIs(Arrays.asList(new Animal(), new Mammal()), Animal.class);
    }

    @Test
    public void subclassInCommon() {
        assertCommonOfListIs(Arrays.asList(new Lion(), new Lion()), Lion.class);
    }

    private static void assertCommonOfListIs(List<Object> list, Class<?> expected) {
        val commonSuperClassFinder = new ClassExtensions.CommonSuperclassFinder();
        list.forEach(commonSuperClassFinder::collect);
        assertEquals(expected, commonSuperClassFinder.getCommonSuperclass().get());
    }


}
