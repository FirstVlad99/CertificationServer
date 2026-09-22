package ru.cert.certificationserver.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

//class JsonFieldPathTest {
//
//  @Test
//  @DisplayName("имя берётся из @JsonProperty, а не из имени Java-поля")
//  void usesJsonPropertyName() {
//    assertThat(JsonFieldPath.of(new ChangePasswordRequest(), "newPassword")).isEqualTo("new_password");
//  }
//
//  @Test
//  @DisplayName("вложенный путь с индексом: элемент списка резолвится по фактическому типу привязки")
//  void resolvesNestedPathThroughListElement() {
//    CreateUserRequest request = new CreateUserRequest();
//    request.setRoles(List.of(new StudentCreateScope()));
//
//    assertThat(JsonFieldPath.of(request, "roles[0].streamId")).isEqualTo("roles[0].stream_id");
//    assertThat(JsonFieldPath.of(request, "roles[0].candidateId")).isEqualTo("roles[0].candidate_id");
//    assertThat(JsonFieldPath.of(request, "roles[0].birthDate")).isEqualTo("roles[0].birth_date");
//  }
//
//  @Test
//  @DisplayName("механическая конвертация camelCase не годится: middleName -> mid_name, cityName -> city")
//  void usesDeclaredNameNotSnakeCaseOfField() {
//    CreateUserRequest request = new CreateUserRequest();
//    request.setRoles(List.of(new StudentCreateScope()));
//
//    assertThat(JsonFieldPath.of(request, "middleName")).isEqualTo("mid_name");
//    assertThat(JsonFieldPath.of(request, "roles[0].cityName")).isEqualTo("roles[0].city");
//  }
//
//  @Test
//  @DisplayName("без @JsonProperty проводное имя равно имени поля: PropertyNamingStrategy в проекте нет")
//  void keepsFieldNameWhenNotAnnotated() {
//    CreateUserRequest request = new CreateUserRequest();
//    request.setRoles(List.of(new StudentCreateScope()));
//
//    assertThat(JsonFieldPath.of(request, "roles")).isEqualTo("roles");
//    assertThat(JsonFieldPath.of(request, "roles[0].track")).isEqualTo("roles[0].track");
//  }
//
//  @Test
//  @DisplayName("нерезолвимый путь отдаётся как есть: @AssertTrue висит на методе, поля под ним нет")
//  void fallsBackToRawPath() {
//    CreateUserRequest request = new CreateUserRequest();
//
//    assertThat(JsonFieldPath.of(request, "atLeastOneEmailProvided")).isEqualTo("atLeastOneEmailProvided");
//    assertThat(JsonFieldPath.of(null, "newPassword")).isEqualTo("newPassword");
//    assertThat(JsonFieldPath.of(request, "roles[7].streamId")).isEqualTo("roles[7].streamId");
//  }
//}