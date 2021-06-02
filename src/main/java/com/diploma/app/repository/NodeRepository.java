package com.diploma.app.repository;

import com.diploma.app.model.Node;
import com.diploma.app.model.NodeType;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface NodeRepository extends CrudRepository<Node, Long> {
    Optional<Node> getByNameAndNodeType(String name, NodeType nodeType);

    List<Node> findAllByNodeType(NodeType nodeType);

    List<Node> findAll();
}
