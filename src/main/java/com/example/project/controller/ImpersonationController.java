package com.example.project.controller;

import com.example.project.entity.Usuarios;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class ImpersonationController {

    private final UserDetailsService userDetailsService;

    public ImpersonationController(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @PostMapping("/impersonation-logout") // <- ¡¡¡Este es el correcto, SIN @{}!!!
    public String impersonationLogout(HttpSession session) {
        Integer originalId = (Integer) session.getAttribute("originalSuperAdminId");
        Usuarios originalObj = (Usuarios) session.getAttribute("originalSuperAdminObj");

        if (originalId != null && originalObj != null) {
            UserDetails superAdminDetails = userDetailsService.loadUserByUsername(String.valueOf(originalObj.getDni()));
            Authentication superAdminAuth = new UsernamePasswordAuthenticationToken(
                    superAdminDetails,
                    superAdminDetails.getPassword(),
                    superAdminDetails.getAuthorities()
            );
            SecurityContext context = SecurityContextHolder.getContext();
            context.setAuthentication(superAdminAuth);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
            session.setAttribute("usuario", originalObj);
            session.removeAttribute("originalSuperAdminId");
            session.removeAttribute("originalSuperAdminObj");
            return "redirect:/superadmin/home";
        }

        return "redirect:/logout";
    }
}
