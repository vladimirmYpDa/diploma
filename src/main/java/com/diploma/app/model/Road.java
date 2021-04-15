package com.diploma.app.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import javax.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "road")
@Getter
@Setter
@NoArgsConstructor
public class Road {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "total_distance")
    private BigDecimal totalDistance;

    @ManyToOne
    @JoinColumn(name = "local_to_regional_conn")
    private Connection localToRegionalConn;

    @ManyToOne
    @JoinColumn(name = "regional_to_national_conn")
    private Connection regionalToNationalConn;

    @ManyToOne
    @JoinColumn(name = "national_to_supplier_conn")
    private Connection nationalToSupplierConn;

    public Road(BigDecimal totalDistance, Connection localToRegionalConn,
                Connection regionalToNationalConn, Connection nationalToSupplierConn) {
        this.totalDistance = totalDistance;
        this.localToRegionalConn = localToRegionalConn;
        this.nationalToSupplierConn = nationalToSupplierConn;
        this.regionalToNationalConn = regionalToNationalConn;
    }
}
