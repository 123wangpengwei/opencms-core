<%@ page import="org.opencms.workplace.commons.*" %><%	

	// initialize the workplace class
	CmsPublishProject wp = new CmsPublishProject(pageContext, request, response);
	
//////////////////// start of switch statement 
	
switch (wp.getAction()) {

case CmsPublishProject.ACTION_CANCEL:
//////////////////// ACTION: cancel button pressed

	wp.actionCloseDialog();

break;


//////////////////// ACTION: other actions handled outside of this JSP
case CmsPublishProject.ACTION_CONFIRMED:
case CmsPublishProject.ACTION_REPORT_BEGIN:
case CmsPublishProject.ACTION_REPORT_UPDATE:
case CmsPublishProject.ACTION_REPORT_END:

	wp.actionReport();

break;


//////////////////// ACTION: show unlock confirmation dialog
case CmsPublishProject.ACTION_UNLOCK_CONFIRMATION:

	wp.setParamAction(CmsPublishProject.DIALOG_UNLOCK_CONFIRMED);

%><%= wp.htmlStart() %>

<%= wp.bodyStart("dialog", null) %>

<%= wp.dialogStart() %>
<%= wp.dialogContentStart(wp.getParamTitle()) %>

<form name="main" action="<%= wp.getDialogUri() %>" method="post" class="nomargin" onsubmit="submitAction('<%= wp.DIALOG_OK %>', null, 'main');">
<%= wp.paramsAsHidden() %>
<input type="hidden" name="<%= wp.PARAM_FRAMENAME %>" value="">

<%= wp.key("messagebox.message1.lockedinfolder") %>

<%= wp.dialogContentEnd() %>
<%= wp.dialogButtonsOkCancel() %>

</form>

<%= wp.dialogEnd() %>
<%= wp.bodyEnd() %>
<%= wp.htmlEnd() %>
<%

break;
//////////////////// ACTION: show start dialog
case CmsPublishProject.ACTION_DEFAULT:
default:

	wp.setParamAction(CmsPublishProject.DIALOG_CONFIRMED);

%><%= wp.htmlStart() %>

<%= wp.bodyStart("dialog", null) %>

<%= wp.dialogStart() %>
<%= wp.dialogContentStart(wp.getParamTitle()) %>

<form name="main" action="<%= wp.getDialogUri() %>" method="post" class="nomargin" onsubmit="return submitAction('<%= wp.DIALOG_OK %>', null, 'main');">
<%= wp.paramsAsHidden() %>
<input type="hidden" name="<%= wp.PARAM_FRAMENAME %>" value="">

<table border="0" cellpadding="2" cellspacing="0">
<tr>
	<td><%= wp.key("messagebox.message1.publishresource") %></td>
</tr>
<tr>
	<td><%= wp.getParamResource() %></td>
</tr>
<tr>
	<td><%= wp.key("messagebox.message2.publishresource") %> <%= wp.getParamModifieddate() %> 
		<%= wp.key("messagebox.message3.publishresource") %> <%= wp.getParamModifieduser() %>
		<%= wp.key("messagebox.message4.publishresource") %>
	</td>
</tr>
<%= wp.buildCheckSiblings() %>
</table>

<%= wp.dialogContentEnd() %>
<%= wp.dialogButtonsOkCancel() %>

</form>

<%= wp.dialogEnd() %>
<%= wp.bodyEnd() %>
<%= wp.htmlEnd() %>
<%
} 
//////////////////// end of switch statement 
%>