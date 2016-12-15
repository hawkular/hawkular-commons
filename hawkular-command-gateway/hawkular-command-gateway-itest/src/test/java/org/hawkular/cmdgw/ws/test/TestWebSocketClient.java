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
package org.hawkular.cmdgw.ws.test;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;
import org.hamcrest.TypeSafeMatcher;
import org.hawkular.bus.common.BasicMessageWithExtraData;
import org.hawkular.cmdgw.api.ApiDeserializer;
import org.hawkular.cmdgw.api.WelcomeResponse;
import org.hawkular.cmdgw.ws.test.TestWebSocketClient.ActualEvent.ActualClose;
import org.hawkular.cmdgw.ws.test.TestWebSocketClient.ActualEvent.ActualFailure;
import org.hawkular.cmdgw.ws.test.TestWebSocketClient.ActualEvent.ActualMessage;
import org.hawkular.cmdgw.ws.test.TestWebSocketClient.ExpectedEvent.ExpectedClose;
import org.hawkular.cmdgw.ws.test.TestWebSocketClient.ExpectedEvent.ExpectedMessage;
import org.testng.Assert;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.ws.WebSocket;
import okhttp3.ws.WebSocketCall;
import okhttp3.ws.WebSocketListener;

import okio.Buffer;
import okio.BufferedSource;

/**
 * A client for testing WebSocket conversations. A typical usage starts by {@link TestWebSocketClient#builder()}.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class TestWebSocketClient implements Closeable {

    public abstract static class ActualEvent {
        public static class ActualClose extends ActualEvent {
            private final int code;
            private final String reason;
            public ActualClose(TestListener testListener, int index, int code, String reason) {
                super(testListener, index);
                this.code = code;
                this.reason = reason;
            }
            public int getCode() {
                return code;
            }
            public String getReason() {
                return reason;
            }
            @Override
            public String toString() {
                return "ActualClose [code=" + code + ", reason=" + reason + "]";
            }
        }
        public static class ActualFailure extends ActualEvent {
            private final IOException exception;
            private final Response response;
            public ActualFailure(TestListener testListener, int index, IOException exception, Response response) {
                super(testListener, index);
                this.exception = exception;
                this.response = response;
            }
            public IOException getException() {
                return exception;
            }
            public Response getResponse() {
                return response;
            }
            @Override
            public String toString() {
                String stackTrace = "";
                if (exception != null) {
                    try (StringWriter w = new StringWriter(); PrintWriter pw = new PrintWriter(w, true)) {
                        exception.printStackTrace(pw);
                        stackTrace = w.toString();
                    } catch (IOException ignored) {
                    }
                }
                return "ActualFailure [response=" + response + ", exception=" + exception + ", stackTrace=["+ stackTrace +"]]";
            }

        }
        public static class ActualMessage extends ActualEvent {
            private final ReusableBuffer body;
            private final MediaType type;
            public ActualMessage(TestListener testListener, int index, ResponseBody body) throws IOException {
                super(testListener, index);
                try (BufferedSource payload = body.source()) {
                    this.type = body.contentType();
                    this.body = new ReusableBuffer(body.source());
                }
            }

            public ReusableBuffer getBody() {
                return body;
            }

            public MediaType getType() {
                return type;
            }

            @Override
            public String toString() {
                return "ActualMessage [type=" + type + ", body=" + body + "]";
            }

        }
        public static class ActualOpen extends ActualEvent {
            private final Response response;
            private final WebSocket webSocket;
            public ActualOpen(TestListener testListener, int index, WebSocket webSocket, Response response) {
                super(testListener, index);
                this.webSocket = webSocket;
                this.response = response;
            }
            public Response getResponse() {
                return response;
            }
            public WebSocket getWebSocket() {
                return webSocket;
            }
            @Override
            public String toString() {
                return "ActualOpen [response=" + response + "]";
            }
        }

        private final int index;
        private final TestListener testListener;
        public ActualEvent(TestListener testListener, int index) {
            super();
            this.testListener = testListener;
            this.index = index;
        }

        public int getIndex() {
            return index;
        }

        public TestListener getTestListener() {
            return testListener;
        }
    }

    public interface Answer {
        Answer CLOSE = new Answer() {

            @Override
            public void schedule(ExecutorService executor, WebSocket webSocket) {
                executor.execute(() -> {
                    try {
                        log.fine("About to send manual close");
                        webSocket.close(1000, "OK");
                        log.fine("Close sent");
                    } catch (IOException e) {
                        log.warning("Could not close WebSocket");
                    }
                });
            }
        };

        void schedule(final ExecutorService executor, final WebSocket webSocket);
    }

    /**
     * A matcher for messages having binary attachments.
     *
     * @see Builder#expectBinary(String, TypeSafeMatcher)
     */
    public static class BinaryAwareMatcher extends PatternMatcher {

        private TypeSafeMatcher<InputStream> binaryMatcher;

        public BinaryAwareMatcher(String pattern, TypeSafeMatcher<InputStream> binaryMatcher) {
            super(pattern);
            this.binaryMatcher = binaryMatcher;
        }

        @Override
        public void describeTo(Description description) {
            super.describeTo(description);
            description.appendDescriptionOf(binaryMatcher);
        }

        @Override
        public boolean matches(ReusableBuffer actual, TestListener testListener) {
            return super.matches(actual, testListener) && binaryMatcher.matches(actual.getBinaryPart());
        }

    }

    /**
     * @see TestWebSocketClient#builder()
     */
    public static class Builder {

        private String authentication = ClientConfig.authHeader;

        private final List<ExpectedEvent> expectedEvents = new ArrayList<>();

        private String url;

        private int connectTimeoutSeconds = 10;
        private int readTimeoutSeconds = 120;

        public Builder authentication(String authentication) {
            this.authentication = authentication;
            return this;
        }

        public TestWebSocketClient build() {
            okhttp3.Request.Builder rb = new Request.Builder().url(url);
            if (authentication != null) {
                rb.addHeader("Authorization", authentication);
            }
            Request request = rb.build();
            TestListener listener = new TestListener(Collections.unmodifiableList(expectedEvents));
            return new TestWebSocketClient(request, listener, connectTimeoutSeconds, readTimeoutSeconds);
        }

        /**
         * @param textPattern a regular expression
         * @param binaryMatcher a custom matcher to check the incoming binary attachment
         * @param answer the answer to send after receiving the expected binary message
         * @return this builder
         */
        public Builder expectBinary(String textPattern, TypeSafeMatcher<InputStream> binaryMatcher, Answer answer) {
            ExpectedEvent expectedEvent =
                    new ExpectedMessage(new BinaryAwareMatcher(textPattern, binaryMatcher),
                            CoreMatchers.equalTo(WebSocket.BINARY), answer);
            expectedEvents.add(expectedEvent);
            return this;
        }

        public Builder expectBinary(String textPattern, TypeSafeMatcher<InputStream> binaryMatcher) {
            return expectBinary(textPattern, binaryMatcher, null);
        }

        public Builder expectGenericSuccess(String feedId) {
            ExpectedEvent expectedEvent = new ExpectedMessage(
                    new PatternMatcher("\\QGenericSuccessResponse={\"message\":"
                            + "\"The request has been forwarded to feed ["
                            + feedId + "] (\\E.*"),
                    CoreMatchers.equalTo(WebSocket.TEXT), null);
            expectedEvents.add(expectedEvent);
            return this;
        }

        public Builder expectMessage(ExpectedEvent expectedEvent) {
            expectedEvents.add(expectedEvent);
            return this;
        }

        /**
         * @param expectedRegex a text message regular expression to match
         * @return this builder
         */
        public Builder expectRegex(String expectedRegex) {
            ExpectedEvent expectedEvent = new ExpectedMessage(new PatternMatcher(expectedRegex),
                    CoreMatchers.equalTo(WebSocket.TEXT), null);
            expectedEvents.add(expectedEvent);
            return this;
        }
        /**
         * @param expectedTextMessage a plain text message to compare (rather than a regular expression)
         * @return this builder
         */
        public Builder expectText(String expectedTextMessage) {
            return expectText(expectedTextMessage, null);
        }

        public Builder expectText(String expectedTextMessage, Answer messageAnswer) {
            ExpectedEvent expectedEvent =
                    new ExpectedMessage(new PatternMatcher("\\Q" + expectedTextMessage + "\\E.*"),
                            CoreMatchers.equalTo(WebSocket.TEXT), messageAnswer);
            expectedEvents.add(expectedEvent);
            return this;
        }

        public Builder expectClose() {
            expectedEvents.add(new ExpectedClose(1000, "OK"));
            return this;
        }

        /**
         * @param answer the text message to send out if the welcome came as expected
         * @param attachment bits to send out as a binary attachment of {@code answer}
         * @return this builder
         */
        public Builder expectWelcome(MessageAnswer messageAnswer) {
            ExpectedEvent expectedEvent = new ExpectedMessage(new WelcomeMatcher(),
                    CoreMatchers.equalTo(WebSocket.TEXT), messageAnswer);
            expectedEvents.add(expectedEvent);
            return this;
        }

        public Builder expectWelcome(String answer) {
            return expectWelcome(new MessageAnswer(answer));
        }

        /**
         * @param url the URL of the WebSocket endpoint
         * @return this builder
         */
        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder connectTimeout(int connectTimeoutSeconds) {
            this.connectTimeoutSeconds = connectTimeoutSeconds;
            return this;
        }

        public Builder readTimeout(int readTimeoutSeconds) {
            this.readTimeoutSeconds = readTimeoutSeconds;
            return this;
        }

    }

    public abstract static class ExpectedEvent {
        public static class ExpectedAny extends ExpectedEvent {
            private static Set<Class<? extends ActualEvent>> collectExpectedTypes(List<ExpectedEvent> expectedEvents) {
                Set<Class<? extends ActualEvent>> result = new LinkedHashSet<>();
                for (ExpectedEvent expectedEvent : expectedEvents) {
                    result.addAll(expectedEvent.getExpectedTypes());
                }
                return result;

            }

            private final List<ExpectedEvent> expectedEvents;

            public ExpectedAny(List<ExpectedEvent> expectedEvents) {
                super(collectExpectedTypes(expectedEvents), null);
                this.expectedEvents = expectedEvents;
            }

            @Override
            public MessageReport match(ActualEvent actualEvent) {
                for (ExpectedEvent expectedEvent : expectedEvents) {
                    if (expectedEvent.getExpectedTypes().contains(actualEvent.getClass())) {
                        return expectedEvent.match(actualEvent);
                    }
                }
                return unexpectedType(actualEvent);
            }
            public void scheduleAnswer(ActualEvent actualEvent, ExecutorService executor, WebSocket webSocket) {

                for (ExpectedEvent expectedEvent : expectedEvents) {
                    if (expectedEvent.getExpectedTypes().contains(actualEvent.getClass())) {
                        expectedEvent.scheduleAnswer(actualEvent, executor, webSocket);
                    }
                }
            }
        }
        public static class ExpectedClose extends ExpectedEvent {
            private final int code;
            private final String reason;

            public ExpectedClose(int code, String reason) {
                super(ActualClose.class, null);
                this.code = code;
                this.reason = reason;
            }

            public int getCode() {
                return code;
            }

            public String getReason() {
                return reason;
            }

            /** @see org.hawkular.cmdgw.ws.test.TestWebSocketClient.ExpectedEvent#match(org.hawkular.cmdgw.ws.test.TestWebSocketClient.ActualEvent) */
            @Override
            public MessageReport match(ActualEvent actualEvent) {
                if (actualEvent instanceof ActualClose) {
                    ActualClose actualClose = (ActualClose) actualEvent;
                    if (this.code == actualClose.getCode() && Objects.equals(this.reason, actualClose.getReason())) {
                        return MessageReport.passed(actualEvent.getIndex());
                    } else {
                        Description description = new StringDescription() //
                        .appendText("Expected: code ") //
                        .appendValue(this.code) //
                        .appendText(" and reason ") //
                        .appendValue(this.reason) //
                        .appendText("\n     but got: code ") //
                        .appendValue(code) //
                        .appendText(" and reason ") //
                        .appendValue(reason);
                        return new MessageReport(new AssertionError(description.toString()), actualEvent.getIndex());
                    }
                } else {
                    return unexpectedType(actualEvent);
                }
            }
        }
        public static class ExpectedFailure extends ExpectedEvent {
            public static final ExpectedEvent UNAUTHORIZED = new ExpectedFailure(401, "Unauthorized");
            private final int code;
            private final String message;
            public ExpectedFailure(int code, String message) {
                super(ActualFailure.class, null);
                this.code = code;
                this.message = message;
            }

            public MessageReport match(ActualEvent actualEvent) {
                if (actualEvent instanceof ActualFailure) {
                    ActualFailure actualFailure = (ActualFailure) actualEvent;
                    Response response = actualFailure.getResponse();
                    if (this.code == response.code() && Objects.equals(this.message, response.message())) {
                        return MessageReport.passed(actualEvent.getIndex());
                    } else {
                        Description description = new StringDescription() //
                        .appendText("Expected a failure with: code ") //
                        .appendValue(this.code) //
                        .appendText(" and message ") //
                        .appendValue(this.message) //
                        .appendText("\n     but got: code ") //
                        .appendValue(response.code()) //
                        .appendText(" and message ") //
                        .appendValue(response.message());
                        return new MessageReport(new AssertionError(description.toString()), actualEvent.getIndex());
                    }
                } else {
                    return unexpectedType(actualEvent);
                }
            }
        }
        /**
         * A pair of {@link Matcher}s for checking the incoming messages optionally bundled with a text and binary answer to
         * send out if the incoming message matched the expectations.
         */
        public static class ExpectedMessage extends ExpectedEvent {

            protected final WebSocketArgumentMatcher<ReusableBuffer> inMessageMatcher;
            private final org.hamcrest.Matcher<MediaType> inTypeMatcher;


            public ExpectedMessage(WebSocketArgumentMatcher<ReusableBuffer> inMessageMatcher,
                    org.hamcrest.Matcher<MediaType> inTypeMatcher, Answer answer) {
                super(ActualMessage.class, answer);
                this.inMessageMatcher = inMessageMatcher;
                this.inTypeMatcher = inTypeMatcher;
            }

            public MessageReport match(ActualEvent actualEvent) {
                if (actualEvent instanceof ActualMessage) {
                    ActualMessage msg = (ActualMessage) actualEvent;
                    MediaType type = msg.getType();
                    ReusableBuffer message = msg.getBody();


                    Description description = new StringDescription();
                    boolean fail = false;
                    if (!inMessageMatcher.matches(message, actualEvent.getTestListener())) {
                        description
                                .appendText("Expected: ")
                                .appendDescriptionOf(inMessageMatcher)
                                .appendText("\n     but: ");
                        inMessageMatcher.describeMismatch(message, description);
                        fail = true;
                    }
                    if (!inTypeMatcher.matches(type)) {
                        description
                                .appendText("Expected: ")
                                .appendDescriptionOf(inTypeMatcher)
                                .appendText("\n     but: ");
                        inTypeMatcher.describeMismatch(type, description);
                        fail = true;
                    }

                    if (fail) {
                        return new MessageReport(new AssertionError(description.toString()), actualEvent.getIndex());
                    } else {
                        return MessageReport.passed(actualEvent.getIndex());
                    }
                } else {
                    return unexpectedType(actualEvent);
                }

            }


        }
        public static ExpectedAny anyOf(ExpectedEvent... events) {
            return new ExpectedAny(Arrays.asList(events));
        }
        protected final Answer answer;
        protected final Set<Class<? extends ActualEvent>> expectedTypes;

        public ExpectedEvent(Class<? extends ActualEvent> expectedType, Answer answer) {
            this(Collections.singleton(expectedType), answer);
        }

        public ExpectedEvent(Set<Class<? extends ActualEvent>> expectedTypes, Answer answer) {
            super();
            this.expectedTypes = expectedTypes;
            this.answer = answer;
        }
        public Set<Class<? extends ActualEvent>> getExpectedTypes() {
            return expectedTypes;
        }

        public abstract MessageReport match(ActualEvent actualEvent);

        public void scheduleAnswer(ActualEvent actualEvent, ExecutorService executor, WebSocket webSocket) {
            if (answer != null) {
                answer.schedule(executor, webSocket);
            } else {
                log.fine("No answer to send for message[" + actualEvent.getIndex() + "]");
            }
        }

        protected MessageReport unexpectedType(ActualEvent actualEvent) {
            final String msg = "Expected one of [" + getExpectedTypes()
                    + "] found [" + actualEvent.toString() +"]";
            log.fine(msg);
            return new MessageReport(new IllegalStateException(msg), actualEvent.getIndex());
        }
    }

    public static class MessageAnswer implements Answer {
        private final URL binaryAnswer;

        private final long sleepAfterAnswerMs;
        private final String textAnswer;
        public MessageAnswer(String textAnswer) {
            this(textAnswer, null, 0);
        }
        public MessageAnswer(String textAnswer, URL binaryAnswer, long sleepAfterAnswerMs) {
            super();
            this.textAnswer = textAnswer;
            this.binaryAnswer = binaryAnswer;
            this.sleepAfterAnswerMs = sleepAfterAnswerMs;
        }

        /**
         * @param webSocket
         * @return
         */
        public void schedule(final ExecutorService executor, final WebSocket webSocket) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try (Buffer b1 = new Buffer()) {
                        if (textAnswer != null) {
                            log.fine("Sending over WebSocket: " + textAnswer);
                            b1.writeUtf8(textAnswer);
                        }
                        if (binaryAnswer != null) {
                            try (InputStream in = binaryAnswer.openStream()) {
                                int b;
                                while ((b = in.read()) != -1) {
                                    b1.writeByte(b);
                                    // System.out.println("Writing binary data");
                                }
                            }
                        }
                        RequestBody body = RequestBody.create(binaryAnswer == null ? WebSocket.TEXT : WebSocket.BINARY,
                                b1.readByteArray());
                        webSocket.sendMessage(body);

                        if (sleepAfterAnswerMs > 0) {
                            log.fine("About to sleep for [" + sleepAfterAnswerMs + "] ms");
                            try {
                                Thread.sleep(sleepAfterAnswerMs);
                            } catch (InterruptedException e) {
                                log.fine("Interrupted while sleeping for [" + sleepAfterAnswerMs + "] ms");
                            }
                            log.fine("Woke up after [" + sleepAfterAnswerMs + "] ms");
                        } else {
                            log.fine("No sleep configured");
                        }
                    } catch (IOException e) {
                        throw new RuntimeException("Unable to send message", e);
                    }
                }
            });
        }

    }

    /**
     * Some basic data about how a given message fulfilled the expectations set in {@link ExpectedEvent}.
     */
    public static class MessageReport {

        public static MessageReport passed(int index) {
            return new MessageReport(null, index);
        }

        private final int messageIndex;
        private final Throwable throwable;

        public MessageReport(Throwable throwable, int messageIndex) {
            super();
            this.throwable = throwable;
            this.messageIndex = messageIndex;
        }

        /**
         * @return the order in which the given message came. The first index is {@code 0}
         */
        public int getMessageIndex() {
            return messageIndex;
        }

        /**
         * @return the {@link Throwable} associated with this report
         */
        public Throwable getThrowable() {
            return throwable;
        }

        /**
         * @return {@code true} if the received message matched the expectations set in {@link ExpectedEvent}.
         */
        public boolean passed() {
            return this.throwable == null;
        }

        public String toString() {
            return "Message[" + messageIndex + "]: [" + (passed() ? "PASSED" : "FAILED - " + throwable.getMessage())
                    + "]";
        }
    }

    /**
     * A regular expression matcher.
     */
    public static class PatternMatcher extends TypeSafeMatcher<ReusableBuffer>
            implements WebSocketArgumentMatcher<ReusableBuffer> {

        protected final String pattern;

        /**
         * @param pattern a regular expression to match the text part of the incoming message. The pattern may contain
         *            placeholders (such as {@code sessionId}) that will be resolved against an instance of
         *            {@link TestListener}.
         */
        public PatternMatcher(String pattern) {
            super();
            this.pattern = pattern;
        }

        /**
         * Resolve placeholders (such as {@code sessionId}) against the given instance of {@link TestListener}
         *
         * @return the resolved and compiled {@link Pattern}
         */
        protected Pattern compile(TestListener testListener) {
            String resolvedPattern = testListener == null ? pattern : resolvePlaceHolders(pattern, testListener);
            return Pattern.compile(resolvedPattern);
        }

        /*
         * @see org.hamcrest.SelfDescribing#describeTo(org.hamcrest.Description)
         */
        @Override
        public void describeTo(Description description) {
            description.appendText("to match pattern ").appendValue(pattern);
        }

        @Override
        public boolean matches(ReusableBuffer actual, TestListener testListener) {
            return compile(testListener).matcher(actual.getTextPart()).matches();
        }

        @Override
        protected boolean matchesSafely(ReusableBuffer item) {
            /* make sure we do not circumvent matches(ReusableBuffer actual, TestListener testListener) */
            throw new UnsupportedOperationException();
        }

        private String resolvePlaceHolders(String stringPattern, Object context) {
            String method = "";
            Class<?> ctxClass = context.getClass();

            try {
                String placeholderPattern = "(\\Q{{\\E[[a-zA-Z0-9]| |\t]+?\\Q}}\\E)";
                Pattern rg = Pattern.compile(placeholderPattern);
                java.util.regex.Matcher m = rg.matcher(stringPattern);
                while (m.find()) {
                    method = m.group();
                    method = method.replaceAll("\\{", "").replaceAll("\\}", "");
                    method = "get" + method.substring(0, 1).toUpperCase() + method.substring(1);
                    Method getter = ctxClass.getDeclaredMethod(method);
                    String p = (String) getter.invoke(context);
                    stringPattern = stringPattern.replaceAll(Pattern.quote(m.group()), p);
                }
                return stringPattern;
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("Method " + method + " not found in context " + ctxClass.getName());
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Method " + method + " not found in context " + ctxClass.getName());
            } catch (java.lang.reflect.InvocationTargetException e) {
                throw new RuntimeException("Method " + method + " not found in context " + ctxClass.getName());
            }
        }

    }

    public static class PingForeverAnswer implements Answer {

        /** @see org.hawkular.cmdgw.ws.test.TestWebSocketClient.Answer#schedule(java.util.concurrent.ExecutorService, okhttp3.ws.WebSocket) */
        @Override
        public void schedule(final ExecutorService executor, final WebSocket webSocket) {
            Runnable r = new Runnable() {
                private int counter = 0;
                @Override
                public void run() {
                    try {
                        int ping = counter++;
                        log.fine("about to sent ping ["+ ping +"]");
                        try (Buffer b = new Buffer()) {
                            b.writeUtf8(String.valueOf(ping));
                            webSocket.sendPing(b);
                        }
                        /* ... and repeat */
                        try {
                            Thread.sleep(250);
                            executor.execute(this);
                        } catch (InterruptedException e) {
                            log.fine("Interrupted.");
                        }
                    } catch (IOException e) {
                        log.log(Level.FINE, "Could not ping", e);
                    }
                }
            };
            executor.execute(r);
        }

    }

    /**
     * A utility to manipulate {@link BufferedSource} representations of incoming messages.
     */
    public static class ReusableBuffer {
        /**
         * The index of the first byte of the binary attachment in {@link #bytes} array
         */
        private final int binaryOffset;
        private final byte[] bytes;
        private boolean lastWasHighSurrogate = false;
        private final String textPart;

        public ReusableBuffer(BufferedSource payload) throws IOException {
            this.bytes = payload.readByteArray();
            payload.close();

            int binOffset = 0;
            StringBuilder sb = new StringBuilder();
            try (Reader r = new InputStreamReader(new ByteArrayInputStream(bytes), "utf-8")) {
                int numberOfOpenedObjects = 0;
                int c;
                LOOP: while ((c = r.read()) >= 0) {
                    char ch = (char) c;
                    switch (ch) {
                        case '{':
                            numberOfOpenedObjects++;
                            sb.append(ch);
                            binOffset += byteLength(ch);
                            break;
                        case '}':
                            numberOfOpenedObjects--;
                            sb.append(ch);
                            binOffset += byteLength(ch);
                            if (numberOfOpenedObjects == 0) {
                                /* at this point, we closed all json objects that we have opened, so, we are at
                                 * the end of the text part of the message */
                                break LOOP;
                            }
                            break;
                        default:
                            sb.append(ch);
                            binOffset += byteLength(ch);
                            break;
                    }
                }
                this.textPart = sb.toString();
                this.binaryOffset = binOffset;
            }

        }

        /**
         * Based on http://stackoverflow.com/a/8512877
         *
         * @param ch
         * @return
         */
        private int byteLength(char ch) {
            if (lastWasHighSurrogate) {
                lastWasHighSurrogate = false;
                return 2;
            } else if (ch <= 0x7F) {
                return 1;
            } else if (ch <= 0x7FF) {
                return 2;
            } else if (Character.isHighSurrogate(ch)) {
                lastWasHighSurrogate = true;
                return 2;
            } else {
                return 3;
            }
        }

        public Buffer copy() {
            Buffer payloadCopy = new Buffer();
            payloadCopy.write(bytes);
            return payloadCopy;
        }

        public int getBinaryLength() {
            return bytes.length - binaryOffset;
        }

        public InputStream getBinaryPart() {
            if (binaryOffset >= bytes.length) {
                throw new IllegalStateException("No binary attachment in this buffer");
            }
            return new ByteArrayInputStream(bytes, binaryOffset, bytes.length - binaryOffset);
        }

        public String getTextPart() {
            return textPart;
        }

        public boolean hasBinaryPart() {
            return binaryOffset < bytes.length;
        }

        public String toString() {
            return getTextPart()
                    + (hasBinaryPart() ? " + [" + getBinaryLength() + "] bytes of binary attachment" : "");
        }
    }

    /**
     * An implementation of {@link WebSocketListener} that does matches the {@link ExpectedEvent}s against incoming
     * messages and reports the results.
     */
    public static class TestListener implements WebSocketListener {

        private boolean closed;
        /**
         * Actually a blocing variable rather than a queue. Used to hand over the results to a consumer that runs in
         * another thread.
         */
        private final BlockingQueue<List<MessageReport>> conversationResult;

        private final List<ExpectedEvent> expectedEvents;

        private int inMessageCounter = 0;
        private final List<MessageReport> reports = new ArrayList<>();
        private final ExecutorService sendExecutor;

        private String sessionId;
        private WebSocket webSocket;

        private TestListener(List<ExpectedEvent> expectedEvents) {
            super();
            this.expectedEvents = expectedEvents;
            this.sendExecutor = Executors.newSingleThreadExecutor();
            /* we will put reports to conversationResult when we get all expected messages */
            this.conversationResult = new ArrayBlockingQueue<>(1);
        }

        public void close(ActualEvent actual) {
            boolean sendClose = !(actual instanceof ActualFailure || actual instanceof ActualClose);
            log.fine("Closing the websocket");
            closed = true;
            if (sendClose) {
                sendExecutor.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            log.fine("About to send automatic close");
                            webSocket.close(1000, "OK");
                            log.fine("Close sent");
                        } catch (IOException e) {
                            log.warning("Could not close WebSocket");
                        }
                    }
                });
            }
            shutDownExecutor();
            /* we are done, we can let the validation thread in */
            try {
                conversationResult.put(Collections.unmodifiableList(reports));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * @return a {@code sessionId} extracted from a welcome message.
         */
        public String getSessionId() {
            if (sessionId == null) {
                throw new IllegalStateException(
                        "sessionId was not initialized yet. A welcome message has probably not arrived yet.");
            }
            return sessionId;
        }

        /**
         * @param actual
         */
        private void handle(ActualEvent actual) {
            log.fine("Received message[" + actual.getIndex() + "] from WebSocket: [" + actual + "]");
            if (closed) {
                throw new IllegalStateException("Received message[" + actual + "] message when only ["
                        + expectedEvents.size() + "] messages were expected. The received message was ["
                        + actual.toString() + "]");
            }
            ExpectedEvent expected = expectedEvents.get(actual.getIndex());
            MessageReport report = expected.match(actual);
            log.fine(report.toString());
            reports.add(report);

            if (report.passed()) {
                expected.scheduleAnswer(actual, sendExecutor, webSocket);
            } else {
                close(actual);
            }
            if (inMessageCounter == expectedEvents.size()) {
                /* this was the last expected message */
                log.fine("Message[" + actual.getIndex() + "] was the last expected message, sending close.");
                close(actual);
            }
        }

        public void onClose(int code, String reason) {
            handle(new ActualClose(this, inMessageCounter++, code, reason));
        }

        public void onFailure(IOException e, Response response) {
            handle(new ActualFailure(this, inMessageCounter++, e, response));
        }

        /** @see okhttp3.ws.WebSocketListener#onMessage(okhttp3.ResponseBody) */
        @Override
        public void onMessage(ResponseBody body) throws IOException {
            handle(new ActualMessage(this, inMessageCounter++, body));
        }

        public void onOpen(WebSocket webSocket, Response response) {
            log.fine("WebSocket opened");
            this.webSocket = webSocket;
        }

        public void onPong(Buffer payload) {
            log.fine("Got pong ["+ payload.readUtf8() +"]");
        }


        /**
         *
         */
        private void shutDownExecutor() {
            log.fine("Shutting down the executor");
            try {
                sendExecutor.shutdown();
                sendExecutor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            log.fine("Executor shut down");
        }

        /**
         * @param timeout in milliseconds
         * @throws Throwable rethrown from the underlying {@link MessageReport}s
         */
        public void validate(long timeout) throws Throwable {

            List<MessageReport> finalReports = conversationResult.poll(timeout, TimeUnit.MILLISECONDS);

            List<Throwable> errors = new ArrayList<>();

            if (finalReports != null) {
                errors = finalReports.stream().filter(r -> !r.passed()).map(MessageReport::getThrowable).collect(Collectors.toList());
            } else {
                log.fine("Timeout: Shutting down the executor");
                shutDownExecutor();
                this.closed = true;
                List<MessageReport> reps = new ArrayList<>(this.reports);
                errors.add(new Throwable("Could not get conversation results after " + timeout + "ms. Collected ["
                        + reps.size() + "] reports: [" + reps + "], expected ["+ expectedEvents.size() +"] events"));
            }

            switch (errors.size()) {
                case 0:
                    /* passed */
                    return;
                case 1:
                    throw errors.get(0);
                default:
                    /* Several errors - report the number and the first one */
                    Throwable e = errors.get(0);
                    throw new AssertionError("[" + errors.size() + "] assertion errors, the first one being ["
                            + e.getMessage() + "]", e);
            }

        }

    }

    /**
     * A extension of {@link Matcher} that allows us to use Mustache placeholders in expected patterns.
     *
     * @param <T> the type of the object to match
     */
    protected interface WebSocketArgumentMatcher<T> extends org.hamcrest.Matcher<T> {
        default boolean matches(T actual, TestListener testListener) {
            return matches(actual);
        }
    }

    /**
     * Matches a WelcomeResponse, extracts a {@code sessionId} out of it and sets {@link TestListener#sessionId}.
     */
    public static class WelcomeMatcher extends PatternMatcher {

        private String sessionId;

        public WelcomeMatcher() {
            super("\\QWelcomeResponse={\"sessionId\":\"\\E.*");
        }

        public String getSessionId() {
            return sessionId;
        }

        @Override
        public boolean matches(ReusableBuffer actual, TestListener testListener) {
            if (super.matches(actual, testListener)) {
                BasicMessageWithExtraData<WelcomeResponse> envelope =
                        new ApiDeserializer().deserialize(actual.getTextPart());
                String sessionId = envelope.getBasicMessage().getSessionId();
                testListener.sessionId = sessionId;
                return true;
            }
            return false;
        }

    }

    /**
     * Asserts that the given {@link InputStream} is a {@link ZipInputStream} with at leat one {@link ZipEntry}.
     */
    public static class ZipWithOneEntryMatcher extends TypeSafeMatcher<InputStream> {

        /**
         * @see org.hamcrest.SelfDescribing#describeTo(org.hamcrest.Description)
         */
        @Override
        public void describeTo(Description description) {
            description.appendText("expected ZIP stream");
        }

        /**
         * @see org.hamcrest.TypeSafeMatcher#matchesSafely(java.lang.Object)
         */
        @Override
        protected boolean matchesSafely(InputStream in) {

            try (ZipInputStream zipInputStream = new ZipInputStream(in)) {
                ZipEntry entry = zipInputStream.getNextEntry();
                // it should be enough to assert that it's a valid ZIP file
                // when the ZIP is not valid, the entry will be null
                Assert.assertNotNull(entry);
                Assert.assertNotNull(entry.getName());
                // if we have a valid ZIP, we should be able to get at least one entry of it
                return true;
            } catch (IOException e) {
                throw new RuntimeException();
            }
        }

    }

    private static final Logger log = Logger.getLogger(TestWebSocketClient.class.getName());

    /**
     * @return a new {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    protected final OkHttpClient client;

    private final TestListener listener;

    private TestWebSocketClient(Request request, TestListener testListener, int connectTimeoutSeconds,
            int readTimeoutSeconds) {
        super();
        if (request == null) {
            throw new IllegalStateException(
                    "Cannot build a [" + TestWebSocketClient.class.getName() + "] with a null request");
        }
        this.listener = testListener;
        OkHttpClient c = new OkHttpClient.Builder()
                .connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
                .build();
        this.client = c;

        WebSocketCall.create(client, request).enqueue(testListener);
    }

    /**
     * @see java.io.Closeable#close()
     */
    @Override
    public void close() throws IOException {
        ExecutorService executor = client.dispatcher().executorService();
        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        }
    }

    /**
     * @param timeout in milliseconds
     * @throws Throwable rethrown from the underlying {@link MessageReport}s
     */
    public void validate(long timeout) throws Throwable {
        listener.validate(timeout);
    }

}
