package com.turtletracerlib.auto;

import android.content.Context;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * The list of autos a driver can choose from.
 * <p>
 * {@code T} is the team's auto type, such as SolversLib's, NextFTC's or Ivy's {@code Command}, so this
 * library does not depend on a command framework.
 * </p>
 *
 * @param <T> The type of auto.
 */
public final class AutoRegistry<T> {
  private final List<AutoEntry<T>> entries = new ArrayList<>();
  /** What {@link TurtleAuto} classes' constructors can be given, besides the AutoContext. */
  private final List<Object> dependencies = new ArrayList<>();
  /** Why annotated classes were left out of the menu. */
  private final List<String> skipped = new ArrayList<>();

  /**
   * Objects the constructors of {@link TurtleAuto} classes can take, such as the follower, the
   * hardware map and telemetry. Call it before {@link #scan}. A class is built with its public
   * constructor that takes the most parameters, as long as each one is an {@link AutoContext} or one
   * of these; code exported from Turtle Tracer takes {@code (follower, hardwareMap, telemetry)} and
   * sometimes a context.
   *
   * @param values The objects; nulls are ignored.
   * @return This registry.
   */
  public AutoRegistry<T> using(Object... values) {
    for (Object value : values) {
      if (value != null) {
        dependencies.add(value);
      }
    }
    return this;
  }

  /**
   * @param name    The name shown in the menu.
   * @param factory Builds the auto.
   * @return This registry.
   * @throws IllegalArgumentException If the name is already registered.
   */
  public AutoRegistry<T> add(String name, AutoFactory<T> factory) {
    return add(new AutoEntry<>(name, factory));
  }

  /**
   * @param name    The name shown in the menu.
   * @param factory Builds the auto.
   * @param policy  When this auto is mirrored, replacing the config's policy.
   * @return This registry.
   * @throws IllegalArgumentException If the name is already registered.
   */
  public AutoRegistry<T> add(String name, AutoFactory<T> factory, MirrorPolicy policy) {
    return add(new AutoEntry<>(name, factory).mirror(policy));
  }

  /**
   * @param name     The name shown in the menu.
   * @param factory  Builds the auto.
   * @param drawnFor The alliance this auto's paths are drawn for, replacing the config's base
   *                 alliance.
   * @return This registry.
   * @throws IllegalArgumentException If the name is already registered.
   */
  public AutoRegistry<T> add(String name, AutoFactory<T> factory, Alliance drawnFor) {
    return add(new AutoEntry<>(name, factory).drawnFor(drawnFor));
  }

  /**
   * @param entry The entry to add.
   * @return This registry.
   * @throws IllegalArgumentException If the name is already registered.
   */
  public AutoRegistry<T> add(AutoEntry<T> entry) {
    if (indexOf(entry.getName()) >= 0) {
      throw new IllegalArgumentException("An auto named '" + entry.getName() + "' is already registered");
    }
    entries.add(entry);
    return this;
  }

  /**
   * Adds every class annotated with {@link TurtleAuto} that is a {@code type}, found among the
   * app's classes. Autos already added by name are kept. Best effort: see {@link AutoScanner}.
   * <p>
   * If TeamCode is loaded separately from the app, as hot reloading (such as Sloth) does, this may
   * not see it: use {@link #scan(Object, Class, String...)} with one of your own objects instead.
   * </p>
   *
   * @param context  The app context, such as {@code hardwareMap.appContext}.
   * @param type     The auto type; classes that aren't one are skipped.
   * @param prefixes Package prefixes to search, such as {@code "org.firstinspires.ftc.teamcode"}.
   * @return This registry.
   */
  public AutoRegistry<T> scan(Context context, Class<? extends T> type, String... prefixes) {
    List<ClassLoader> loaders = new ArrayList<>();
    loaders.add(Thread.currentThread().getContextClassLoader());
    loaders.add(context == null ? null : context.getClassLoader());
    return scanLoaders(loaders, context, type, prefixes);
  }

  /**
   * Adds every class annotated with {@link TurtleAuto} that is a {@code type}, found alongside
   * {@code owner}: among the classes loaded with it, wherever they came from. Use it from your own
   * code, such as {@code autos.scan(this, Command.class, "org.firstinspires.ftc.teamcode")}, so autos
   * are found even when TeamCode is hot reloaded (such as with Sloth).
   *
   * @param owner    One of your objects, such as {@code this}, or one of your classes.
   * @param type     The auto type; classes that aren't one are skipped.
   * @param prefixes Package prefixes to search, such as {@code "org.firstinspires.ftc.teamcode"}.
   * @return This registry.
   */
  public AutoRegistry<T> scan(Object owner, Class<? extends T> type, String... prefixes) {
    if (owner instanceof Context) {
      return scan((Context) owner, type, prefixes);
    }
    Class<?> ownerClass = owner instanceof Class ? (Class<?>) owner : owner.getClass();
    List<ClassLoader> loaders = new ArrayList<>();
    loaders.add(ownerClass.getClassLoader());
    loaders.add(Thread.currentThread().getContextClassLoader());
    return scanLoaders(loaders, null, type, prefixes);
  }

  private AutoRegistry<T> scanLoaders(
      List<ClassLoader> loaders, Context apkFallback, Class<? extends T> type, String... prefixes) {
    AutoScanner.Result result = AutoScanner.findAnnotated(loaders, apkFallback, prefixes);
    scan(type, result.found);
    if (result.found.isEmpty()) {
      // Nothing was found: say so, so an empty menu explains itself.
      String where = prefixes == null || prefixes.length == 0 ? "the app" : joined(java.util.Arrays.asList(prefixes));
      String why = result.problems.isEmpty() ? "" : " (" + result.problems.get(0) + ")";
      skipped.add("No @TurtleAuto autos found in " + where + why
          + ". From your own code, try scan(this, ...)");
    }
    return this;
  }

  /**
   * Adds the {@link TurtleAuto} classes among {@code candidates} that are a {@code type}. A class
   * this registry can't build (see {@link #using}) is left out, and {@link #skipped()} says why, so
   * it never fails when the driver presses Start.
   *
   * @param type       The auto type; classes that aren't one are skipped.
   * @param candidates The classes to consider.
   * @return This registry.
   */
  public AutoRegistry<T> scan(Class<? extends T> type, Collection<Class<?>> candidates) {
    for (Class<?> cls : candidates) {
      TurtleAuto info = cls.getAnnotation(TurtleAuto.class);
      if (info == null || !type.isAssignableFrom(cls)) {
        continue;
      }
      String name = info.name().isEmpty() ? cls.getSimpleName() : info.name();
      if (indexOf(name) >= 0) {
        continue;
      }
      StringBuilder whyNot = new StringBuilder();
      AutoFactory<T> factory = reflectiveFactory(cls, whyNot);
      if (factory == null) {
        skipped.add("Skipped " + name + ": " + whyNot);
        continue;
      }
      AutoEntry<T> entry = new AutoEntry<>(name, factory).group(info.group());
      if (info.overrideMirror()) {
        entry.mirror(info.mirror());
      }
      entry.drawnFor(info.drawnFor());
      entries.add(entry);
    }
    return this;
  }

  /**
   * @return What scanning couldn't add, one line each, such as
   *     {@code "Skipped 2 Score: needs Follower (give it to using(...))"}, or why it found nothing.
   *     Empty when everything was added.
   */
  public List<String> skipped() {
    return Collections.unmodifiableList(skipped);
  }

  /** @return The entries in the order added. */
  public List<AutoEntry<T>> entries() {
    return Collections.unmodifiableList(entries);
  }

  /** @return The number of autos. */
  public int size() {
    return entries.size();
  }

  /**
   * @param name An auto's name.
   * @return Its index, or -1 if there is none.
   */
  public int indexOf(String name) {
    for (int i = 0; i < entries.size(); i++) {
      if (entries.get(i).getName().equals(name)) {
        return i;
      }
    }
    return -1;
  }

  /** The object given to {@link #using} for a parameter of this type, or null. */
  private Object dependencyFor(Class<?> type) {
    for (Object value : dependencies) {
      if (type.isInstance(value)) {
        return value;
      }
    }
    return null;
  }

  /** The parameter types {@code types} needs that this registry can't give, by simple name. */
  private List<String> missing(Class<?>[] types) {
    List<String> names = new ArrayList<>();
    for (Class<?> type : types) {
      if (type != AutoContext.class && dependencyFor(type) == null) {
        names.add(type.getSimpleName());
      }
    }
    return names;
  }

  /**
   * How to build {@code cls}, decided now so a class that can't be built is never offered: a Kotlin
   * {@code object}, a {@code static create(AutoContext)}, or the public constructor with the most
   * parameters that can all be given. Null, with the reason in {@code whyNot}, if there is no way.
   */
  @SuppressWarnings("unchecked")
  private AutoFactory<T> reflectiveFactory(Class<?> cls, StringBuilder whyNot) {
    // Kotlin object.
    try {
      Field instance = cls.getField("INSTANCE");
      if (Modifier.isStatic(instance.getModifiers())) {
        return context -> {
          try {
            return (T) instance.get(null);
          } catch (IllegalAccessException e) {
            throw new IllegalStateException("Could not build auto " + cls.getName(), e);
          }
        };
      }
    } catch (NoSuchFieldException ignored) {
      // Not a Kotlin object.
    }
    try {
      Method create = cls.getMethod("create", AutoContext.class);
      if (Modifier.isStatic(create.getModifiers())) {
        return context -> build(cls, () -> create.invoke(null, context));
      }
    } catch (NoSuchMethodException ignored) {
      // No factory method.
    }

    Constructor<?> best = null;
    List<String> fewestMissing = null;
    for (Constructor<?> constructor : cls.getConstructors()) {
      List<String> missing = missing(constructor.getParameterTypes());
      if (missing.isEmpty()) {
        if (best == null || constructor.getParameterTypes().length > best.getParameterTypes().length) {
          best = constructor;
        }
      } else if (fewestMissing == null || missing.size() < fewestMissing.size()) {
        fewestMissing = missing;
      }
    }
    if (best == null) {
      whyNot.append(
          fewestMissing == null
              ? "no public constructor"
              : "needs " + joined(fewestMissing) + " (give it to using(...))");
      return null;
    }
    Constructor<?> chosen = best;
    return context -> {
      Class<?>[] types = chosen.getParameterTypes();
      Object[] args = new Object[types.length];
      for (int i = 0; i < types.length; i++) {
        args[i] = types[i] == AutoContext.class ? context : dependencyFor(types[i]);
      }
      return build(cls, () -> chosen.newInstance(args));
    };
  }

  /** "A, B" (String.join needs a newer Android than the FTC SDK supports). */
  private static String joined(List<String> names) {
    StringBuilder out = new StringBuilder();
    for (String name : names) {
      if (out.length() > 0) {
        out.append(", ");
      }
      out.append(name);
    }
    return out.toString();
  }

  /** Something that builds an auto by reflection. */
  private interface Builder {
    Object build() throws ReflectiveOperationException;
  }

  /** Runs {@code builder}, reporting what the auto's own constructor threw. */
  @SuppressWarnings("unchecked")
  private T build(Class<?> cls, Builder builder) {
    try {
      return (T) builder.build();
    } catch (InvocationTargetException e) {
      throw new IllegalStateException("Could not build auto " + cls.getName(), e.getCause());
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Could not build auto " + cls.getName(), e);
    }
  }
}
