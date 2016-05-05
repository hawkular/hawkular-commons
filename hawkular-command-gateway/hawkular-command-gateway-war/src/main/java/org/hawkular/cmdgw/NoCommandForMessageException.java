/*
 * Copyright 2014-2016 Red Hat, Inc. and/or its affiliates
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
package org.hawkular.cmdgw;

/**
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class NoCommandForMessageException extends Exception {
    private static final long serialVersionUID = -9190882295360443385L;

    public NoCommandForMessageException() {
        super();
    }

    public NoCommandForMessageException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoCommandForMessageException(String message) {
        super(message);
    }

    public NoCommandForMessageException(Throwable cause) {
        super(cause);
    }

}
