package com.aimaster.controller;

import com.aimaster.service.CourseRegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/course/namoro")
@RequiredArgsConstructor
public class CourseAdminController {

    private final CourseRegistrationService courseRegistrationService;

    @GetMapping
    public String namoroCourseAdmin(Model model) {
        model.addAttribute("activeTab", "admin-namoro");
        var stats = courseRegistrationService.getStats("namoro-com-proposito");
        model.addAttribute("statTotal",      stats.get("total"));
        model.addAttribute("statRegistered", stats.get("registered"));
        model.addAttribute("statConfirmed",  stats.get("confirmed"));
        model.addAttribute("statCancelled",  stats.get("cancelled"));
        return "admin-namoro";
    }
}
