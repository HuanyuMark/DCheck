package tech.amikos.chromadb;

import com.google.gson.*;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Delegate;
import tech.amikos.chromadb.handler.ApiException;
import tech.amikos.chromadb.model.GetEmbedding;
import tech.amikos.chromadb.model.QueryEmbedding;

import java.lang.reflect.Type;

/**
 * Date 2025/03/03
 *
 * @author 三石而立Sunsy
 */
@RequiredArgsConstructor
public class ChromaCollection {
    @Getter
    @Delegate
    protected final Collection target;

    protected final static Gson gson = Collection.gson.newBuilder()
            .registerTypeAdapter(GetEmbeddingInclude.class, (JsonSerializer<GetEmbeddingInclude>)(GetEmbeddingInclude src, Type typeOfSrc, JsonSerializationContext context)-> context.serialize(src.name().toLowerCase()))
            .create();

    public GetResult get(GetEmbedding req) throws ApiException {
        String json = gson.toJson(target.api.get(req, this.target.collectionId));
        return gson.fromJson(json, GetResult.class);
    }

    public Collection.QueryResponse query(QueryEmbedding query) throws ChromaException {
        try {
            String json = gson.toJson(target.api.getNearestNeighbors(query, target.collectionId));
            return gson.fromJson(json, Collection.QueryResponse.class);
        } catch (ApiException e) {
            throw new ChromaException(e);
        }
    }
}
