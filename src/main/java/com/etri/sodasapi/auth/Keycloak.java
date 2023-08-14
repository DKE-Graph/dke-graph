package com.etri.sodasapi.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class Keycloak {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    final DefaultOidcUser


}
