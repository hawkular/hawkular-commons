<?xml version="1.0" encoding="UTF-8"?>
<!--

    Copyright 2014-2016 Red Hat, Inc. and/or its affiliates
    and other contributors as indicated by the @author tags.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan" version="2.0" exclude-result-prefixes="xalan">

  <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes" xalan:indent-amount="4" standalone="no" />

  <xsl:template
      match="//*[local-name()='config']/*[local-name()='subsystem']/*[local-name()='root-logger']/*[local-name()='level']">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
      <xsl:attribute name="name">${hawkular.log.root:INFO}</xsl:attribute>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="//*[local-name()='config']/*[local-name()='subsystem']/*[local-name()='console-handler']/*[local-name()='level']">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
      <xsl:attribute name="name">${hawkular.log.console:INFO}</xsl:attribute>
    </xsl:copy>
  </xsl:template>

  <xsl:template
      match="//*[local-name()='logger' and @category='org.jboss.as.config']/*[local-name()='level']">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
      <xsl:attribute name="name">${hawkular.log.jboss.as.config:INFO}</xsl:attribute>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="//*[local-name()='config']/*[local-name()='supplement' and @name='default']/*[local-name()='replacement' and @placeholder='LOGGERS']">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
      <logger category="org.hawkular.bus">
        <level name="${{hawkular.log.bus:INFO}}" />
      </logger>
      <logger category="org.hawkular.cmdgw">
        <level name="${{hawkular.log.cmdgw:INFO}}" />
      </logger>
      <logger category="org.hawkular.nest">
        <level name="${{hawkular.log.nest:INFO}}" />
      </logger>
      <logger category="com.datastax.driver">
        <level name="${{hawkular.log.datastax.driver:INFO}}"/>
      </logger>
    </xsl:copy>
  </xsl:template>

  <!-- copy everything else as-is -->
  <xsl:template match="node()|comment()|@*">
    <xsl:copy>
      <xsl:apply-templates select="node()|comment()|@*" />
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>
