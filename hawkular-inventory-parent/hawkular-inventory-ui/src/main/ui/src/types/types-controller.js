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
angular.module('hwk.typesModule').controller( 'hwk.typesController', ['$scope', '$rootScope', '$resource', '$window', '$interval', '$q', '$modal', 'hwk.typesService', 'hwk.filterService', 'Notifications',
  function ($scope, $rootScope, $resource, $window, $interval, $q, $modal, typesService, filterService, Notifications) {
    'use strict';

    console.debug("[Types] Start: " + new Date());

    $scope.jsonModal = {
      text: null,
      title: null,
      placeholder: null,
      readOnly: false
    };

    $scope.filter = {
      name: null,
      ignoreCase: false
    };

    var toastError = function (reason) {
      console.debug('[Types] Backend error ' + new Date());
      console.debug(reason);
      var errorMsg = new Date() + " Status [" + reason.status + "] " + reason.statusText;
      if (reason.status === -1) {
        errorMsg = new Date() + " Hawkular Inventory is not responding. Please review browser console for further details";
      }
      Notifications.error(errorMsg);
    };

    var updateTypes = function () {
      var typesPromise = typesService.Types().query();
      $q.all([typesPromise.$promise]).then(function(results) {
        var types = results[0].results;
        types.sort(function(a,b) {
          return a.id.localeCompare(b.id);
        });
        $scope.allTypes = types;
        $scope.filteredTypes = filterTypes();
        console.debug("[Types] Types query returned [" + $scope.allTypes.length + "] types");
        console.debug("[Types] Types query returned [" + $scope.filteredTypes.length + "] filtered types");
      }, toastError);
    };

    $scope.deleteType = function (typeId) {
      var encodedId = encodeURIComponent(typeId);
      var typesPromise = typesService.Delete(typeId).remove();
      $q.all([typesPromise.$promise]).then(function(results) {
        updateTypes();
      }, toastError);
    };

    $scope.viewType = function(type) {
      $scope.jsonModal.title = 'View Resource Type';
      $scope.jsonModal.placeholder = 'Resource Type JSON...';
      $scope.jsonModal.json = angular.toJson(type,true);
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

    $scope.refreshTypes = function () {
      updateTypes();
    };

    $scope.refreshFilter = function () {
      console.log("Filter=" + $scope.filter.name);
      $scope.filteredTypes = filterTypes();
    };

    var filterTypes = function(types) {
      var filteredTypes = $scope.allTypes;

      if ( $scope.filter.name && $scope.filter.name.length > 0 ) {
        filteredTypes = [];
        var f = $scope.filter.ignoreCase ? $scope.filter.name.toLowerCase() : $scope.filter.name;
        for ( var i = 0; i < $scope.allTypes.length; ++i ) {
          var t = $scope.allTypes[i];
          var id = $scope.filter.ignoreCase ? t.id.toLowerCase() : t.id;
          if (id.includes(f)) {
            filteredTypes.push(t);
          }
        }
      }
      return filteredTypes;
    };

    $scope.$on('ngRepeatDoneTypes', function(ngRepeatDoneEvent) {
      // row checkbox selection
      //$("input[type='checkbox']").change(function (e) {
      //  if ($(this).is(":checked")) {
      //    $(this).closest('.list-group-item').addClass("active");
      //  } else {
      //    $(this).closest('.list-group-item').removeClass("active");
      //  }
      //});

      // toggle dropdown menu
      $(".list-view-pf-actions").on("show.bs.dropdown", function () {
        var $this = $(this);
        var $dropdown = $this.find(".dropdown");
        var space = $(window).height() - $dropdown[0].getBoundingClientRect().top - $this.find(".dropdown-menu").outerHeight(true);
        $dropdown.toggleClass("dropup", space < 10);
      });

      // compound expansion
      $(".list-view-pf-expand").on("click", function () {
        var $this = $(this);
        var $heading = $(this).parents(".list-group-item");
        var $subPanels = $heading.find(".list-group-item-container");
        var index = $heading.find(".list-view-pf-expand").index(this);

        // remove all active status
        $heading.find(".list-view-pf-expand.active").find(".fa-angle-right").removeClass("fa-angle-down")
          .end().removeClass("active")
          .end().removeClass("list-view-pf-expand-active");
        // add active to the clicked item
        $(this).addClass("active")
          .parents(".list-group-item").addClass("list-view-pf-expand-active")
          .end().find(".fa-angle-right").addClass("fa-angle-down");
        // check if it needs to hide
        if ($subPanels.eq(index).hasClass("hidden")) {
          $heading.find(".list-group-item-container:visible").addClass("hidden");
          $subPanels.eq(index).removeClass("hidden");
        } else {
          $subPanels.eq(index).addClass("hidden");
          $heading.find(".list-view-pf-expand.active").find(".fa-angle-right").removeClass("fa-angle-down")
            .end().removeClass("active")
            .end().removeClass("list-view-pf-expand-active");
        }
      });

      // click close button to close the panel
      $(".list-group-item-container .close").on("click", function () {
        var $this = $(this);
        var $panel = $this.parent();

        // close the container and remove the active status
        $panel.addClass("hidden")
          .parent().removeClass("list-view-pf-expand-active")
          .find(".list-view-pf-expand.active").removeClass("active")
          .find(".fa-angle-right").removeClass("fa-angle-down");
      });
    });

    $scope.getLength = function (json) {
      return json ? Object.keys(json).length : 0;
    };

    updateTypes();
  }
]);
