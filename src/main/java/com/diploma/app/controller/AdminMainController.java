package com.diploma.app.controller;

import com.diploma.app.service.TransportationServiceImpl;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Controller
@RequestMapping("/admin/")
public class AdminMainController {

    @Autowired
    private TransportationServiceImpl transportationService;

    @PostMapping("upload")
    public ResponseEntity<Void> upload(
            @RequestParam("file") MultipartFile file)
    {
        System.out.println("Received file");
        HttpStatus status = HttpStatus.OK;

        try {
            transportationService.processUpload(file.getInputStream());
        } catch (IOException | InvalidFormatException ex) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        return new ResponseEntity<>(status);
    }

    @GetMapping
    public String adminIndex() {
        return "admin/index";
    }
}
