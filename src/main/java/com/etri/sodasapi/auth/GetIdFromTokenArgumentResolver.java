package com.etri.sodasapi.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component

public class GetIdFromTokenArgumentResolver implements HandlerMethodArgumentResolver {
    private final KeycloakAdapter keycloakAdapter;

    public GetIdFromTokenArgumentResolver(KeycloakAdapter tokenProvider){
        this.keycloakAdapter = tokenProvider;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterAnnotation(GetIdFromToken.class) != null;
    }

    @Override
    public Object resolveArgument(@NotNull MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if(request == null){
            throw new IllegalStateException("No HttpServletRequest found");
        }

        String authorizationHeader = request.getHeader("Authorization");
        if(authorizationHeader == null){
//            throw new BaseException(BaseExceptionCode.AUTHORIZATION_HEADER_NULL);
        }

        return keycloakAdapter.getUserPk(authorizationHeader.substring(6));
    }
}
