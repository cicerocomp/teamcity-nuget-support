/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

package jetbrains.buildServer.nuget.feed.server.tab;

import jetbrains.buildServer.RootUrlHolder;
import jetbrains.buildServer.controllers.AuthorizationInterceptor;
import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.controllers.RequestPermissionsChecker;
import jetbrains.buildServer.nuget.feed.server.PermissionChecker;
import jetbrains.buildServer.nuget.feed.server.NuGetServerSettings;
import jetbrains.buildServer.serverSide.auth.AccessDeniedException;
import jetbrains.buildServer.serverSide.auth.AuthorityHolder;
import jetbrains.buildServer.serverSide.auth.LoginConfiguration;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.UriBuilder;

/**
 * @author Eugene Petrenko (eugene.petrenko@gmail.com)
 *         Date: 26.10.11 19:21
 */
public class FeedServerController extends BaseController {
  @NotNull private final PluginDescriptor myDescriptor;
  @NotNull private final NuGetServerSettings mySettings;
  @NotNull private final LoginConfiguration myLoginConfiguration;
  @NotNull private final RootUrlHolder myRootUrlHolder;

  private final String myIncludePath;
  private final String mySettingsPath;

  public FeedServerController(@NotNull final AuthorizationInterceptor auth,
                              @NotNull final PermissionChecker checker,
                              @NotNull final WebControllerManager web,
                              @NotNull final PluginDescriptor descriptor,
                              @NotNull final LoginConfiguration loginConfiguration,
                              @NotNull final NuGetServerSettings settings,
                              @NotNull final RootUrlHolder rootUrlHolder) {
    myDescriptor = descriptor;
    mySettings = settings;
    myLoginConfiguration = loginConfiguration;
    myRootUrlHolder = rootUrlHolder;
    myIncludePath = descriptor.getPluginResourcesPath("feed/status.html");
    mySettingsPath = descriptor.getPluginResourcesPath("feed/settings.html");

    auth.addPathBasedPermissionsChecker(myIncludePath, new RequestPermissionsChecker() {
      public void checkPermissions(@NotNull AuthorityHolder authorityHolder, @NotNull HttpServletRequest request) throws AccessDeniedException {
        checker.assertAccess(authorityHolder);
      }
    });
    web.registerController(myIncludePath, this);
  }

  @Override
  protected ModelAndView doHandle(@NotNull final HttpServletRequest request,
                                  @NotNull final HttpServletResponse response) throws Exception {
    final ModelAndView mv = new ModelAndView(myDescriptor.getPluginResourcesPath("feedServerSettingsWindows.jsp"));
    final UriBuilder uriBuilder = UriBuilder.fromUri(myRootUrlHolder.getRootUrl());
    final String privateFeedUrl = uriBuilder.replacePath(mySettings.getNuGetHttpAuthFeedControllerPath()).build().toString();
    final String publicFeedUrl = uriBuilder.replacePath(mySettings.getNuGetGuestAuthFeedControllerPath()).build().toString();

    mv.getModel().put("nugetStatusRefreshUrl", myIncludePath);
    mv.getModel().put("nugetSettingsPostUrl", mySettingsPath);
    mv.getModel().put("privateFeedUrl", privateFeedUrl);
    mv.getModel().put("publicFeedUrl", publicFeedUrl);
    mv.getModel().put("serverEnabled", mySettings.isNuGetServerEnabled());
    mv.getModel().put("isGuestEnabled", myLoginConfiguration.isGuestLoginAllowed());

    return mv;
  }
}
