package com.kindlerss.web;

import com.kindlerss.service.ArticleService;
import com.kindlerss.service.DisplayPreferencesService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Maps common service exceptions to error pages. */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ArticleService.NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound(ArticleService.NotFoundException ex, HttpServletRequest request, Model model) {
        model.addAttribute("message", ex.getMessage());
        return accessibleErrorView(request, model);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String badRequest(IllegalArgumentException ex, HttpServletRequest request, Model model) {
        model.addAttribute("message", ex.getMessage());
        return accessibleErrorView(request, model);
    }

    /** Exception handlers run after interceptor view processing, so add shared page attributes here. */
    private static String accessibleErrorView(HttpServletRequest request, Model model) {
        model.addAttribute("accessibleEdition", true);
        model.addAttribute("display", DisplayPreferencesService.resolved(request));
        model.addAttribute("currentPath", "/topics");
        return "accessible/error";
    }
}
