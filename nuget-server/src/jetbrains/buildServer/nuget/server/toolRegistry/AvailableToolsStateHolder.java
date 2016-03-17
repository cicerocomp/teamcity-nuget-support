/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

package jetbrains.buildServer.nuget.server.toolRegistry;

import jetbrains.buildServer.tools.available.*;
import jetbrains.buildServer.util.TimeService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

/**
 * Created by Evgeniy.Koshkin on 17-Mar-16.
 */
public class AvailableToolsStateHolder implements AvailableToolsState {

  private final AvailableToolsState myAvailableTools;

  public AvailableToolsStateHolder(@NotNull TimeService timeService, @NotNull Collection<AvailableToolsFetcher> fetchers) {
    myAvailableTools = new AvailableToolsStateImpl(timeService, fetchers);
  }

  @Nullable
  @Override
  public DownloadableToolVersion findTool(@NotNull String version) {
    return myAvailableTools.findTool(version);
  }

  @NotNull
  @Override
  public FetchAvailableToolsResult getAvailable(FetchToolsPolicy fetchToolsPolicy) {
    return myAvailableTools.getAvailable(fetchToolsPolicy);
  }
}
