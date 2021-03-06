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

package jetbrains.buildServer.nuget.feed.server.olingo.processor;

import jetbrains.buildServer.nuget.feed.server.MetadataConstants;
import jetbrains.buildServer.nuget.feed.server.NuGetAPIVersion;
import jetbrains.buildServer.nuget.feed.server.index.PackagesIndex;
import jetbrains.buildServer.nuget.feed.server.olingo.data.OlingoDataSource;
import jetbrains.buildServer.nuget.feedReader.NuGetPackageAttributes;
import jetbrains.buildServer.util.Action;
import org.apache.olingo.odata2.api.*;
import org.apache.olingo.odata2.api.edm.EdmTargetPath;
import org.apache.olingo.odata2.api.edm.FullQualifiedName;
import org.apache.olingo.odata2.api.edm.provider.*;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.apache.olingo.odata2.api.processor.ODataContext;
import org.apache.olingo.odata2.core.edm.provider.EdmxProvider;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Creates a new NuGet service.
 */
public class NuGetServiceFactory extends ODataServiceFactory {

  private final OlingoDataSource myDataSource;
  private static final Map<String, Action<Property>> PROPERTY_CONFIGS;
  private static final Map<String, EdmxProvider> EDMX_PROVIDERS;

  public NuGetServiceFactory(OlingoDataSource dataSource) {
    myDataSource = dataSource;
  }

  @Override
  public ODataService createService(final ODataContext context) throws ODataException {
    final EdmxProvider edmxProvider = EDMX_PROVIDERS.get(NuGetAPIVersion.getVersionToUse());
    return createODataSingleProcessorService(edmxProvider, new NuGetPackagesProcessor(myDataSource));
  }

  private static EdmxProvider getEdmProvider(final String version) throws ODataException {
    final String metadataPath = String.format("/feed-metadata/NuGet-%s.xml", version);

    final InputStream inputStream = NuGetServiceFactory.class.getResourceAsStream(metadataPath);
    final EdmxProvider provider = new EdmxProvider().parse(inputStream, true);

    // Customize download link and content type
    final String name = version + MetadataConstants.ENTITY_TYPE_NAME;
    final EntityType packageType = provider.getEntityType(new FullQualifiedName(MetadataConstants.ENTITY_NAMESPACE, name));
    packageType.setMapping(new Mapping()
            .setMediaResourceSourceKey(PackagesIndex.TEAMCITY_DOWNLOAD_URL)
            .setMediaResourceMimeTypeKey(MetadataConstants.CONTENT_TYPE));

    // Customize properties
    for (Property property : packageType.getProperties()) {
      final Action<Property> config = PROPERTY_CONFIGS.get(property.getName());
      if (config != null) {
        config.apply(property);
      }
    }

    return provider;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends ODataCallback> T getCallback(final Class<T> callbackInterface) {
    return (T) (callbackInterface.isAssignableFrom(NuGetErrorCallback.class) ?
            new NuGetErrorCallback() : callbackInterface.isAssignableFrom(ODataDebugCallback.class) ?
            new ScenarioDebugCallback() : super.getCallback(callbackInterface));
  }

  private final class ScenarioDebugCallback implements ODataDebugCallback {
    @Override
    public boolean isDebugEnabled() {
      return true;
    }
  }

  static {
    PROPERTY_CONFIGS = new HashMap<>();
    PROPERTY_CONFIGS.put(NuGetPackageAttributes.ID, property -> property.setCustomizableFeedMappings(
            new CustomizableFeedMappings().setFcTargetPath(EdmTargetPath.SYNDICATION_TITLE)));
    PROPERTY_CONFIGS.put(NuGetPackageAttributes.LAST_UPDATED, property -> property.setCustomizableFeedMappings(
            new CustomizableFeedMappings().setFcTargetPath(EdmTargetPath.SYNDICATION_UPDATED)));
    PROPERTY_CONFIGS.put(NuGetPackageAttributes.AUTHORS, property -> property.setCustomizableFeedMappings(
            new CustomizableFeedMappings().setFcTargetPath(EdmTargetPath.SYNDICATION_AUTHORNAME)));

    EDMX_PROVIDERS = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    try {
      EDMX_PROVIDERS.put(NuGetAPIVersion.V1, getEdmProvider(NuGetAPIVersion.V1));
      EDMX_PROVIDERS.put(NuGetAPIVersion.V2, getEdmProvider(NuGetAPIVersion.V2));
    } catch (ODataException e) {
      throw new RuntimeException(e);
    }
  }
}
