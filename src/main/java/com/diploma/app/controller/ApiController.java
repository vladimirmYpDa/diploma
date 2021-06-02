package com.diploma.app.controller;

import com.diploma.app.model.NodeType;
import com.diploma.app.model.ResultDto;
import com.diploma.app.repository.NodeRepository;
import com.diploma.app.service.TransportationServiceImpl;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Optional;

@Controller
public class ApiController {

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private TransportationServiceImpl transportationService;

    @GetMapping("getResult")
    @ResponseBody
    public ResponseEntity<ResultDto> getResult(
            @RequestParam(value = "regionalWhAmount", required = false) Optional<Integer> regionalWhAmount,
            @RequestParam(value = "transportPrice", required = false) Optional<BigDecimal> transportPrice)
            throws IOException, InvalidFormatException
    {
        return new ResponseEntity<>(
                transportationService.calculateResult(
                    regionalWhAmount.orElse(0),
                    transportPrice.orElse(BigDecimal.valueOf(0.678))),
                HttpStatus.OK);
    }

    @GetMapping
    public String getIndex(ModelMap model) {
        int maxWhAmount = nodeRepository.findAllByNodeType(NodeType.REGIONAL).size();
        model.put("maxWhAmount", maxWhAmount);
        return "index";
    }
}
