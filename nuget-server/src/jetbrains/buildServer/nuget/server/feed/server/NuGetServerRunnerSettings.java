/*
 * Copyright 2000-2011 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.nuget.server.feed.server;

import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * @author Eugene Petrenko (eugene.petrenko@gmail.com)
 *         Date: 21.10.11 18:53
 */
public interface NuGetServerRunnerSettings {
  boolean isNuGetFeedEnabled();

  @NotNull
  String getPackagesControllerUrl();

  @NotNull
  File getLogFilePath();

  /**
   * @return context based path of nuget feed OData service
   */
  @NotNull
  String getNuGetFeedControllerPath();
  
  @NotNull
  String getNuGetHttpAuthFeedControllerPath();

  @NotNull
  String getNuGetGuestAuthFeedControllerPath();
}
