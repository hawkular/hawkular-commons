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
var typesModule = angular.module( 'hwk.typesModule', []);

typesModule.directive('repeatDone', function($timeout) {
  'use strict';

  return {
    restrict: 'A',
    link: function (scope, element, attr) {
      if (scope.$last === true) {
        $timeout(function () {
          scope.$emit('ngRepeatDoneTypes');
        }, 0);
      }
    }
  };
});

typesModule.directive('ngConfirmClick', function() {
  'use strict';

  return {
    link: function (scope, element, attr) {
      var msg = attr.ngConfirmClick || "Are you sure?";
      var clickAction = attr.confirmedClick;
      element.bind('click', function (event) {
        if ( window.confirm(msg) ) {
          scope.$eval(clickAction);
        }
      });
    }
  };
});