package com.aimaster.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/portal")
public class PortalController {

    @GetMapping({"", "/"})
    public String home(Model model) {
        model.addAttribute("activeTab", "portal");
        return "portal/home";
    }
}
