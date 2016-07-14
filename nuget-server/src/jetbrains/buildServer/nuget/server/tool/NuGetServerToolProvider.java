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

package jetbrains.buildServer.nuget.server.tool;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.nuget.common.FeedConstants;
import jetbrains.buildServer.nuget.server.tool.impl.ToolUnpacker;
import jetbrains.buildServer.nuget.server.tool.impl.NuGetPackageValidationUtil;
import jetbrains.buildServer.tools.*;
import jetbrains.buildServer.tools.available.AvailableToolsState;
import jetbrains.buildServer.tools.available.DownloadableToolVersion;
import jetbrains.buildServer.tools.available.FetchToolsPolicy;
import jetbrains.buildServer.util.CollectionsUtil;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by Evgeniy.Koshkin on 15-Jan-16.
 */
public class NuGetServerToolProvider extends ServerToolProviderAdapter {

  private static final Logger LOG = Logger.getInstance(NuGetServerToolProvider.class.getName());

  public static final ToolType NUGET_TOOL_TYPE = new ToolTypeAdapter() {
    @NotNull
    public String getType() {
      return FeedConstants.NUGET_COMMANDLINE;
    }

    @NotNull
    @Override
    public String getDisplayName() {
      return "NuGet.exe";
    }

    @Nullable
    @Override
    public String getDescription() {
      return "Is used in NuGet specific build steps and NuGet Dependency Trigger.";
    }

    @NotNull
    @Override
    public String getShortDisplayName() {
      return "NuGet.exe";
    }

    @Override
    public boolean isSupportDownload() {
      return true;
    }

    @Nullable
    @Override
    public String getValidPackageDescription() {
      return "Specify the path to a NuGet package (.nupkg file) with the <em>tools/NuGet.exe</em> file inside.\n" +
              "<br/>Download the <em>NuGet.Commandline.&lt;VERSION&gt;.nupkg</em> file from\n" +
              "<a href=\"http://nuget.org/packages/NuGet.CommandLine\" target=\"_blank\">NuGet.org</a> and upload it here";
    }
  };

  @NotNull private final AvailableToolsState myAvailableTools;
  @NotNull private final NuGetToolDownloader myToolDownloader;

  @NotNull private final ToolUnpacker myUnpacker = new ToolUnpacker();

  public NuGetServerToolProvider(@NotNull AvailableToolsState availableTools,
                                 @NotNull NuGetToolDownloader toolDownloader) {
    myAvailableTools = availableTools;
    myToolDownloader = toolDownloader;
  }

  @NotNull
  public ToolType getType() {
    return NUGET_TOOL_TYPE;
  }

  @NotNull
  @Override
  public Collection<? extends ToolVersion> getAvailableToolVersions() {
    return myAvailableTools.getAvailable(FetchToolsPolicy.FetchNew).getFetchedTools();
  }

  @Nullable
  @Override
  public ToolVersion tryGetPackageVersion(@NotNull File toolPackage) {
    if (!FeedConstants.NUGET_TOOL_FILE_FILTER.accept(toolPackage)) {
      LOG.debug(String.format("File %s is not a valid NuGet redistributable package since its name do not suite.", toolPackage.getAbsolutePath()));
      return null;
    }
    if(FeedConstants.PACKAGE_FILE_FILTER.accept(toolPackage)){
      try {
        NuGetPackageValidationUtil.validatePackage(toolPackage);
      } catch (ToolException e) {
        LOG.debug(e);
        return null;
      }
    }

    final String toolId = ToolIdUtils.getIdForPackage(toolPackage);
    final String nugetVersion = ToolIdUtils.getPackageVersion(toolPackage);
    if(StringUtil.isEmpty(nugetVersion)){
      LOG.debug(String.format("Failed to determine NuGet version based on its package file name %s. Checked package %s", toolPackage.getName(), toolPackage.getAbsolutePath()));
      return null;
    }
    return new SimpleToolVersion(NUGET_TOOL_TYPE, nugetVersion, toolId);
  }

  @NotNull
  @Override
  public File fetchToolPackage(@NotNull ToolVersion toolVersion, @NotNull File targetDirectory) throws ToolException {
    final String id = toolVersion.getId();
    final DownloadableToolVersion downloadableNuGetTool = CollectionsUtil.findFirst(myAvailableTools.getAvailable(FetchToolsPolicy.ReturnCached).getFetchedTools(), data -> data.getId().equals(id));
    if(downloadableNuGetTool == null){
      throw new ToolException("Failed to fetch tool " + toolVersion + ". Download source info wasn't prefetched.");
    }
    final File location = new  File(targetDirectory, downloadableNuGetTool.getDestinationFileName());
    myToolDownloader.downloadTool(downloadableNuGetTool, location);
    return location;
  }

  @Override
  public void unpackToolPackage(@NotNull File toolPackage, @NotNull File targetDirectory) throws ToolException {
    try {
      if(FeedConstants.EXE_FILE_FILTER.accept(toolPackage)){
        FileUtil.copy(toolPackage, new File(targetDirectory, FeedConstants.PATH_TO_NUGET_EXE));
      } else {
        myUnpacker.extractPackage(toolPackage, targetDirectory);
      }
    } catch (IOException e) {
      throw new ToolException("Failed to unpack NuGet tool package " + toolPackage, e);
    }
  }
}
