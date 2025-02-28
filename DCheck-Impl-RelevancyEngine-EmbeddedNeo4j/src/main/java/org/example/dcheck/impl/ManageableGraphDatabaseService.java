package org.example.dcheck.impl;

import org.neo4j.graphdb.GraphDatabaseService;

import java.io.IOException;

/**
 * Date: 2025/2/28
 *
 * @author 三石而立Sunsy
 */
public interface ManageableGraphDatabaseService extends GraphDatabaseService {
    void drop() throws IOException;
}
