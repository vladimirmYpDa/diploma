package com.diploma.app.model;

import lombok.*;

import javax.persistence.*;
import javax.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "node")
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

    public Node(String name) {
        this.name = name;
    }
}
