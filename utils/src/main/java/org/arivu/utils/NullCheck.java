package org.arivu.utils;

import java.util.Collection;
import java.util.Map;

public final class NullCheck {

  public static boolean isNullOrEmpty(final Map<?, ?> v) {
    return !(v != null && v.size() > 0);
  }

  public static boolean isNullOrEmpty(final CharSequence v) {
    return !(v != null && v.length() > 0);
  }

  public static boolean isNullOrEmpty(final Collection<?> v) {
    return !(v != null && v.size() > 0);
  }

  public static <T> boolean isNullOrEmpty(final T[] v) {
    return !(v != null && v.length > 0);
  }

}
