package com.etri.datalake.auth;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.keycloak.representations.AccessToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.IOException;

@RequiredArgsConstructor
public class KeycloakFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(KeycloakFilter.class);
    public static final String AUTHORIZATION_HEADER = "Authorization";
    private final KeycloakAdapter keycloakAdapter;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        String jwt = resolveToken(httpServletRequest);
        String requestURI = httpServletRequest.getRequestURI();


        if(jwt != null){
            AccessToken accessToken = keycloakAdapter.verifyToken(jwt);
            if (StringUtils.hasText(jwt) && accessToken != null) {
                logger.info("인증된 사용자 입니다. ");
                chain.doFilter(request, response);
            }else {
                logger.info("인증되지 않은 사용자입니다., uri: {}", requestURI);
            }
        }
        else {
            logger.info("유효한 JWT 토큰이 없습니다, uri: {}", requestURI);
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }
}
