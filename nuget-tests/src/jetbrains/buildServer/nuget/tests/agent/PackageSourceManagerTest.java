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

package jetbrains.buildServer.nuget.tests.agent;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.agent.AgentBuildFeature;
import jetbrains.buildServer.agent.AgentRunningBuildEx;
import jetbrains.buildServer.agent.Constants;
import jetbrains.buildServer.agent.impl.BuildParametersMapImpl;
import jetbrains.buildServer.nuget.agent.parameters.PackageSourceManager;
import jetbrains.buildServer.nuget.agent.parameters.impl.PackageSourceManagerImpl;
import jetbrains.buildServer.nuget.common.NuGetServerConstants;
import jetbrains.buildServer.nuget.common.PackagesConstants;
import jetbrains.buildServer.nuget.common.auth.PackageSource;
import jetbrains.buildServer.nuget.server.runner.auth.AuthBean;
import jetbrains.buildServer.parameters.ReferencesResolverUtil;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.parameters.BuildParametersProvider;
import jetbrains.buildServer.util.CollectionsUtil;
import jetbrains.buildServer.util.filters.Filter;
import org.jetbrains.annotations.NotNull;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.*;

import static jetbrains.buildServer.nuget.common.NuGetServerConstants.FEED_AUTH_REFERENCE_AGENT_PROVIDED;
import static jetbrains.buildServer.nuget.common.NuGetServerConstants.FEED_AUTH_REFERENCE_SERVER_PROVIDED;

/**
 * @author Evgeniy.Koshkin
 */
public class PackageSourceManagerTest extends BaseTestCase {

    private static final AuthBean AUTH_BEAN = new AuthBean();
    private static final String AGENT_BASED_URL = "agent";
    private static final String SERVER_BASED_URL = "server";
    private static final Map<String, String> PARAMETERS = CollectionsUtil.asMap(
            FEED_AUTH_REFERENCE_AGENT_PROVIDED, AGENT_BASED_URL,
            Constants.SYSTEM_PREFIX + FEED_AUTH_REFERENCE_SERVER_PROVIDED, SERVER_BASED_URL);
    private List<AgentBuildFeature> myFeatures;

    private PackageSourceManager mySources;
    private AgentRunningBuildEx myBuild;
    private Mockery m;

    @BeforeMethod
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        m = new Mockery();
        final BuildParametersProvider parametersProvider = m.mock(BuildParametersProvider.class);
        myBuild = m.mock(AgentRunningBuildEx.class);
        myFeatures = new ArrayList<>();
        m.checking(new Expectations() {{
            allowing(parametersProvider).getParameters(with(any(SBuild.class)), with(any(Boolean.class)));
            will(returnValue(PARAMETERS));
            allowing(parametersProvider).getParametersAvailableOnAgent(with(any(SBuild.class)));
            will(returnValue(Collections.emptyList()));
            allowing(myBuild).getBuildFeaturesOfType(PackagesConstants.ATHU_FEATURE_TYPE);
            will(returnValue(myFeatures));
            allowing(myBuild).getSharedConfigParameters();
            will(returnValue(PARAMETERS));
            allowing(myBuild).getSharedBuildParameters();
            will(returnValue(new BuildParametersMapImpl(PARAMETERS)));
            allowing(myBuild).getAccessUser();
            will(returnValue("user"));
            allowing(myBuild).getAccessCode();
            will(returnValue("code"));
        }});
        mySources = new PackageSourceManagerImpl();
    }

    @Test
    public void shouldSetupCredentialsForInternalFeed_IfNoAuthFeaturesConfigured() throws Exception {
        final AgentRunningBuildEx runningBuild = myBuild;

        final List<PackageSource> packageSources = getPackageSourcesOfRunningBuild(runningBuild);

        assertEquals(2, packageSources.size());
        assertContainsPackageSource(AGENT_BASED_URL, runningBuild.getAccessUser(), runningBuild.getAccessCode(), packageSources);
        assertContainsPackageSource(SERVER_BASED_URL, runningBuild.getAccessUser(), runningBuild.getAccessCode(), packageSources);
    }

    @Test
    public void shouldSetupCredentialsForInternalFeed_AuthFeatureConfiguredForInternalFeed() throws Exception {
        addAuthBuildFeature(ReferencesResolverUtil.makeReference(NuGetServerConstants.FEED_AUTH_REFERENCE_AGENT_PROVIDED), "user1", "password1");

        final AgentRunningBuildEx runningBuild = myBuild;
        final List<PackageSource> packageSources = getPackageSourcesOfRunningBuild(runningBuild);

        assertEquals(2, packageSources.size());
        assertContainsPackageSource(AGENT_BASED_URL, runningBuild.getAccessUser(), runningBuild.getAccessCode(), packageSources);
        assertContainsPackageSource(SERVER_BASED_URL, runningBuild.getAccessUser(), runningBuild.getAccessCode(), packageSources);
    }

    @Test
    public void shouldHandleMultipleFeaturesConfiguredForGivenFeedUrl() throws Exception {
        addAuthBuildFeature("some_url", "user1", "password1");
        addAuthBuildFeature("some_url", "user2", "password2");

        final AgentRunningBuildEx runningBuild = myBuild;
        final List<PackageSource> packageSources = getPackageSourcesOfRunningBuild(runningBuild);

        assertEquals(3, packageSources.size());

        assertContainsPackageSource(AGENT_BASED_URL, runningBuild.getAccessUser(), runningBuild.getAccessCode(), packageSources);
        assertContainsPackageSource(SERVER_BASED_URL, runningBuild.getAccessUser(), runningBuild.getAccessCode(), packageSources);
        assertContainsPackageSource("some_url", "user2", "password2", packageSources);
    }

    @Test
    public void shouldNotFailOnNetworkPaths() throws Exception {
        addAuthBuildFeature("\\\\some\\path\\", "", "");

        final AgentRunningBuildEx runningBuild = myBuild;
        final List<PackageSource> packageSources = getPackageSourcesOfRunningBuild(runningBuild);

        assertEquals(3, packageSources.size());

        assertContainsPackageSource("\\\\some\\path\\", "", "", packageSources);
    }

    private void assertContainsPackageSource(final String expectedUrl, final String expectedUser, final String expectedPassword, List<PackageSource> actualPackageSources) {
        assertTrue(CollectionsUtil.contains(actualPackageSources, new Filter<PackageSource>() {
            public boolean accept(@NotNull PackageSource data) {
                return data.getSource().equals(expectedUrl) && expectedUser.equals(data.getUsername()) && expectedPassword.equals(data.getPassword());
            }
        }));
    }

    private void addAuthBuildFeature(String url, String user, String password) {
        if (ReferencesResolverUtil.containsReference(url)) {
            url = PARAMETERS.get(url.substring(1, url.length() - 1));
        }

        final Map<String, String> featureParams = CollectionsUtil.asMap(AUTH_BEAN.getFeedKey(), url, AUTH_BEAN.getUsernameKey(), user, AUTH_BEAN.getPasswordKey(), password);
        AgentBuildFeature feature = m.mock(AgentBuildFeature.class, Integer.toString(myFeatures.size()));
        m.checking(new Expectations(){{
            allowing(feature).getParameters();
            will(returnValue(featureParams));
        }});
        myFeatures.add(feature);
    }

    @NotNull
    private List<PackageSource> getPackageSourcesOfRunningBuild(AgentRunningBuildEx runningBuild) {
        return new ArrayList<PackageSource>(mySources.getGlobalPackageSources(runningBuild));
    }
}
