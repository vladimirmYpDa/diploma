package com.diploma.app.repository;

import com.diploma.app.model.Connection;
import com.diploma.app.model.Node;
import com.diploma.app.model.NodeType;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface ConnectionRepository extends CrudRepository<Connection, Long> {
    Optional<Connection> getBySourceNodeAndDestinationNode(Node sourceNode, Node destinationNode);

    Optional<Connection> getBySourceNode_NameAndDestinationNode_Name(String sourceNode, String destinationNode);

    List<Connection> findAllBySourceNode_NodeTypeAndDestinationNode_NodeType(NodeType sourceType, NodeType destinationType);
}
