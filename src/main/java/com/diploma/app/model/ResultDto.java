package com.diploma.app.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class ResultDto {
    private CitiesRoadsDto citiesRoadsDto;
    private BigDecimal sumConnection;
    private BigDecimal sumToRegionalConnection;
    private BigDecimal sumToNationalConnection;
    private List<RegionalToLocalDto> regionalToLocalDtos;
    private List<List<String>> roadsMatrix;
    private String downloadFilename;
}
