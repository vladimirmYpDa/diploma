package com.diploma.app.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ResultDto {
    private List<Road> roads;
    private BigDecimal sumToRegionalConnection;
    private BigDecimal sumToNationalConnection;
    private BigDecimal sumConnection;
    private Map<String, BigDecimal> regionalSums;
    private String downloadFilename;
}
