/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package jetbrains.buildServer.nuget.common;

import jetbrains.buildServer.nuget.feedReader.NuGetPackageInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.TreeSet;

/**
 * Represent list of downloaded dependecies of a build
 * Created by Eugene Petrenko (eugene.petrenko@gmail.com)
 * Date: 19.07.11 11:41
 */
public class PackageDependencies {
  private final Collection<NuGetPackageInfo> myUsedPackages;
  private final Collection<NuGetPackageInfo> myCreatedPackages;
  private final Collection<SourcePackageInfo> myPublishedPackages;

  public PackageDependencies(@NotNull final Collection<? extends NuGetPackageInfo> usedPackages,
                             @NotNull final Collection<? extends NuGetPackageInfo> createdPackages,
                             @NotNull final Collection<? extends SourcePackageInfo> publishedPackages) {
    myUsedPackages = Collections.unmodifiableCollection(new TreeSet<NuGetPackageInfo>(usedPackages));
    myCreatedPackages = Collections.unmodifiableCollection(new TreeSet<NuGetPackageInfo>(createdPackages));
    myPublishedPackages = Collections.unmodifiableCollection(new TreeSet<SourcePackageInfo>(publishedPackages));
  }

  /**
   * @return sorted list of packages that were used in project
   */
  @NotNull
  public Collection<NuGetPackageInfo> getUsedPackages() {
    return myUsedPackages;
  }

  /**
   * @return sorted list of packages that were used in project
   */
  @NotNull
  public Collection<NuGetPackageInfo> getCreatedPackages() {
    return myCreatedPackages;
  }

  /**
   * @return sorted list of packages that were published from a build
   */
  @NotNull
  public Collection<SourcePackageInfo> getPublishedPackages() {
    return myPublishedPackages;
  }

  @Override
  public String toString() {
    return "PackageDependencies{" +
            "myUsedPackaged=" + myUsedPackages +
            ", myCreatedPackages=" + myCreatedPackages +
            ", myPublishedPackages=" + myPublishedPackages +
            '}';
  }
}
