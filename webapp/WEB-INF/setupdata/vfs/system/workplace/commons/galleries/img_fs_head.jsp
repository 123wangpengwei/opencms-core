<%@ page import="org.opencms.jsp.*, org.opencms.workplace.commons.*" buffer="none" session="false" %><%	
	
	// initialize action element for link substitution
	CmsJspActionElement cms = new CmsJspActionElement(pageContext, request, response);
	// initialize the workplace class
	CmsGalleryImages wp = new CmsGalleryImages(pageContext, request, response);
	String params = "?" + wp.paramsAsRequest();
	
%><!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN">
<html>
<head>
</head>

<frameset rows="50,*" border="0" frameborder="0" framespacing="0">
    <frame <%= wp.getFrameSource("gallery_head", cms.link("img_head.jsp" + params)) %> noresize="noresize" scrolling="no" style="border-bottom: 1px solid WindowFrame;">
    <frame <%= wp.getFrameSource("gallery_list", cms.link("img_list.jsp" + params)) %> scrolling="auto" style="border-bottom: 1px solid Menu;">
</frameset>

<body>
</body>
</html>