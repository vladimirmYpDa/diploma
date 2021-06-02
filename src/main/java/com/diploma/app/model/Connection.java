package com.diploma.app.model;

import com.diploma.app.genericadmin.DisplayParameters;
import com.diploma.app.genericadmin.EntityName;
import com.diploma.app.genericadmin.IEntity;
import lombok.*;

import javax.persistence.*;
import javax.persistence.Table;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

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
@EntityName("Соединения")
public class Connection implements IEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne()
    @JoinColumn(name = "source_node_id")
    @NotNull
    @DisplayParameters(name = "Отправитель", order = 1)
    private Node sourceNode;

    @ManyToOne()
    @JoinColumn(name = "destination_node_id")
    @NotNull
    @DisplayParameters(name = "Получатель", order = 2)
    private Node destinationNode;

    @Column(name = "distance")
    @NotNull
    @Min(0)
    @DisplayParameters(name = "Расстояние", order = 3)
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
