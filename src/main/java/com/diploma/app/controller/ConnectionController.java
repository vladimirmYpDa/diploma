package com.diploma.app.controller;

import com.diploma.app.genericadmin.EntityController;
import com.diploma.app.genericadmin.IFieldConstraint;
import com.diploma.app.genericadmin.ListConstraint;
import com.diploma.app.model.Connection;
import com.diploma.app.model.Node;
import com.diploma.app.repository.NodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Field;

@Controller
@RequestMapping("/admin/connections/")
public class ConnectionController extends EntityController<Connection> {

    @Autowired
    private NodeRepository nodeRepository;

    @Override
    public IFieldConstraint getConstraint(Field field) {
        if (field.getType() == Node.class)
            return new ListConstraint<>(nodeRepository.findAll());

        return super.getConstraint(field);
    }

    @Override
    public String getTemplateInputType(Field field) {
        if (field.getType() == Node.class)
            return "select";

        return super.getTemplateInputType(field);
    }
}
