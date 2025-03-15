package org.example.dcheck.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.configuration.BootloaderSettings;
import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.dbms.api.DatabaseNotFoundException;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.QueryExecutionException;
import org.neo4j.graphdb.ResultTransformer;
import org.neo4j.graphdb.Transaction;
import org.neo4j.io.fs.FileUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Date: 2025/2/28
 *
 * @author 三石而立Sunsy
 */
@Slf4j
public class Neo4jDbms {
    public final static boolean SUPPORT_JDK21_VECTOR_API;

    static {
        SUPPORT_JDK21_VECTOR_API = determineSupportedJDK21VectorAPI();
        if (SUPPORT_JDK21_VECTOR_API) {
            log.info("JDK21 Vector API is supported");
        }
    }

    private static boolean determineSupportedJDK21VectorAPI() {
        try {
            Class.forName("jdk.incubator.vector.Vector");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }


    private final Path rootPath;


    public Neo4jDbms(Path rootPath) {
        this.rootPath = rootPath;
    }

    private final Map<String, DatabaseManagementService> databasesServices = new ConcurrentSkipListMap<>();

    private final Map<String, ManageableGraphDatabaseService> databases = new ConcurrentSkipListMap<>();

    public ManageableGraphDatabaseService getOrCreateDatabase(String dbName) {
        return databases.computeIfAbsent(dbName, dbName1 -> {
            try {
                return new GraphDatabaseServiceProxy(databasesServices.computeIfAbsent(dbName1, name -> config(new DatabaseManagementServiceBuilder(rootPath.resolve(name))
                ).build()).database(GraphDatabaseSettings.DEFAULT_DATABASE_NAME), dbName1);
            } catch (DatabaseNotFoundException e) {
                throw new IllegalStateException("load neo4j default db fail:" + e.getMessage(), e);
            }
        });
    }

    protected DatabaseManagementServiceBuilder config(DatabaseManagementServiceBuilder builder) {
        if (SUPPORT_JDK21_VECTOR_API) {
            //@see https://neo4j.com/docs/cypher-manual/current/indexes/semantic-indexes/vector-indexes/#performance
            builder.setConfig(BootloaderSettings.additional_jvm, "--add-modules=jdk.incubator.vector");
        }
        return builder;
    }

    public void dropDatabase(String dbName) throws IOException {
        var service = databasesServices.get(dbName);
        if (service == null) return;
        service.dropDatabase("neo4j");
        service.shutdown();
        FileUtils.deleteDirectory(rootPath.resolve(dbName));
    }

    public void shutdown() {
        var s = System.currentTimeMillis();
        databasesServices.values().forEach(DatabaseManagementService::shutdown);
        log.info("Neo4j Database Closed. cost {}ms", System.currentTimeMillis() - s);
    }

    public void destroy() {
        shutdown();
        try {
            FileUtils.deleteDirectory(rootPath);
        } catch (IOException e) {
            log.warn("delete neo4j root path fail: {}", e.getMessage(), e);
        }
    }

    @RequiredArgsConstructor
    protected class GraphDatabaseServiceProxy implements ManageableGraphDatabaseService {
        private final GraphDatabaseService target;
        private final String dbName;

        public String databaseName() {
            return dbName;
        }

        @Override
        public void drop() throws IOException {
            dropDatabase(dbName);
        }

        @Override
        public boolean isAvailable() {
            return target.isAvailable();
        }

        @Override
        public boolean isAvailable(long timeoutMillis) {
            return target.isAvailable(timeoutMillis);
        }

        @Override
        public Transaction beginTx() {
            return target.beginTx();
        }

        @Override
        public Transaction beginTx(long timeout, java.util.concurrent.TimeUnit unit) {
            return target.beginTx(timeout, unit);
        }

        @Override
        public void executeTransactionally(String query) throws QueryExecutionException {
            target.executeTransactionally(query);
        }

        @Override
        public void executeTransactionally(String query, Map<String, Object> parameters) throws QueryExecutionException {
            target.executeTransactionally(query, parameters);
        }

        @Override
        public <T> T executeTransactionally(String query, Map<String, Object> parameters, ResultTransformer<T> resultTransformer) throws QueryExecutionException {
            return target.executeTransactionally(query, parameters, resultTransformer);
        }

        @Override
        public <T> T executeTransactionally(String query, Map<String, Object> parameters, ResultTransformer<T> resultTransformer, Duration timeout) throws QueryExecutionException {
            return target.executeTransactionally(query, parameters, resultTransformer, timeout);
        }
    }
}