
package com.example.project.config;

import jakarta.servlet.ServletException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.savedrequest.DefaultSavedRequest;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.project.entity.Usuarios;
import com.example.project.repository.UsuariosRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(WebSecurityConfig.class);
    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();
    @Autowired private UsuariosRepository usuariosRepository;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            Optional<Usuarios> usuarioOpt;
            if (username.matches("\\d+")) {
                // Solo números => DNI
                usuarioOpt = usuariosRepository.findByDni(username);
            } else {
                // Contiene letras o símbolos => Correo
                usuarioOpt = usuariosRepository.findByCorreoIgnoreCase(username);
            }

            return usuarioOpt.map(usuario -> User.withUsername(username)
                            .password(usuario.getContrasena())
                            .disabled(!(usuario.getEstado() != null && usuario.getEstado().getIdEstado() == 1))
                            .authorities(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().getRol()))
                            .build())
                    .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));
        };
    }

    @Bean
    public LogoutSuccessHandler customLogoutSuccessHandler() {
        return (request, response, authentication) -> {
            HttpSession session = request.getSession(false);
            if (session != null && session.getAttribute("originalSuperAdminId") != null) {
                Usuarios originalSuperAdminObj = (Usuarios) session.getAttribute("originalSuperAdminObj");
                UserDetails superAdminDetails = userDetailsService().loadUserByUsername(String.valueOf(originalSuperAdminObj.getDni()));

                Authentication superAdminAuthentication = new UsernamePasswordAuthenticationToken(
                        superAdminDetails,
                        superAdminDetails.getPassword(),
                        superAdminDetails.getAuthorities()
                );

                SecurityContextHolder.getContext().setAuthentication(superAdminAuthentication);
                session.setAttribute("usuario", originalSuperAdminObj);
                session.removeAttribute("originalSuperAdminId");
                session.removeAttribute("originalSuperAdminObj");
                redirectStrategy.sendRedirect(request, response, "/superadmin/home");
            } else {
                redirectStrategy.sendRedirect(request, response, "/login?logout");
            }
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .addFilterBefore(loginPageRedirectFilter(), UsernamePasswordAuthenticationFilter.class)
                .formLogin(formLogin -> formLogin
                        .permitAll()
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler(customAuthenticationSuccessHandler())
                )
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(
                                "/vecino/api/chatbot",
                                "/pago/mercadopago/preferencia"
                        )
                )
                .authorizeHttpRequests(authorizeRequests -> authorizeRequests
                                // Permitir acceso público a estas rutas (incluyendo login, registro, estáticos, e impersonate POST)
                        .requestMatchers("/", "/login", "/principal", "/olvido", "/olvido/**",
                                    "/confirmoContrasena","/renovarContrasena","/logout","/css/**","/js/**",
                                    "/img/**","/vendor/**","/superadmin/impersonate", "/public/**", "/pago/mercadopago/preferencia",
                                    "/enviar-consulta", "/registro", "/registro/**").permitAll()
                        .requestMatchers("/subir-archivo","/calificacion","/calificacion/**", "/espacio", "/espacio/**").hasAuthority("ROLE_Usuario final")
                        .requestMatchers("/superadmin/impersonate").hasAuthority("ROLE_SuperAdmin")
                        .requestMatchers("/superadmin/**").hasAuthority("ROLE_SuperAdmin")
                        .requestMatchers("/admin/**", "/admin/api/**").hasAnyAuthority("ROLE_Administrador", "ROLE_SuperAdmin")
                        .requestMatchers("/admin/coordinador/**").hasAnyAuthority("ROLE_Administrador", "ROLE_SuperAdmin")
                        .requestMatchers("/coordinador/**").hasAnyAuthority("ROLE_Coordinador", "ROLE_SuperAdmin")
                        .requestMatchers("/vecino/**").hasAnyAuthority("ROLE_Usuario final", "ROLE_SuperAdmin")
                        .requestMatchers("/export/actividad-coordinadores", "/export/asistencia-coordinadores").hasAnyAuthority("ROLE_Coordinador", "ROLE_Administrador", "ROLE_SuperAdmin")
                        .requestMatchers("/export/mi-actividad").hasAnyAuthority("ROLE_Administrador")
                        .anyRequest().authenticated()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler(customLogoutSuccessHandler())
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                );

        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler customAuthenticationSuccessHandler() {
        return new AuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                                Authentication authentication) throws IOException, ServletException {
                // --- DEBUG: [SuccessHandler] Inicio del customAuthenticationSuccessHandler ---
                System.out.println("--- DEBUG: [SuccessHandler] Inicio del customAuthenticationSuccessHandler ---");
                String authenticatedUsername = authentication.getName();
                System.out.println("--- DEBUG: [SuccessHandler] Usuario autenticado por Spring Security: " + authenticatedUsername + " ---");

                DefaultSavedRequest defaultSavedRequest =
                        (DefaultSavedRequest) request.getSession().getAttribute("SPRING_SECURITY_SAVED_REQUEST");

                Optional<Usuarios> optUser = Optional.empty();
                if (authenticatedUsername.matches("\\d+")) {
                    // Es un DNI
                    optUser = usuariosRepository.findByDni(authenticatedUsername);
                } else {
                    // Es un correo
                    optUser = usuariosRepository.findByCorreoIgnoreCase(authenticatedUsername);
                }


                if (optUser.isPresent()) {
                    Usuarios loggedUser = optUser.get();
                    System.out.println("--- DEBUG: [SuccessHandler] Entidad Usuarios encontrada. ID: " + loggedUser.getIdUsuarios() + ", Correo: " + loggedUser.getCorreo() + " ---");
                    System.out.println("--- DEBUG: [SuccessHandler] Rol de la entidad: " + (loggedUser.getRol() != null ? loggedUser.getRol().getRol() : "N/A") + " ---");
                    System.out.println("--- DEBUG: [SuccessHandler] Estado de la entidad: " + (loggedUser.getEstado() != null ? loggedUser.getEstado().getEstado() : "N/A") + ", ID Estado: " + (loggedUser.getEstado() != null ? loggedUser.getEstado().getIdEstado() : "N/A") + " ---");

                    // ¡ESTA ES LA LÍNEA CLAVE! Guarda el objeto Usuarios completo en la sesión con el nombre "usuario".
                    request.getSession().setAttribute("usuario", loggedUser);
                    String idConversacion = UUID.randomUUID().toString();
                    request.getSession().setAttribute("idConversacionActual", idConversacion);
                    System.out.println("--- DEBUG: [SuccessHandler] ID de Conversación generada: " + idConversacion + " ---");
                    System.out.println("--- DEBUG: [SuccessHandler] Atributo 'usuario' guardado en la sesión. ID de Sesión: " + request.getSession().getId() + " ---");

                    if (defaultSavedRequest != null) {
                        String rolName = loggedUser.getRol().getRol();
                        if (rolName.equals("Usuario final")) {
                            redirectStrategy.sendRedirect(request, response, "/");
                        } else if (rolName.equals("Coordinador")) {
                            redirectStrategy.sendRedirect(request, response, "/coordinador/mi-perfil");
                        } else if (rolName.equals("SuperAdmin")) {
                            redirectStrategy.sendRedirect(request, response, "/superadmin/home");
                        } else if (rolName.equals("Administrador")) {
                            redirectStrategy.sendRedirect(request, response, "/admin/mi-perfil");

                        } else {
                            String targetURL = defaultSavedRequest.getRedirectUrl();
                            System.out.println("--- DEBUG: [SuccessHandler] Redirigiendo a URL guardada: " + targetURL + " ---");
                            redirectStrategy.sendRedirect(request, response, targetURL);
                        }

                    } else {
                        String rolName = loggedUser.getRol().getRol();
                        System.out.println("--- DEBUG: [SuccessHandler] Redirigiendo basado en rol: " + rolName + " ---");

                        switch (rolName) {
                            case "SuperAdmin" -> redirectStrategy.sendRedirect(request, response, "/superadmin/home");
                            case "Administrador" ->
                                    redirectStrategy.sendRedirect(request, response, "/admin/mi-perfil");
                            case "Coordinador" ->
                                    redirectStrategy.sendRedirect(request, response, "/coordinador/mi-perfil");
                            case "Usuario final" -> redirectStrategy.sendRedirect(request, response, "/");
                            default -> {
                                System.out.println("--- DEBUG: [SuccessHandler] Rol no reconocido: " + rolName + ". Redirigiendo a la raíz. ---");
                                redirectStrategy.sendRedirect(request, response, "/");
                            }
                        }
                    }
                } else {
                    System.out.println("--- DEBUG: [SuccessHandler] Entidad Usuarios NO encontrada después de autenticación exitosa. Redirigiendo a login con error. ---");
                    redirectStrategy.sendRedirect(request, response, "/login?error=auth_failed");
                }
                System.out.println("--- DEBUG: [SuccessHandler] Fin del customAuthenticationSuccessHandler ---");
            }
        };
    }

    @Bean
    public OncePerRequestFilter loginPageRedirectFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                    throws ServletException, java.io.IOException {
                String uri = request.getRequestURI();
                Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                if (uri.equals("/login") && auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                    for (GrantedAuthority authority : auth.getAuthorities()) {
                        String role = authority.getAuthority();
                        if (role.equals("ROLE_SuperAdmin")) {
                            response.sendRedirect("/superadmin/home");
                            return;
                        } else if (role.equals("ROLE_Administrador")) {
                            response.sendRedirect("/admin/mi-perfil");
                            return;
                        } else if (role.equals("ROLE_Coordinador")) {
                            response.sendRedirect("/coordinador/mi-perfil");
                            return;
                        } else if (role.equals("ROLE_Usuario final")) {
                            response.sendRedirect("/");
                            return;
                        }
                    }
                    response.sendRedirect("/");
                    return;
                }
                filterChain.doFilter(request, response);
            }
        };
    }
}