<%@page import="java.util.*" %><%--

--%><% /* Initialize the setup bean */ %><%--
--%><jsp:useBean id="Bean" class="org.opencms.setup.CmsSetup" scope="session" /><%--
--%><jsp:setProperty name="Bean" property="*" /><%--

--%><%
/* next page in the setup process */
String nextPage = "step_2_check_components.jsp";

/* true if properties are initialized */
boolean setupOk = (Bean.getProperties()!=null);

// Reading the system properties
Properties vmProperties = System.getProperties();
String vmEncoding = vmProperties.getProperty("file.encoding");
boolean encodingOk = false;

if (setupOk) {
	encodingOk = Bean.getDefaultContentEncoding().equalsIgnoreCase(vmEncoding);
} else {
	Bean.initHtmlParts();
}

if (encodingOk) {
	response.sendRedirect(nextPage);
}
%><%--

--%><%= Bean.getHtmlPart("C_HTML_START") %>
OpenCms Setup Wizard
<%= Bean.getHtmlPart("C_HEAD_START") %>
<%= Bean.getHtmlPart("C_STYLES") %>
<%= Bean.getHtmlPart("C_STYLES_SETUP") %>
<%= Bean.getHtmlPart("C_HEAD_END") %>
OpenCms Setup Wizard - Wrong content encoding!
<%= Bean.getHtmlPart("C_CONTENT_SETUP_START") %>
<% if (setupOk) { %>
<table border="0" cellpadding="0" cellspacing="0" style="height: 100%;">
<tr><td style="vertical-align: bottom;">

<%= Bean.getHtmlPart("C_BLOCK_START", "Error") %>
<table border="0" cellpadding="5" cellspacing="0" style="width: 100%;">
<tr>
	<td style="vertical-align: middle;" rowspan="2"><img src="resources/error.gif" width="32" height="32" border="0"></td>
	<td style="font-weight: bold;">
		The encoding of your Java VM is different from the OpenCms encoding!
	</td>
</tr>
<tr>
	<td>
		<table border="0" cellpadding="0" cellspacing="0">
			<tr>
				<td style="text-align: left; padding-bottom: 4px;">Java VM file encoding:&nbsp;</td><td style="text-align: left; padding-bottom: 4px; font-weight: bold;"><%= vmEncoding %></td>
			</tr>
			
			<tr>
				<td style="text-align: left;">OpenCms encoding:</td><td style="text-align: left; font-weight: bold;"><%= Bean.getDefaultContentEncoding() %></td>
			</tr>
		</table>
	</td>
</tr>
</table>
<%= Bean.getHtmlPart("C_BLOCK_END") %>

<div class="dialogspacer" unselectable="on">&nbsp;</div>
<div class="dialogspacer" unselectable="on">&nbsp;</div>

</td></tr>
<tr><td style="vertical-align: top;">

<%= Bean.getHtmlPart("C_BLOCK_START", "How to continue the setup process") %>

<table border="0" cellpadding="5" cellspacing="0" style="width: 100%;>
<tr><td colspan="2">&nbsp;</td></tr>
<tr>
	<td colspan="2" style="vertical-align: top;">
		<ul>
		<li>Change the encoding of your Java VM.
		To do that you must modify the <tt>file.encoding</tt> setting.
		Using Apache Tomcat, a different encoding can be set in the environment
		variable <tt>CATALINA_OPTS</tt> by the -D parameter, e.g.:<br><tt>CATALINA_OPTS=-Dfile.encoding=ISO-8859-1</tt><br>&nbsp;</li>
		<li>If you want to use an encoding different from <span style="font-weight: bold;">ISO-8859-1</span>, you must also
		adjust the <tt>defaultContentEncoding</tt> setting in <tt>WEB-INF/config/opencms.properties</tt>.</li>
		</ul>
		Unless you have specific encoding requirements, you should use the default <b>ISO-8859-1</b> setting.
		Please refer to the <a href="http://java.sun.com/j2se/1.4/docs/guide/intl/encoding.doc.html" target="_blank">Sun documentation</a> for a list of supported encodings for your OS.
	</td>
</tr>
</table>

<%= Bean.getHtmlPart("C_BLOCK_END") %>

</td></tr></table>

<%= Bean.getHtmlPart("C_CONTENT_END") %>

<%= Bean.getHtmlPart("C_BUTTONS_START") %>
<form action="" method="post" class="nomargin">
<input name="back" type="button" value="&#060;&#060; Back" class="dialogbutton" onclick="location.href='index.jsp';">
<input name="submit" type="submit" value="Continue &#062;&#062;" class="dialogbutton" disabled="disabled">
<input name="cancel" type="button" value="Cancel" class="dialogbutton" onclick="location.href='index.jsp';" style="margin-left: 50px;">
</form>
<%= Bean.getHtmlPart("C_BUTTONS_END") %>
<% } else	{ %>

<%@ include file="error.jsp" %>

<%= Bean.getHtmlPart("C_CONTENT_END") %>
<% } %>
<%= Bean.getHtmlPart("C_HTML_END") %>