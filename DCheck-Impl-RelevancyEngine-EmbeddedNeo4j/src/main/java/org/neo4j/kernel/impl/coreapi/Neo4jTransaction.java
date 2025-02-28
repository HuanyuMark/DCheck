package org.neo4j.kernel.impl.coreapi;

import lombok.RequiredArgsConstructor;
import org.example.dcheck.impl.ManageableGraphDatabaseService;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.schema.Schema;
import org.neo4j.graphdb.traversal.BidirectionalTraversalDescription;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.internal.kernel.api.*;
import org.neo4j.internal.kernel.api.connectioninfo.ClientConnectionInfo;
import org.neo4j.internal.kernel.api.connectioninfo.RoutingInfo;
import org.neo4j.internal.kernel.api.security.SecurityContext;
import org.neo4j.internal.schema.IndexDescriptor;
import org.neo4j.internal.schema.SchemaDescriptors;
import org.neo4j.io.pagecache.context.CursorContext;
import org.neo4j.kernel.api.KernelTransaction;
import org.neo4j.kernel.api.ResourceMonitor;
import org.neo4j.kernel.api.exceptions.Status;
import org.neo4j.kernel.impl.core.NodeEntity;
import org.neo4j.kernel.impl.core.RelationshipEntity;
import org.neo4j.memory.MemoryTracker;
import org.neo4j.values.ElementIdMapper;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.neo4j.internal.helpers.collection.Iterators.emptyResourceIterator;

/**
 * Date: 2025/3/1
 *
 * @author 三石而立Sunsy
 */
@RequiredArgsConstructor
public class Neo4jTransaction extends DataLookup implements InternalTransaction {

    private final TransactionImpl target;

    public static Neo4jTransaction beginTx(ManageableGraphDatabaseService collection) {
        return new Neo4jTransaction((TransactionImpl) collection.beginTx());
    }

    public ResourceIterator<Node> findNearestNeighbors(Label label, String propertyKey, float[] value, int topK) {
        checkLabel(label);
        checkPropertyKey(propertyKey);
        TokenRead tokenRead = tokenRead();
        int labelId = tokenRead.nodeLabel(label.name());
        int propertyId = tokenRead.propertyKey(propertyKey);
        if (invalidTokens(labelId, propertyId)) {
            return emptyResourceIterator();
        }
        var query = PropertyIndexQuery.nearestNeighbors(topK, value);
        IndexDescriptor index = findUsableMatchingIndex(SchemaDescriptors.forLabel(labelId, propertyId), query);
        return nodesByLabelAndProperty(labelId, query, index);
    }


    public void registerCloseableResource(AutoCloseable closeableResource) {
        target.registerCloseableResource(closeableResource);
    }

    public void unregisterCloseableResource(AutoCloseable closeableResource) {
        target.unregisterCloseableResource(closeableResource);
    }

    public void commit() {
        target.commit();
    }

    public void commit(KernelTransaction.KernelTransactionMonitor kernelTransactionMonitor) {
        target.commit(kernelTransactionMonitor);
    }

    public void rollback() {
        target.rollback();
    }

    public Node createNode() {
        return target.createNode();
    }

    public Node createNode(Label... labels) {
        return target.createNode(labels);
    }

    public Result execute(String query) throws QueryExecutionException {
        return target.execute(query);
    }

    public Result execute(String query, Map<String, Object> parameters) throws QueryExecutionException {
        return target.execute(query, parameters);
    }

    public BidirectionalTraversalDescription bidirectionalTraversalDescription() {
        return target.bidirectionalTraversalDescription();
    }

    public TraversalDescription traversalDescription() {
        return target.traversalDescription();
    }

    public ResourceIterable<Node> getAllNodes() {
        return target.getAllNodes();
    }

    public ResourceIterable<Relationship> getAllRelationships() {
        return target.getAllRelationships();
    }

    public void terminate() {
        target.terminate();
    }

    public void terminate(Status reason) {
        target.terminate(reason);
    }

    public UUID getDatabaseId() {
        return target.getDatabaseId();
    }

    public String getDatabaseName() {
        return target.getDatabaseName();
    }

    public void close() {
        target.close();
    }

    public void setTransaction(KernelTransaction transaction) {
        target.setTransaction(transaction);
    }

    public Lock acquireWriteLock(Entity entity) {
        return target.acquireWriteLock(entity);
    }

    public Lock acquireReadLock(Entity entity) {
        return target.acquireReadLock(entity);
    }

    public KernelTransaction kernelTransaction() {
        return target.kernelTransaction();
    }

    public KernelTransaction.Type transactionType() {
        return target.transactionType();
    }

    public SecurityContext securityContext() {
        return target.securityContext();
    }

    public ClientConnectionInfo clientInfo() {
        return target.clientInfo();
    }

    public RoutingInfo routingInfo() {
        return target.routingInfo();
    }

    public KernelTransaction.Revertable overrideWith(SecurityContext context) {
        return target.overrideWith(context);
    }

    public Optional<Status> terminationReason() {
        return target.terminationReason();
    }

    public void setMetaData(Map<String, Object> txMeta) {
        target.setMetaData(txMeta);
    }

    @Override
    public RelationshipEntity newRelationshipEntity(long id) {
        return target.newRelationshipEntity(id);
    }

    public Relationship newRelationshipEntity(String elementId) {
        return target.newRelationshipEntity(elementId);
    }

    @Override
    public RelationshipEntity newRelationshipEntity(long id, long startNodeId, int typeId, long endNodeId) {
        return target.newRelationshipEntity(id, startNodeId, typeId, endNodeId);
    }

    public Relationship newRelationshipEntity(RelationshipDataAccessor cursor) {
        return target.newRelationshipEntity(cursor);
    }

    @Override
    public NodeEntity newNodeEntity(long nodeId) {
        return target.newNodeEntity(nodeId);
    }

    @Override
    public CursorFactory cursors() {
        return target.cursors();
    }

    @Override
    public CursorContext cursorContext() {
        return target.cursorContext();
    }

    @Override
    public MemoryTracker memoryTracker() {
        return target.memoryTracker();
    }

    @Override
    public QueryContext queryContext() {
        return target.queryContext();
    }

    public RelationshipType getRelationshipTypeById(int type) {
        return target.getRelationshipTypeById(type);
    }

    public Schema schema() {
        return target.schema();
    }

    @Override
    public TokenRead tokenRead() {
        return target.tokenRead();
    }

    @Override
    public SchemaRead schemaRead() {
        return target.schemaRead();
    }

    @Override
    public Read dataRead() {
        return target.dataRead();
    }

    @Override
    public ResourceMonitor resourceMonitor() {
        return target.resourceMonitor();
    }

    public Entity validateSameDB(Entity entity) {
        return target.validateSameDB(entity);
    }

    public void checkInTransaction() {
        target.checkInTransaction();
    }

    public boolean isOpen() {
        return target.isOpen();
    }

    @Override
    public ElementIdMapper elementIdMapper() {
        return target.elementIdMapper();
    }

    @Override
    public void performCheckBeforeOperation() {
        target.performCheckBeforeOperation();
    }

    public static Entity validateSameDB(InternalTransaction tx, Entity entity) {
        return TransactionImpl.validateSameDB(tx, entity);
    }

    @Override
    @SuppressWarnings("all")
    public Node getNodeById(long id) {
        return target.getNodeById(id);
    }

    @Override
    public Node getNodeByElementId(String elementId) {
        return target.getNodeByElementId(elementId);
    }

    @Override
    public ResourceIterator<Node> findNodes(Label myLabel) {
        return target.findNodes(myLabel);
    }

    @Override
    public ResourceIterator<Node> findNodes(Label myLabel, String key, String value, StringSearchMode searchMode) {
        return target.findNodes(myLabel, key, value, searchMode);
    }

    @Override
    public Node findNode(Label myLabel, String key, Object value) {
        return target.findNode(myLabel, key, value);
    }

    @Override
    public ResourceIterator<Node> findNodes(Label myLabel, String key, Object value) {
        return target.findNodes(myLabel, key, value);
    }

    @Override
    public ResourceIterator<Node> findNodes(Label label, String key1, Object value1, String key2, Object value2) {
        return target.findNodes(label, key1, value1, key2, value2);
    }

    @Override
    public ResourceIterator<Node> findNodes(Label label, Map<String, Object> propertyValues) {
        return target.findNodes(label, propertyValues);
    }

    @Override
    public ResourceIterator<Node> findNodes(Label label, String key1, Object value1, String key2, Object value2, String key3, Object value3) {
        return target.findNodes(label, key1, value1, key2, value2, key3, value3);
    }

    @Override
    @SuppressWarnings("all")
    public Relationship getRelationshipById(long id) {
        return target.getRelationshipById(id);
    }

    @Override
    public Relationship getRelationshipByElementId(String elementId) {
        return target.getRelationshipByElementId(elementId);
    }

    @Override
    public ResourceIterator<Relationship> findRelationships(RelationshipType relationshipType, String key, String template, StringSearchMode searchMode) {
        return target.findRelationships(relationshipType, key, template, searchMode);
    }

    @Override
    public ResourceIterator<Relationship> findRelationships(RelationshipType relationshipType, Map<String, Object> propertyValues) {
        return target.findRelationships(relationshipType, propertyValues);
    }

    @Override
    public ResourceIterator<Relationship> findRelationships(RelationshipType relationshipType, String key1, Object value1, String key2, Object value2, String key3, Object value3) {
        return target.findRelationships(relationshipType, key1, value1, key2, value2, key3, value3);
    }

    @Override
    public ResourceIterator<Relationship> findRelationships(RelationshipType relationshipType, String key1, Object value1, String key2, Object value2) {
        return target.findRelationships(relationshipType, key1, value1, key2, value2);
    }

    @Override
    public Relationship findRelationship(RelationshipType relationshipType, String key, Object value) {
        return target.findRelationship(relationshipType, key, value);
    }

    @Override
    public ResourceIterator<Relationship> findRelationships(RelationshipType relationshipType, String key, Object value) {
        return target.findRelationships(relationshipType, key, value);
    }

    @Override
    public ResourceIterator<Relationship> findRelationships(RelationshipType relationshipType) {
        return target.findRelationships(relationshipType);
    }

    @Override
    public Iterable<Label> getAllLabelsInUse() {
        return target.getAllLabelsInUse();
    }

    @Override
    public Iterable<RelationshipType> getAllRelationshipTypesInUse() {
        return target.getAllRelationshipTypesInUse();
    }

    @Override
    public Iterable<Label> getAllLabels() {
        return target.getAllLabels();
    }

    @Override
    public Iterable<RelationshipType> getAllRelationshipTypes() {
        return target.getAllRelationshipTypes();
    }

    @Override
    public Iterable<String> getAllPropertyKeys() {
        return target.getAllPropertyKeys();
    }


}
