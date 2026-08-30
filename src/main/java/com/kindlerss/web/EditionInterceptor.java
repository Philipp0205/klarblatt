package com.kindlerss.web;

import com.kindlerss.service.DisplayPreferencesService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Applies Klarblatt's accessible view layer and display preferences to every
 * request. Shared controller views such as login and registration are rewritten
 * to their accessible templates.
 */
public class EditionInterceptor implements HandlerInterceptor {

    /** Folder (and view-name prefix) the accessible edition's templates live under. */
    static final String PREFIX = "accessible/";

    /** Templates already resolved to “has a Klarblatt accessible template” or not. */
    private final Map<String, Boolean> accessibleViews = new ConcurrentHashMap<>();

    private final EditionResolver resolver;
    private final DisplayPreferencesService preferencesService;

    public EditionInterceptor(EditionResolver resolver, DisplayPreferencesService preferencesService) {
        this.resolver = resolver;
        this.preferencesService = preferencesService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        Edition edition = resolver.resolve(request);
        request.setAttribute(EditionResolver.ATTRIBUTE, edition);
        // Resolved before the handler runs, so that a page produced by an exception
        // handler — which the interceptor never gets to see — still has the reader's
        // colours and type size to render with.
        preferencesService.forRequest(request);
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String replacement = accessiblePathFor(request);
        if (replacement == null) {
            return true;
        }
        response.sendRedirect(request.getContextPath() + replacement);
        return false;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           ModelAndView modelAndView) {
        if (modelAndView == null) {
            return;
        }
        String viewName = modelAndView.getViewName();
        boolean renderable = viewName != null
                && !viewName.startsWith("redirect:") && !viewName.startsWith("forward:");
        if (isAccessible(request) && renderable && !viewName.startsWith(PREFIX)
                && hasAccessibleTemplate(viewName)) {
            viewName = PREFIX + viewName;
            modelAndView.setViewName(viewName);
        }
        // Attributes added to a redirect become query parameters, so a redirect gets
        // nothing: there is no page here to render, only a location header.
        if (!renderable) {
            return;
        }
        // A page without a rewritten view still needs Klarblatt's display settings.
        if (!isAccessible(request) && !viewName.startsWith(PREFIX)) {
            return;
        }
        modelAndView.addObject(EditionResolver.ATTRIBUTE, Edition.ACCESSIBLE);
        modelAndView.addObject("accessibleEdition", true);
        modelAndView.addObject("display", preferencesService.forRequest(request));
        // Controls in the page furniture (larger/smaller text) post and come back to
        // where they were used, so every page has to know its own address. Thymeleaf
        // 3.1 no longer exposes the request, so it is handed over here.
        modelAndView.addObject("currentPath", currentPath(request));
    }

    private static String currentPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isBlank() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        String query = request.getQueryString();
        return query == null || query.isBlank() ? path : path + "?" + query;
    }

    static boolean isAccessible(HttpServletRequest request) {
        return request.getAttribute(EditionResolver.ATTRIBUTE) == Edition.ACCESSIBLE;
    }

    private boolean hasAccessibleTemplate(String viewName) {
        return accessibleViews.computeIfAbsent(viewName,
                name -> new ClassPathResource("templates/" + PREFIX + name + ".html").exists());
    }

    /**
     * Redirects legacy Kindle-oriented reading URLs to Klarblatt's equivalents.
     * Old bookmarks and shared addresses still arrive somewhere readable.
     */
    private static String accessiblePathFor(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isBlank() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        if (path.isEmpty() || "/".equals(path)) {
            return "/topics";
        }
        if ("/items".equals(path)) {
            StringBuilder target = new StringBuilder("/list");
            appendParameter(target, "topic", request.getParameter("category"));
            appendParameter(target, "source", request.getParameter("feed"));
            return target.toString();
        }
        if (path.startsWith("/articles/")) {
            String rest = path.substring("/articles/".length());
            if (rest.matches("\\d+")) {
                return "/read/" + rest;
            }
        }
        return null;
    }

    private static void appendParameter(StringBuilder target, String name, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        target.append(target.indexOf("?") < 0 ? '?' : '&')
                .append(name).append('=').append(URLEncoder.encode(value, StandardCharsets.UTF_8));
    }
}
