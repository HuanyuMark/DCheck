//package tech.amikos.chromadb;
//
//import com.google.gson.annotations.SerializedName;
//import io.swagger.v3.oas.annotations.media.Schema;
//import lombok.Data;
//import tech.amikos.chromadb.model.QueryEmbedding;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
///**
// * QueryEmbedding
// */
//@Data
//public class ChromaQueryEmbedding {
//    @SerializedName("where")
//    private Map<String, Object> where = null;
//
//    @SerializedName("where_document")
//    private Map<String, Object> whereDocument = null;
//
//    @SerializedName("query_embeddings")
//    private List<Object> queryEmbeddings = new ArrayList<Object>();
//
//    @SerializedName("n_results")
//    private Integer nResults = 10;
//
//    private List<QueryEmbedding.IncludeEnum> include = null;
//
//    public ChromaQueryEmbedding where(Map<String, Object> where) {
//        this.where = where;
//        return this;
//    }
//
//    public ChromaQueryEmbedding putWhereItem(String key, Object whereItem) {
//        if (this.where == null) {
//            this.where = new HashMap<String, Object>();
//        }
//        this.where.put(key, whereItem);
//        return this;
//    }
//
//    public ChromaQueryEmbedding whereDocument(Map<String, Object> whereDocument) {
//        this.whereDocument = whereDocument;
//        return this;
//    }
//
//    public ChromaQueryEmbedding putWhereDocumentItem(String key, Object whereDocumentItem) {
//        if (this.whereDocument == null) {
//            this.whereDocument = new HashMap<String, Object>();
//        }
//        this.whereDocument.put(key, whereDocumentItem);
//        return this;
//    }
//
//    public ChromaQueryEmbedding queryEmbeddings(List<Object> queryEmbeddings) {
//        this.queryEmbeddings = queryEmbeddings;
//        return this;
//    }
//
//    public ChromaQueryEmbedding addQueryEmbeddingsItem(Object queryEmbeddingsItem) {
//        this.queryEmbeddings.add(queryEmbeddingsItem);
//        return this;
//    }
//
//    public ChromaQueryEmbedding nResults(Integer nResults) {
//        this.nResults = nResults;
//        return this;
//    }
//
//    /**
//     * Get nResults
//     *
//     * @return nResults
//     **/
//    @Schema(description = "")
//    public Integer getNResults() {
//        return nResults;
//    }
//
//    public void setNResults(Integer nResults) {
//        this.nResults = nResults;
//    }
//
//    public ChromaQueryEmbedding include(List<QueryEmbedding.IncludeEnum> include) {
//        this.include = include;
//        return this;
//    }
//
//    public ChromaQueryEmbedding addIncludeItem(QueryEmbedding.IncludeEnum includeItem) {
//        if (this.include == null) {
//            this.include = new ArrayList<>();
//        }
//        this.include.add(includeItem);
//        return this;
//    }
//}
