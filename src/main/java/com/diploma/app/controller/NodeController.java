package com.diploma.app.controller;

import com.diploma.app.genericadmin.EntityController;
import com.diploma.app.model.Node;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/nodes/")
public class NodeController extends EntityController<Node> {
}
