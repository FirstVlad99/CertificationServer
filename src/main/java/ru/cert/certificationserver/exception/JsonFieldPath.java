package ru.cert.certificationserver.exception;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Bean Validation строит путь по именам JAVA-полей, а на проводе имена другие: глобальной
// PropertyNamingStrategy в проекте нет, конвенция - пофайловый @JsonProperty. Фронт ищет ошибку по
// имени поля из тела запроса, поэтому переводим путь в проводной.
// фронт получал roles[0].streamId вместо roles[0].stream_id и newPassword вместо
// new_password и не мог подсветить поле.
final class JsonFieldPath {

  private static final Pattern SEGMENT = Pattern.compile("^([^\\[]+)((?:\\[\\d+])*)$");
  private static final Pattern INDEX = Pattern.compile("\\[(\\d+)]");

  private JsonFieldPath() {
  }

  // Идём по реальному объекту запроса, а не по объявленным типам
  static String of(Object target, String javaPath) {
    if (target == null || javaPath == null || javaPath.isBlank()) {
      return javaPath;
    }

    String[] segments = javaPath.split("\\.");
    StringBuilder path = new StringBuilder();
    Object current = target;

    for (int i = 0; i < segments.length; i++) {
      if (i > 0) {
        path.append('.');
      }

      Matcher segment = SEGMENT.matcher(segments[i]);
      Field field = segment.matches() && current != null
          ? findField(current.getClass(), segment.group(1))
          : null;

      // Не разрешили сегмент (например, @AssertTrue объявлен на методе) - остаток отдаём как есть.
      if (field == null) {
        return path.append(String.join(".", List.of(segments).subList(i, segments.length))).toString();
      }

      path.append(jsonName(field)).append(segment.group(2));
      current = valueAt(field, current, segment.group(2));
    }

    return path.toString();
  }

  private static Field findField(Class<?> type, String name) {
    for (Class<?> level = type; level != null && level != Object.class; level = level.getSuperclass()) {
      try {
        return level.getDeclaredField(name);
      } catch (NoSuchFieldException ignored) {
        // поле объявлено выше по иерархии - StudentPrivateScope extends StudentPublicScope
      }
    }
    return null;
  }

  private static String jsonName(Field field) {
    JsonProperty onField = field.getAnnotation(JsonProperty.class);
    if (onField != null && !onField.value().isEmpty()) {
      return onField.value();
    }

    String suffix = Character.toUpperCase(field.getName().charAt(0)) + field.getName().substring(1);
    for (String prefix : List.of("get", "is")) {
      try {
        Method getter = field.getDeclaringClass().getMethod(prefix + suffix);
        JsonProperty onGetter = getter.getAnnotation(JsonProperty.class);
        if (onGetter != null && !onGetter.value().isEmpty()) {
          return onGetter.value();
        }
      } catch (NoSuchMethodException ignored) {
        // геттера с таким именем нет - значит имя поля и есть проводное
      }
    }

    return field.getName();
  }

  private static Object valueAt(Field field, Object owner, String indices) {
    try {
      field.setAccessible(true);
      Object value = field.get(owner);

      Matcher index = INDEX.matcher(indices);
      while (index.find()) {
        if (!(value instanceof List<?> items)) {
          return null;
        }
        int at = Integer.parseInt(index.group(1));
        value = at < items.size() ? items.get(at) : null;
      }

      return value;
    } catch (ReflectiveOperationException | RuntimeException e) {
      return null;
    }
  }
}
