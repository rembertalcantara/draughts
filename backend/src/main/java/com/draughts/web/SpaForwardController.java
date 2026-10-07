package com.draughts.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Serves the bundled React app for client-side routes (paths without a file extension). */
@Controller
class SpaForwardController {

    @GetMapping({"/", "/games/{id}", "/history"})
    String forward() {
        return "forward:/index.html";
    }
}
