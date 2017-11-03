/*
 * Copyright 2014-2017 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
angular.module('hwk.resourcesModule').controller( 'hwk.resourcesController', ['$scope', '$rootScope', '$q', '$modal', '$window', 'hwk.resourcesService', 'Notifications',
  function ($scope, $rootScope, $q, $modal, $window, resourcesService, Notifications) {
    'use strict';

    console.debug("[Resources] Start: " + new Date());

    var toastError = function (reason) {
      console.debug('[Resources] Backend error ' + new Date());
      console.debug(reason);
      var errorMsg = new Date() + " Status [" + reason.status + "] " + reason.statusText;
      if (reason.status === -1) {
        errorMsg = new Date() + " Hawkular Inventory is not responding. Please review browser console for further details";
      }
      Notifications.error(errorMsg);
    };

    $scope.jsonModal = {
      text: null,
      title: null,
      placeholder: null,
      readOnly: false
    };

    $scope.resource = {
      json: "Click Resource to See Detail",
      metrics: []
    };

    $scope.promBaseUrl = 'http://localhost:9090';

    var updateTree = function () {
      console.debug("[Resources] refresh tree roots at " + new Date());

      // fetch root resources
      var promise1 = resourcesService.Roots().query();
      $q.all([promise1.$promise]).then(function (result) {
        var resources = result[0].results;
        resources.sort(function(a,b) {
          return sortResource(a,b);
        });

        $scope.tree = [];
        for (var i = 0; i < resources.length; ++i) {
          var resource = resources[i];
          var text = '[' + resource.type.id + '] ' + resource.name;
          $scope.tree.push({
            checkable: false,
            lazyLoad: true,
            selectable: true,
            text: text,
            // store entire resource json as custom prop
            resource: resource
          });
        }

        refreshTreeView();

      }, toastError);
    };

    var refreshTreeView = function () {
      $('#resourceTree').treeview({
        data: $scope.tree,
        lazyLoad: function(n,f) {
          expandNode(n, f);
        },

        onNodeSelected: function(event, data) {
          $scope.resource.json = angular.toJson(data.resource,true);
          $scope.resource.metrics = data.resource.metrics;
          $scope.resource.metrics.sort(
            function(a,b) {
              return a.displayName.localeCompare(b.displayName);
            });

          $scope.$apply();
        },

        collapseIcon: "fa fa-angle-down",
        emptyIcon: 'fa',
        expandIcon: "fa fa-angle-right",
        loadingIcon: 'fa fa-clock-o',
        nodeIcon: "fa",
        selectedIcon: 'fa fa-star',
        checkedIcon: 'fa fa-check',
        partiallyCheckedIcon: 'fa fa-minus',
        uncheckedIcon: 'fa fa-close',

        // highlightSelected: true, // not working as expected, maybe need a custom color
        levels: 1
        // showBorder: true // not working as expected
      });
    };

    var expandNode = function (node, expanderFunc) {

      // fetch child resources
      var encodedId = encodeURIComponent(node.resource.id);
      var promise1 = resourcesService.Children(encodedId).query();

      $q.all([promise1.$promise]).then(function (result) {
        var resources = result[0].results;
        resources.sort(function(a,b) {
          return sortResource(a,b);
        });

        var branch = [];
        for (var i = 0; i < resources.length; ++i) {
          var resource = resources[i];
          var text = '[' + resource.type.id + '] ' + resource.name;
          branch.push( { text: text, lazyLoad: true, resource: resource } );
        }
        expanderFunc(branch);
      }, toastError);
    };

    var sortResource = function (r1,r2) {
      return r1.type.id.localeCompare(r2.type.id) || r1.name.localeCompare(r2.name);
    };

    $scope.viewResourceModal = function(resource) {
      $scope.jsonModal.title = 'View Resource';
      $scope.jsonModal.placeholder = 'Resource JSON...';
      $scope.jsonModal.json = angular.toJson(resource,true);
      $scope.jsonModal.readOnly = true;

      var modalInstance = $modal.open({
        templateUrl: 'jsonModal.html',
        backdrop: false, // keep modal up if someone clicks outside of the modal
        controller: function ($scope, $modalInstance, $log, jsonModal) {
          $scope.jsonModal = jsonModal;
          $scope.cancel = function () {
            $modalInstance.dismiss('cancel');
          };
        },
        resolve: {
          jsonModal: function () {
            return $scope.jsonModal;
          }
        }
      });
    };

    $scope.deleteResource = function(resource) {
      if (resource) {
        var promise1 = resourcesService.RemoveResource(resourceId).remove();

        $q.all([promise1.$promise]).then(function (result) {
          console.debug("[Resources] deleteResourceResult=" + result);
        }, toastError);
      }
    };

    $scope.refreshTree = function() {
      updateTree();
    };

    $scope.showMetric = function (metric) {
      if ( !metric.family || !metric.labels ) {
        console.log("Unable to show graph for metric [" + metric.displayName + "]. No family and/or no labels.");
        return;
      }

      // construct the prometheus expression
      var labels = "{";
      var comma = "";
      for (var l in metric.labels) {
        if (metric.labels.hasOwnProperty(l)) {
          labels = labels + comma + l + "='" + metric.labels[l] + "'";
          comma = ",";
        }
      }
      labels += "}";
      var expression = metric.family + labels;
      var url = $scope.promBaseUrl + "/graph?g0.range_input=1h&g0.tab=0&g0.expr=" + encodeURIComponent(expression);
      $window.open(url, '_blank');
    };

    // initial population of root nodes
    updateTree();
  }
]);