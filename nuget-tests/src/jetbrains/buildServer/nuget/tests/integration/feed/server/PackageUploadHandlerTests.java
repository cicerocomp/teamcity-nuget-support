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

package jetbrains.buildServer.nuget.tests.integration.feed.server;

import jetbrains.buildServer.BuildProblemData;
import jetbrains.buildServer.controllers.MockResponse;
import jetbrains.buildServer.nuget.feed.server.cache.ResponseCacheReset;
import jetbrains.buildServer.nuget.feed.server.controllers.PackageUploadHandler;
import jetbrains.buildServer.nuget.feed.server.index.PackageAnalyzer;
import jetbrains.buildServer.nuget.feed.server.index.impl.NuGetArtifactsMetadataProvider;
import jetbrains.buildServer.nuget.feed.server.index.impl.NuGetPackageAnalyzer;
import jetbrains.buildServer.nuget.feedReader.NuGetPackageAttributes;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.artifacts.limits.ArtifactsUploadLimit;
import jetbrains.buildServer.serverSide.metadata.MetadataStorage;
import jetbrains.buildServer.serverSide.metadata.impl.metadata.BuildMetadataEntryImpl;
import jetbrains.buildServer.serverSide.metadata.impl.metadata.EntryImpl;
import jetbrains.buildServer.serverSide.metadata.impl.metadata.EntryKey;
import jetbrains.buildServer.util.CollectionsUtil;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

/**
 * @author Dmitry.Tretyakov
 *         Date: 22.12.2016
 *         Time: 16:37
 */
@Test
public class PackageUploadHandlerTests {

  private static final String SERVLET_PATH = "/app/nuget/v1/FeedService.svc";
  private static final String REQUEST_BODY = "--3576595b-8e57-4d70-91bb-701d5aab54ea\r\n" +
    "Content-Type: application/octet-stream\r\n" +
    "Content-Disposition: form-data; name=package; filename=package.nupkg; filename*=utf-8''package.nupkg\r\n" +
    "\r\n" +
    "Hello\r\n" +
    "--3576595b-8e57-4d70-91bb-701d5aab54ea--\r\n";

  public void testNonMultipartRequest() throws Exception {
    Mockery m = new Mockery();
    RunningBuildsCollection runningBuilds = m.mock(RunningBuildsCollection.class);
    MetadataStorage metadataStorage = m.mock(MetadataStorage.class);
    ResponseCacheReset cacheReset = m.mock(ResponseCacheReset.class);
    PackageAnalyzer packageAnalyzer = new NuGetPackageAnalyzer();

    PackageUploadHandler handler = new PackageUploadHandler(runningBuilds, metadataStorage,
      packageAnalyzer, cacheReset);
    RequestWrapper request = new RequestWrapper(SERVLET_PATH, SERVLET_PATH + "/");
    ResponseWrapper response = new ResponseWrapper(new MockResponse());

    request.setContentType("form/url-encoded");

    handler.handleRequest(request, response);

    Assert.assertEquals(response.getStatus(), 400);
  }

  public void testUploadWithoutApiKey() throws Exception {
    Mockery m = new Mockery();
    RunningBuildsCollection runningBuilds = m.mock(RunningBuildsCollection.class);
    MetadataStorage metadataStorage = m.mock(MetadataStorage.class);
    PackageAnalyzer packageAnalyzer = new NuGetPackageAnalyzer();
    ResponseCacheReset cacheReset = m.mock(ResponseCacheReset.class);

    PackageUploadHandler handler = new PackageUploadHandler(runningBuilds, metadataStorage,
      packageAnalyzer, cacheReset);
    RequestWrapper request = new RequestWrapper(SERVLET_PATH, SERVLET_PATH + "/");
    ResponseWrapper response = new ResponseWrapper(new MockResponse());

    request.setContentType("multipart/form-data; boundary=\"3576595b-8e57-4d70-91bb-701d5aab54ea\"");
    request.setBody("".getBytes());

    handler.handleRequest(request, response);

    Assert.assertEquals(response.getStatus(), 403);
  }

  public void testUploadWithInvalidApiKey() throws Exception {
    Mockery m = new Mockery();
    RunningBuildsCollection runningBuilds = m.mock(RunningBuildsCollection.class);
    MetadataStorage metadataStorage = m.mock(MetadataStorage.class);
    PackageAnalyzer packageAnalyzer = new NuGetPackageAnalyzer();
    ResponseCacheReset cacheReset = m.mock(ResponseCacheReset.class);

    PackageUploadHandler handler = new PackageUploadHandler(runningBuilds, metadataStorage,
      packageAnalyzer, cacheReset);
    RequestWrapper request = new RequestWrapper(SERVLET_PATH, SERVLET_PATH + "/");
    ResponseWrapper response = new ResponseWrapper(new MockResponse());

    request.setContentType("multipart/form-data; boundary=\"3576595b-8e57-4d70-91bb-701d5aab54ea\"");
    request.setHeader("X-Nuget-ApiKey", "aaa");
    request.setBody("".getBytes());

    handler.handleRequest(request, response);

    Assert.assertEquals(response.getStatus(), 403);
  }

  public void testUploadWithoutRunningBuild() throws Exception {
    Mockery m = new Mockery();
    RunningBuildsCollection runningBuilds = m.mock(RunningBuildsCollection.class);
    MetadataStorage metadataStorage = m.mock(MetadataStorage.class);
    PackageAnalyzer packageAnalyzer = new NuGetPackageAnalyzer();
    ResponseCacheReset cacheReset = m.mock(ResponseCacheReset.class);

    m.checking(new Expectations() {{
      oneOf(runningBuilds).findRunningBuildById(3641L);
      will(returnValue(null));
    }});

    PackageUploadHandler handler = new PackageUploadHandler(runningBuilds, metadataStorage,
      packageAnalyzer, cacheReset);
    RequestWrapper request = new RequestWrapper(SERVLET_PATH, SERVLET_PATH + "/");
    ResponseWrapper response = new ResponseWrapper(new MockResponse());

    request.setContentType("multipart/form-data; boundary=\"3576595b-8e57-4d70-91bb-701d5aab54ea\"");
    request.setHeader("X-Nuget-ApiKey", "zxxbe88b7ae8ef923157da5d6c0a4328e5bbb66c5f55a62469ba6d36bedf0ebc0ef");
    request.setBody("".getBytes());

    handler.handleRequest(request, response);

    Assert.assertEquals(response.getStatus(), 403);
    m.assertIsSatisfied();
  }

  public void testUploadInvalidFile() throws Exception {
    Mockery m = new Mockery();
    RunningBuildsCollection runningBuilds = m.mock(RunningBuildsCollection.class);
    RunningBuildEx build = m.mock(RunningBuildEx.class);
    MetadataStorage metadataStorage = m.mock(MetadataStorage.class);
    PackageAnalyzer packageAnalyzer = new NuGetPackageAnalyzer();
    ResponseCacheReset cacheReset = m.mock(ResponseCacheReset.class);

    m.checking(new Expectations() {{
      oneOf(runningBuilds).findRunningBuildById(3641L);
      will(returnValue(build));
    }});

    PackageUploadHandler handler = new PackageUploadHandler(runningBuilds, metadataStorage,
      packageAnalyzer, cacheReset);
    RequestWrapper request = new RequestWrapper(SERVLET_PATH, SERVLET_PATH + "/");
    ResponseWrapper response = new ResponseWrapper(new MockResponse());

    request.setContentType("multipart/form-data; boundary=\"3576595b-8e57-4d70-91bb-701d5aab54ea\"");
    request.setHeader("X-Nuget-ApiKey", "zxxbe88b7ae8ef923157da5d6c0a4328e5bbb66c5f55a62469ba6d36bedf0ebc0ef");
    request.setBody((
      "--3576595b-8e57-4d70-91bb-701d5aab54ea\r\n" +
        "Content-Type: application/octet-stream\r\n" +
        "Content-Disposition: form-data; name=file; filename=file.ext; filename*=utf-8''file.ext\r\n" +
        "\r\n" +
        "Hello\r\n" +
        "--3576595b-8e57-4d70-91bb-701d5aab54ea--\r\n").getBytes());

    handler.handleRequest(request, response);

    Assert.assertEquals(response.getStatus(), 400);
    m.assertIsSatisfied();
  }

  public void testUploadWithExceedingArtifactLimit() throws Exception {
    Mockery m = new Mockery();
    RunningBuildsCollection runningBuilds = m.mock(RunningBuildsCollection.class);
    RunningBuildEx build = m.mock(RunningBuildEx.class);
    MetadataStorage metadataStorage = m.mock(MetadataStorage.class);
    PackageAnalyzer packageAnalyzer = new NuGetPackageAnalyzer();
    ResponseCacheReset cacheReset = m.mock(ResponseCacheReset.class);

    m.checking(new Expectations() {{
      oneOf(runningBuilds).findRunningBuildById(3641L);
      will(returnValue(build));
      oneOf(build).getArtifactsLimit();
      will(returnValue(new ArtifactsUploadLimit(1L, null)));
      oneOf(build).addBuildProblem(with(any(BuildProblemData.class)));
    }});

    PackageUploadHandler handler = new PackageUploadHandler(runningBuilds, metadataStorage,
      packageAnalyzer, cacheReset);
    RequestWrapper request = new RequestWrapper(SERVLET_PATH, SERVLET_PATH + "/");
    ResponseWrapper response = new ResponseWrapper(new MockResponse());

    request.setContentType("multipart/form-data; boundary=\"3576595b-8e57-4d70-91bb-701d5aab54ea\"");
    request.setHeader("X-Nuget-ApiKey", "zxxbe88b7ae8ef923157da5d6c0a4328e5bbb66c5f55a62469ba6d36bedf0ebc0ef");
    request.setBody(REQUEST_BODY.getBytes());

    handler.handleRequest(request, response);

    Assert.assertEquals(response.getStatus(), 413);
    m.assertIsSatisfied();
  }

  public void testUploadWithExceedingTotalLimit() throws Exception {
    Mockery m = new Mockery();
    RunningBuildsCollection runningBuilds = m.mock(RunningBuildsCollection.class);
    RunningBuildEx build = m.mock(RunningBuildEx.class);
    MetadataStorage metadataStorage = m.mock(MetadataStorage.class);
    PackageAnalyzer packageAnalyzer = new NuGetPackageAnalyzer();
    ResponseCacheReset cacheReset = m.mock(ResponseCacheReset.class);

    m.checking(new Expectations() {{
      oneOf(runningBuilds).findRunningBuildById(3641L);
      will(returnValue(build));
      oneOf(build).getArtifactsLimit();
      will(returnValue(new ArtifactsUploadLimit(null, 0L)));
      oneOf(build).addBuildProblem(with(any(BuildProblemData.class)));
    }});

    PackageUploadHandler handler = new PackageUploadHandler(runningBuilds, metadataStorage,
      packageAnalyzer, cacheReset);
    RequestWrapper request = new RequestWrapper(SERVLET_PATH, SERVLET_PATH + "/");
    ResponseWrapper response = new ResponseWrapper(new MockResponse());

    request.setContentType("multipart/form-data; boundary=\"3576595b-8e57-4d70-91bb-701d5aab54ea\"");
    request.setHeader("X-Nuget-ApiKey", "zxxbe88b7ae8ef923157da5d6c0a4328e5bbb66c5f55a62469ba6d36bedf0ebc0ef");
    request.setBody(REQUEST_BODY.getBytes());

    handler.handleRequest(request, response);

    Assert.assertEquals(response.getStatus(), 413);
    m.assertIsSatisfied();
  }

  public void testUploadWithTotalLimitLessThanArtifact() throws Exception {
    Mockery m = new Mockery();
    RunningBuildsCollection runningBuilds = m.mock(RunningBuildsCollection.class);
    RunningBuildEx build = m.mock(RunningBuildEx.class);
    MetadataStorage metadataStorage = m.mock(MetadataStorage.class);
    PackageAnalyzer packageAnalyzer = new NuGetPackageAnalyzer();
    ResponseCacheReset cacheReset = m.mock(ResponseCacheReset.class);

    m.checking(new Expectations() {{
      oneOf(runningBuilds).findRunningBuildById(3641L);
      will(returnValue(build));
      oneOf(build).getArtifactsLimit();
      will(returnValue(new ArtifactsUploadLimit(1000000L, 1L)));
      oneOf(build).addBuildProblem(with(any(BuildProblemData.class)));
    }});

    PackageUploadHandler handler = new PackageUploadHandler(runningBuilds, metadataStorage,
      packageAnalyzer, cacheReset);
    RequestWrapper request = new RequestWrapper(SERVLET_PATH, SERVLET_PATH + "/");
    ResponseWrapper response = new ResponseWrapper(new MockResponse());

    request.setContentType("multipart/form-data; boundary=\"3576595b-8e57-4d70-91bb-701d5aab54ea\"");
    request.setHeader("X-Nuget-ApiKey", "zxxbe88b7ae8ef923157da5d6c0a4328e5bbb66c5f55a62469ba6d36bedf0ebc0ef");
    request.setBody(REQUEST_BODY.getBytes());

    handler.handleRequest(request, response);

    Assert.assertEquals(response.getStatus(), 413);
    m.assertIsSatisfied();
  }

  public void testUploadInvalidPackage() throws Exception {
    Mockery m = new Mockery();
    RunningBuildsCollection runningBuilds = m.mock(RunningBuildsCollection.class);
    RunningBuildEx build = m.mock(RunningBuildEx.class);
    MetadataStorage metadataStorage = m.mock(MetadataStorage.class);
    PackageAnalyzer packageAnalyzer = new NuGetPackageAnalyzer();
    ResponseCacheReset cacheReset = m.mock(ResponseCacheReset.class);

    m.checking(new Expectations() {{
      oneOf(runningBuilds).findRunningBuildById(3641L);
      will(returnValue(build));
      oneOf(build).getArtifactsLimit();
      will(returnValue(ArtifactsUploadLimit.UNLIMITED));
    }});

    PackageUploadHandler handler = new PackageUploadHandler(runningBuilds, metadataStorage,
      packageAnalyzer, cacheReset);
    RequestWrapper request = new RequestWrapper(SERVLET_PATH, SERVLET_PATH + "/");
    ResponseWrapper response = new ResponseWrapper(new MockResponse());

    request.setContentType("multipart/form-data; boundary=\"3576595b-8e57-4d70-91bb-701d5aab54ea\"");
    request.setHeader("X-Nuget-ApiKey", "zxxbe88b7ae8ef923157da5d6c0a4328e5bbb66c5f55a62469ba6d36bedf0ebc0ef");
    request.setBody(REQUEST_BODY.getBytes());

    handler.handleRequest(request, response);

    Assert.assertEquals(response.getStatus(), 400);
    m.assertIsSatisfied();
  }

  public void testInternalFailureWhileUpload() throws Exception {
    Mockery m = new Mockery();
    RunningBuildsCollection runningBuilds = m.mock(RunningBuildsCollection.class);
    RunningBuildEx build = m.mock(RunningBuildEx.class);
    MetadataStorage metadataStorage = m.mock(MetadataStorage.class);
    PackageAnalyzer packageAnalyzer = m.mock(PackageAnalyzer.class);
    ResponseCacheReset cacheReset = m.mock(ResponseCacheReset.class);
    Map<String, String> metadata = CollectionsUtil.asMap(
      NuGetPackageAttributes.ID, "Id",
      NuGetPackageAttributes.NORMALIZED_VERSION, "1.0.0");

    m.checking(new Expectations() {{
      oneOf(runningBuilds).findRunningBuildById(3641L);
      will(returnValue(build));
      one(build).getArtifactsLimit();
      will(returnValue(ArtifactsUploadLimit.UNLIMITED));
      oneOf(packageAnalyzer).analyzePackage(with(any(InputStream.class)));
      will(returnValue(metadata));
      oneOf(packageAnalyzer).getSha512Hash(with(any(InputStream.class)));
      will(returnValue("hash"));
      oneOf(build).getBuildTypeId();
      will(returnValue("type"));
      oneOf(build).getBuildId();
      will(returnValue(3641L));
      oneOf(metadataStorage).getEntriesByKey(
        NuGetArtifactsMetadataProvider.NUGET_PROVIDER_ID,
        "id.1.0.0");
      will(returnIterator());
      oneOf(build).publishArtifact(with(any(String.class)), with(any(InputStream.class)));
      will(throwException(new IOException("Failure")));
    }});

    PackageUploadHandler handler = new PackageUploadHandler(runningBuilds, metadataStorage,
      packageAnalyzer, cacheReset);
    RequestWrapper request = new RequestWrapper(SERVLET_PATH, SERVLET_PATH + "/");
    ResponseWrapper response = new ResponseWrapper(new MockResponse());

    request.setContentType("multipart/form-data; boundary=\"3576595b-8e57-4d70-91bb-701d5aab54ea\"");
    request.setHeader("X-Nuget-ApiKey", "zxxbe88b7ae8ef923157da5d6c0a4328e5bbb66c5f55a62469ba6d36bedf0ebc0ef");
    request.setBody(REQUEST_BODY.getBytes());

    handler.handleRequest(request, response);

    Assert.assertEquals(response.getStatus(), 500);
    m.assertIsSatisfied();
  }

  public void testUploadAlreadyPublishedPackage() throws Exception {
    Mockery m = new Mockery();
    RunningBuildsCollection runningBuilds = m.mock(RunningBuildsCollection.class);
    RunningBuildEx build = m.mock(RunningBuildEx.class);
    MetadataStorage metadataStorage = m.mock(MetadataStorage.class);
    PackageAnalyzer packageAnalyzer = m.mock(PackageAnalyzer.class);
    ResponseCacheReset cacheReset = m.mock(ResponseCacheReset.class);
    Map<String, String> metadata = CollectionsUtil.asMap(
      NuGetPackageAttributes.ID, "Id",
      NuGetPackageAttributes.NORMALIZED_VERSION, "1.0.0");

    m.checking(new Expectations() {{
      oneOf(runningBuilds).findRunningBuildById(3641L);
      will(returnValue(build));
      one(build).getArtifactsLimit();
      will(returnValue(ArtifactsUploadLimit.UNLIMITED));
      oneOf(packageAnalyzer).analyzePackage(with(any(InputStream.class)));
      will(returnValue(metadata));
      oneOf(metadataStorage).getEntriesByKey(
        NuGetArtifactsMetadataProvider.NUGET_PROVIDER_ID,
        "id.1.0.0"
      );
      will(returnIterator(new BuildMetadataEntryImpl(
        new EntryKey(1, "key"),
        new EntryImpl(true, Collections.emptyMap()))
      ));
    }});

    PackageUploadHandler handler = new PackageUploadHandler(runningBuilds, metadataStorage,
      packageAnalyzer, cacheReset);
    RequestWrapper request = new RequestWrapper(SERVLET_PATH, SERVLET_PATH + "/");
    ResponseWrapper response = new ResponseWrapper(new MockResponse());

    request.setContentType("multipart/form-data; boundary=\"3576595b-8e57-4d70-91bb-701d5aab54ea\"");
    request.setHeader("X-Nuget-ApiKey", "zxxbe88b7ae8ef923157da5d6c0a4328e5bbb66c5f55a62469ba6d36bedf0ebc0ef");
    request.setBody(REQUEST_BODY.getBytes());

    handler.handleRequest(request, response);

    Assert.assertEquals(response.getStatus(), 409);
    m.assertIsSatisfied();
  }

  public void testOverrideAlreadyPublishedPackage() throws Exception {
    Mockery m = new Mockery();
    RunningBuildsCollection runningBuilds = m.mock(RunningBuildsCollection.class);
    RunningBuildEx build = m.mock(RunningBuildEx.class);
    MetadataStorage metadataStorage = m.mock(MetadataStorage.class);
    PackageAnalyzer packageAnalyzer = m.mock(PackageAnalyzer.class);
    ResponseCacheReset cacheReset = m.mock(ResponseCacheReset.class);
    Map<String, String> metadata = CollectionsUtil.asMap(
      NuGetPackageAttributes.ID, "Id",
      NuGetPackageAttributes.NORMALIZED_VERSION, "1.0.0");

    m.checking(new Expectations() {{
      oneOf(runningBuilds).findRunningBuildById(3641L);
      will(returnValue(build));
      one(build).getArtifactsLimit();
      will(returnValue(ArtifactsUploadLimit.UNLIMITED));
      oneOf(packageAnalyzer).analyzePackage(with(any(InputStream.class)));
      will(returnValue(metadata));
      oneOf(packageAnalyzer).getSha512Hash(with(any(InputStream.class)));
      will(returnValue("hash"));
      oneOf(build).getBuildTypeId();
      will(returnValue("type"));
      oneOf(build).getBuildId();
      will(returnValue(3641L));
      oneOf(build).isPersonal();
      will(returnValue(false));
      oneOf(build).publishArtifact(with(any(String.class)), with(any(InputStream.class)));
      oneOf(metadataStorage).addBuildEntry(with(any(Long.class)), with(any(String.class)),
        with(any(String.class)), with(any(Map.class)), with(any(Boolean.class)));
      oneOf(cacheReset).resetCache();
    }});

    PackageUploadHandler handler = new PackageUploadHandler(runningBuilds, metadataStorage,
      packageAnalyzer, cacheReset);
    RequestWrapper request = new RequestWrapper(SERVLET_PATH, SERVLET_PATH + "/");
    ResponseWrapper response = new ResponseWrapper(new MockResponse());

    request.setContentType("multipart/form-data; boundary=\"3576595b-8e57-4d70-91bb-701d5aab54ea\"");
    request.setHeader("X-Nuget-ApiKey", "zxxbe88b7ae8ef923157da5d6c0a4328e5bbb66c5f55a62469ba6d36bedf0ebc0ef");
    request.setBody(REQUEST_BODY.getBytes());
    request.setParameter("replace", "true");

    handler.handleRequest(request, response);

    m.assertIsSatisfied();
  }

  public void testUploadValidPackage() throws Exception {
    Mockery m = new Mockery();
    RunningBuildsCollection runningBuilds = m.mock(RunningBuildsCollection.class);
    RunningBuildEx build = m.mock(RunningBuildEx.class);
    MetadataStorage metadataStorage = m.mock(MetadataStorage.class);
    PackageAnalyzer packageAnalyzer = m.mock(PackageAnalyzer.class);
    ResponseCacheReset cacheReset = m.mock(ResponseCacheReset.class);
    Map<String, String> metadata = CollectionsUtil.asMap(
      NuGetPackageAttributes.ID, "Id",
      NuGetPackageAttributes.NORMALIZED_VERSION, "1.0.0");

    m.checking(new Expectations() {{
      oneOf(runningBuilds).findRunningBuildById(3641L);
      will(returnValue(build));
      one(build).getArtifactsLimit();
      will(returnValue(ArtifactsUploadLimit.UNLIMITED));
      oneOf(packageAnalyzer).analyzePackage(with(any(InputStream.class)));
      will(returnValue(metadata));
      oneOf(packageAnalyzer).getSha512Hash(with(any(InputStream.class)));
      will(returnValue("hash"));
      oneOf(build).getBuildTypeId();
      will(returnValue("type"));
      oneOf(build).getBuildId();
      will(returnValue(3641L));
      oneOf(build).isPersonal();
      will(returnValue(false));
      oneOf(metadataStorage).getEntriesByKey(
        NuGetArtifactsMetadataProvider.NUGET_PROVIDER_ID,
        "id.1.0.0");
      will(returnIterator());
      oneOf(build).publishArtifact(with(any(String.class)), with(any(InputStream.class)));
      oneOf(metadataStorage).addBuildEntry(with(any(Long.class)), with(any(String.class)),
        with(any(String.class)), with(any(Map.class)), with(any(Boolean.class)));
      oneOf(cacheReset).resetCache();
    }});

    PackageUploadHandler handler = new PackageUploadHandler(runningBuilds, metadataStorage,
      packageAnalyzer, cacheReset);
    RequestWrapper request = new RequestWrapper(SERVLET_PATH, SERVLET_PATH + "/");
    ResponseWrapper response = new ResponseWrapper(new MockResponse());

    request.setContentType("multipart/form-data; boundary=\"3576595b-8e57-4d70-91bb-701d5aab54ea\"");
    request.setHeader("X-Nuget-ApiKey", "zxxbe88b7ae8ef923157da5d6c0a4328e5bbb66c5f55a62469ba6d36bedf0ebc0ef");
    request.setBody(REQUEST_BODY.getBytes());

    handler.handleRequest(request, response);

    m.assertIsSatisfied();
  }

  public void testUploadWithoutMaxArtifactSize() throws Exception {
    Mockery m = new Mockery();
    RunningBuildsCollection runningBuilds = m.mock(RunningBuildsCollection.class);
    RunningBuildEx build = m.mock(RunningBuildEx.class);
    MetadataStorage metadataStorage = m.mock(MetadataStorage.class);
    PackageAnalyzer packageAnalyzer = m.mock(PackageAnalyzer.class);
    ResponseCacheReset cacheReset = m.mock(ResponseCacheReset.class);
    Map<String, String> metadata = CollectionsUtil.asMap(
      NuGetPackageAttributes.ID, "Id",
      NuGetPackageAttributes.NORMALIZED_VERSION, "1.0.0");

    m.checking(new Expectations() {{
      oneOf(runningBuilds).findRunningBuildById(3641L);
      will(returnValue(build));
      one(build).getArtifactsLimit();
      will(returnValue(new ArtifactsUploadLimit(-1L, null)));
      oneOf(packageAnalyzer).analyzePackage(with(any(InputStream.class)));
      will(returnValue(metadata));
      oneOf(packageAnalyzer).getSha512Hash(with(any(InputStream.class)));
      will(returnValue("hash"));
      oneOf(build).getBuildTypeId();
      will(returnValue("type"));
      oneOf(build).getBuildId();
      will(returnValue(3641L));
      oneOf(build).isPersonal();
      will(returnValue(false));
      oneOf(metadataStorage).getEntriesByKey(
        NuGetArtifactsMetadataProvider.NUGET_PROVIDER_ID,
        "id.1.0.0");
      will(returnIterator());
      oneOf(build).publishArtifact(with(any(String.class)), with(any(InputStream.class)));
      oneOf(metadataStorage).addBuildEntry(with(any(Long.class)), with(any(String.class)),
        with(any(String.class)), with(any(Map.class)), with(any(Boolean.class)));
      oneOf(cacheReset).resetCache();
    }});

    PackageUploadHandler handler = new PackageUploadHandler(runningBuilds, metadataStorage,
      packageAnalyzer, cacheReset);
    RequestWrapper request = new RequestWrapper(SERVLET_PATH, SERVLET_PATH + "/");
    ResponseWrapper response = new ResponseWrapper(new MockResponse());

    request.setContentType("multipart/form-data; boundary=\"3576595b-8e57-4d70-91bb-701d5aab54ea\"");
    request.setHeader("X-Nuget-ApiKey", "zxxbe88b7ae8ef923157da5d6c0a4328e5bbb66c5f55a62469ba6d36bedf0ebc0ef");
    request.setBody(REQUEST_BODY.getBytes());

    handler.handleRequest(request, response);

    m.assertIsSatisfied();
  }
}
