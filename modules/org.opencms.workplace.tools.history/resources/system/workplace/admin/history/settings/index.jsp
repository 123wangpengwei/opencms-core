<%@ page import="
	org.opencms.workplace.*,
	org.opencms.workplace.tools.history.*
"%><%	
	// initialize the workplace class
	CmsAdminHistorySettings wp = new CmsAdminHistorySettings(pageContext, request, response);
		
//////////////////// start of switch statement 
switch (wp.getAction()) {
    case CmsDialog.ACTION_CANCEL:
//////////////////// ACTION: cancel button pressed
	wp.actionCloseDialog();
	break;

    case CmsAdminHistorySettings.ACTION_SAVE_EDIT:
//////////////////// ACTION: save edited history settings
	wp.actionEdit(request);
	break;

    case CmsDialog.ACTION_DEFAULT:
    default:
//////////////////// ACTION: show history settings dialog (default)
	wp.setParamAction(CmsAdminHistorySettings.DIALOG_SAVE_EDIT);
%>

    <%= wp.htmlStart("administration/index.html") %>
    <%= wp.bodyStart(null) %>

    <%= wp.dialogStart() %>
    <%= wp.dialogContentStart(wp.getParamTitle()) %>

<form name="main" class="nomargin" action="<%= wp.getDialogUri() %>" method="post" onsubmit="submitAction('<%= CmsDialog.DIALOG_OK %>', null, 'main');">
<%= wp.paramsAsHidden() %>
<% if (wp.getParamFramename()==null) { %>
<input type="hidden" name="<%= CmsDialog.PARAM_FRAMENAME %>" value="">
<%  } %>

<%= wp.buildSettingsForm() %>

<%= wp.dialogContentEnd() %>

<%= wp.dialogButtonsOkCancel() %>

</form>

<%= wp.dialogEnd() %>

<script type="text/javascript">
<!--

function isDigit() {
	return ((event.keyCode >= 48) && (event.keyCode <= 57)) 
}

function checkEnabled() {
	var isEnabled = document.getElementById("enabled").checked;
	if (isEnabled) {
		document.getElementById("settings").className = "show";
	} else {
		document.getElementById("settings").className = "hide";
	}
}

checkEnabled();

//-->
</script>

<%= wp.bodyEnd() %>
<%= wp.htmlEnd() %>
<%
} 
//////////////////// end of switch statement 
%>