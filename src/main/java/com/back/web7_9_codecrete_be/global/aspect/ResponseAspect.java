package com.back.web7_9_codecrete_be.global.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import com.back.web7_9_codecrete_be.global.rsData.RsData;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Aspect
@Component
@RequiredArgsConstructor
public class ResponseAspect {

	private final HttpServletResponse response;

	@Around("""
		(
			within
			(
				@org.springframework.web.bind.annotation.RestController *
			)
			&&
			(
				@annotation(org.springframework.web.bind.annotation.GetMapping)
				||
				@annotation(org.springframework.web.bind.annotation.PostMapping)
				||
				@annotation(org.springframework.web.bind.annotation.PutMapping)
				||
				@annotation(org.springframework.web.bind.annotation.DeleteMapping)
				||
				@annotation(org.springframework.web.bind.annotation.PatchMapping)
			)
		)
		||
		@annotation(org.springframework.web.bind.annotation.ResponseBody)
		""")
	public Object responseAspect(ProceedingJoinPoint joinPoint) throws Throwable {

		Object rst = joinPoint.proceed();

		if (rst instanceof RsData rsData) {
			response.setStatus(rsData.getStatusCode());
		}

		return rst;
	}

}
