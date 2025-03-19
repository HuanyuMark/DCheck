package org.example.dcheck.impl.embedding.remote;

/**
 * Date: 2025/3/8
 *
 * @author 三石而立Sunsy
 */
public class ConfigPropertyKey {
    public static final String READ_TIME_OUT = "relevancy-engine.model.embedding.remote.timeout";

    public static final String REMOTE_TYPE = "relevancy-engine.model.embedding.remote.type";

    /**
     * set embedding base url
     */
    public static final String EMBEDDING_REMOTE_BASE_URL = "relevancy-engine.model.embedding.remote.base-url";

    /**
     * limit the max token count of each embedding request
     */
    public static final String EMBEDDING_REMOTE_MAX_TOKEN = "relevancy-engine.model.embedding.remote.max-token";

    /**
     * set embedding model name
     */
    public static final String EMBEDDING_REMOTE_MODEL_NAME = "relevancy-engine.model.embedding.remote.model-name";

    /**
     * set max parallelism for send embedding request per host
     */
    public static final String EMBEDDING_REMOTE_PARALLELISM = "relevancy-engine.model.embedding.remote.parallelism";


    public static final String TOKENIZER_REMOTE_BASE_URL = "relevancy-engine.model.tokenizer.remote.base-url";
    public static final String TOKENIZER_REMOTE_MODEL_NAME = "relevancy-engine.model.tokenizer.remote.model-name";
    public static final String TOKENIZER_REMOTE_ESTIMATE_CACHE_SIZE = "relevancy-engine.model.tokenizer.remote.estimate-cache-size";
    // unit: ms
    public static final String TOKENIZER_REMOTE_ESTIMATE_CACHE_EXPIRE_TIME = "relevancy-engine.model.tokenizer.remote.estimate-cache-expire";


    public static final String DIMENSION_CONFIG = "relevancy-engine.model.embedding.remote.dimension";
    public static final String API_KEY_CONFIG = "relevancy-engine.model.embedding.remote.api-key";


}
