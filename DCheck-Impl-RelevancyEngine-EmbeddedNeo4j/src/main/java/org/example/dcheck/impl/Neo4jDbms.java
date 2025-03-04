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
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Date: 2025/2/28
 *
 * @author 三石而立Sunsy
 */
@Slf4j
public class Neo4jDbms {
    private final static boolean SUPPORT_JDK21_VECTOR_API;

    static {
        boolean res;
        try {
            Class.forName("jdk.incubator.vector.Vector");
            res = true;
        } catch (ClassNotFoundException e) {
            res = false;
        }
        SUPPORT_JDK21_VECTOR_API = res;
        if (SUPPORT_JDK21_VECTOR_API) {
            log.info("JDK21 Vector API is supported");
        }
    }

    // extract plugin jar in plugin_dir
    static {
        var resolver = new PathMatchingResourcePatternResolver();
        try {
            Files.createDirectories(GraphDatabaseSettings.plugin_dir.defaultValue());
            Resource[] resources = resolver.getResources("classpath*:neo4j/plugins/*");
            for (Resource resource : resources) {
                String filename = Objects.requireNonNull(resource.getFilename());
                Path target = GraphDatabaseSettings.plugin_dir.defaultValue().resolve(filename);
                if (Files.exists(target)) continue;
                try (InputStream in = resource.getInputStream(); OutputStream out = Files.newOutputStream(target)) {
                    in.transferTo(out);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
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
        // enable apoc.map.* procedures
        var procedures = Collections.singletonList("apoc.map.*");
        builder.setConfig(GraphDatabaseSettings.procedure_allowlist, procedures);
        builder.setConfig(GraphDatabaseSettings.procedure_unrestricted, procedures);
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