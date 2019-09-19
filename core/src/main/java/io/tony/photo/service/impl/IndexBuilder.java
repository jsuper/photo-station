package io.tony.photo.service.impl;

import com.google.common.primitives.Primitives;

import org.apache.lucene.document.Document;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.tony.photo.common.Index;
import io.tony.photo.common.Indexes;
import io.tony.photo.utils.Strings;

public class IndexBuilder {
  static final Map<Class, Map<String, Method>> beanIndexesCache = new ConcurrentHashMap<>();

  public static Document buildDocument(Object bean) {
    if (bean != null && (beanIndexesCache.containsKey(bean.getClass()) || isIndexes(bean.getClass()))) {

    }
    return null;
  }

  private static Map<String, Method> parseIndexMetadata(Class bean) {

    Field[] declaredFields = bean.getDeclaredFields();
    for (Field field : declaredFields) {
      Index index = field.getAnnotation(Index.class);
      if (index != null) {
        String fieldName = Strings.isBlank(index.name()) ? field.getName() : index.name();
        Class<?> type = field.getType();
        if (type.isPrimitive() || Primitives.isWrapperType(type) || type == String.class ||
          type == Date.class) {
          //simple type

        } else {

        }
      }
    }
    return null;
  }

  private static boolean isIndexes(Class beanClass) {
    Annotation indexed = beanClass.getDeclaredAnnotation(Indexes.class);
    return indexed != null;
  }
}
