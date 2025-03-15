package org.example.dcheck.impl;

/**
 * Date: 2025/3/15
 *
 * @author 三石而立Sunsy
 * @see <a href="https://neo4j.com/docs/cypher-manual/current/indexes/semantic-indexes/vector-indexes/">neo4j official config doc</a>
 */
public class EmbeddedNeo4jConfigKey {
    ///// here are builtin api config
    public static final String DB_ROOT = "relevancy-engine.embedded-neo4j.data-path";
    public static final String SIMILARITY_FUNCTION = "relevancy-engine.embedded-neo4j.config.similarity_function";
    public static final String QUANTIZATION_ENABLE = "relevancy-engine.embedded-neo4j.config.quantization.enable";
    public static final String HNSW_M = "relevancy-engine.embedded-neo4j.config.hnsw.m";
    public static final String HNSW_EF_CONSTRUCTION = "relevancy-engine.embedded-neo4j.config.hnsw.ef_construction";
    /////
}
