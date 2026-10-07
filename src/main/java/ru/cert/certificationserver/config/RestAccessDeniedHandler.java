package ru.cert.certificationserver.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import ru.cert.certificationserver.dto.error.response.ErrorResponse;
import ru.cert.certificationserver.model.enums.ErrorCodeEnum;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

// 403 как JSON ErrorResponse (вынесено из инлайн-лямбды в SecurityConfig).
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {
  private final ObjectMapper objectMapper;

  public RestAccessDeniedHandler(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException
  ) throws IOException {
    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    response.setContentType("application/json;charset=UTF-8");
    response.getWriter().write(objectMapper.writeValueAsString(
        new ErrorResponse(ErrorCodeEnum.FORBIDDEN, "Insufficient permissions to perform this operation.")
    ));
  }
}
