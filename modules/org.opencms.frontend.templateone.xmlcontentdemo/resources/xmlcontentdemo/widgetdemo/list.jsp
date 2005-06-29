<%@ page session="false" %>
<%@ taglib prefix="cms" uri="http://www.opencms.org/taglib/cms" %>
<cms:include property="template" element="head" />

<div class="element">

<cms:include file="list_content.html" element="header" editable="true"/>

<cms:contentload collector="allInFolderDateReleasedDesc" param="${property.xmlcontent-demo}widgetdemo_${number}|widgetdemo" editable="true">

<%@ include file="detail_include.txt" %>

</cms:contentload>

</div>

<cms:include property="template" element="foot" />