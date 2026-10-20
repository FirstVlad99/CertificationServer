package ru.cert.certificationserver.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import ru.cert.certificationserver.dto.error.response.ErrorResponse;
import ru.cert.certificationserver.model.enums.ErrorCodeEnum;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

// 401 как JSON ErrorResponse (вынесено из инлайн-лямбды в SecurityConfig).
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {
  private final ObjectMapper objectMapper;

  public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException
  ) throws IOException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType("application/json;charset=UTF-8");
    response.getWriter().write(objectMapper.writeValueAsString(
        new ErrorResponse(ErrorCodeEnum.UNAUTHORIZED, "Требуется аутентификация.")
    ));
  }
}
