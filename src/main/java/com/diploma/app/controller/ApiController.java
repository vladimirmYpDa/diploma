package com.diploma.app.controller;

import com.diploma.app.model.ResultDto;
import com.diploma.app.service.TransportationServiceImpl;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.util.Optional;

@Controller
public class ApiController {

    @Autowired
    private TransportationServiceImpl transportationService;

    @GetMapping("getResult")
    @ResponseBody
    public ResponseEntity<ResultDto> getResult(
            @RequestParam("regionalWhAmount") Optional<Integer> regionalWhAmount)
            throws IOException, InvalidFormatException {
        return new ResponseEntity<>(transportationService.calculateResult(regionalWhAmount.orElse(0)), HttpStatus.OK);
    }

    @GetMapping
    public String getIndex() {
        return "index";
    }
}
