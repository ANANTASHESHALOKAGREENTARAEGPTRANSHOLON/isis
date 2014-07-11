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
package org.apache.isis.applib.services.eventbus;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.isis.applib.Identifier;
import org.apache.isis.applib.services.command.Command;
import org.apache.isis.applib.util.ObjectContracts;

public abstract class ActionInteractionEvent<S> extends AbstractInteractionEvent<S> {

    private static final long serialVersionUID = 1L;

    //region > Default class

    /**
     * Propagated if no custom subclass was specified using
     * {@link org.apache.isis.applib.annotation.ActionInteraction} annotation.
     */
    public static class Default extends ActionInteractionEvent<Object> {
        private static final long serialVersionUID = 1L;
        public Default(Object source, Identifier identifier, Object... arguments) {
            super(source, identifier, arguments);
        }
    }
    //endregion

    //region > constructors
    public ActionInteractionEvent(
            final S source,
            final Identifier identifier) {
        super(source, identifier);
    }

    public ActionInteractionEvent(
            final S source,
            final Identifier identifier,
            final Object... arguments) {
        this(source, identifier, arguments != null? Arrays.asList(arguments): Collections.emptyList());
    }

    public ActionInteractionEvent(
            final S source,
            final Identifier identifier,
            final List<Object> arguments) {
        this(source, identifier);
        this.arguments = Collections.unmodifiableList(arguments);
    }
    //endregion

    //region > command
    private Command command;

    /**
     * The {@link org.apache.isis.applib.services.command.Command} for this action.
     *
     * <p>
     * Set when in {@link Phase#EXECUTING} and {@link Phase#EXECUTED}, but not for earlier phases.
     *
     * <p>
     * The command is set by the framework based on the configured
     * {@link org.apache.isis.applib.services.command.CommandContext}) service).  Ths command may or may not be
     * persisted, depending on the which implementation of
     * {@link org.apache.isis.applib.services.command.spi.CommandService} service is configured.
     */
    public Command getCommand() {
        return command;
    }

    /**
     * Not API - set by the framework.
     */
    public void setCommand(Command command) {
        this.command = command;
    }
    //endregion

    //region > arguments
    private List<Object> arguments;
    /**
     * The arguments being used to invoke the action; populated at {@link Phase#VALIDATE} and subsequent phases
     * (but null for {@link Phase#HIDE hidden} and {@link Phase#DISABLE disable} phases).
     */
    public List<Object> getArguments() {
        return arguments;
    }

    /**
     * Not API - set by the framework.
     */
    public void setArguments(List<Object> arguments) {
        this.arguments = arguments;
    }
    //endregion

    //region > toString
    @Override
    public String toString() {
        return ObjectContracts.toString(this, "source,identifier,phase");
    }
    //endregion

}