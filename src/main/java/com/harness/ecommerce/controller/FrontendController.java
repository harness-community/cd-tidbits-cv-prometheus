package com.harness.ecommerce.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Forwards all non-API, non-static-file requests to index.html
 * so that React Router can handle client-side navigation.
 *
 * Pattern explanation:
 *   "/{path:[^.]*}"   — matches any single path segment with no dot (e.g. /products, /cart)
 *   "/{path:[^.]*}/**" — matches any deeper path with no dot in the first segment
 *
 * This deliberately does NOT match:
 *   /api/**         — handled by @RestControllers
 *   *.js / *.css    — served directly by Spring's static resource handler
 */
@Controller
public class FrontendController {

    @GetMapping(value = {"/", "/{path:[^.]*}", "/{path:[^.]*}/**"})
    public String forward() {
        return "forward:/index.html";
    }
}
