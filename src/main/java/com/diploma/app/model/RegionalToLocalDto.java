package com.diploma.app.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@AllArgsConstructor
@Getter
@Setter
@ToString
public class RegionalToLocalDto {
    private String regional;
    private List<String> locals;
}
