package com.example.beanstalkdemo.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UiController {

    @Value("${app.version:unknown}")
    private String version;

    @GetMapping({"/ui", "/dashboard"})
    public String dashboard(Model model) {
        model.addAttribute("appVersion", version);
        model.addAttribute("apiBasePath", "");
        return "dashboard";
    }
}