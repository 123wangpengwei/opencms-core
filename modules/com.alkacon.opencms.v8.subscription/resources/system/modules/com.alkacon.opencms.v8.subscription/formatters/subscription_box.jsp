<%@page taglibs="c,cms,fmt"%>
<%@page import="org.opencms.file.*" %>
<%@page import="org.opencms.jsp.*" %>
<cms:formatter var="content" val="value">
<fmt:setLocale value="${cms.locale}" />
<fmt:bundle basename="com.alkacon.opencms.v8.subscription.frontend">
<div class="box">
<h4>
<c:choose>
<c:when test="${content.value.Title.isSet}">
${content.value.Title}
</c:when>
<c:otherwise>
<fmt:message key="subscriptionbox.title" />
</c:otherwise>
</c:choose>
</h4>
<div class="boxbody">
<c:if test="${not empty param.action}">
	<c:choose>
		<c:when test="${param.action == 'subscribe'}">
			<cms:usertracking action="subscribe" file="${param.file}" />
		</c:when>
		<c:when test="${param.action == 'unsubscribe'}">
			<cms:usertracking action="unsubscribe" file="${param.file}" />
		</c:when>
	</c:choose>
</c:if>
<c:set var="formtarget"><c:choose><c:when test="${cms.detailContentId != null}"><cms:link>${cms.detailContentSitePath}</cms:link></c:when><c:otherwise><cms:link>${cms.requestContext.uri}</cms:link></c:otherwise></c:choose></c:set>

<form name="changesub" method="GET" action="${formtarget}">
<c:set var="checkuri"><c:choose><c:when test="${cms.detailContentId != null}">${cms.detailContentSitePath}</c:when><c:otherwise>${cms.requestContext.uri}</c:otherwise></c:choose></c:set>
<c:set var="subscribed"><cms:usertracking action="checksubscribed" currentuser="true" file="${checkuri}"/></c:set>
<c:set var="formaction"><c:choose><c:when test="${subscribed}">unsubscribe</c:when><c:otherwise>subscribe</c:otherwise></c:choose></c:set>
<c:set var="label"><c:choose><c:when test="${subscribed}"><fmt:message key="button.unsubscribe"/></c:when><c:otherwise><fmt:message key="button.subscribe"/></c:otherwise></c:choose></c:set>
<input name="action" type="hidden" value="${formaction}" />
<input name="file" type="hidden" value="${checkuri}" />
<input style="-moz-border-radius: 2px; -webkit-border-radius:2px;  border: 1px solid black; background-color: #acf; height: 20px; width: 10em; " type="submit" value="${label}" />
</form>
</div>
</div>
</fmt:bundle>
</cms:formatter>
