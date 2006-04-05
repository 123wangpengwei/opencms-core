<%@ page import="org.opencms.workplace.commons.*" %><%	

	// initialize the workplace class
	CmsLock wp = new CmsLock(pageContext, request, response);
	
//////////////////// start of switch statement 
	
switch (wp.getAction()) {

case CmsLock.ACTION_CANCEL:
//////////////////// ACTION: cancel button pressed

	wp.actionCloseDialog();

break;


case CmsLock.ACTION_CONFIRMED:
case CmsLock.ACTION_WAIT:
//////////////////// ACTION: main locking action

	wp.actionToggleLock();

break;


case CmsLock.ACTION_SUBMIT_NOCONFIRMATION:
//////////////////// ACTION: auto submits the form without user confirmation

	wp.setParamAction(CmsLock.DIALOG_CONFIRMED);
%>
<%= wp.htmlStart() %>
<%= wp.bodyStart(null) %>
<form name="main" action="<%= wp.getDialogUri() %>" method="post" class="nomargin">
<%= wp.paramsAsHidden() %>
<input type="hidden" name="<%= wp.PARAM_FRAMENAME %>" value="">
</form>
<script type="text/javascript">
    submitAction('<%= wp.DIALOG_OK %>', null, 'main');
	document.forms["main"].submit();
</script>
<%= wp.bodyEnd() %>
<%= wp.htmlEnd() %>
<%
break;


case CmsLock.ACTION_DEFAULT:
default:
//////////////////// ACTION: show confirmation dialog (default)

	wp.setParamAction(CmsLock.DIALOG_CONFIRMED);

%><%= wp.htmlStart("help.explorer.contextmenu.lock") %>
<%= wp.bodyStart("dialog") %>

<%= wp.dialogStart() %>
<%= wp.dialogContentStart(wp.getParamTitle()) %><%
if (wp.isMultiOperation()) { %>
	<%@ include file="includes/multiresourcelist.txt" %><%
} else { %>
	<%@ include file="includes/resourceinfo.txt" %><%
} %>

<%= wp.dialogSpacer() %>

<form name="main" action="<%= wp.getDialogUri() %>" method="post" class="nomargin" onsubmit="return submitAction('<%= wp.DIALOG_OK %>', null, 'main');">
<%= wp.paramsAsHidden() %>
<input type="hidden" name="<%= wp.PARAM_FRAMENAME %>" value="">

<%= wp.buildDialogText() %>

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