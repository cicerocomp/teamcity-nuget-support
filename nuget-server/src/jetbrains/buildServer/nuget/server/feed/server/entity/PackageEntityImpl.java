/****
****
**** THIS CODE IS GENERATED BY jetbrains.buildServer.nuget.tests.server.entity.EntityGenerator$EntityBeanGenerator
**** DO NOT CHANGE!
*****/
package jetbrains.buildServer.nuget.server.feed.server.entity;

import java.util.*;
import java.lang.*;
import org.odata4j.core.*;
import org.odata4j.internal.*;
import org.joda.time.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class PackageEntityImpl  implements PackageEntityV2, OAtomEntity { 


  @NotNull
  public final java.lang.String getId(){ 
    final String v = getValue("Id");
    if (v == null) { 
      return "";
    }
    return v;
  }


  @NotNull
  public final java.lang.String getVersion(){ 
    final String v = getValue("Version");
    if (v == null) { 
      return "";
    }
    return v;
  }


  @Nullable
  public final java.lang.String getAuthors(){ 
    final String v = getValue("Authors");
    return v;
  }


  @Nullable
  public final java.lang.String getCopyright(){ 
    final String v = getValue("Copyright");
    return v;
  }


  @Nullable
  public final java.lang.String getDependencies(){ 
    final String v = getValue("Dependencies");
    return v;
  }


  @Nullable
  public final java.lang.String getDescription(){ 
    final String v = getValue("Description");
    return v;
  }


  @Nullable
  public final java.lang.String getIconUrl(){ 
    final String v = getValue("IconUrl");
    return v;
  }


  @NotNull
  public final java.lang.Boolean getIsLatestVersion(){ 
    final String v = getValue("IsLatestVersion");
    if (v == null) { 
      return false;
    }
    return Boolean.valueOf(v);
  }


  @NotNull
  public final java.lang.Boolean getIsAbsoluteLatestVersion(){ 
    final String v = getValue("IsAbsoluteLatestVersion");
    if (v == null) { 
      return false;
    }
    return Boolean.valueOf(v);
  }


  @NotNull
  public final org.joda.time.LocalDateTime getLastUpdated(){ 
    final String v = getValue("LastUpdated");
    if (v == null) { 
      return new org.joda.time.LocalDateTime();
    }
    return jetbrains.buildServer.nuget.server.feed.server.index.ODataDataFormat.parseDate(v);
  }


  @Nullable
  public final java.lang.String getLanguage(){ 
    final String v = getValue("Language");
    return v;
  }


  @Nullable
  public final java.lang.String getLicenseUrl(){ 
    final String v = getValue("LicenseUrl");
    return v;
  }


  @Nullable
  public final java.lang.String getPackageHash(){ 
    final String v = getValue("PackageHash");
    return v;
  }


  @Nullable
  public final java.lang.String getPackageHashAlgorithm(){ 
    final String v = getValue("PackageHashAlgorithm");
    return v;
  }


  @NotNull
  public final java.lang.Long getPackageSize(){ 
    final String v = getValue("PackageSize");
    if (v == null) { 
      return 0L;
    }
    return Long.parseLong(v);
  }


  @Nullable
  public final java.lang.String getProjectUrl(){ 
    final String v = getValue("ProjectUrl");
    return v;
  }


  @Nullable
  public final java.lang.String getReportAbuseUrl(){ 
    final String v = getValue("ReportAbuseUrl");
    return v;
  }


  @Nullable
  public final java.lang.String getReleaseNotes(){ 
    final String v = getValue("ReleaseNotes");
    return v;
  }


  @NotNull
  public final java.lang.Boolean getRequireLicenseAcceptance(){ 
    final String v = getValue("RequireLicenseAcceptance");
    if (v == null) { 
      return false;
    }
    return Boolean.valueOf(v);
  }


  @Nullable
  public final java.lang.String getTags(){ 
    final String v = getValue("Tags");
    return v;
  }


  @Nullable
  protected abstract String getValue(@NotNull final String key);


  public final java.lang.String getAtomEntityTitle() {
    return getId();
  }


  public final java.lang.String getAtomEntityAuthor() {
    return getAuthors();
  }


  public final org.joda.time.LocalDateTime getAtomEntityUpdated() {
    return getLastUpdated();
  }


  public final java.lang.String getAtomEntitySummary() {
    return getSummary();
  }

}

