/*
 * File   : $Source: /alkacon/cvs/opencms/modules/org.opencms.workplace.explorer/resources/system/workplace/resources/commons/explorer.js,v $
 * Date   : $Date: 2006/10/22 13:55:30 $
 * Version: $Revision: 1.13.4.10 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * Copyright (c) 2005 Alkacon Software GmbH (http://www.alkacon.com)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * For further information about Alkacon Software GmbH, please see the
 * company website: http://www.alkacon.com
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

var help_url="ExplorerAnsicht/index.html";
var flaturl="";
var mode="explorerview";
var showlinks=false;
var openfolderMethod="openFolder";
var showKon=true;
var autolock=false;
var plainresid=-1;
var win;
var buttonType=1;
var link_newresource="/system/workplace/commons/newresource.jsp";
var link_uploadresource="/system/workplace/commons/newresource_upload.jsp";
var link_showresource="/system/workplace/commons/displayresource.jsp";
var link_searchresource="/system/workplace/views/admin/admin-main.jsp?root=explorer&path=%2Fsearch";
var last_id=-1;
var active_mouse_id=-1;
var active_from_text=false;
var cancelNextOpen=false;
var selectedResources=new Array();
var selectedStyles=new Array();
var contextOpen=false;
var displayResource="/";
var g_histLoc=0;
var g_history=null;
var m_rootFolder="/";


function show_help(){
	return help_url;
}


function windowStore(body, head, tree, files) {
	this.body = body;
	this.head = head;
	this.tree = tree;
	this.files = files.document;
	this.fileswin = files;
}


function menuItem(name, link, target, rules){
	this.name = name;
	this.link = link;
	this.target = target;
	this.rules = rules;
}


//            1     2     3      4     5         6     7      8            9        10                11                   12           13              14            15           16           17        18        19                   20                 21                      22             23
function file(name, path, title, type, linkType, size, state, layoutstyle, project, dateLastModified, userWhoLastModified, dateCreated, userWhoCreated, dateReleased, dateExpired, permissions, lockedBy, lockType, lockedInProjectName, lockedInProjectId, isInsideCurrentProject, workflowState, workflowInfo){
	this.name = name;
	this.path = path;
	this.title = decodeURIComponent(title);
	this.type = type;
	this.linkType = linkType;
	this.size = size;
	this.state = state;
	this.layoutstyle = layoutstyle;
	this.project = project;
	this.dateLastModified = dateLastModified;
	this.userWhoLastModified = userWhoLastModified
	this.dateCreated = dateCreated;
	this.userWhoCreated = userWhoCreated;
	this.dateReleased = dateReleased;
	this.dateExpired = dateExpired;
	this.permissions = permissions;
	this.lockedBy = lockedBy;
	this.lockType = lockType;
	this.lockedInProjectName = lockedInProjectName;
	this.lockedInProjectId = lockedInProjectId;
	this.isInsideCurrentProject = (isInsideCurrentProject=='I') ? true : false;
	this.workflowState = workflowState;
	this.workflowInfo = workflowInfo;
	this.isFolder = (size < 0) ? true : false;
}


function aF(name, path, title, type, linkType, size, state, layoutstyle, project, dateLastModified, userWhoLastModified, dateCreated, userWhoCreated, dateReleased, dateExpired, permissions, lockedBy, lockType, lockedInProjectName, lockedInProjectId, isInsideCurrentProject, workflowState, workflowInfo){
	if(path == "") {
		path=vr.actDirectory;
	}
	vi.liste[vi.liste.length] = new file(name, path, title, type, linkType, size, state, layoutstyle, project, dateLastModified, userWhoLastModified, dateCreated, userWhoCreated, dateReleased, dateExpired, permissions, lockedBy, lockType, lockedInProjectName, lockedInProjectId, isInsideCurrentProject, workflowState, workflowInfo);
}


function vars_index() {
	this.icons = new Array();
	this.liste = new Array();
	this.lockIcons = new Array();
	this.lockStatus = new Array();
	this.iconPath;
	this.skinPath;
	this.newButtonActive;

	this.check_name;
	this.check_title;
	this.check_type;
	this.check_size;
	this.check_workflow;
	this.check_permissions;
	this.check_dateLastModified;
	this.check_userWhoLastModified;
	this.check_dateCreated;
	this.check_userWhoCreated;
	this.check_dateReleased;
	this.check_dateExpired;
	this.check_state;
	this.check_lockedBy;

	this.userName;
	this.resource = new Array();
	this.menus = new Array();
}


function res(text, nicename, icon, createLink, isEditable){
	this.text = text;
	this.nicename = nicename;
	this.icon = icon;
	this.createLink = createLink;
	this.editable = isEditable;
}


function setDisplayResource(resource) {
	displayResource = resource;
	if (mode == "explorerview") {
		win.head.forms.urlform.resource.value = displayResource.substring(getRootFolder().length - 1);
	}
}


function getDisplayResource(param) {
	if (param == "true") {
		return displayResource.substring(getRootFolder().length - 1);
	}
	return displayResource;
}


function getRootFolder() {
	if (m_rootFolder == null) {
		return "/";
	} else {
		return m_rootFolder;
	}
}


function setRootFolder(value) {
	m_rootFolder = value;
}


function initHist() {
	g_histLoc = 0;
	g_history = new Array();
}


function addHist(entry) {
	if (g_history[g_histLoc] != entry && entry.indexOf("siblings:") == -1) {
		g_histLoc++;
		g_history[g_histLoc] = entry;
	}
}


function histGoBack() {
	if (g_histLoc > 1) {
		g_histLoc--;
		setDisplayResource(g_history[g_histLoc]);
	} else {
		setDisplayResource(removeSiblingPrefix(getDisplayResource()));
	}
	openurl();
}


function dU(doc, pages, actpage) {

	vi.locklength = 0;
	vi.doc = doc;
	updateWindowStore();
	openfolderMethod="openFolder";
	showCols(vr.viewcfg);
	printList(doc);
	if (mode == "explorerview") {
		displayHead(win.head, pages, actpage);
	}
}


function updateWindowStore() {
	var theTree = null;
	if (window.body.explorer_body && window.body.explorer_body.explorer_tree) {
		theTree = window.body.explorer_body.explorer_tree;
	}

	if ((mode == "listview") || (mode == "galleryview")) {
                var theDoc = null;
                if (window.body.admin_content) {
                   if (window.body.admin_content.tool_content) {
                      theDoc = window.body.admin_content.tool_content;
                   } else {
                      theDoc = window.body.admin_content;
                   }
                } else {
                   if (window.body.explorer_body.explorer_files.tool_content) {
                      theDoc = window.body.explorer_body.explorer_files.tool_content;
                   } else {
                      theDoc = window.body.explorer_body.explorer_files;
                   }
                }
        if (window.body.admin_head) {
 			win = new windowStore(window.body.document, window.body.admin_head.document, theTree, theDoc);
        } else if (window.body.explorer_head) {
 			win = new windowStore(window.body.document, window.body.explorer_head.document, theTree, theDoc);
        } else {
 			win = new windowStore(window.body.document, null, theTree, theDoc);
        }
	} else {
		try {
			win = new windowStore(window.body.document, window.body.explorer_head.document, theTree, window.body.explorer_body.explorer_files);
		} catch (e) {}
	}
}


function rD() {
	vi.liste = new Array();
	vi.icons = new Array();
}


function showCols(cols) {
	var check = new Array();

	check[9] = 'vi.check_name';
	check[0] = 'vi.check_title';
	check[1] = 'vi.check_type';
	check[3] = 'vi.check_size';
	check[7] = 'vi.check_permissions';
	check[2] = 'vi.check_dateLastModified';
	check[10] = 'vi.check_dateCreated';
	check[11] = 'vi.check_userWhoLastModified';
	check[5] = 'vi.check_userWhoCreated';
	check[12] = 'vi.check_dateReleased';
	check[13] = 'vi.check_dateExpired';
	check[4] = 'vi.check_state';
	check[8] = 'vi.check_lockedBy';
	check[14] = 'vi.check_workflow';

	for (i = 0; i <= 14; i++) {
		if (i != 6) {
			if ((cols & Math.pow(2, i)) > 0) {
				eval(check[i] + "=true;");
			} else {
				eval(check[i] + "=false;");
			}
		}
	}
}


// set the last selected menu id (from icon)
function setId(id) {
	active_mouse_id = id;
	active_from_text = false;
}


// set the last selected menu id (from link text)
function setId2(id) {
	active_mouse_id = id;
	active_from_text = true;
}


// handle the context menu to show
function handleContext(e) {

	if (selectedResources.length > 1) {
		// multi context menu
		showContext(win.files, "multi", false);
	} else {
		// single context menu
		if (active_mouse_id >= 0) {
			showContext(win.files, active_mouse_id, true);
		}
	}
	// stop event bubbling
	e.cancelBubble = true;
	if (e.stopPropagation) {
		e.stopPropagation();
	}
	return false;
}


// builds the HTML for a context menu (single or multi context menu)
function showContext(doc, i, isSingleContext) {

	var spanstart    = "<span class=\"cmenorm\" onmouseover=\"className='cmehigh';\" onmouseout=\"className='cmenorm';\">";
	var spanstartina = "<span class=\"inanorm\" onmouseover=\"className='inahigh';\" onmouseout=\"className='inanorm';\">";
	var spanend      = "</span>";

	var menu = "";
	// the type id of the current context menu
	var typeId;
	// the resource name needed for single context menu
	var resourceName;

	var access = true;
	if (isSingleContext) {
		resourceName = getResourceAbsolutePath(i);
		typeId = vi.liste[i].type;
		if ((typeof vi.resource[typeId] == 'undefined') || (vi.resource[typeId].editable == false)) {
			// the user has no access to this resource type
			access = false;
		}
	} else {
		// multi context menu uses special menu type ID
		typeId = "multi";
		if (vi.menus[typeId] == null) {
			// no multi context menu defined, do not show menu
			return;
		}
		// set resource list in hidden form field value
		var resourceList = "";
		var isFirst = true;
		for (i=0; i<selectedResources.length; i++) {
			if (!isFirst) {
				resourceList += "|";
			}
			resourceList += getResourceAbsolutePath(selectedResources[i]);
			isFirst = false;

		}
		doc.forms["formmulti"].elements["resourcelist"].value = resourceList;
	}

	if (access) {
		menu += "<div class=\"cm2\">";
		menu += "<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" class=\"cm\">";

		var lastWasSeparator = false;
		var firstEntryWritten = false;
		for (a = 0; a < vi.menus[typeId].items.length; a++) {

			// 0:unchanged, 1:changed, 2:new, 3:deleted
			var result = -1;

			if (vi.menus[typeId].items[a].name == "-") {
				result = 1;
			} else if (vr.actProject == vr.onlineProject) {
				// online project
				if (isSingleContext) {
					if (vi.menus[typeId].items[a].rules.charAt(0) == 'i') {
						result = 2;
					} else {
						if (vi.menus[typeId].items[a].rules.charAt(0) == 'a') {
							if ((vi.menus[typeId].items[a].link.indexOf("showlinks=true") > 0)
							&& (vi.liste[i].linkType == 0)) {
								// special case: resource without siblings
								result = 2;
							} else {
								result = (typeId == 0)?3:4;
							}
						}
					}
				} else {
					// multi context menu
					result = 2;
				}
			} else {
				// offline project
				if (isSingleContext) {
					if (! vi.liste[i].isInsideCurrentProject) {
						// resource is from online project
						if (vi.menus[typeId].items[a].rules.charAt(1) == 'i') {
							result = (vi.menus[typeId].items[a].name == "-")?1:2;
						} else {
							if (vi.menus[typeId].items[a].rules.charAt(1) == 'a') {
								if (vi.menus[typeId].items[a].name == "-") {
									result = 1;
								} else {
									if ((vi.menus[typeId].items[a].link.indexOf("showlinks=true") > 0)
									&& (vi.liste[i].linkType == 0)) {
										// special case: resource without siblings
										result = 2;
									} else {
										result = (typeId == 0)?3:4;
									}
								}
							}
						}
					} else {
						// resource is in this project => we have to differ 4 cases
						if (vi.liste[i].lockedBy == '' || vi.liste[i].lockType == 5) {
							// resource is not locked...
							if (autolock) {
								// autolock is enabled
								display = vi.menus[typeId].items[a].rules.charAt(vi.liste[i].state + 6);
							} else {
								// autolock is disabled
								display = vi.menus[typeId].items[a].rules.charAt(vi.liste[i].state + 2);
							}
						} else {
							var isSharedLock = (vi.liste[i].lockType == 1 || vi.liste[i].lockType == 2)?true:false;
							if (vi.liste[i].lockedInProjectId == vr.actProject) {
								// locked in this project from ...
								if (vi.liste[i].lockedBy == vr.userName) {
									// ... the current user ...
									if (isSharedLock) {
										// ... as shared lock
										display = vi.menus[typeId].items[a].rules.charAt(vi.liste[i].state + 14);
									} else {
										// ... as exclusive lock
										display = vi.menus[typeId].items[a].rules.charAt(vi.liste[i].state + 10);
									}

								} else {
									// ... someone else
									display = vi.menus[typeId].items[a].rules.charAt(vi.liste[i].state + 14);
								}
							} else {
								// locked in an other project ...
								display = vi.menus[typeId].items[a].rules.charAt(vi.liste[i].state + 14);
							}
						}
						if (display == 'i') {
							result = 2;
						} else {
							if (display == 'a') {
								if ((vi.menus[typeId].items[a].link.indexOf("showlinks=true") > 0)
								&& (vi.liste[i].linkType == 0)) {
									// special case: resource without siblings
									result = 2;
								} else {
									result = (typeId == 0)?3:4;
								}
							}
						}
					}
				} else {
					// multi context menu
					result = 3;
				}
			}
			switch (result) {
				case 1:
					// separator line
					if ((firstEntryWritten) && (!lastWasSeparator) && (a != (vi.menus[typeId].items.length - 1))) {
						menu += "<tr><td class=\"cmsep\"><span class=\"cmsep\"></div></td></tr>";
						lastWasSeparator = true;
					}
					break;
				case 2:
					// inactive entry
					menu += "<tr><td>" + spanstartina + vi.menus[typeId].items[a].name + spanend + "</td></tr>";
					lastWasSeparator = false;
					firstEntryWritten = true;
					break;
				case 3:
				case 4:
					// active entry
					var link;
					if (isSingleContext) {
						link = "href=\"" + vi.menus[typeId].items[a].link;
						if (link.indexOf("?") > 0) {
							link += "&";
						} else {
							link += "?";
						}

						link += "resource=" + resourceName + "\"";
						if (vi.menus[typeId].items[a].target != null && vi.menus[typeId].items[a].target != "''") {
							// href has a target set
							link += " target=" + vi.menus[typeId].items[a].target;
						}

						menu += "<tr><td><a class=\"cme\" " + link + ">" + spanstart + vi.menus[typeId].items[a].name + spanend + "</a></td></tr>";
					} else {
						// multi context menu
						link = "href=\"javascript:top.submitMultiAction('" + vi.menus[typeId].items[a].link + "');\"";
						menu += "<tr><td><a class=\"cme\" " + link + ">" + spanstart + vi.menus[typeId].items[a].name + spanend + "</a></td></tr>";
					}
					lastWasSeparator = false;
					firstEntryWritten = true;
					break;
				default:
					// alert("Undefined result for menu " + a);
					break;
			}
		} // end for ...
		menu += "</table></div>";

		var el = doc.getElementById("contextmenu");
		el.innerHTML = menu;
		var x = 12;
		el.style.left = x + "px";
		el.style.visibility = "visible";
		// calculate menu y position after setting visibility to avoid display errors
		var y = getMenuPosY(doc, active_mouse_id);
		el.style.top =  y + "px";
	} // end if (access)
	last_id = active_mouse_id;
	contextOpen = true;
}


// closes a context menu
function closeContext() {

	var cm = win.files.getElementById("contextmenu");
	cm.style.visibility = "hidden";
	contextOpen = false;
}


// submits a selected multi action
function submitMultiAction(dialog) {

	var doc = win.files;
	doc.forms["formmulti"].action = dialog;
	doc.forms["formmulti"].submit();
}

// handle the mouse clicks
function handleOnClick(e) {

	e = checkEvent(e);
	cancelNextOpen = (selectedResources.length > 0);
	if (contextOpen) {
		// close eventually open context menu
		closeContext();
		if (active_mouse_id == last_id) {
			// clicked on same icon again, leave handler
			return false;
		}
	}
	// unselect resources;
	toggleSelectionStyle(false);
	selectedStyles = new Array();

	var btp = e.button;
	var keyHold = e.shiftKey || e.ctrlKey || e.altKey;
	if (keyHold) {
		// stop event bubbling
		e.cancelBubble = true;
		if (e.stopPropagation) {
			e.stopPropagation();
		}
	}
	if ((active_mouse_id < 0) || (active_from_text && !keyHold && (selectedResources.length <= 1) && (btp != 2))) {
		// no icon clicked, reset selected resources and leave handler
		last_id = -1;
		selectedResources = new Array();
		return true;
	}

	if (e.shiftKey) {
		// shift pressed, mark resources
		if (last_id >= 0) {
			// mark resources from last clicked to current one
			var incrementor = 1;
			if (last_id < active_mouse_id) {
				incrementor = -1;
			}
			var count = active_mouse_id;
			selectedResources = new Array();
			selectedResources[selectedResources.length] = count;
			while (count != last_id) {
				count += incrementor;
				selectedResources[selectedResources.length] = count;

			}
		} else {
			// first click, mark single resource
			selectedResources = new Array();
			selectedResources[selectedResources.length] = active_mouse_id;
		}
		last_id = active_mouse_id;
	} else if (e.ctrlKey || e.altKey) {
		// control or alt key pressed, add or remove resource from marked resources
		var found = false;
		for (i=0; i<selectedResources.length; i++) {
			if (selectedResources[i] == active_mouse_id) {
				// resource was previously selected, remove it from selection
				selectedResources[i] = -1;
				found = true;
				i = selectedResources.length;
			}
		}
		if (found) {
			// remove resource from selection array, rebuild array
			var tempResources = new Array();
			for (i=0; i<selectedResources.length; i++) {
				if (selectedResources[i] > -1) {
					tempResources[tempResources.length] = selectedResources[i];
				}
			}
			selectedResources = tempResources;
		} else {
			// not found, add resource to selection
			selectedResources[selectedResources.length] = active_mouse_id;
		}
	} else {
		// common click, mark currently clicked resource if not clicked before
		if (active_mouse_id != last_id) {
			var clickedMarked = false;
			for (i=0; i<selectedResources.length; i++) {
				if (selectedResources[i] == active_mouse_id) {
					clickedMarked = true;
				}
			}
			if (!clickedMarked || selectedResources.length <= 1) {
				// left mouse button clicked or only one resource selected, select the current resource
				selectedResources = new Array();
				selectedResources[selectedResources.length] = active_mouse_id;
			}
		}
		toggleSelectionStyle(true);
		cancelNextOpen = keyHold || (selectedResources.length > 0);
		return handleContext(e);
	}
	toggleSelectionStyle(true);
	cancelNextOpen = keyHold || (selectedResources.length > 0);

	return false;
}


// check if the event object is available and gets it if necessary
function checkEvent(e) {

	// fix for IE if window access is refused in some cases
	try {
		win.files.getElementById("contextmenu");
	} catch (e) {
		updateWindowStore();
	}
	// check event
	if (!e) {
		e = win.fileswin.event;
	}
	return e;
}

// toggles the style of the selected resources
function toggleSelectionStyle(isSelected) {

	var doc = win.files;
	var styleName = "selected";
	if (! isSelected) {
		styleName = "unselected";
	}

	for (i=0; i<selectedResources.length; i++) {
		var last_id_style = "";
		var ah = doc.getElementById("a" + selectedResources[i]);
		var td = doc.getElementById("td3_" + selectedResources[i]);
		if (ah == null) {
			ah = td;
		}
		var rowStyle;
		if (ah != null) {
			last_id_style = ah.className;
			if (isSelected) {
				selectedStyles[selectedResources[i]] = last_id_style;
			}

			if (isSelected) {
				ah.className = styleName;
			} else {
				var cls = selectedStyles[selectedResources[i]];
				if (cls.charAt(cls.length - 1) == 'i') {
					cls = cls.substring(0, cls.length-1);
				}
				ah.className = cls;
			}

			rowStyle = styleName;
			if (last_id_style == "fd") {
				rowStyle += " fd";
				td.className = rowStyle;
			} else {
				td.className = ah.className;
			}
		}

		for (k=0; k<3; k++) {
			// change style of columns 0 to 2
			try {
				var elem = doc.getElementById("td" + k + "_" + selectedResources[i]);
				if (elem.className != "fd") {
					elem.className = rowStyle;
				}
			} catch (e) {}
		}
	}
}

function linkOver(obj, id) {

	var cls = obj.className;
	if (cls.charAt(cls.length - 1) != 'i') {
		cls = cls + 'i';
	}
	obj.className = cls;
	active_mouse_id = id;
	active_from_text = true;
}

function linkOut(obj) {

	var cls = obj.className;
	if (cls.charAt(cls.length - 1) == 'i') {
		cls = cls.substring(0, cls.length-1);
	}
	obj.className = cls;
	active_mouse_id = -1;
	active_from_text = false;
}


function printList(wo) {
	var i;
	var lockedBystring;
	var ssclass;
	var temp =
	"<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\">"
	+ "<html><head>"
	+ "<meta HTTP-EQUIV=\"content-type\" CONTENT=\"text/html; charset="
	+ top.frames.head.encoding
	+ "\">\n"
	+ "<script language=\"JavaScript\">\n"
	+ "document.oncontextmenu = new Function('return false;');\n"
	+ "document.onmousedown = new Function('return false;');\n"
	+ "document.onmouseup = top.handleOnClick;\n"
	+ "</script>"
	+ "<style type='text/css'>\n"
	+ "body { font-family: Verdana, Arial, Helvetica, sans-serif; font-size: 11px; padding: 0px; margin: 0px; background-color: Window; } "
	+ "p, td { font-family: Verdana, Arial, Helvetica, sans-serif; font-size: 11px; white-space: nowrap; } "
	+ "td.t { white-space: nowrap; background-color:ThreedFace; border-right: 1px solid ThreedDarkShadow; border-top: 1px solid ThreeDHighlight; border-bottom: 1px solid ThreedDarkShadow; border-left: 1px solid ThreeDHighlight; } "
	+ "td.t125 { white-space: nowrap; width: 125px; background-color:ThreedFace; border-right: 1px solid ThreedDarkShadow; border-top: 1px solid ThreeDHighlight; border-bottom: 1px solid ThreedDarkShadow; border-left: 1px solid ThreeDHighlight; } "
	+ "td.t100 { white-space: nowrap; width: 100px; background-color:ThreedFace; border-right: 1px solid ThreedDarkShadow; border-top: 1px solid ThreeDHighlight; border-bottom: 1px solid ThreedDarkShadow; border-left: 1px solid ThreeDHighlight; } "
	+ "td.t75 { white-space: nowrap; width: 75px; background-color:ThreedFace; border-right: 1px solid ThreedDarkShadow; border-top: 1px solid ThreeDHighlight; border-bottom: 1px solid ThreedDarkShadow; border-left: 1px solid ThreeDHighlight; } "
	+ "a { text-decoration: none; cursor: pointer; } "

	+ "td.fc { color: #b40000; } "
	+ "a.fc { color: #b40000; } "
	+ "a.fci { text-decoration: underline; color: #000088; } "

	+ "td.fn { color: #0000aa; } "
	+ "a.fn { color: #0000aa; } "
	+ "a.fni { text-decoration: underline; color: #000088; } "

	+ "td.fd { color: #000000; text-decoration: line-through; } "
	+ "a.fd { color: #000000; text-decoration: line-through; } "
	+ "a.fdi { text-decoration: line-through underline; color: #000088; } "

	+ "td.fp { color: #888888; } "
	+ "a.fp { color: #888888; } "
	+ "a.fpi { text-decoration: underline; color: #000088; } "

	+ "td.nf { color:#000000; } "
	+ "a.nf { color:#000000; } "
	+ "a.nfi { text-decoration: underline; color: #000088; } "

	+ "div.cm { position: absolute; visibility: hidden; top: 0px; left: 0px; background-color: ThreeDFace; z-index: 100; border-left: 1px solid ThreeDFace; border-top: 1px solid ThreeDFace; border-bottom: 1px solid ThreedDarkShadow; border-right: 1px solid ThreedDarkShadow; filter:progid:DXImageTransform.Microsoft.Shadow(color=ThreeDShadow, Direction=135, Strength=3); } "
	+ "div.cm2 { border-left: 1px solid ThreeDHighlight; border-top: 1px solid ThreeDHighlight; border-bottom: 1px solid ThreeDShadow; border-right: 1px solid ThreeDShadow; } "
	+ "table.cm { width: 150px; } "
	+ "span.cmsep { display: block; width: 100%; height: 1px; font-size: 0px; background-color: ThreeDShadow; padding: 0px; border-bottom: 1px solid ThreeDHighlight;} "
	+ "td.cmsep { box-sizing: border-box; -moz-box-sizing: border-box; padding: 2px; } "
	+ "a.cme { color: MenuText; text-decoration: none;} "
	+ "span.cmenorm { box-sizing: border-box; -moz-box-sizing: border-box; cursor: hand; display: block; width: 100%; padding: 2px 0px 2px 10px; } "
	+ "span.cmehigh { box-sizing: border-box; -moz-box-sizing: border-box; cursor: hand; display: block; width: 100%; padding: 2px 0px 2px 10px; color: CaptionText; background-color: ActiveCaption; } "
	+ "span.inanorm { box-sizing: border-box; -moz-box-sizing: border-box; cursor: default; display: block; width: 100%; padding: 2px 0px 2px 10px; color: GrayText; } "
	+ "span.inahigh { box-sizing: border-box; -moz-box-sizing: border-box; cursor: default; display: block; width: 100%; padding: 2px 0px 2px 10px; color: InactiveCaptionText; background-color: InactiveCaption; } "

	+ ".selected { background: ActiveCaption; color: CaptionText; } "
	+ ".unselected { background: Window; color:WindowText; } "

	+ "</style></head>";

	var returnplace = wo.location.href;
	if ((openfolderMethod != "openthisfolderflat") && (mode != "listview")) {
		var pos = returnplace.indexOf("/commons/");
		if (pos >= 0) {
			returnplace = returnplace.substring(0, pos + 1) + returnplace.substring(pos + 9);

			var pos2 = returnplace.indexOf("?");
			if (pos2 < 0) {
				pos2 = returnplace.length + 1;
			}
			var loc = returnplace.substring(0, pos2 - 1);
			loc = loc.substring(0, loc.lastIndexOf("/")) + "/views/explorer/explorer_files.jsp";
			returnplace = loc + returnplace.substring(pos2);
		}

	}
	returnplace = returnplace.replace(/\?/g, "%3F");
	returnplace = returnplace.replace(/\&/g, "%26");
	returnplace = returnplace.replace(/\=/g, "%3D");
	returnplace = returnplace.replace(/\//g, "%2F");
	wo.open();
	wo.writeln(temp);

	wo.write("<body unselectable=\"on\">");
	wo.writeln("<table cellpadding=\"1\" cellspacing=\"0\" border=\"0\"><tr>");

	wo.writeln("<td nowrap unselectable=\"on\" class=\"t\" width=\"20\">&nbsp;</td>");
	wo.writeln("<td nowrap unselectable=\"on\" class=\"t\" width=\"20\">&nbsp;</td>");
	wo.writeln("<td nowrap unselectable=\"on\" class=\"t\" width=\"20\">&nbsp;</td>");

	if (vi.check_name)			wo.writeln("<td nowrap unselectable=\"on\" class=\"t100\">&nbsp;" + vr.descr[0] + "&nbsp;</td>");
	if (vi.check_title)			wo.writeln("<td nowrap unselectable=\"on\" class=\"t100\">&nbsp;" + vr.descr[1] + "&nbsp;</td>");
	if (vi.check_type)			wo.writeln("<td nowrap unselectable=\"on\" class=\"t75\">&nbsp;"  + vr.descr[2] + "&nbsp;</td>");
	if (vi.check_size)			wo.writeln("<td nowrap unselectable=\"on\" class=\"t75\">&nbsp;"  + vr.descr[3] + "&nbsp;</td>");
	if (vi.check_workflow)		wo.writeln("<td nowrap unselectable=\"on\" class=\"t100\">&nbsp;" + vr.descr[4] + "&nbsp;</td>");
	if (vi.check_permissions)		wo.writeln("<td nowrap unselectable=\"on\" class=\"t75\">&nbsp;"  + vr.descr[5] + "&nbsp;</td>");
	if (vi.check_dateLastModified)		wo.writeln("<td nowrap unselectable=\"on\" class=\"t125\">&nbsp;" + vr.descr[6] + "&nbsp;</td>");
	if (vi.check_userWhoLastModified)	wo.writeln("<td nowrap unselectable=\"on\" class=\"t125\">&nbsp;"  + vr.descr[7] + "&nbsp;</td>");
	if (vi.check_dateCreated)		wo.writeln("<td nowrap unselectable=\"on\" class=\"t125\">&nbsp;" + vr.descr[8] + "&nbsp;</td>");
	if (vi.check_userWhoCreated)		wo.writeln("<td nowrap unselectable=\"on\" class=\"t125\">&nbsp;"  + vr.descr[9] + "&nbsp;</td>");
	if (vi.check_dateReleased)		wo.writeln("<td nowrap unselectable=\"on\" class=\"t125\">&nbsp;" + vr.descr[10] + "&nbsp;</td>");
	if (vi.check_dateExpired)		wo.writeln("<td nowrap unselectable=\"on\" class=\"t125\">&nbsp;" + vr.descr[11] + "&nbsp;</td>");
	if (vi.check_state)			wo.writeln("<td nowrap unselectable=\"on\" class=\"t75\">&nbsp;"  + vr.descr[12] + "&nbsp;</td>");
	if (vi.check_lockedBy)			wo.writeln("<td nowrap unselectable=\"on\" class=\"t100\">&nbsp;"  + vr.descr[13] + "&nbsp;</td>");


	wo.writeln("</tr>");

	for (var i = 0; i < vi.liste.length; i++) {

		var vi_icon;
		var vi_text;
		var noaccess = false;

		if (typeof vi.resource[vi.liste[i].type] == 'undefined') {
			// type does not exist, the user has no access to this resource type
			noaccess = true;
			vi_icon = vi.resource[plainresid].icon;
			vi_text = vi.resource[plainresid].text;
		} else if (vi.resource[vi.liste[i].type].editable == false) {
			// type exists but the user has no access to this resource type
			noaccess = true;
			vi_icon = vi.resource[plainresid].icon;
			vi_text = vi.resource[vi.liste[i].type].text;
		} else {
			vi_icon = vi.resource[vi.liste[i].type].icon;
			vi_text = vi.resource[vi.liste[i].type].text;
		}

		ssclass = "class=\"";

		if (!vi.liste[i].isInsideCurrentProject || noaccess) {
			ssclass += "fp";
		} else {
			if (vi.liste[i].state == 0)
			ssclass += "nf";
			if (vi.liste[i].state == 1)
			ssclass += "fc";
			if (vi.liste[i].state == 2)
			ssclass += "fn";
			if (vi.liste[i].state == 3)
			ssclass += "fd";
		}


		ssclass += "\"";

		if ((vi.liste[i].layoutstyle) == 1) ssclass += " style=\"font-style:italic;\"";
		if ((vi.liste[i].layoutstyle) == 2) ssclass += " style=\"font-style:italic;\"";

		var vi_bg = "";
		if (vi.liste[i].linkType != 0) {
			vi_bg = " style=\"background-image:url(" + vi_icon + "); background-position: 1px 1px; background-repeat: no-repeat; \"";
			vi_icon = vi.skinPath + 'explorer/';
			if (vi.liste[i].linkType == 2) {
				vi_icon += 'link_labeled.gif';
			} else {
				vi_icon += 'link.gif';
			}
		}

		wo.writeln("<tr>");
		wo.write("<td unselectable=\"on\" id=\"td0_" + i + "\"" + vi_bg + ">");

		if (showKon && !noaccess) {
			wo.write("<a style=\"cursor:pointer;\"");
			wo.write(" onmouseover=\"top.setId(" + i + ")\" onmouseout=\"top.setId(-1)\">");
		}
		wo.write("<img id=\"ic" + i + "\" src='" + vi_icon + "' border=0 width=16 height=16>");
		if (showKon && !noaccess) {
			wo.write("</a>");
		}
		wo.writeln("</td>");

		if (vi.liste[i].isInsideCurrentProject) {
			wo.write("<td unselectable=\"on\" id=\"td1_" + i + "\">");
			// the resource is in the current project, so display the lock and project state

			var lockIcon;

			if (vi.liste[i].lockedBy != "") {
				if ((vr.userName == vi.liste[i].lockedBy) && (vi.liste[i].lockedInProjectId == vr.actProject)) {
					if (vi.liste[i].lockType == 1 || vi.liste[i].lockType == 2) {
						lockIcon = vi.skinPath + 'explorer/lock_shared.gif';
					} else {
						lockIcon = vi.skinPath + 'explorer/lock_user.gif';
					}
					lockedBystring = vr.altlockedby + " " + vi.liste[i].lockedBy + vr.altlockedin + vi.liste[i].lockedInProjectName;
				  wo.write("<img src=\"" + lockIcon + "\" alt=\"" + lockedBystring + "\" title=\"" + lockedBystring + "\" border=\"0\" width=\"16\" height=\"16\"></a>");

				} else if (vi.liste[i].lockType != 5){
					lockIcon = vi.skinPath + 'explorer/lock_other.gif';
				lockedBystring = vr.altlockedby + " " + vi.liste[i].lockedBy + vr.altlockedin + vi.liste[i].lockedInProjectName;
				wo.write("<img src=\"" + lockIcon + "\" alt=\"" + lockedBystring + "\" title=\"" + lockedBystring + "\" border=\"0\" width=\"16\" height=\"16\"></a>");
			}
			}
			wo.write("</td>");

			wo.write("<td unselectable=\"on\" id=\"td2_" + i + "\">");
			var projectIcon;
			var projectAltText;
			if (vi.liste[i].workflowInfo != "") {
				projectIcon = vi.skinPath + 'explorer/project_myworkflow.png';
				projectAltText = vi.liste[i].workflowInfo;	
			} else if (vi.liste[i].state != 0) {
				if (vi.liste[i].project == vr.actProject) {
					projectIcon = vi.skinPath + 'explorer/project_this.png';
					projectAltText = vr.altbelongto + vi.liste[i].lockedInProjectName;
				} else if (vi.liste[i].lockType == 5) {
					projectIcon = vi.skinPath + 'explorer/project_other.png';
					projectAltText = vr.altbelongto + vi.liste[i].lockedInProjectName;					
				} else {
					projectIcon = vi.skinPath + 'explorer/project_other.png';
					projectAltText = vr.altbelongto + vi.liste[i].lockedInProjectName;
				}			
			} else {
				projectIcon = vi.skinPath + 'explorer/project_none.gif';
				projectAltText = "";
			}

			wo.write("<img src=\"" + projectIcon + "\" alt=\"" + projectAltText + "\" title=\"" + projectAltText + "\" border=\"0\" width=\"16\" height=\"16\"></a>");
			wo.write("</td>\n");
		} else if (vi.liste[i].workflowInfo != "") {
			projectIcon = vi.skinPath + 'explorer/project_otherworkflow.png';
			projectAltText = vi.liste[i].workflowInfo;
			wo.write("<td unselectable=\"on\" id=\"td1_" + i + "\"></td>\n");
			wo.write("<td unselectable=\"on\" id=\"td2_" + i + "\">");
			wo.write("<img src=\"" + projectIcon + "\" alt=\"" + projectAltText + "\" title=\"" + projectAltText + "\" border=\"0\" width=\"16\" height=\"16\"></a>");
			wo.write("</td>\n");							
		} else {
			// nothing to do here
			wo.write("<td unselectable=\"on\" id=\"td1_" + i + "\"></td>\n");
			wo.write("<td unselectable=\"on\" id=\"td2_" + i + "\"></td>\n");
		}

		if (vi.check_name) {
			wo.write("<td nowrap unselectable=\"on\" id=\"td3_" + i + "\" " + ssclass + ">&nbsp;");
			if (mode == "listview") {
				wo.write("<a onclick=\"top.openwinfull('");
				wo.write(vi.liste[i].path);
				wo.write("');\"")
				wo.write(" onmouseover=\"top.linkOver(this, " + i + ")\" onmouseout=\"top.linkOut(this)\"");
				wo.writeln(" id=\"a" + i + "\" " + ssclass + ">" + vi.liste[i].path + "</a>");
			} else {
				if (vi.liste[i].isFolder) {
					if (mode == "galleryview" || showlinks) {
						wo.write(vi.liste[i].path);
					} else if (vi.liste[i].state == 3) {
						wo.write(vi.liste[i].name);
					} else {
						wo.write("<a onclick=\"top." + openfolderMethod + "('" + vi.liste[i].name + "')\"");
						wo.write(" onmouseover=\"top.linkOver(this, " + i + ")\" onmouseout=\"top.linkOut(this)\"");
						wo.write(" id=\"a" + i + "\" " + ssclass + ">");
						wo.write(vi.liste[i].name);
						wo.write("</a>");
					}
				} else {
					if ((mode == "galleryview") || showlinks) {
						wo.writeln(vi.liste[i].path);
					} else if (vi.liste[i].state == 3) {
						wo.write(vi.liste[i].name);
					} else if (flaturl != "") {
						wo.write("<a onclick=\"top.openwinfull('");
						wo.write(vr.actDirectory + vi.liste[i].name);
						wo.write("');\"");
						wo.write(" onmouseover=\"top.linkOver(this, " + i + ")\" onmouseout=\"top.linkOut(this)\"");
						wo.writeln("id=\"a" + i + "\" " + ssclass + ">&" + vi.liste[i].name + "</a>");
					} else {
						wo.write("<a onclick=\"top.openwinfull('");
						wo.write(vr.actDirectory + vi.liste[i].name);
						wo.write("');\"");
						wo.write(" onmouseover=\"top.linkOver(this, " + i + ")\" onmouseout=\"top.linkOut(this)\"");
						wo.writeln(" id=\"a" + i + "\" " + ssclass + ">" + vi.liste[i].name + "</a>");
					}
				}
			}
			wo.writeln("</td>");
		}
		var ressize = (vi.liste[i].isFolder) ? "" : "" + vi.liste[i].size;
		var lockedBy = (vi.liste[i].isInsideCurrentProject && vi.liste[i].lockType != 5) ? vi.liste[i].lockedBy : "";
		if (vi.check_title)			wo.writeln("<td nowrap unselectable=\"on\" " + ssclass + ">&nbsp;" + vi.liste[i].title + "&nbsp;</td>");
		if (vi.check_type)			wo.writeln("<td nowrap unselectable=\"on\" " + ssclass + ">&nbsp;" + vi_text + "</td>");
		if (vi.check_size)			wo.writeln("<td nowrap unselectable=\"on\" " + ssclass + ">&nbsp;" + ressize + "</td>");
		if (vi.check_workflow)	wo.writeln("<td nowrap unselectable=\"on\" " + ssclass + ">&nbsp;" + vi.liste[i].workflowState + "</td>");
		if (vi.check_permissions)		wo.writeln("<td nowrap unselectable=\"on\" " + ssclass + ">&nbsp;" + vi.liste[i].permissions + "</td>");
		if (vi.check_dateLastModified)		wo.writeln("<td nowrap unselectable=\"on\" " + ssclass + ">&nbsp;" + vi.liste[i].dateLastModified + "</td>");
		if (vi.check_userWhoLastModified)	wo.writeln("<td nowrap unselectable=\"on\" " + ssclass + ">&nbsp;" + vi.liste[i].userWhoLastModified + "</td>");
		if (vi.check_dateCreated)		wo.writeln("<td nowrap unselectable=\"on\" " + ssclass + ">&nbsp;" + vi.liste[i].dateCreated + "</td>");
		if (vi.check_userWhoCreated)		wo.writeln("<td nowrap unselectable=\"on\" " + ssclass + ">&nbsp;" + vi.liste[i].userWhoCreated + "</td>");
		if (vi.check_dateReleased)		wo.writeln("<td nowrap unselectable=\"on\" " + ssclass + ">&nbsp;" + vi.liste[i].dateReleased + "</td>");
		if (vi.check_dateExpired)		wo.writeln("<td nowrap unselectable=\"on\" " + ssclass + ">&nbsp;" + vi.liste[i].dateExpired + "</td>");
		if (vi.check_state)			wo.writeln("<td nowrap unselectable=\"on\" " + ssclass + ">&nbsp;" + vr.stati[vi.liste[i].state] + "</td>");
		if (vi.check_lockedBy)			wo.writeln("<td nowrap unselectable=\"on\" " + ssclass + ">&nbsp;" + lockedBy + "</td>");

		wo.writeln("</td></tr>");
	}

	wo.writeln("</tr></table>");

	// create multi context menu form
	wo.writeln("<form name=\"formmulti\" action=\"\" method=\"post\">");
	wo.writeln("<input type=\"hidden\" name=\"resourcelist\" value=\"\">");
	wo.writeln("</form>");

	// create div for context menus
	wo.writeln("<div id=\"contextmenu\" class=\"cm\"></div>");

	wo.write("<br></body></html>");
	wo.close();
}


// Returns the absolute path of the resource with the index i
function getResourceAbsolutePath(i) {

	var resourceName = vr.actDirectory + vi.liste[i].name;
	if ((mode == "listview") || (mode == "galleryview") || showlinks) {
		if (vi.liste[i].type == 0) {
			resourceName = vi.liste[i].path.substring(0, vi.liste[i].path.lastIndexOf("/"));
		} else {
			resourceName = vi.liste[i].path;
		}
	}
	return resourceName;
}


function simpleEscape(text) {
	return text.replace(/ \ //g, "%2F");
}


function openwinfull(url) {
	if (cancelNextOpen) {
		return;
	}
	if (url != '#') {
		w = screen.availWidth - 50;
		h = screen.availHeight - 200;
		workplace = window.open(vr.servpath + link_showresource + "?resource=" + url, 'preview', 'toolbar = yes, location = yes, directories = no, status = yes, menubar = 1, scrollbars = yes, resizable = yes, left = 20, top = 20, width = '+w+', height = '+h);
		if (workplace != null) {
			workplace.focus();
		}
	}
}


function display_ex() {
	if(window.body.explorer_content.explorer_head) {
		explorer_head = window.body.explorer_content.explorer_head.document;
	}
}


function submitResource() {
	setDisplayResource(getRootFolder() + win.head.forms.urlform.resource.value.substring(1));
	openurl();
}


function openurl() {
	updateTreeFolder(getDisplayResource());
	try {
		win.files.open();
	} catch (e) {
		updateWindowStore();
		win.files.open();
	}
	win.files.writeln("<html>");
	win.files.writeln("<head><meta HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=" + top.frames.head.encoding + "\"></head>");
	win.files.writeln("<body>\n<center><br><br><br><br><font face=Helvetica size=2>"+vr.langloading+"</center></body>\n</html>");
	win.files.close();
	var selectedpage = "";
	if(win.head.forms.urlform && win.head.forms.urlform.pageSelect){
		selectedpage = "&page=" + win.head.forms.urlform.pageSelect.value;
	}
	win.files.location.href = vr.servpath + "/system/workplace/views/explorer/explorer_files.jsp?resource=" + getDisplayResource() + selectedpage;
}


function addProjectDir(nodid) {
	var pfad = "";
	while (nodid != tree.root.id) {
		var nodeName = '_n' + nodid;
		pfad  = tree.nodes[nodeName].name + "/" + pfad;
		nodid = tree.nodes[nodeName].parent.id;
		test  = tree.nodes[nodeName].parent.id;
	}
	pfad = '/' + pfad;
	win.files.forms[0].tempFolder.value = pfad;
	if (win.files.copySelection) {
		win.files.copySelection();
	}
}


function dirUp(){
	var temp;
	var marke=0;
	var directory = removeSiblingPrefix(getDisplayResource());
	var zaehler=0;
	var newDir = directory.substring(0, directory.length - 1);
	var res = newDir.substring(0, newDir.lastIndexOf("/") + 1);

	if (res.length < (getRootFolder().length + 1)) {
		res = getRootFolder();
	}
	setDisplayResource(res);
	openurl();
}


function removeSiblingPrefix(directory) {

	if (directory.indexOf("siblings:") == 0) {
		directory = directory.substring(9);
		var lastSlashPos = directory.lastIndexOf("/");
		if (lastSlashPos != (directory.length - 1)) {
			directory = directory.substring(0, lastSlashPos + 1);
		}
	}
	return directory;
}


// output the html for the head
function displayHead(doc, pages, actpage){

	var btUp = "";
	var btWizard = "";
	var btUpload = "";
	var btSearch = "";
	var pageSelect = "";

	if(vr.actDirectory == getRootFolder()) {
		btUp = button(null, null, "folder_up_in.png", vr.langup, buttonType);
	} else {
		btUp = button("javascript:top.dirUp();", null, "folder_up.png", vr.langup, buttonType);
	}

	if((vr.actProject != vr.onlineProject) && (vi.newButtonActive == true)) {
		btWizard = button(vr.servpath + link_newresource, "explorer_files", "wizard.png", vr.langnew, buttonType);
		if(vr.showUpload) {
			btUpload = button(vr.servpath + link_uploadresource, "explorer_files", "upload.png", vr.langupload, buttonType);
		}
	} else {
		btWizard = button(null, null, "wizard_in.png", vr.langnew, buttonType);
		if(vr.showUpload) {
			btUpload = button(null, null, "upload_in.png", vr.langupload, buttonType);
		}
	}

	btSearch = button(vr.servpath + link_searchresource, "explorer_files", "ex_search.png", vr.langsearch, buttonType);

	if(pages > 1){
		pageSelect=
		"<td>&nbsp;&nbsp;"+vr.langpage+"&nbsp;</td>"
		+ "<td class=menu>"
		+ "<select name=\"pageSelect\" class=\"location\" onchange=\"top.openurl();return false;\">";
		for(i=1; i<=pages; i++){
			if(i==actpage){
				pageSelect+="<option value='"+i+"' selected>"+i;
			} else {
				pageSelect+="<option value='"+i+"'>"+i;
			}
		}
		pageSelect+="</select></td>";
	}

	var html =
	"<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\">\n"
	+ "<html>\n<head>\n"
	+ "<META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=" + top.frames.head.encoding + "\">\n"
	+ "<link rel=\"stylesheet\" type=\"text/css\" href=\"" + vi.stylePath + "\"></link>\n"

	+ "<style type='text/css'>\n"
	+ "input.location { font-family: Verdana, Arial, Helvetica, sans-serif; font-size: 11px; font-weight: normal; width: 99% }\n"
	+ "select.location { font-family: Verdana, Arial, Helvetica, sans-serif; font-size: 11px; font-weight: normal; width: 50px }\n"
	+ "</style>\n"

	+ "<script type=\"text/javascript\">\n"
	+ "<!--\n"
	+ "function doSet() {\n"
	+ "\tdocument.urlform.resource.value=\"" + getDisplayResource("true") + "\";\n"
	+ "}\n"
	+ "//-->\n"
	+ "</script>\n"

	+ "</head>\n"
	+ "<body class=\"buttons-head\" onload=\"window.setTimeout('doSet()',50);\">\n"
	+ "<table cellspacing=\"0\" cellpadding=\"0\" border=\"0\">\n"
	+ "<form name=\"urlform\" onsubmit=\"top.submitResource();return false;\">\n"
	+ "<tr>\n"

	+ buttonSep(0, 0, 0)
	+ button("javascript:top.histGoBack();", null, "back.png", vr.langback, buttonType)
	+ btUpload
	+ btSearch
	+ btWizard
	+ btUp

	+ buttonSep(5, 5, 1)
	+ "<td>"+vr.langadress+"&nbsp;</td>\n"
	+ "<td width=\"100%\"><input value=\"\" maxlength=\"255\" name=\"resource\" class=\"location\"></td>\n"
	+ pageSelect

	+ "</tr>\n</form>\n</table>\n"
	+ "</body>\n</html>";

	doc.open();
	doc.write(html);
	doc.close();
}


// formats a button in one of 3 styles (type 0..2)
function button(href, target, image, label, type) {

	if (image != null && image.indexOf('.') == -1) {
        // append default suffix for images
        image += ".png";
    }

	var result = "<td>";
	switch (type) {
		case 1:
		// image and text
		if (href != null) {
			result += "<a href=\"";
			result += href;
			result += "\" class=\"button\"";
			if (target != null) {
				result += " target=\"";
				result += target;
				result += "\"";
			}
			result += ">";
		}
		result += "<span unselectable=\"on\"";
		if (href != null) {
			result += " class=\"norm\" onmouseover=\"className='over'\" onmouseout=\"className='norm'\" onmousedown=\"className='push'\" onmouseup=\"className='over'\"";
		} else {
			result += " class=\"disabled\"";
		}
		result += "><span unselectable=\"on\" class=\"combobutton\" ";
		result += "style=\"background-image: url('";
		result += vi.skinPath;
		result += "buttons/";
		result += image;
		result += "');\">";
		result += label;
		result += "</span></span>";
		if (href != null) {
			result += "</a>";
		}
		break;

		case 2:
		// text only
		if (href != null) {
			result += "<a href=\"";
			result += href;
			result += "\" class=\"button\"";
			if (target != null) {
				result += " target=\"";
				result += target;
				result += "\"";
			}
			result += ">";
		}
		result += "<span unselectable=\"on\"";
		if (href != null) {
			result += " class=\"norm\" onmouseover=\"className='over'\" onmouseout=\"className='norm'\" onmousedown=\"className='push'\" onmouseup=\"className='over'\"";
		} else {
			result += " class=\"disabled\"";
		}
		result += "><span unselectable=\"on\" class=\"txtbutton\">";
		result += label;
		result += "</span></span>";
		if (href != null) {
			result += "</a>";
		}
		break;

		default:
		// only image
		if (href != null) {
			result += "<a href=\"";
			result += href;
			result += "\" class=\"button\"";
			if (target != null) {
				result += " target=\"";
				result += target;
				result += "\"";
			}
			result += " title=\"";
			result += label;
			result += "\">";
		}
		result += "<span unselectable=\"on\"";
		if (href != null) {
			result += " class=\"norm\" onmouseover=\"className='over'\" onmouseout=\"className='norm'\" onmousedown=\"className='push'\" onmouseup=\"className='over'\"";
		} else {
			result += " class=\"disabled\"";
		}
		result += "><img class=\"button\" src=\"";
		result += vi.skinPath;
		result += "buttons/";
		result += image;
		result += "\">";
		result += "</span>";
		break;
	}
	result += "</td>\n";
	return result;
}


// formats a button separator line
function buttonSep(left, right, type) {
	var style = "starttab";
	if (type == 1) {
		style = "separator";
	}
	var result = "<td><span class=\"norm\"><span unselectable=\"on\" class=\"txtbutton\" style=\"padding-right: 0px; padding-left: " + left + "px;\"></span></span></td>\n"
	+ "<td><span class=\"" + style + "\"></span></td>\n"
	+ "<td><span class=\"norm\"><span unselectable=\"on\" class=\"txtbutton\" style=\"padding-right: 0px; padding-left: " + right + "px;\"></span></span></td>\n";
	return result;
}


function setOnlineProject(id){
	vr.onlineProject=id;
}


function setProject(id){
	vr.actProject=id;
}


function setDirectory(id, dir){
	displayResource = dir;
	if (mode == "explorerview") {
		addHist(dir);
	}
	vr.actDirId=id;
	dir = removeSiblingPrefix(dir);
	vr.actDirectory = dir;
	last_id = -1;
	selectedResources = new Array();
}


function enableNewButton(showit){
	vi.newButtonActive=showit;
}


function openFolder(folderName) {
	if (cancelNextOpen) {
		return;
	}
	if (folderName.charAt(0) != '/') {
		folderName = getDisplayResource() + folderName + "/";
	}
	setDisplayResource(folderName);
	openurl();
}


function openthisfolderflat(thisdir){
	if (cancelNextOpen) {
		return;
	}
	eval(flaturl + "?resource=" + vr.actDirectory+thisdir+"/\"");
}


function updateTreeFolder(folderName) {
	if (window.body.explorer_body && window.body.explorer_body.explorer_tree) {
		window.body.explorer_body.explorer_tree.updateCurrentFolder(window.body.explorer_body.explorer_tree.tree_display.document, folderName, false);
	}
}


function addNodeToLoad(nodeName) {
	if (window.body.explorer_body && window.body.explorer_body.explorer_tree) {
		window.body.explorer_body.explorer_tree.addNodeToLoad(null, nodeName);
	}
}


function reloadNodeList() {
	if (window.body.explorer_body && window.body.explorer_body.explorer_tree) {
		window.body.explorer_body.explorer_tree.loadNodeList(window.body.explorer_body.explorer_tree.tree_display.document, "&rootloaded=true");
	}
}


// returns the X position of an object in the body
function findPosX(obj) {
	var curleft = 0;
	if (obj.offsetParent) {
		while (obj.offsetParent) {
			curleft += obj.offsetLeft;
			obj = obj.offsetParent;
		}
	} else if (obj.x) {
		curleft += obj.x;
	}
	return curleft;
}


// returns the Y position of an object in the body
function findPosY(obj) {
	var curtop = 0;
	if (obj.offsetParent) {
		while (obj.offsetParent) {
			curtop += obj.offsetTop;
			obj = obj.offsetParent;
		}
	} else if (obj.y) {
		curtop += obj.y;
	}
	return curtop;
}


function getMenuPosY(doc, id) {

	var y = findPosY(doc.getElementById("ic" + id)) + 16;
	var scrollTop = 0;
	var clientHeight = 0;

	if (doc.documentElement && (doc.documentElement.scrollTop || doc.documentElement.clientHeight)) {
		scrollTop = doc.documentElement.scrollTop;
		clientHeight = doc.documentElement.clientHeight;
	} else if (doc.body) {
		scrollTop = doc.body.scrollTop;
		clientHeight = doc.body.clientHeight;
	}

	var el = doc.getElementById("contextmenu");
	var elementHeight = el.scrollHeight;
	var oy = y;
	var scrollSize = 20;

	if ((y + elementHeight + scrollSize) > (clientHeight + scrollTop)) {
		y = y - 16 - elementHeight;
	}
	if (y < scrollTop) {
		y = (clientHeight + scrollTop) - (elementHeight + scrollSize);
	}
	if (y < scrollTop) {
		y = scrollTop;
	}
	return y;
}


function menu(nr) {
	this.nr = nr;
	this.items = new Array();
}


function addMenuEntry(nr,text,link,target,rules){
	if(!vi.menus[nr])vi.menus[nr] = new menu(vi.menus.length);
	vi.menus[nr].items[vi.menus[nr].items.length] = new menuItem(text,link,target,rules);
}


var treewin = null;
var treeForm = null;
var treeField = null;
var treeDoc = null;

function openTreeWin(treeType, includeFiles, formName, fieldName, curDoc) {
	var paramString = "";
	if (treeType) {
		paramString += "?type=" + treeType;
	}
	if (includeFiles) {
		paramString += ((paramString == "")?"?":"&");
		paramString += "includefiles=true";
	}
	var target = vr.servpath + "/system/workplace/views/explorer/tree_fs.jsp" + paramString;
	treewin = openWin(target, "opencms", 300, 450);
	if (treewin.opener == null){
		treewin.opener = self;
	}
	treeForm = formName;
	treeField = fieldName;
	treeDoc = curDoc;
}


function openWin(url, name, w, h) {
	var newwin = window.open(url, name, 'toolbar=no,location=no,directories=no,status=yes,menubar=0,scrollbars=yes,resizable=yes,top=150,left=660,width='+w+',height='+h);
	if(newwin != null) {
		if (newwin.opener == null) {
			newwin.opener = self;
		}
	}
	newwin.focus();
	return newwin;
}


function closeTreeWin() {
	if (treewin != null) {
		window.treewin.close();
		treewin = null;
		treeForm = null;
		treeField = null;
		treeDoc = null;
	}
}


function setFormValue(filename) {
	// target form where the link is to be pasted to
	var curForm;
	// the document of the target form
	var curDoc;
	// update the window store
	updateWindowStore();

	if (treeDoc != null) {
		curDoc = treeDoc;
	} else {
		curDoc = win.files;
	}

	if (treeForm != null) {
		curForm = curDoc.forms[treeForm];
	} else {
		curForm = curDoc.forms[0];
	}

	if (curForm.elements[treeField]) {
		curForm.elements[treeField].value = filename;
	} else if (curForm.folder) {
		curForm.folder.value = filename;
	} else if (curForm.target) {
		curForm.target.value = filename;
	}

	// this calls the fillValues() function in the explorer window, if present
	if (window.body.explorer_body && window.body.explorer_body.explorer_files) {
		var filesDoc = window.body.explorer_body.explorer_files;
		if (filesDoc.fillValues) {
			filesDoc.fillValues(filename);
		}
	}

	// this fills the parameter from the hidden field to the select box
	if (window.body.admin_content) {
		if (treeField == "tempChannel") {
			window.body.admin_content.copyChannelSelection();
		} else {
			try {
				window.body.admin_content.copySelection();
			} catch (e) {

			}
		}
	}
}
