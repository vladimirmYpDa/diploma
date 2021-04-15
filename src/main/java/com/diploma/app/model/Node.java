package com.diploma.app.model;

import lombok.*;

import javax.persistence.*;
import javax.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(
        name = "node",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"name", "node_type"})}
)
@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
public class Node {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "demand")
    private BigDecimal demand;

    @Column(name = "node_type")
    @Enumerated(EnumType.STRING)
    private NodeType nodeType;

    public Node(String name) {
        this.name = name;
    }

    public Node(String name, NodeType nodeType)
    {
        this.name = name;
        this.nodeType = nodeType;
    }
}

