package org.example.dcheck.impl;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.configuration.BootloaderSettings;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.io.fs.FileUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
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


    private final Path rootPath;

    public Neo4jDbms(Path rootPath) {
        this.rootPath = rootPath;
    }

    private final Map<String, DatabaseManagementService> databasesServices = new ConcurrentSkipListMap<>();

    private final Map<String, ManageableGraphDatabaseService> databases = new ConcurrentSkipListMap<>();

    public ManageableGraphDatabaseService getOrCreateDatabase(String dbName) {
        return databases.computeIfAbsent(dbName, dbName1 -> new GraphDatabaseServiceProxy(databasesServices.computeIfAbsent(dbName1, name -> config(new DatabaseManagementServiceBuilder(rootPath.resolve(name))
        ).build()).database("neo4j"), dbName1));
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

    @RequiredArgsConstructor
    protected class GraphDatabaseServiceProxy implements ManageableGraphDatabaseService {
        @Delegate
        private final GraphDatabaseService target;
        private final String dbName;

        public String databaseName() {
            return dbName;
        }

        @Override
        public void drop() throws IOException {
            dropDatabase(dbName);
        }
    }

    public void shutdown() {
        var s = System.currentTimeMillis();
        databasesServices.values().forEach(DatabaseManagementService::shutdown);
        log.info("Neo4j Database Closed. cost {}ms", System.currentTimeMillis() - s);
    }
}