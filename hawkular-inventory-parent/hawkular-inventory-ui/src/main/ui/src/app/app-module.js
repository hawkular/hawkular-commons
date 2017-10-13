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
angular.module ('hwk.appModule', [
  'ngResource',
  'ngRoute',
  'ui.bootstrap',
  'pascalprecht.translate',
  'patternfly',
  'patternfly.toolbars',
  'patternfly.charts',
  'patternfly.notification',
  'hwk.resourcesModule',
  'hwk.typesModule',
]).config(['$routeProvider', '$translateProvider', 'NotificationsProvider',
  function ($routeProvider, $translateProvider, NotificationsProvider) {
    'use strict';

    $routeProvider
      .when('/', {
        redirectTo: '/resources'
      })
      .when('/resources', {
        templateUrl: 'src/resources/resources.html'
      })
      .when('/types', {
        templateUrl: 'src/types/types.html'
      })

      // Default
      .otherwise({
      });

    $translateProvider.translations('default', 'en');
    $translateProvider.preferredLanguage('default');

    NotificationsProvider.setDelay(10000).setVerbose(false).setPersist({'error': true, 'httpError': true, 'warn': true});
  }
]);
