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
package org.hawkular.commons.cassandra.driver.itest;

import java.io.IOException;
import java.util.List;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ProtocolVersion;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

/**
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 *
 */
public class CassandraDriverITest extends Arquillian {
    private static final Logger log = Logger.getLogger(CassandraDriverITest.class);
    public static final String GROUP = "cassandra";

    @Deployment
    public static WebArchive createDeployment() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, CassandraDriverITest.class.getSimpleName() + ".war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsWebInfResource(
                        CassandraDriverITest.class.getResource("/cassandra-driver/jboss-deployment-structure.xml"),
                        "jboss-deployment-structure.xml");
        // ZipExporter exporter = new ZipExporterImpl(archive);
        // exporter.exportTo(new File("target", CassandraDriverITest.class.getSimpleName() + ".war"));
        return archive;
    }

    private static final String[] cassandraNodes = new String[] { "127.0.0.1" };
    private static final int cassandraPort = 9042;
    private static int attempts = 15;
    private static final int interval = 4000;

    @Test(groups = { GROUP })
    public void testCassandraSession() throws IOException, InterruptedException {

        try (Session session = getSession()) {
            Assert.assertNotNull(session, "Injected Cassandra session is null");
            session.execute("CREATE KEYSPACE test_keyspace"
                    + " WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1}");
            session.execute("CREATE TABLE test_keyspace.test_table (id varchar PRIMARY KEY)");

            session.execute("INSERT INTO test_keyspace.test_table (id) VALUES ('deadbeef')");

            List<Row> rows = session.execute("SELECT id FROM test_keyspace.test_table").all();
            Assert.assertEquals(rows.size(), 1);
            Assert.assertEquals(rows.get(0).getString(0), "deadbeef");

            session.execute("DROP TABLE test_keyspace.test_table");
            session.execute("DROP KEYSPACE test_keyspace");
        }

    }

    private Session getSession() {
        try {
            return new Cluster.Builder()
                    .addContactPoints(cassandraNodes)
                    .withPort(cassandraPort)
                    .withProtocolVersion(ProtocolVersion.V3)
                    .withoutJMXReporting()
                    .build().connect();
        } catch (Exception e) {
            if (attempts != 0) {
                log.infof(e, "Cassandra is not available (yet?). Attempts left: [%d]. Reason", attempts);
                attempts--;
                try {
                    Thread.sleep(interval);
                } catch (InterruptedException e1) {
                    throw new RuntimeException(e1);
                }
                return getSession();
            } else {
                log.infof(e, "Could not connect to Cassandra after enough attempts. Giving up. Reason");
                return null;
            }
        }
    }

}
