package com.gabia.bshop.security.support;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.gabia.bshop.security.CurrentMember;
import com.gabia.bshop.security.MemberPayload;
import com.gabia.bshop.security.provider.JwtProvider;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuthArgumentResolver implements HandlerMethodArgumentResolver {

	private final JwtProvider jwtProvider;

	@Override
	public boolean supportsParameter(final MethodParameter parameter) {
		boolean hasAnnotation = parameter.hasParameterAnnotation(CurrentMember.class);
		boolean hasMemberPayload = MemberPayload.class.isAssignableFrom(parameter.getParameterType());
		return hasAnnotation && hasMemberPayload;
	}

	@Override
	public Object resolveArgument(final MethodParameter parameter,
		final ModelAndViewContainer mavContainer,
		final NativeWebRequest webRequest, final WebDataBinderFactory binderFactory) {
		final String authorizationHeader = webRequest.getHeader(HttpHeaders.AUTHORIZATION);
		if (authorizationHeader == null) {
			return null;
		}
		return jwtProvider.getPayload(authorizationHeader);
	}
}
