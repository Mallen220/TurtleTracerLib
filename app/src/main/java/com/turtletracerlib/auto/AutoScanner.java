package com.turtletracerlib.auto;

import android.content.Context;
import dalvik.system.BaseDexClassLoader;
import dalvik.system.DexFile;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Finds classes annotated with {@link TurtleAuto}.
 * <p>
 * It reads the dex files of the class loaders it is given, and of their parents, and loads each
 * class with the loader its dex file belongs to. That finds team code wherever it was loaded from:
 * the app itself, or a loader that hot-reloads TeamCode (such as Sloth's), which the app's own class
 * loader doesn't see. If no loader's dex files can be read, it falls back to the app's APK. It is
 * best effort: what it couldn't read is reported, not thrown. Pass package prefixes to keep it fast.
 * </p>
 */
final class AutoScanner {
  private AutoScanner() {}

  /** What a scan found, and anything that got in its way. */
  static final class Result {
    final List<Class<?>> found = new ArrayList<>();
    final List<String> problems = new ArrayList<>();
  }

  /**
   * @param loaders     Class loaders to search, the team's own first; nulls are ignored.
   * @param apkFallback The app context, for reading its APK when no loader's dex files can be read;
   *                    may be null.
   * @param prefixes    Package prefixes to look in. All classes are checked if none are given.
   * @return The annotated classes, each loaded by the loader that holds it.
   */
  static Result findAnnotated(List<ClassLoader> loaders, Context apkFallback, String... prefixes) {
    Result result = new Result();
    Set<String> seen = new HashSet<>();
    boolean readAny = false;

    for (ClassLoader loader : withParents(loaders)) {
      List<DexFile> dexFiles = dexFilesOf(loader, result.problems);
      for (DexFile dex : dexFiles) {
        readAny = true;
        collect(dex, loader, prefixes, seen, result.found);
      }
    }

    if (!readAny && apkFallback != null) {
      readApk(apkFallback, prefixes, seen, result);
    }
    return result;
  }

  /** The loaders and their parents, each once, in order. */
  private static Set<ClassLoader> withParents(List<ClassLoader> loaders) {
    Set<ClassLoader> all = new LinkedHashSet<>();
    for (ClassLoader loader : loaders) {
      for (ClassLoader l = loader; l != null; l = l.getParent()) {
        all.add(l);
      }
    }
    return all;
  }

  /**
   * The dex files an Android class loader reads from (its private dex element list). Empty for
   * loaders that aren't dex based, such as a delegating parent.
   */
  private static List<DexFile> dexFilesOf(ClassLoader loader, List<String> problems) {
    List<DexFile> files = new ArrayList<>();
    if (!(loader instanceof BaseDexClassLoader)) {
      return files;
    }
    try {
      Field pathListField = BaseDexClassLoader.class.getDeclaredField("pathList");
      pathListField.setAccessible(true);
      Object pathList = pathListField.get(loader);
      if (pathList == null) {
        return files;
      }
      Field elementsField = pathList.getClass().getDeclaredField("dexElements");
      elementsField.setAccessible(true);
      Object[] elements = (Object[]) elementsField.get(pathList);
      if (elements == null) {
        return files;
      }
      for (Object element : elements) {
        Field dexFileField = element.getClass().getDeclaredField("dexFile");
        dexFileField.setAccessible(true);
        Object dex = dexFileField.get(element);
        if (dex instanceof DexFile) {
          files.add((DexFile) dex);
        }
      }
    } catch (Throwable e) {
      problems.add("couldn't read the classes of " + loader.getClass().getSimpleName() + ": " + e);
    }
    return files;
  }

  /** Adds the annotated classes in {@code dex} not seen yet, loaded by {@code loader}. */
  @SuppressWarnings("deprecation")
  private static void collect(
      DexFile dex, ClassLoader loader, String[] prefixes, Set<String> seen, List<Class<?>> found) {
    Enumeration<String> names = dex.entries();
    while (names.hasMoreElements()) {
      String name = names.nextElement();
      if (!matches(name, prefixes) || !seen.add(name)) {
        continue;
      }
      try {
        Class<?> cls = Class.forName(name, false, loader);
        if (cls.isAnnotationPresent(TurtleAuto.class)) {
          found.add(cls);
        }
      } catch (Throwable ignored) {
        // A class that can't load (missing optional dependency) can't be an auto we can build.
      }
    }
  }

  /** The old way: the app's own APK, through the app's class loader. */
  @SuppressWarnings("deprecation")
  private static void readApk(Context context, String[] prefixes, Set<String> seen, Result result) {
    List<String> paths = new ArrayList<>();
    paths.add(context.getApplicationInfo().sourceDir);
    String[] splits = context.getApplicationInfo().splitSourceDirs;
    if (splits != null) {
      for (String split : splits) {
        paths.add(split);
      }
    }
    for (String path : paths) {
      DexFile dex = null;
      try {
        dex = new DexFile(path);
        collect(dex, context.getClassLoader(), prefixes, seen, result.found);
      } catch (Throwable e) {
        result.problems.add("couldn't read " + path + ": " + e);
      } finally {
        if (dex != null) {
          try {
            dex.close();
          } catch (Throwable ignored) {
            // Nothing to do.
          }
        }
      }
    }
  }

  private static boolean matches(String className, String[] prefixes) {
    if (prefixes == null || prefixes.length == 0) {
      return true;
    }
    for (String prefix : prefixes) {
      if (className.startsWith(prefix)) {
        return true;
      }
    }
    return false;
  }
}
