package com.turtletracerlib.auto;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class to be found by {@link AutoRegistry#scan}. The class needs a
 * {@code static create(AutoContext)} method, a constructor taking an {@link AutoContext}, a no-argument
 * constructor, or (Kotlin) to be an {@code object}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface TurtleAuto {
  /** @return The name shown in the menu; the simple class name if empty. */
  String name() default "";

  /** @return A group name, shown in front of the name. Empty for none. */
  String group() default "";

  /** @return When this auto is mirrored; the config's policy if not set. */
  MirrorPolicy mirror() default MirrorPolicy.FOLLOW_ALLIANCE;

  /** @return Set true to use {@link #mirror()} instead of the config's policy. */
  boolean overrideMirror() default false;

  /** @return The alliance this auto's paths are drawn for; NONE to use the config's base alliance. */
  Alliance drawnFor() default Alliance.NONE;
}
