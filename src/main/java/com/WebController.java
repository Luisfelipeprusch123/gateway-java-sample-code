package com;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

public interface WebController {

    /**
     * Display VERIFY operation page
     *
     * @return ModelAndView for verify.html
     */
    ModelAndView showVerify();

}