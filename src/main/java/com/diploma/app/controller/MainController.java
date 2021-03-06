package com.diploma.app.controller;

import com.diploma.app.model.CitiesRoadsDto;
import com.diploma.app.model.ResultDto;
import com.diploma.app.service.TransportationServiceImpl;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Controller
public class MainController {

    @Autowired
    private TransportationServiceImpl transportationService;

    @RequestMapping("upload")
    @ResponseBody
    public ResponseEntity<ResultDto> upload(@RequestParam("file") MultipartFile file,
` `                                            @RequestParam("regionalWhAmount") Integer regionalWhAmount) throws IOException, InvalidFormatException {
        ResultDto resultDto = transportationService.process(file.getInputStream(), regionalWhAmount);
        return new ResponseEntity<>(resultDto, HttpStatus.OK);
    }
}
