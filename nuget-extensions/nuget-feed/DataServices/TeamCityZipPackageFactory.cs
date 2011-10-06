using System;
using System.IO;
using NuGet;
using System.Linq;

namespace JetBrains.TeamCity.NuGet.Feed.DataServices
{
  public static class TeamCityZipPackageFactory
  {
    private static readonly CryptoHashProvider HashProvider = new CryptoHashProvider();

    public static TeamCityPackage LoadPackage(TeamCityPackagesRepo repo, TeamCityPackageSpec spec)
    {
      var fileName = spec.PackageFile;

      var zip = new ZipPackage(fileName);
      byte[] fileBytes = LoadFileBytes(fileName);

      var hash = Convert.ToBase64String(HashProvider.CalculateHash(fileBytes));
      return new TeamCityPackage
               {
                 Id = zip.Id,
                 Version = zip.Version.ToString(),
                 Title = zip.Title,
                 Authors = String.Join(", ", zip.Authors),
                 RequireLicenseAcceptance = zip.RequireLicenseAcceptance,
                 Description = zip.Description,
                 Summary = zip.Summary,
                 ReleaseNotes = zip.ReleaseNotes,
                 Language = zip.Language,
                 Tags = zip.Tags,
                 Dependencies = String.Join("|", zip.Dependencies.Select(d => String.Format("{0}:{1}", d.Id, d.VersionSpec))),
                 IconUrl = ConvertUrl(zip.IconUrl),
                 LicenseUrl = ConvertUrl(zip.LicenseUrl),
                 ProjectUrl = ConvertUrl(zip.ProjectUrl),

                 PackageHash = hash,
                 PackageSize = fileBytes.Length,
                 LastUpdated = File.GetLastWriteTimeUtc(fileName),
                 Published = File.GetCreationTimeUtc(fileName),

                 IsLatestVersion = spec.IsLatest,
                 DownloadUrl = new Uri(new Uri(repo.ServerUrl), spec.DownloadUrl),

                 DownloadCount = -1,
                 Rating = -1,
                 VersionRating = -1,
                 VersionDownloadCount = -1,
                 VersionRatingsCount = -1,
               };
    }

    private static byte[] LoadFileBytes(string fileName)
    {
      byte[] fileBytes;
      using (Stream stream = File.OpenRead(fileName))
      {
        fileBytes = stream.ReadAllBytes();
      }
      return fileBytes;
    }

    private static string ConvertUrl(Uri uri)
    {
      if (uri == null) return null;
      return uri.GetComponents(UriComponents.HttpRequestUrl, UriFormat.Unescaped);
    }
  }
}