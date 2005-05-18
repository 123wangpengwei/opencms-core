<%@ page import="org.opencms.workplace.*" buffer="none" %><%
	
	// get workplace class from request attribute
	CmsDialog wp = CmsDialog.initCmsDialog(pageContext, request, response);
        wp.setParamAction(wp.DIALOG_CONFIRMED);
%>

<%= wp.htmlStart() %>

<script type="text/javascript">
var init = false;

function toggleElement(id) {
	var el = document.getElementById(id);
	var cl = el.className;
	if (cl == "hide") {
		el.className = "show";
		if (! init) {
			init = true;
			setTimeout("initTrace();", 0);
		}
	} else {
		el.className = "hide";
	}
}

function initTrace() {
	trace.document.open();
	trace.document.write("<%= wp.getFormattedErrorstack() %>");
	trace.document.close();
}

function closeErrorDialog(actionValue, theForm) {
	// removed history.back() in order to avoid odd dialog behaviour (does not close anymore)
	submitAction(actionValue, theForm);
}
</script>

<%= wp.bodyStart("dialog") %>
<%= wp.dialogStart() %>
<%= wp.dialogContentStart(wp.getParamTitle()) %>

<table border="0" cellpadding="4" cellspacing="0">
<tr>
	<td style="vertical-align: middle;"><img src="<%= wp.getSkinUri() %>commons/error.gif" border="0"></td>
	<td style="vertical-align: middle;"><%= wp.getParamMessage() %></td>
</tr>
</table>

<%= wp.dialogBlockStart(wp.key("message.reason")) %>

<%= wp.getParamReasonSuggestion() %>

<%= wp.dialogBlockEnd() %>
<%= wp.dialogSpacer() %>

<%= wp.dialogSpacer() %>

<%= wp.dialogContentEnd() %>

<form name="close" action="<%= wp.getDialogUri() %>" method="post" class="nomargin">
<% wp.setParamAction(CmsDialog.DIALOG_CANCEL); %>
<%= wp.paramsAsHidden() %>
<%
	if (wp.getParamErrorstack() == null || "".equals(wp.getParamErrorstack())) {
%>
<%= wp.dialogButtonsClose("onclick=\"closeErrorDialog('" + CmsDialog.DIALOG_CANCEL + "', form);\"") %>
<%	} else { %>
<%= wp.dialogButtonsCloseDetails("onclick=\"closeErrorDialog('" + CmsDialog.DIALOG_CANCEL + "', form);\"", "onclick=\"toggleElement('errordetails');\"") %>
<%	} %>
</form>

<div name="errordetails" id="errordetails" class="hide">
<div style="margin: 10px; ">
<iframe name="trace" id="trace" src="about:blank" style="width:100%; height:400px; margin: 0; padding: 0;"></iframe>
</div>
</div>

<%= wp.dialogEnd() %>
<%= wp.bodyEnd() %>
<%= wp.htmlEnd() %>
