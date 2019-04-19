package com.diploma.app.repository;

import com.diploma.app.model.Node;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface NodeRepository extends CrudRepository<Node, Long> {
    Node getByName(String name);

    List<Node> findAll();
}
