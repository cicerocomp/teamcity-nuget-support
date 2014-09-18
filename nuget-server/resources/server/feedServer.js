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

if (!BS) BS = {};
if (!BS.NuGet) BS.NuGet = {};

BS.NuGet.FeedServer = {
  refreshStatus : function () {
    $('nugetServerStatus').refresh();
  },

  registerStatusRefresh : function () {
    var that = this;
    setTimeout(function () {
      that.refreshStatus();
      that.registerStatusRefresh();
    }, 1000);
  },

  _request : function(el, enabled) {
    var url = $j(el).closest("div").data("url");

    $j(el).closest("div").find("span:last").html(BS.loadingIcon);
    $j(el).prop("disabled", "true");

    BS.ajaxRequest(url, {
      method : "POST",
      parameters : {
        'nuget-feed-enabled' : enabled
      },
      onComplete : function() {
        $j(el).closest("div").find("span:last").html('');
        $('nugetEnableDisable').refresh();
      }
    });

  },

  disableFeedServer : function(el) {
    BS.NuGet.FeedServer._request(el, false);
  },

  enableFeedServer : function(el) {
    BS.NuGet.FeedServer._request(el, true);
  },

  collectStatistics : function(url){
    $j('#update_progress').html('<div class="resultsTitle"><i class="icon-refresh icon-spin"></i> Collecting NuGet server statistics...</div>');
    BS.ajaxUpdater($('statisticsDataContainer'), url, {
      method: 'get',
      onComplete: function() {
        $j('#update_progress').empty();
      }
    });
    return false;
  }
};
