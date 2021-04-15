package com.diploma.app.model;

import lombok.*;

import javax.persistence.*;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(
        name = "connection",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"source_node_id", "destination_node_id"})}
)
@Getter
@Setter
@NoArgsConstructor
@ToString
public class Connection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "source_node_id")
    private Node sourceNode;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "destination_node_id")
    private Node destinationNode;

    @Column(name = "distance")
    private BigDecimal distance;

    public Connection(Node sourceNode, Node destinationNode, BigDecimal distance) {
        this.sourceNode = sourceNode;
        this.destinationNode = destinationNode;
        this.distance = distance;
    }

    public Connection(Node sourceNode, Node destinationNode) {
        this.sourceNode = sourceNode;
        this.destinationNode = destinationNode;
    }
}
