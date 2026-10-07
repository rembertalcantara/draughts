package com.draughts.adapter.in.web.identity;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.UUID;

public class CurrentPlayerResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentPlayer.class) && parameter.getParameterType() == UUID.class;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        var id = webRequest.getAttribute(GuestIdentityFilter.PLAYER_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        if (id == null) {
            throw new IllegalStateException("No player identity on request");
        }
        return id;
    }
}
