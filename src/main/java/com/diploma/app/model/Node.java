package com.diploma.app.model;

import com.diploma.app.genericadmin.DisplayParameters;
import com.diploma.app.genericadmin.EntityName;
import com.diploma.app.genericadmin.IEntity;
import lombok.*;

import javax.persistence.*;
import javax.persistence.Table;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Entity
@Table(
        name = "node",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"name", "node_type"})}
)
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@EntityName("Узлы")
public class Node implements IEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    @NotNull
    @NotBlank
    @DisplayParameters(name = "Узел", order = 1)
    private String name;

    @Column(name = "demand")
    @Min(0)
    @DisplayParameters(name = "Спрос", order = 2)
    private BigDecimal demand;

    @Column(name = "node_type")
    @NotNull
    @DisplayParameters(name = "Тип узла", order = 3)
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

    @Override
    public String toString() {
        return name + " (" + nodeType.toString() + ")";
    }
}

