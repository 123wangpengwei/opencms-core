<%@ page import="org.opencms.workplace.*, org.opencms.workplace.editors.*" %><%
    
    // get workplace class from request attribute
    CmsDialog wp = CmsDialog.initCmsDialog(pageContext, request, response);

%><%= wp.htmlStart() %>

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

function confirmAction(actionValue, theForm) {
    if (actionValue == "ok") {
        return true;
    } else {
	theForm.target = "_top";
        theForm.action.value = "<%= CmsEditor.EDITOR_EXIT %>";
        theForm.submit();
        return false;
    }
}
</script>

<%= wp.bodyStart("dialog") %>

<%= wp.dialogStart() %>
<%= wp.dialogContentStart(wp.getParamTitle()) %>

<table border="0" cellpadding="4" cellspacing="0">
<tr>
    <td style="vertical-align: middle;"><img src="<%= wp.getSkinUri() %>commons/error.png" border="0"></td>
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
<%= wp.paramsAsHidden() %>

<%
    if (wp.getParamErrorstack() == null || "".equals(wp.getParamErrorstack())) {
%>

<%= wp.dialogButtonsClose("onclick=\"closeErrorDialog('" + CmsDialog.DIALOG_CANCEL + "', form);\"") %>

<%  } else { 
    
    String okAttribute = "";
    String discardAttribute = "onclick=\"confirmAction('" + CmsDialog.DIALOG_CANCEL + "', form);\"";
    String detailsAttribute = "onclick=\"toggleElement('errordetails');\"";
%>

<%= wp.dialogButtons(new int[] {wp.BUTTON_EDIT, wp.BUTTON_DISCARD, wp.BUTTON_DETAILS}, new String[] {okAttribute, discardAttribute, detailsAttribute}) %>

<%  } %>
</form>

<div name="errordetails" id="errordetails" class="hide">
<div style="margin: 10px; ">
<iframe name="trace" id="trace" src="about:blank" style="width:100%; height:400px; margin: 0; padding: 0;"></iframe>
</div>
</div>

<%= wp.dialogEnd() %>
<%= wp.bodyEnd() %>
<%= wp.htmlEnd() %>