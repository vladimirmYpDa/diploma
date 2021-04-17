package com.diploma.app.controller;

import com.diploma.app.model.ResultDto;
import com.diploma.app.service.TransportationServiceImpl;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

@Controller
public class MainController {

    @Autowired
    private TransportationServiceImpl transportationService;

    @RequestMapping(value = "upload", method = RequestMethod.POST)
    public ResponseEntity<Void> upload(
            @RequestParam("file") MultipartFile file)
    {
        HttpStatus status = HttpStatus.OK;

        try {
            transportationService.processUpload(file.getInputStream());
        } catch (IOException | InvalidFormatException ex) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        return new ResponseEntity<>(status);
    }

    @RequestMapping(value = "getResult", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<ResultDto> getResult(
            @RequestParam("regionalWhAmount") Optional<Integer> regionalWhAmount)
            throws IOException, InvalidFormatException {
        return new ResponseEntity<>(transportationService.calculateResult(regionalWhAmount.orElse(0)), HttpStatus.OK);
    }
}
