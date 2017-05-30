/*
 * Copyright 2014-2017 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hawkular.commons.log;

import org.jboss.logging.Logger;

/**
 *
 * Utility class to simplify logger lookup.
 *
 *
 * @author Thomas Segismont
 * @author Lucas Ponce
 */
public class MsgLogging {
    public static <T> T getMsgLogger(Class<T> loggerClass, Class<?> loggedClass) {
        return Logger.getMessageLogger(loggerClass, loggedClass.getName());
    }

    public static MsgLogger getMsgLogger(Class<?> loggedClass) {
        return Logger.getMessageLogger(MsgLogger.class, loggedClass.getName());
    }

    private MsgLogging() {
    }
}
