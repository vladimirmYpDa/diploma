package com.diploma.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.diploma.app.model.Node;
import com.diploma.app.model.NodeType;
import com.diploma.app.repository.NodeRepository;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@TestMethodOrder(OrderAnnotation.class)
public class AppApplicationTests {

    @Autowired
    private NodeRepository nodeRepository;

    private final String TEST_NAME = "TestNodeName";
    private final String TEST_NAME_EDIT = "TestNodeNameEdit";
    private final NodeType TEST_TYPE = NodeType.LOCAL;

    @Test
    @Order(1)
    public void assertNodeCreate() {
        Node node = new Node(TEST_NAME, TEST_TYPE);
        nodeRepository.save(node);

        Long id = node.getId();
        assertEquals(node, nodeRepository.findById(id).get());
    }

    @Test
    @Order(2)
    public void assertNodeEdit() {
        Node node = nodeRepository.getByNameAndNodeType(TEST_NAME, TEST_TYPE).get();
        node.setName(TEST_NAME_EDIT);
        nodeRepository.save(node);

        Long id = node.getId();
        assertEquals(node, nodeRepository.findById(id).get());
    }

    @Test
    @Order(3)
    public void assertNodeDelete() {
        Node node = nodeRepository.getByNameAndNodeType(TEST_NAME_EDIT, TEST_TYPE).get();
        Long id = node.getId();
        nodeRepository.delete(node);
        
        assertTrue(!nodeRepository.findById(id).isPresent());;
    }
}
