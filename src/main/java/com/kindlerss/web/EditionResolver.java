package com.kindlerss.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/** Resolves every request to Klarblatt's single, accessible edition. */
@Component
public class EditionResolver {

    /** Request attribute (and model attribute) carrying the resolved edition. */
    public static final String ATTRIBUTE = "edition";

    public Edition resolve(HttpServletRequest request) {
        return Edition.ACCESSIBLE;
    }
}
