package jetbrains.buildServer.nuget.common;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created 27.12.12 15:46
 *
 * @author Eugene Petrenko (eugene.petrenko@jetbrains.com)
 * @since v0.9
 */
public class NuGetTools {
  private static final String TOOL_REFERENCE_PREFIX = "?";

  @NotNull
  public static String getToolReference(@NotNull final String id) {
    return TOOL_REFERENCE_PREFIX + id;
  }

  public static boolean isToolReference(@Nullable final String id) {
    return id != null && id.startsWith(TOOL_REFERENCE_PREFIX);
  }

  @Nullable
  public static String getReferredToolId(@Nullable final String id) {
    if (id == null || id.length() == 0) return null;

    if (isToolReference(id)) {
      return id.substring(TOOL_REFERENCE_PREFIX.length());
    }
    return null;
  }
}