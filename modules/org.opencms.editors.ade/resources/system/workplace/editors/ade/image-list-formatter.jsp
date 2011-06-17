<%@ page import="org.opencms.workplace.editors.ade.*"%>
<%@ taglib prefix="cms" uri="http://www.opencms.org/taglib/cms"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt"  uri="http://java.sun.com/jsp/jstl/fmt" %>
<% 
org.opencms.workplace.editors.ade.CmsDefaultFormatterHelper cms = new org.opencms.workplace.editors.ade.CmsDefaultFormatterHelper(pageContext, request, response);
pageContext.setAttribute("cms", cms);
%>
<li class="cms-item cms-list cms-item-flexview">
<div class="cms-list-item ui-widget-content ui-corner-all">
	<div class="cms-list-itemcontent  ui-state-default ui-corner-all">
		<div style="background-image: url(${cms.icon});" class="cms-list-image"></div>
    	<div class="cms-list-title" rel="${cms.title.name}" unselectable="on"><c:if test="${not empty cms.additionalInfo}"><a class="cms-left ui-icon ui-icon-triangle-1-e"></a></c:if>${cms.title.value}</div>
		<div rel="${cms.subTitle.name}" unselectable="on">${cms.subTitle.value}</div>
	</div>
	<c:if test="${not empty cms.additionalInfo}">
		<div class="cms-additional">
			<c:forEach var="info" items="${cms.additionalInfo}">
				<div class="cms-additional-item" rel="${info.name}">
					<span class="cms-additional-item-title">${info.title}:</span><%-- no line-break here
				--%><span class= "cms-additional-item-value">${info.value}</span>
				</div>
			</c:forEach>
		</div>
	</c:if>
	<div class="cms-item-tile">
		<img src="<cms:link>${cms.sitePath}</cms:link>?__scale=w:140,h:100,t:1,c:transparent,r:2" title="${cms.imageInfo}"/>
	</div>
</div>
</li>