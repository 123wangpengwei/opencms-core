<%@page buffer="none" session="false" import="org.opencms.jsp.*, org.opencms.frontend.templateone.form.*, java.util.*" %><%

// Initialize JSP action element
CmsJspActionElement cms = new CmsJspActionElement(pageContext, request, response);

CmsFormHandler formHandler = (CmsFormHandler)request.getAttribute("formhandler"); 

%><%= formHandler.getFormConfiguration().getFormConfirmationText() %>
<table border="0" style="margin-top: 14px;">
<%
List resultList = formHandler.createValuesFromFields();

for (int i = 0, n = resultList.size(); i < n; i++) {
	CmsFieldValue current = (CmsFieldValue)resultList.get(i);
	if (current.isShow()) {
		out.print("<tr>\n\t<td valign=\"top\">" + current.getLabel() + "</td>");
		out.print("\n\t<td valign=\"top\" style=\"font-weight: bold;\">" + formHandler.convertToHtmlValue(current.getValue()) + "</td></tr>\n");
	}
}

%>
</table>