<%@ page session="false" %>
<%@ taglib prefix="cms" uri="http://www.opencms.org/taglib/cms" %>
<cms:include property="template" part="head" />

<h1>Congratulations!</h1>

<h3>If you're seeing this page via a web browser, it means you've setup OpenCms successfully.</h3>


<p>Your installed OpenCms version is: <cms:info property="opencms.version" /><br>
<span class="small">Running on 
<cms:info property="java.vm.vendor" /> 
<cms:info property="java.vm.name" /> 
<cms:info property="java.vm.version" /> 
<cms:info property="java.vm.info" /> with
<cms:info property="os.name" /> 
<cms:info property="os.version" /> 
(<cms:info property="os.arch" />)</span></p>


<p>As you may have guessed by now, this is the default OpenCms home page. 
It can <i>not</i> be found on the local file system ;-) but in the OpenCms
<i>virtual file system</i> or VFS, which is served from the connected database.
You can access the VFS though the OpenCms workplace.</p>


<p class="high">To login to the OpenCms workplace, point your browser to the following URL:<br>&nbsp;<br>
<a href="#" onclick="window.open('<cms:link>/system/login/</cms:link>');return false;"><cms:link>/system/login/</cms:link></a></p>


<table align="right" cellpadding="0" cellspacing="0"><tr><td class="note">

You are currently identified as:<br>
User: <tt><cms:user property="name" /></tt><br>
Group: <tt><cms:user property="group" /></tt><br>
Description: <tt><cms:user property="description" /></tt>

</td></tr></table>

<p>Use the following account information for the first login:<br>&nbsp;<br>

Username: <tt>Admin</tt><br>
Password: <tt>admin</tt></p>


<p><b>Important:</b> You should change this default password immediately,
before someone else does it for you.</p>


<p>If you're seeing this page, and you don't think you should be, 
then either you're a a user who has arrived at a new installation of OpenCms, 
or you're an administrator who hasn't got his/her setup quite right. 
Providing the latter is the case, please refer to the OpenCms Documentation 
available at <a href="http://www.opencms.org" target="_blank">http://www.opencms.org</a>
for more detailed setup and administration information.</p>


<p>In case you have questions about OpenCms, please use the <b>opencms-dev</b> mailing list.
This is currently the list for all questions regarding OpenCms setup, configuration and development.
To subscribe to this list, send a message to: <a href="mailto:majordomo@opencms.com">majordomo@opencms.com</a>
and include the text<br><tt>subscribe opencms-dev <i>{insert-your-email-address-here}</i></tt><br>
in the body of your mail. The mail does not need a subject.
</p>

<p>Thank you for using OpenCms!</p>

<cms:include property="template" part="foot" />


