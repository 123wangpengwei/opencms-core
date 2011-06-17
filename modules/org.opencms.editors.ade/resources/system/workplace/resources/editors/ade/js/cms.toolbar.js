﻿(function(cms) {
   var $ = jQuery;
   var M = cms.messages;
   var galleryInitialized = false;
   /** current toolbar-mode ('Move', 'Edit', 'Delete' etc.) */
   var /** string */ mode = cms.toolbar.mode = '';
   
   /** hashmap for jquery-dom-objects */
   var /** map */ dom = cms.toolbar.dom = {};
   
   /** jquery-document.body-object */
   var /** jquery */ _bodyEl;
   
   /** original body offset */
   var /** integer */ _oldBodyMargin = 0;
   
   /** favorite-list ids */
   /** array */
   cms.toolbar.favorites = [];
   
   /** recent-list ids */
   /** array */
   cms.toolbar.recent = [];
   
   /** max-size of recent-list */
   /** integer */
   cms.toolbar.recentSize = 10;
   
   /** flag to indicate all necessary data has been loaded */
   var /** boolean */ toolbarReady = cms.toolbar.toolbarReady = false;
   
   /** flag to indicate the user is leaving the page */
   var /** boolean */ leavingPage = cms.toolbar.leavingPage = false;
   
   /** id of current menu */
   /** string */
   cms.toolbar.currentMenu = cms.html.favoriteMenuId;
   
   /** id of current menu item list */
   /** string */
   cms.toolbar.currentMenuItems = cms.html.favoriteListId;
   
   cms.toolbar.editingSubcontainerId = null;
   
   /**
    * This function will check if the given mode-string is one of the three editing modes (edit, move, delete).<p>
    *
    * @param {String} mode
    * @return {boolean}
    */
   var _isEditingMode = function(/** String */mode) {
      return (modeMap[mode].isEdit);
   }
   
   
   /**
    * Dummy function which does nothing.
    */
   var doNothing = function() {
      }
   
   /**
    * This function will open the sub container editing mode.
    *
    * @param {Object} container the sub container to edit
    */
   var editSubcontainer = function(container) {
      toggleMode(getCurrentMode());
      var isNew = container.hasClass('cms-new-element');
      var parentContainerElement = container.parent();
      var containerName = parentContainerElement.attr('id');
      var parentContainer = cms.data.containers[containerName];
      var cOffset = container.offset();
      cms.toolbar.editingSubcontainerId = container.attr('rel');
      var containerElement = cms.data.elements[cms.toolbar.editingSubcontainerId];
      if (isNew || containerElement['types'].length == 0) {
         containerElement['types'].push(parentContainer['type']);
      }
      cms.toolbar.dom.appendBox.css('position', 'absolute');
      var overlay = $('<div id="cms-overlay"></div>').appendTo(parentContainerElement);
      var overlayEditing = $(cms.html.subcontainerDialog(containerElement)).appendTo(parentContainerElement);
      overlayEditing.css({
         'top': container.position().top - 2,
         'left': container.position().left - 345
      });
      if (isNew) {
         container.removeClass('cms-new-element');
      }
      container.attr('id', cms.toolbar.editingSubcontainerId).addClass('cms-editing-subcontainer');
      container.css('z-index', 1000);
      //      cms.move.addSubcontainerBackground(container, true);
      cms.data.containers[cms.toolbar.editingSubcontainerId] = new cms.data.Container({
         'elements': containerElement.subItems,
         'name': cms.toolbar.editingSubcontainerId,
         'type': parentContainer.type,
         'maxElem': parentContainer.maxElem,
         'objType': parentContainer.objType
      });
      overlayEditing.find('button[name="subcontainerSave"]').click(function() {
         toggleMode(getCurrentMode());
         var subItems = new Array();
         var subElements = new Array();
         container.children('.cms-element').each(function() {
            var itemId = $(this).attr('rel')
            subItems.push(itemId);
            subElements.push({
               'id': itemId,
               'uri': cms.data.elements[itemId]['file']
            });
         });
         containerElement.subItems = subItems;
         containerElement['title'] = $('input[name="title"]', overlayEditing).val();
         containerElement['description'] = $('input[name="description"]', overlayEditing).val();
         var postElement = {
            'file': containerElement['file'],
            'title': containerElement['title'],
            'description': containerElement['description'],
            'types': containerElement['types'],
            'subItems': subElements
         };
         if (isNew) {
            var parentContainerType = parentContainer.type;
            cms.data.postJSON('newsub', {
               'types' : [parentContainerType],
               'elem': postElement
            }, function(ok, data) {
               var newId = data['id'];
               var oldId = containerElement['id'];
               delete cms.data.elements[oldId];
               var element = data.elements[newId];
               cms.data.elements[newId] = element;
               container.attr('rel', newId);
               container.removeClass('cms-new-element');
               cms.util.replaceNewElement(oldId, newId);
            }, true);
         } else {
            cms.data.postJSON('subcnt', {
               'elem': postElement
            }, function(ok, data) {
                        //TODO: after post
            });
         }
         delete cms.data.containers[cms.toolbar.editingSubcontainerId];
         container.removeAttr('id').removeClass('cms-editing-subcontainer').css('z-index', '');
         //         cms.move.removeSubcontainerBackground(container);
         overlay.remove();
         overlayEditing.remove();
         cms.toolbar.editingSubcontainerId = null;
         cms.move.refreshNewElementHighlighting();
      });
      overlayEditing.find('button[name="subcontainerCancel"]').click(function() {
         toggleMode(getCurrentMode());
         delete cms.data.containers[cms.toolbar.editingSubcontainerId];
         container.replaceWith(containerElement.getContent(parentContainer['type']));
         overlay.remove();
         overlayEditing.remove();
         //         if (isNew){
         //             container.addClass('cms-new-element');
         //         }
         cms.toolbar.editingSubcontainerId = null;
         cms.move.refreshNewElementHighlighting();
      });
   }
   
   /**
    * This function will display the publish dialog.<p>
    */
   var showPublishList = cms.toolbar.showPublishList = /** void */ function() {
      var button = $(this);
      if (button.hasClass('ui-state-active')) {
      
      
         markAsInactive(button);
      } else {
         $('button.ui-state-active').trigger('click');
         // appending publish-dialog content
         $(document.body).append(cms.publish.publishDialog);
         
         var buttons = {};
         buttons[M.GUI_PUBLISH_DIALOG_CANCEL_0] = function() {
            $(this).dialog('close');
         }
         
         buttons[M.GUI_PUBLISH_DIALOG_OK_0] = function() {
            $(this).dialog('close');
         }
         $('#' + cms.publish.publishDialogId).dialog({
            buttons: buttons,
            width: 340,
            title: M.GUI_PUBLISH_DIALOG_TITLE_0,
            modal: true,
            autoOpen: true,
            draggable: true,
            resizable: false,
            position: ['center', 20],
            close: function() {
               markAsInactive($('button[name="Publish"]'));
               $('#' + cms.publish.publishDialogId).dialog('destroy');
            },
            zIndex: 10000
         });
         $('#' + cms.publish.publishDialogId + ' span.cms-check-icon').click(function() {
            $(this).toggleClass('cms-check-icon-inactive')
         });
         
         markAsActive(button);
      }
   };
   
   /**
    * Marks a button as active
    * @param {Object} $button
    */
   var markAsActive = function($button) {
      $button.addClass('ui-state-active');
   }
   
   /**
    * Marks a button as inactive.
    * @param {Object} $button
    */
   var markAsInactive = function($button) {
      $button.removeClass('ui-state-active');
   }
   
   /**
    * This event-handler function will remove an element from a container.
    */
   var deleteItem = cms.toolbar.deleteItem = /** void */ function() {
      var $item = $(this).closest('.cms-element');
      if (!$item || !$item.length) {
         $item = $(this).closest('.' + cms.html.subcontainerClass);
      }
      var $container = $item.parent();
      
      cms.move.removeElementHighlighting();
      if ($item.hasClass('cms-new-element')) {
         cms.move.removeNewElementHighlighting($item);
      }
      var elemId = $item.attr('rel');
      if (elemId && cms.data.elements[elemId]) {
      
         // a resource that has been edited and saved should not be removed from the vfs without comment/warning.
         
         //         if (cms.data.elements[elemId].status == cms.data.STATUS_CREATED) {
         //            cms.data.deleteResources([elemId], function(ok) {
         //               deleteFromFavListAndRecList([elemId]);
         //               if (!ok) {
         //                  // TODO
         //                  alert("error");
         //               }
         //            });
         //         } else {
         //            addToRecent($item.attr('rel'));
         //         }
         
         addToRecent($item.attr('rel'));
      }
      $item.remove();
      cms.move.updateContainer($container.attr('id'));
      if (cms.toolbar.editingSubcontainerId == null) {
         setPageChanged(true);
      }
   };
   
   /**
    * This event-handler function will delete an element from the vfs. It is to be assigned to delete buttons in content-collector lists only.
    * It matches the functionallity of the 'old' direct edit.
    */
   var directDeleteItem = cms.toolbar.directDeleteItem = /** void */ function() {
      var elemId = $(this).closest('.cms-editable').attr('rel');
      var buttons = {};
      buttons[M.GUI_DIRECT_DELETE_CANCEL_0] = function() {
         $(this).dialog('close');
      }
      
      buttons[M.GUI_DIRECT_DELETE_OK_0] = function() {
         $(this).dialog('close');
         cms.data.deleteResources([elemId], function(ok) {
            var elemList = [elemId];
            deleteFromFavListAndRecList(elemList);
            $(cms.util.getContainerSelector()).find('.cms-element[rel="' + elemId + '"], .' + cms.html.subcontainerClass + '[rel="' + elemId + '"]').remove();
            for (var key in cms.data.containers) {
               cms.move.updateContainer(key);
            }
            elemList = [];
            $(cms.util.getContainerSelector()).find('.cms-element:has(div.cms-editable), .' + cms.html.subcontainerClass + ':has(div.cms-editable)').each(function() {
               elemList.push($(this).attr('rel'));
            });
            cms.data.loadElements(elemList, function(ok) {
               if (ok) {
                  cms.data.fillContainers();
                  // to reset the mode we turn it off and on again
                  var activeButton = $("#toolbar button.ui-state-active");
                  activeButton.trigger('click');
                  activeButton.trigger('click');
               } else {
                              // TODO
               }
            });
         });
      }
      
      $('<div id="cms-delete-dialog" style="display:none;" title="' + M.GUI_DIRECT_DELETE_TITLE_0 + '">\
          <p>\
            ' +
      M.GUI_DIRECT_DELETE_CONFIRM_0 +
      '\
          </p>\
        </div>').appendTo("body").dialog({
         autoOpen: true,
         buttons: buttons,
         resizable: false,
         modal: true,
         zIndex: 10000
      
      });
   }
   
   /**
    * Deletes the given elements from the favorites and recent list.<p>
    *
    * @param {Array} ids a list of ids of elements to be deleted
    */
   var deleteFromFavListAndRecList = cms.toolbar.deleteFromFavListAndRecList = /** void */ function(/**Array<String>*/ids) {
   
      var /**boolean*/ saveFavorites = false;
      var /**boolean*/ saveRecentList = false;
      $.each(ids, function() {
         // HACK: javascript converts the list elements from native string to String objects :(
         var /**string*/ id = "" + this;
         // remove from favorites
         var /**Number*/ pos = $.inArray(id, cms.toolbar.favorites);
         if (pos >= 0) {
            saveFavorites = true;
            cms.toolbar.favorites.splice(pos, 1);
         }
         // remove from recent list
         pos = $.inArray(id, cms.toolbar.recent);
         if (pos >= 0) {
            saveRecentList = true;
            cms.toolbar.recent.splice(pos, 1);
         }
      });
      if (saveFavorites) {
         // save favorites
         cms.data.persistFavorites(function(ok) {
            resetFavList();
            if (!ok) {
                        // TODO
            }
         });
      }
      if (saveRecentList) {
         // save recent list
         cms.data.persistRecent(function(ok) {
            resetRecentList();
            if (!ok) {
                        // TODO
            }
         });
      }
   };
   
   /**
    * Timer object used for delayed hover effect.
    */
   var timer = cms.toolbar.timer = {
      id: null,
      handleDiv: null,
      adeMode: null
   };
   
   /**
    * Starts a hover timeout.<p>
    *
    * @param {Object} handleDiv
    * @param {Object} adeMode
    */
   var startHoverTimeout = cms.toolbar.startHoverTimeout = /** void */ function(/** jquery-object */handleDiv, /** string */ adeMode) {
      if (timer.id) {
         clearTimeout(timer.id);
      }
      timer.id = setTimeout(cms.toolbar.showAddButtons, 1000);
      timer.handleDiv = handleDiv;
      timer.adeMode = adeMode;
   }
   
   /**
    * Shows additional editing buttons within hover effect.
    */
   var showAddButtons = cms.toolbar.showAddButtons = /** void */ function() {
      timer.id = null;
      var numButtons = $(timer.handleDiv).children().size();
      var width = numButtons * 24 + 'px';
      
      timer.handleDiv.addClass('ui-widget-header').css({
         'width': width
      }).children().css('display', 'block').addClass('ui-corner-all ui-state-default');
      
   }
   
   /**
    * Cancels current hover timeout.<p>
    */
   var stopHover = cms.toolbar.stopHover = /** void */ function() {
      if (timer.id) {
         clearTimeout(timer.id);
         timer.id = null;
      }
      
      
      // sometimes out is triggered without over being triggered before, especially in IE
      if (!timer.handleDiv) {
         return;
      }
      timer.handleDiv.removeClass('ui-widget-header').css({
         'width': '24px'
      }).children().removeClass('ui-corner-all ui-state-default').not('a.cms-' + timer.adeMode).css('display', 'none');
   }
   
   
   /**
    * Initializes the hover event handler for a handle div.
    *
    * @param {Object} handleDiv the handle div
    * @param {Object} elem the element which the handle div belongs to
    * @param {String} adeMode the current mode
    */
   var initHandleDiv = cms.toolbar.initHandleDiv = /** void */ function(/** jquery-object */handleDiv, /** jquery-object */ elem, /** String */ adeMode) {
      handleDiv.unbind('mouseenter').unbind('mouseleave');
      handleDiv.hover(function() {
         cms.move.removeNewElementHighlighting(elem);
         cms.move.hightlightElement(elem, 2);
         startHoverTimeout(handleDiv, cms.toolbar.mode);
      }, function() {
         cms.move.removeElementHighlighting();
         stopHover();
         if (elem.hasClass('cms-new-element')) {
            cms.move.highlightNewElement(elem);
         }
      });
   }
   
   /**
    * Event handler for the 'edit properties' button.
    */
   var openPropertyDialog = function() {
      var $elem = $(this).closest('.cms-element');
      cms.property.editProperties($elem);
   }
   
   /**
    * Adds handle div to element.<p>
    *
    * @param {Object} elem jquery-element-object
    * @param {Object} elemId the resource-id
    * @param {Object} adeMode current mode
    * @param {Object} isMoving indicates if the current element is a sortable helper object
    */
   var addHandles = cms.toolbar.addHandles = /** void */ function(/** jquery-object */elem, /** String */ elemId, /** String */ adeMode, /** boolean */ isMoving) {
      var handleDiv = $('<div class="cms-handle"></div>').appendTo(elem);
      
      var handles = {}
      for (var modeName in modeMap) {
         var modeObj = modeMap[modeName];
         if (modeObj.isEdit) {
            handles[modeName] = modeMap[modeName].createHandle(elemId, elem);
         }
      }
      var currentMode = modeMap[adeMode];
      
      
      if (currentMode.isCompatibleWith(elemId)) {
         handles[adeMode].appendTo(handleDiv);
      }
      for (handleName in handles) {
         handleMode = modeMap[handleName];
         if (handleName != adeMode && handleMode.isCompatibleWith(elemId)) {
            handles[handleName].appendTo(handleDiv).css('display', 'none');
         }
      }
      if (isMoving) {
         if (adeMode != 'move') {
            handles[adeMode].css('display', 'none');
            handles['move'].css('display', 'block');
         }
      } else {
         initHandleDiv(handleDiv, elem, adeMode);
      }
      handleDiv.css({
         'right': '0px',
         'width': '24px'
      });
   }
   
   
   /**
    * Click-event-handler for edit-handles.<p>
    * Opens the content-editor-dialog.<p>
    */
   var openEditDialog = cms.toolbar.openEditDialog = /** void */ function() {
      var $domElement = $(this).closest('.cms-element, .' + cms.html.subcontainerClass);
      if ($domElement.hasClass(cms.html.subcontainerClass)) {
         editSubcontainer($domElement);
      } else {
         var elemId = $domElement.attr('rel');
         
         if (elemId && cms.data.elements[elemId]) {
            if (!cms.data.elements[elemId].noEditReason) {
               var element = cms.data.elements[elemId];
               
               if (element.status == cms.data.STATUS_NEWCONFIG) {
                  cms.data.createResource(element.type, function(ok, id, uri) {
                     if (!ok) {
                        // TODO
                        return;
                     }
                     cms.move.removeBorder($domElement, '.' + cms.move.HOVER_NEW);
                     var elem = cms.data.elements[elemId];
                     delete cms.data.elements[elemId];
                     cms.data.elements[id] = elem;
                     elem.id = id;
                     elem.status = cms.data.STATUS_CREATED;
                     cms.util.replaceNewElement(elemId, id);
                     _openContentEditor(uri, id);
                  });
                  
               } else {
                  _openContentEditor(element.file, elemId);
                  
               }
            }
         }
      }
   }
   
   /**
    * Opens the content editor for elements loaded within an element by a content-collector. 'Old' direct edit function.
    */
   var openSubelementEditDialog = cms.toolbar.openSubelementDialog = /** boolean */ function() {
      var editable = $(this).closest('.cms-editable');
      var elemId = editable.attr('rel');
      var parentId = $(this).closest('.cms-element').attr('rel');
      var path = editable.find('input[name="resource"]').val();
      _openContentEditor(path, [elemId, parentId]);
      return false;
   }
   
   /**
    * Opens the content editor for new elements loaded within an element by a content-collector. 'Old' direct edit function.
    */
   var openEditNewDialog = cms.toolbar.openEditNewDialog = /** boolean */ function() {
      var editable = $(this).closest('.cms-editable');
      var elemId = editable.attr('rel');
      var parentId = $(this).closest('.cms-element').attr('rel');
      var path = editable.find('input[name="resource"]').val();
      var newLink = '&amp;newlink=' + escape(editable.find('input[name="newlink"]').val()) + '&amp;editortitle=Editing+%28new+resource%29';
      _openContentEditor(path, [elemId, parentId], newLink);
      return false;
   }
   
   /**
    * Opens the content-editor dialog for a regular container element (if a single id string is given),
    * a content-collector element (if an array of ids is given, editing the element of the first)
    * or for a new content-collector element (if newLink != null)
    *
    * @param {String} path
    * @param {String / Array} ids may be an array or a single element-id
    * @param {String} newLink
    */
   var _openContentEditor = /** void */ function( /** String */path, /** String / Array<String> */ ids, /** String */ newLink) {
      var _afterReload = function(ok) {
         if (ok) {
            cms.data.fillContainers();
            var currentMode = getCurrentMode();
            if (currentMode != NullMode) {
               // to reset the mode we turn it off and on again
               currentMode.button.trigger('click');
               currentMode.button.trigger('click');
            }
            
         } else {
                  // TODO
         }
      };
      var id = $.isArray(ids) ? ids[0] : ids;
      var windowSize = cms.util.getWindoDimensions();
      var dialogWidth = windowSize['width'] > 1400 ? 1350 : (windowSize['width'] - 50);
      var dialogHeight = windowSize['height'] - 10;
      var iFrameHeight = dialogHeight - 31;
      var editorLink = cms.data.EDITOR_URL + '?resource=' + path + '&amp;directedit=true&amp;elementlanguage=' + cms.data.locale + '&amp;backlink=' + cms.data.BACKLINK_URL + '&amp;redirect=true';
      if (newLink) {
         editorLink += newLink;
      }
      var editorFrame = '<iframe style="border:none; width:100%; height:' + iFrameHeight + 'px;" name="cmsAdvancedDirectEditor" src="' + editorLink + '"></iframe>';
      var editorDialog = $('#cms-editor');
      if (!editorDialog.length) {
         editorDialog = $('<div id="cms-editor"  rel="' + id + '"></div>').appendTo(document.body);
      } else {
         editorDialog.empty().attr('rel', id);
      }
      
      editorDialog.append(editorFrame);
      editorDialog.dialog({
         width: dialogWidth - 50,
         height: dialogHeight,
         title: cms.util.format(M.GUI_EDITOR_TITLE_1, ((newLink) ? M.GUI_EDITOR_NEW_RESOURCE_0 : path)), // mmoossen: resource name in title
         modal: true,
         autoOpen: true,
         closeOnEscape: false,
         draggable: true,
         resizable: true,
         resize: function(event) {
            $('#cms-editor iframe').height($(this).height() - 5);
         },
         resizeStop: function(event) {
            $('#cms-editor iframe').height($(this).height() - 5);
         },
         position: ['center', 0],
         open: function(event) {
            cms.toolbar.dom.appendBox.css({
               zIndex: 10100,
               position: 'fixed'
            }).append(editorDialog.parent());
            $('a.ui-dialog-titlebar-close').hide();
            editorDialog.parent().css('top', '0px')
         },
         close: function() {
            var idsToLoad = [];
            
            editorDialog.empty().dialog('destroy');
            if ($.isArray(ids)) {
               idsToLoad = ids
               
            } else {
               idsToLoad = [id];
            }
            idsToLoad = idsToLoad.concat(cms.property.getElementsWithSamePrefix(id));
            cms.data.loadElements(idsToLoad, _afterReload);
         },
         zIndex: 10000
      });
   }
   
   /**
    * Removes the toolbar-html-element from the dom.<p>
    * Currently not used.<p>
    */
   var removeToolbar = cms.toolbar.removeToolbar = /** void */ function() {
      $('#toolbar').remove();
      $(document.body).css('margin-top', _oldBodyMarginTop + 'px');
   };
   
   /**
    * Hides the toolbar.<p>
    */
   var hideToolbar = cms.toolbar.hideToolbar = /** boolean */ function() {
      $(document.body).animate({
         marginTop: _oldBodyMarginTop + 'px'
      }, 200, 'swing', function() {
         $('#show-button').show(50);
      });
      
      return false;
   };
   
   /**
    * Toggles the toolbar.<p>
    */
   var toggleToolbar = cms.toolbar.toggleToolbar = /** boolean */ function() {
      var button = $('#show-button');
      if (button.hasClass('toolbar_hidden')) {
         $('#toolbar').fadeIn(100);
         $(document.body).animate({
            marginTop: _oldBodyMarginTop + 34 + 'px'
         }, 200, 'swing');
         button.removeClass('toolbar_hidden');
      } else {
         $(document.body).animate({
            marginTop: _oldBodyMarginTop + 'px'
         }, 200, 'swing');
         $('#toolbar').fadeOut(100);
         button.addClass('toolbar_hidden');
      }
      return false;
   };
   
   /**
    * Helper class for displaying a 'Loading' sign when loading takes too long
    */
   var LoadingSign = cms.toolbar.LoadingSign = /** Object */ function(/** String */selector, /** long */ waitTime, /** function */ showLoading, /** function */ hideLoading) {
   
      this.isLoading = false;
      
      var self = this;
      
      this.start = function() {
         self.isLoading = true;
         window.setTimeout(function() {
            if (self.isLoading) {
               showLoading();
            }
         }, waitTime);
      }
      
      this.stop = function() {
         self.isLoading = false;
         hideLoading();
      }
      
      return self;
   }
   
   
   /**
    * Moves the dropdown menus to the right if required for small screen-sizes.<p>
    */
   var fixMenuAlignment = /** void */ function() {
      $('.cms-menu').each(function() {
         var $elem = $(this);
         if ($elem.offset().left < 0) {
            cms.util.setLeft($elem, 0);
         }
      });
   }
   
   
   
   /**
    * Removes the sortable-object and event-handler.<p>
    */
   var destroyMove = function() {
   
      var containerSelector = cms.util.getContainerSelector();
      // replace ids
      $(containerSelector + ', #' + cms.html.favoriteDropListId).sortable('destroy');
      var list = $('#' + cms.html.favoriteDropMenuId);
      $('li.cms-item, button', list).css('display', 'block');
      resetFavList();
      $('.cms-element div.cms-handle, .' + cms.html.subcontainerClass + ' div.cms-handle').remove();
   };
   
   
   /**
    * Initializes the sortable.<p>
    */
   var initMove = /** void */ function(selector) {
      var containerSelector;
      if (selector) {
         containerSelector = selector;
      } else {
         containerSelector = cms.util.getContainerSelector();
      }
      
      // replace id
      var list = cms.toolbar.dom.favoritesDrop;
      var favbutton = $('button[name="Favorites"]');
      
      //unnecessary
      $('li.cms-item, button', list).css('display', 'none');
      var favLeft = $('button[name="storage"]').position().left - 1;
      list.css({
         top: 35,
         left: favLeft,
         display: 'none'
      });
      
      // replace id
      $('#' + cms.html.favoriteDropListId).css('height', '40px');
      $('div.ui-widget-shadow', list).css({
         top: 0,
         left: -4,
         width: list.outerWidth() + 8,
         height: list.outerHeight() + 1,
         border: '0px solid',
         opacity: 0.6
      });
      
      $(containerSelector).css('position', 'relative');
      
      // replace id
      var sortSelector = containerSelector + ', #' + cms.html.favoriteDropListId;
      var $lists = $(sortSelector);
      $lists.sortable({
         connectWith: containerSelector + ', #' + cms.html.favoriteDropListId,
         placeholder: 'cms-placeholder',
         dropOnEmpty: true,
         start: cms.move.onStartDrag,
         beforeStop: cms.move.beforeStopFunction,
         over: cms.move.onDragOverContainer,
         out: cms.move.onDragOutOfContainer,
         change: function(event, ui) {
            var helperParent = ui.helper.parent();
            var parentId = helperParent.attr('id');
            if (parentId != cms.html.favoriteDropListId) {
               cms.move.refreshContainerHighlighting('#' + parentId, 2);
            }
         },
         tolerance: 'pointer',
         stop: cms.move.onStopDrag,
         cursorAt: {
            right: 10,
            top: 10
         },
         zIndex: 20000,
         handle: 'a.cms-move',
         items: cms.data.sortitems,
         revert: true,
         // replace ids
         deactivate: cms.move.onDeactivateDrag
      });
   }
   
   
   
   /**
    * Click-event-handler for delete icon in favorites dialog.<p>
    */
   var clickFavDeleteIcon = cms.toolbar.clickFavDeleteIcon = /** void */ function() {
      var button = $(this);
      var toRemove = button.parent().parent();
      toRemove.remove();
   };
   
   /**
    * Utility function to generate a string representing an array.<p>
    * @param {Array} arr
    */
   var arrayToString = function(/** Array<String> */arr) {
      return "[" + arr.join(", ") + "]";
   };
   
   /**
    * Adds another item to the favorite-list.<p>
    */
   var saveFavorites = cms.toolbar.saveFavorites = function() {
      cms.toolbar.favorites.length = 0;
      $("#fav-dialog > ul > li.cms-item").each(function() {
         var resource_id = this.getAttribute("rel");
         cms.toolbar.favorites.push(resource_id);
      });
   };
   
   /**
    * Persists favorite-list on server.<p>
    */
   var favEditOK = cms.toolbar.favEditOK = function() {
      $(this).dialog("close");
      saveFavorites();
      cms.data.persistFavorites(function(ok) {
         if (!ok) {
                  // TODO
         }
      });
   }
   
   /**
    * Closes favorites edit dialog.<p>
    */
   var favEditCancel = cms.toolbar.favEditCancel = function() {
      $(this).dialog("close");
   }
   
   /**
    * Initializes the favorites edit dialog.<p>
    */
   var initFavDialog = cms.toolbar.initFavDialog = function() {
      $("#fav-edit").click(showFavDialog);
      var buttons = {};
      buttons[M.GUI_FAV_EDIT_CANCEL_0] = favEditCancel;
      buttons[M.GUI_FAV_EDIT_OK_0] = favEditOK;
      $('#fav-dialog').dialog({
         width: 380,
         // height: 500,
         title: M.GUI_FAV_EDIT_TITLE_0,
         modal: true,
         autoOpen: false,
         draggable: true,
         resizable: true,
         position: ['center', 20],
         close: function() {
            markAsInactive($('#fav-edit'));
         },
         buttons: buttons,
         zIndex: 10000
      });
      
   };
   
   /**
    * Initializes the favorites edit dialog items.<p>
    */
   var initFavDialogItems = cms.toolbar.initFavDialogItems = function() {
      $("#fav-dialog > ul").empty();
      var html = []
      for (var i = 0; i < cms.toolbar.favorites.length; i++) {
         html.push(cms.html.createItemFavDialogHtml(cms.data.elements[cms.toolbar.favorites[i]]));
      }
      $("#fav-dialog > ul").append(html.join(''));
      $("#fav-dialog .cms-delete-icon").click(clickFavDeleteIcon);
      $("#fav-dialog > ul").sortable({
         axis: 'y',
         forcePlaceholderSize: true
      });
   };
   
   /**
    * Opens the favorites edit dialog.<p>
    */
   var showFavDialog = cms.toolbar.showFavDialog = function() {
      var button = $(this);
      if (button.hasClass("ui-state-active")) {
         markAsInactive(button);
      } else {
         $('button.ui-state-active').trigger('click');
         // enabling move-mode
         initFavDialogItems();
         $('#fav-dialog').dialog('open');
         markAsActive(button);
      }
   };
   
   /**
    * Shows/hides the additional item info in list-views.<p>
    */
   var toggleAdditionalInfo = cms.toolbar.toggleAdditionalInfo = function() {
      var elem = $(this);
      var $additionalInfo = elem.closest('.ui-widget-content').children('.cms-additional');
      if (elem.hasClass('ui-icon-triangle-1-e')) {
         elem.removeClass('ui-icon-triangle-1-e').addClass('ui-icon-triangle-1-s');
         $additionalInfo.show(5, function() {
            var list = $(this).parents('div.cms-menu');
            $('div.ui-widget-shadow', list).css({
               height: list.outerHeight() + 1
            });
         });
      } else {
         elem.removeClass('ui-icon-triangle-1-s').addClass('ui-icon-triangle-1-e');
         
         $additionalInfo.hide(5, function() {
            var list = $(this).parents('div.cms-menu');
            $('div.ui-widget-shadow', list).css({
               height: list.outerHeight() + 1
            });
         });
      }
      return false;
   };
   
   /**
    * Reloads the favorites-list.<p>
    */
   var resetFavList = cms.toolbar.resetFavList = function() {
      $("#" + cms.html.favoriteListId + " > li.cms-item").remove();
      var $favlist = $("#" + cms.html.favoriteListId);
      for (var i = 0; i < cms.toolbar.favorites.length; i++) {
         if (cms.data.elements[cms.toolbar.favorites[i]]) {
            $favlist.append(cms.html.createItemFavListHtml(cms.toolbar.favorites[i]));
         }
      }
   }
   
   
   /**
    * Adds item to recent list.<p>
    *
    * @param {String} itemId
    */
   var addToRecent = cms.toolbar.addToRecent = function(/** String */itemId) {
      cms.util.addToElementList(cms.toolbar.recent, itemId, cms.toolbar.recentSize);
      //cms.util.addUnique(cms.toolbar.recent, itemId, cms.toolbar.recentSize);
      cms.data.persistRecent(function(ok) {
         if (!ok) {
                  // TODO
         }
      });
   }
   
   /**
    * Opens the save dialog.
    */
   var showSaveDialog = function() {
      if (!cms.toolbar.toolbarReady || $(this).hasClass('cms-deactivated')) {
         return;
      }
      
      if ($('#cms-save-dialog').size() == 0) {
         $('<div id="cms-save-dialog" style="display:none;" title="' + M.GUI_SAVE_PAGE_TITLE_0 + '"></div>').appendTo('body');
      }
      $dlg = $('#cms-save-dialog');
      $dlg.empty();
      $dlg.append('<p>' + M.GUI_SAVE_PAGE_CONFIRM_0 + '</p>');
      var buttons = {};
      
      buttons[M.GUI_SAVE_PAGE_CANCEL_0] = function() {
         $dlg.dialog('destroy');
         markAsInactive($('button[name="save"]'));
      }
      
      buttons[M.GUI_SAVE_PAGE_OK_0] = function() {
         markAsInactive($('button[name="save"]'));
         $dlg.dialog('destroy');
         savePage();
      };
      
      
      var options = {
         autoOpen: true,
         modal: true,
         zIndex: 10000,
         resizable: false,
         buttons: buttons
      };
      
      $dlg.dialog(options);
   }
   
   /**
    * Persists container-page on server.<p>
    * @param {Object} callback
    */
   var savePage = cms.toolbar.savePage = function(/** function */callback) {
   
      cms.data.persistContainers(function(ok) {
         $('#cms-save-dialog').dialog('close');
         if (ok) {
            setPageChanged(false);
         }
         if (callback) {
            callback(ok);
         }
      });
   }
   
   /**
    * Flag to indicate the status of the page (changed/unchanged).<p>
    */
   var pageChanged = cms.toolbar.pageChanged = false;
   
   /**
    * Sets the pageChanged flag and activates the save and reset-buttons.<p>
    * @param {boolean} newValue
    */
   var setPageChanged = cms.toolbar.setPageChanged = function(/** boolean */newValue) {
      if (!cms.toolbar.pageChanged && newValue) {
         cms.data.startEdit(function(ok) {
                  });
      }
      if (cms.toolbar.pageChanged && !newValue) {
         cms.data.stopEdit(function(ok) {
                  }, true);
      }
      pageChanged = cms.toolbar.pageChanged = newValue;
      if (newValue) {
         $('#toolbar button[name="Save"], #toolbar button[name="reset"]').removeClass('cms-deactivated');
      } else {
         $('#toolbar button[name="Save"], #toolbar button[name="reset"]').addClass('cms-deactivated');
      }
   }
   
   var leavePage = cms.toolbar.leavePage = function(target) {
      if (cms.toolbar.pageChanged) {
         cms.toolbar.pageChanged = false;
         cms.data.stopEdit(function(ok) {
            window.location.href = target;
         }, true);
      } else {
         window.location.href = target;
      }
   }
   
   var reloadPage = cms.toolbar.reloadPage = function() {
      if (cms.toolbar.pageChanged) {
         cms.toolbar.pageChanged = false;
         cms.data.stopEdit(function(ok) {
            window.location.reload();
         }, true);
      } else {
         window.location.reload();
      }
   }
   
   /**
    * On-unload event-handler to prevent accidental data-loss.<p>
    */
   var onUnload = cms.toolbar.onUnload = function() {
      if (cms.toolbar.pageChanged) {
         var saveChanges = window.confirm(cms.util.format(M.GUI_UNLOAD_SAVE_CONFIRM_1, window.location.href));
         if (saveChanges) {
            cms.toolbar.savePage();
         } else {
            var newElems = [];
            $.each(cms.data.elements, function(key, value) {
               if (value.status == cms.data.STATUS_CREATED) {
                  newElems.push(value.id);
               }
            });
            if (newElems.length > 0) {
               cms.data.deleteResources(newElems, function(ok) {
                  deleteFromFavListAndRecList(newElems);
                  if (!ok) {
                     // TODO
                     alert(M.ERR_UNLOAD_DELETE_0);
                  }
               });
            }
            setPageChanged(false);
         }
      }
   }
   
   /**
    * Initializes the link click handlers for links in elements.<p>
    *
    * If the user clicks on a link, the handler will ask the user whether
    * they really want to leave the page, and if they want to save before this.
    *
    */
   var initLinks = cms.toolbar.initLinks = function() {
   
      $('a:not(.cms-left, .cms-move, .cms-delete, .cms-edit, .cms-properties, .cms-advanced-search, .cms-basic-search)').live('click', function() {
         if (!cms.toolbar.pageChanged) {
            cms.toolbar.leavingPage = true;
            return;
         }
         var $link = $(this);
         var target = $link.attr('href');
         cms.util.leavePageDialog(target, savePage, leavePage);
         return false;
      });
   }
   
   /**
    * Opens the leave page confirm dialog (if page has changed)
    *
    * @param {String} target the target page to open
    */
   var leavePageDialog = cms.toolbar.leavePageDialog = function(target) {
      var buttons = {};
      buttons[M.GUI_LEAVE_PAGE_SAVE_0] = function() {
         $(this).dialog('destroy');
         savePage(function(ok) {
            if (ok) {
               leavePage(target);
            }
         });
      };
      
      
      buttons[M.GUI_LEAVE_PAGE_CANCEL_0] = function() {
         $(this).dialog('destroy');
      };
      
      buttons[M.GUI_LEAVE_PAGE_OK_0] = function() {
         $(this).dialog('destroy');
         leavePage(target);
      };
      
      
      $('#cms-leave-dialog').dialog({
         autoOpen: true,
         modal: true,
         title: M.GUI_LEAVE_PAGE_TITLE_0,
         zIndex: 9999,
         buttons: buttons,
         close: function() {
            $(this).dialog('destroy');
         }
      });
   }
   
   
   /**
    * Reloads the recent-list.<p>
    */
   var resetRecentList = cms.toolbar.resetRecentList = function() {
      $("#" + cms.html.recentListId + " > li.cms-item").remove();
      var $recentlist = $("#" + cms.html.recentListId);
      for (var i = 0; i < cms.toolbar.recent.length; i++) {
         $recentlist.append(cms.html.createItemFavListHtml(cms.toolbar.recent[i]));
      }
   };
   
   /**
    * Creates a short menu bar button.
    * @param {Object} name the button name
    * @param {Object} title the button title
    * @param {Object} cssClass the css class to add to the button
    */
   var makeModeButton = function(name, title, cssClass) {
      return $('<button name="' + name + '" title="' + title + '" class="cms-left ui-state-default ui-corner-all"><span class="ui-icon ' + cssClass + '"></span>&nbsp;</button>');
   }
   
   
   /**
    * Creates a wide menu bar button.
    * @param {Object} name the button name
    * @param {Object} title the button title
    * @param {Object} cssClass the css class to add to the button
    */
   var makeWideButton = function(name, title, cssClass) {
      return $('<button name="' + name + '" title="' + title + '" class="cms-left cms-button-wide ui-state-default ui-corner-all"><span class="ui-icon ' + cssClass + '"></span><span class="cms-button-text">' + title + '</span></button>');
   }
   
   /**
    * Shared method that toggles a mode (enables or disables it depending on the state of the mode's button).
    * @param {Object} button the button of the mode
    */
   var toggleMode = cms.toolbar.toggleMode = /** void */ function(modeObject) {
      if (!cms.toolbar.toolbarReady || modeObject == NullMode) {
         return;
      }
      if (modeObject.name == cms.toolbar.mode) {
         modeObject.disable();
         cms.toolbar.mode = '';
         doHideShowHackForIE();
         markAsInactive(modeObject.button);
      } else {
         modeObject.enable(modeObject.button);
         cms.toolbar.mode = modeObject.name;
         markAsActive(modeObject.button);
      }
   };
   
   var _reinitEditModeHandles = function() {
      var buttonMode = this.name;
      var containerSelector = (cms.toolbar.editingSubcontainerId != null) ? ('#' + cms.toolbar.editingSubcontainerId) : cms.util.getContainerSelector();
      var containers = $(containerSelector);
      if (this.isEdit()) {
         $('.cms-element div.cms-handle, .' + cms.html.subcontainerClass + ' div.cms-handle').remove();
         containers.children('.cms-element, .' + cms.html.subcontainerClass).each(function() {
            var elem = $(this).css('position', 'relative');
            var elemId = elem.attr('rel');
            if (elemId && cms.data.elements[elemId]) {
               addHandles(elem, elemId, buttonMode);
            }
            containers.find('div.cms-editable div.cms-directedit-buttons').addClass('cms-' + buttonMode + 'mode');
         });
      }
   }
   /**
    * Shared method that enables an edit mode (edit, move, delete, properties).
    */
   var _enableEditMode = function() {
      var buttonMode = this.name;
      var containerSelector = (cms.toolbar.editingSubcontainerId != null) ? ('#' + cms.toolbar.editingSubcontainerId) : cms.util.getContainerSelector();
      var containers = $(containerSelector);
      $('button.ui-state-active[name!="' + this.name + '"]', cms.toolbar.dom.toolbar).trigger('click');
      if (modeMap[cms.toolbar.mode].isEdit) {
         // reorder handles
         $('.cms-element div.cms-handle, .' + cms.html.subcontainerClass + ' div.cms-handle').each(function() {
            var handleDiv = $(this);
            $('a', handleDiv).css('display', 'none');
            $('a.cms-' + buttonMode, handleDiv).prependTo(handleDiv).css('display', 'block');
         });
         if (buttonMode != cms.toolbar.mode) {
            markAsInactive(cms.toolbar.dom.buttons[cms.toolbar.mode]);
         }
         containers.find('.cms-element .cms-editable:not(:has(div.cms-hovering)), .' + cms.html.subcontainerClass + ' .cms-editable:not(:has(div.cms-hovering))').each(function() {
            cms.move.drawSiblingBorder($(this), 2, 'cms-editable', false, 'cms-test');
         });
         containers.find('div.cms-editable div.cms-directedit-buttons').removeClass('cms-' + cms.toolbar.mode + 'mode').addClass('cms-' + buttonMode + 'mode');
      } else {
         modeMap[cms.toolbar.mode].disable();
         
         containers.children('.cms-element, .' + cms.html.subcontainerClass).each(function() {
            var elem = $(this).css('position', 'relative');
            var elemId = elem.attr('rel');
            if (elemId && cms.data.elements[elemId]) {
               addHandles(elem, elemId, buttonMode);
               if (elem.hasClass(cms.html.subcontainerClass)) {
                  var backgroundData = cms.move.addSubcontainerBackground(elem);
                  var rightPos = backgroundData['parentDimension']['width'] - backgroundData['dimension']['left'] - backgroundData['dimension']['width'];
                  elem.children('.cms-handle').css('right', rightPos + 'px');
               }
            }
         });
         initMove(containerSelector);
         containers.find('.cms-element .cms-editable:not(:has(div.cms-hovering)), .' + cms.html.subcontainerClass + ' .cms-editable:not(:has(div.cms-hovering))').each(function() {
            cms.move.drawSiblingBorder($(this), 2, 'cms-editable', false, 'cms-test');
         });
         containers.find('div.cms-editable div.cms-directedit-buttons').addClass('cms-' + buttonMode + 'mode');
      }
      
   }
   
   /**
    * Shared method that enables a list mode (favorites, recent, new, add).
    * @param {Object} button the button of the mode
    */
   var _enableListMode = function(button) {
      var self = this;
      var currentMode = getCurrentMode();
      var containerSelector = (cms.toolbar.editingSubcontainerId != null) ? ('#' + cms.toolbar.editingSubcontainerId) : cms.util.getContainerSelector();
      cms.toolbar.currentMenu = self.menuId;
      currentMode.disable();
      $('button.ui-state-active').trigger('click');
      markAsActive(button);
      self.load(function(ok, data) {
         self.prepareAfterLoad();
         var menu = $('#' + self.menuId);
         if (self.menuId == cms.html.storageMenuId) {
            $('.cms-list-itemcontent:not(:has(a.cms-move))', menu).each(function() {
               var elem = $(this);
               $('<a class="cms-handle cms-move"></a>').appendTo(elem);
            });
         }
         menu.css({
            /* position : 'fixed', */
            top: 35,
            left: button.position().left - 1
         }).slideDown(100, function() {
            $('div.ui-widget-shadow', menu).css({
               top: 0,
               left: -3,
               width: menu.outerWidth() + 8,
               height: menu.outerHeight() + 1,
               border: '0px solid',
               opacity: 0.6
            });
         });
         $(containerSelector).css('position', 'relative').children('*:visible').css('position', 'relative');
         //fixMenuAlignment();
         // * current menu
         $(containerSelector + ', #' + self.menuId + ' ul.cms-item-list').sortable({
            // * current menu
            connectWith: containerSelector + ', #' + self.menuId + ' ul.cms-item-list',
            placeholder: 'placeholder',
            dropOnEmpty: true,
            start: cms.move.onStartDrag,
            beforeStop: cms.move.beforeStopFunction,
            over: cms.move.onDragOverContainer,
            out: cms.move.onDragOutOfContainer,
            tolerance: 'pointer',
            stop: cms.move.onStopDrag,
            cursorAt: {
               right: 15,
               top: 10
            },
            handle: '.cms-move',
            items: cms.data.sortitems + ', li.cms-item',
            revert: 100,
            deactivate: function(event, ui) {
               $('a.cms-move', $(this)).removeClass('cms-trigger');
               if ($.browser.msie) {
                  // In IE7 html-block elements may disappear after sorting, this should trigger the html to be re-rendered.
                  setTimeout(function() {
                     $(cms.data.sortitems).css('display', 'block');
                  }, 10);
               }
            }
         });
      });
      
   }
   
   /**
    * Shared method that disables an edit mode (edit, move, delete, properties).
    */
   var _disableEditMode = /** void */ function() {
      var mode = this.name;
      if (mode == '') {
         return;
      }
      cms.move.removeAllHighlighting();
      
      // disabling edit/move/delete
      var containerSelector = cms.util.getContainerSelector();
      // replace ids
      $(containerSelector + ', #' + cms.html.favoriteDropListId).sortable('destroy');
      var list = $('#' + cms.html.favoriteDropMenuId);
      $('li.cms-item, button', list).css('display', 'block');
      resetFavList();
      $('.cms-element div.cms-handle, .' + cms.html.subcontainerClass + ' div.cms-handle').remove();
      $(containerSelector).find('div.cms-editable div.cms-directedit-buttons').removeClass('cms-' + mode + 'mode');
      $('.cms-subcontainer-background').remove();
      //cms.toolbar.dom.buttons[mode].removeClass('ui-state-active');
   }
   
   /**
    * Hides and shows all containers if the browser is IE.<p>
    *
    * This is sometimes necessary because containers vanish for no reason in IE after closing a menu.
    */
   var doHideShowHackForIE = function() {
      // TODO: find a better way to rerender stuff in IE
      if ($.browser.msie) {
         // In IE7 html-block elements may disappear after sorting, this should trigger the html to be re-rendered.
         setTimeout(function() {
            $(cms.util.getContainerSelector()).hide().show();
         }, 0);
      }
   }
   
   /**
    * Shared method for disabling a list mode (favorites, recent, add, new).
    */
   var _disableListMode = /** void */ function() {
      var mode = this.name;
      if (mode == '') {
         return;
      }
      // disabling add/new/favorites/recent
      cms.toolbar.dom[mode + 'Menu'].hide();
      $('ul', cms.toolbar.dom[mode + 'Menu']).add(cms.util.getContainerSelector()).sortable('destroy');
   }
   
   var getCurrentMode = cms.toolbar.getCurrentMode = function() {
      if (cms.toolbar.mode == '') {
         return NullMode;
      }
      return modeMap[cms.toolbar.mode];
      
   }
   
   /*======================================= START MODE DEFINITIONS ==============================================*/
   
   /**
    * Save action.
    */
   var SaveMode = {
      name: 'save',
      createButton: function() {
         var self = this;
         self.button = $('<button name="Save" title="' + M.GUI_SAVE_BUTTON_TITLE_0 + '"  class="cms-right ui-state-default ui-corner-all cms-deactivated"><span class="ui-icon cms-icon-save"/>&nbsp;</button>');
         self.button.click(showSaveDialog);
         return self.button;
      },
      
      initialize: doNothing
   
   
   }
   
   var SitemapMode = {
      name: 'sitemap',
      createButton: function() {
         var self = this;
         if (cms.data.SITEMAP_URL == '') {
            self.button = $([]);
            return self.button;
         }
         self.button = $('<button name="sitemap" title="' + M.GUI_TOOLBAR_SITEMAP_TITLE_0 + '" class="cms-right ui-state-default ui-corner-all"><span class="ui-icon cms-icon-sitemap"/>&nbsp;</button>');
         self.button.click(function() {
            if (!cms.toolbar.toolbarReady || $(this).hasClass('cms-deactivated')) {
               return;
            }
            if (!cms.toolbar.pageChanged) {
               cms.toolbar.leavingPage = true;
               window.location = cms.data.SITEMAP_URL;
            } else {
               cms.util.leavePageDialog(cms.data.SITEMAP_URL, savePage, leavePage);
            }
         });
         return self.button;
      },
      initialize: doNothing
   }
   
   var PublishMode = {
      name: 'publish',
      createButton: function() {
         var self = this;
         self.button = $('<button name="publish" title="' + M.GUI_PUBLISH_DIALOG_TITLE_0 + '" class="cms-right ui-state-default ui-corner-all"><span class="ui-icon cms-icon-publish"/>&nbsp;</button>');
         self.button.click(function() {
            if (!cms.toolbar.toolbarReady || $(this).hasClass('cms-deactivated')) {
               return;
            }
            cms.publish.initProjects(function() {
               (new cms.publish.PublishDialog('')).start();
            });
         });
         return self.button;
      },
      initialize: doNothing
   }
   
   
   var StorageMode = {
      name: 'storage',
      createButton: function() {
         var self = this;
         self.button = makeWideButton('storage', M.GUI_STORAGE_BUTTON_TITLE_0, 'cms-icon-favorites');
         self.button.click(function() {
            toggleMode(self);
         });
         return self.button;
      },
      menuId: cms.html.storageMenuId,
      load: cms.data.loadStorage,
      prepareAfterLoad: function() {
         resetFavList();
         resetRecentList()
      },
      enable: _enableListMode,
      disable: _disableListMode,
      initialize: function() {
         cms.toolbar.dom.storageMenu = $(cms.html.createStorageMenu()).appendTo(cms.toolbar.dom.toolbarContent);
         cms.toolbar.dom.favoritesDrop = $(cms.html.createFavDrop()).appendTo(cms.toolbar.dom.toolbarContent);
         cms.toolbar.dom.favoritesDialog = $(cms.html.favoriteDialog).appendTo(_bodyEl);
         initFavDialog();
         var self = this;
         $('#menuTabs').tabs({
            show: function() {
               var list = $('#' + self.menuId);
               $('div.ui-widget-shadow', list).css({
                  height: list.outerHeight() + 1
               });
            }
         }).removeClass('ui-corner-all');
         $('#menuTabs .ui-tabs-nav').removeClass('ui-widget-header').removeClass('ui-corner-all');
      }
   }
   
   var RecentListMode = {
      name: 'recent',
      createButton: function() {
         var self = this;
         self.button = makeWideButton('recent', M.GUI_RECENT_BUTTON_TITLE_0, 'cms-icon-recent').click(function() {
            toggleMode(self);
         });
         return self.button;
         
      },
      menuId: cms.html.recentMenuId,
      load: cms.data.loadRecent,
      prepareAfterLoad: resetRecentList,
      disable: _disableListMode,
      enable: _enableListMode,
      initialize: function() {
         cms.toolbar.dom.recentMenu = $(cms.html.createMenu(cms.html.recentMenuId)).appendTo(cms.toolbar.dom.toolbarContent);
      }
   }
   
   var GalleryMode = {
      name: 'add',
      menuId: cms.html.galleryMenuId,
      createButton: function() {
         var self = this;
         self.button = makeWideButton('add', M.GUI_ADD_BUTTON_TITLE_0, 'cms-icon-new').click(function() {
            toggleMode(self);
         });
         return self.button;
      },
      prepareAfterLoad: doNothing,
      
      load: function(callback) {
         if (!galleryInitialized) {
            galleryInitialized = true;
            cms.galleries.initValues['tabs'] = ['cms_tab_types', 'cms_tab_galleries', 'cms_tab_categories', 'cms_tab_search'];
            cms.galleries.initAddDialog();
            // remove corner-all class from tabs after initialization 
            $('#' + cms.html.galleryTabsId).removeClass('ui-corner-all');
         }
         callback(true, null);
      },
      disable: _disableListMode,
      enable: _enableListMode,
      initialize: function() {
         cms.toolbar.dom.addMenu = $(cms.html.createGalleryMenu()).appendTo(cms.toolbar.dom.toolbarContent);
      }
   }
   
   var PropertyMode = {
      name: 'properties',
      isEdit: true,
      createButton: function() {
         var self = this;
         self.button = makeModeButton(this.name, M.GUI_PROP_BUTTON_TITLE_0, 'cms-icon-prop').click(function() {
            toggleMode(self);
         });
         return self.button;
      },
      
      isCompatibleWith: function(elemId) {
         return !(cms.data.NO_EDIT_REASON);
      },
      
      createHandle: function(elemId, elem) {
         return $('<a class="cms-properties"></a>');
      },
      
      enable: _enableEditMode,
      disable: _disableEditMode,
      initialize: function() {
         $('a.cms-properties').live('click', openPropertyDialog);
      }
      
   }
   
   var DeleteMode = {
      name: 'delete',
      isEdit: true,
      createButton: function() {
         var self = this;
         self.button = makeModeButton(this.name, M.GUI_DELETE_BUTTON_TITLE_0, 'cms-icon-delete').click(function() {
            toggleMode(self);
         });
         return self.button;
      },
      
      isCompatibleWith: function(elem) {
         return !(cms.data.NO_EDIT_REASON);
      },
      
      createHandle: function(elemId, elem) {
         return $('<a class="cms-delete"></a>');
      },
      
      enable: _enableEditMode,
      disable: _disableEditMode,
      initialize: function() {
         $('div.cms-handle a.cms-delete').live('click', deleteItem);
      }
   }
   
   
   var EditMode = {
      name: 'edit',
      isEdit: true,
      createButton: function() {
         var self = this;
         self.button = makeModeButton(this.name, M.GUI_EDIT_BUTTON_TITLE_0, 'cms-icon-edit').click(function() {
            toggleMode(self);
         });
         return self.button;
      },
      
      isCompatibleWith: function(elem) {
         return true;
      },
      
      createHandle: function(elemId, elem) {
         if (!cms.data.elements[elemId].noEditReason) {
            return $('<a class="cms-edit cms-edit-enabled"></a>');
         } else {
            return $('<a class="cms-edit cms-edit-locked" title="' + cms.data.elements[elemId].noEditReason + '" onclick="return false;"></a>');
         }
      },
      
      enable: _enableEditMode,
      disable: _disableEditMode,
      initialize: function() {
         $('a.cms-edit.cms-edit-enabled').live('click', openEditDialog);
      }
      
   }
   
   
   var MoveMode = {
      name: 'move',
      isEdit: true,
      createButton: function() {
         var self = this;
         self.button = makeModeButton(this.name, M.GUI_MOVE_BUTTON_TITLE_0, 'cms-icon-move').click(function() {
            toggleMode(self);
         });
         return self.button;
      },
      
      isCompatibleWith: function(elem) {
         return !(cms.data.NO_EDIT_REASON);
      },
      
      createHandle: function(elemId, elem) {
         return $('<a class="cms-move"></a>');
      },
      
      enable: _enableEditMode,
      disable: _disableEditMode,
      initialize: doNothing
   }
   
   var NullMode = {
      name: '',
      disable: doNothing,
      enable: doNothing
   }
   
   
   var ResetMode = {
      name: 'reset',
      createButton: function() {
         var self = this;
         self.button = makeModeButton(this.name, M.GUI_RESET_BUTTON_TITLE_0, 'cms-icon-reset').click(function() {
            if (!cms.toolbar.toolbarReady) {
               return;
            }
            if ($('#cms-reset-dialog').size() == 0) {
               $('<div id="cms-reset-dialog" style="display:none">' + M.GUI_RESET_CONFIRM_0 + '</div>').appendTo('body');
            }
            var $dlg = $('#cms-reset-dialog');
            if (self.button.hasClass('cms-deactivated')) {
               return;
            }
            var buttons = {};
            buttons[M.GUI_RESET_CANCEL_0] = function() {
               $(this).dialog('destroy');
            }
            
            buttons[M.GUI_RESET_OK_0] = function() {
               $(this).dialog('destroy');
               reloadPage();
               
            }
            
            
            $('#cms-reset-dialog').dialog({
               autoOpen: true,
               modal: true,
               zIndex: 9999,
               title: M.GUI_RESET_TITLE_0,
               buttons: buttons,
               close: function() {
                  $(this).dialog('destroy');
               }
            });
            return false;
         });
         self.button.addClass('cms-deactivated');
         return self.button;
      },
      
      initialize: doNothing
   }
   
   
   /*====================================== END MODE DEFINITIONS =================================*/
   
   
   var addToolbar = cms.toolbar.addToolbar = /** void */ function() {
      _bodyEl = $(document.body).css('position', 'relative');
      
      // remember old margins/offset of body 
      _oldBodyMarginTop = _bodyEl.offset().top;
      _bodyEl.css({
         marginTop: _oldBodyMarginTop + 34 + 'px'
      });
      // appending all necessary toolbar components and keeping their references
      cms.toolbar.dom.toolbar = $(cms.html.toolbar).appendTo(_bodyEl);
      cms.toolbar.dom.toolbarContent = $('#toolbar_content', cms.toolbar.dom.toolbar);
      cms.toolbar.dom.appendBox = $('<div id="cms_appendbox"></div>').appendTo(_bodyEl);
      cms.toolbar.dom.buttons = {};
      for (var i = 0; i < modeNames.length; i++) {
         //we iterate over editModes instead of editModeMap because the order should be fixed
         var modeName = modeNames[i];
         var modeObj = modeMap[modeName];
         modeObj.initialize();
         modeObj.createButton();
         cms.toolbar.dom.toolbarContent.append(modeObj.button);
         cms.toolbar.dom.buttons[modeName] = modeObj.button;
      }
      
      cms.toolbar.dom.showToolbar = $('<button id="show-button" title="' + M.GUI_TOOLBAR_TOGGLE_TOOLBAR_0 + '" class="ui-state-default ui-corner-all"><span class="ui-icon cms-icon-logo"/></button>').appendTo(_bodyEl);
      
      // initializing dialogs and event-handler
      window.onbeforeunload = function() {
         // DO NOT use jquery to bind this!
         cms.comm.abortAllRequests();
      };
      $(window).unload(onUnload); /* TODO */
      $('button[name="Save"]', cms.toolbar.dom.toolbar).click(showSaveDialog);
      
      cms.toolbar.dom.showToolbar.click(toggleToolbar);
      $('#toolbar button, #show-button').mouseover(function() {
         if (!$(this).hasClass('cms-deactivated')) {
            $(this).addClass('ui-state-hover');
         }
      }).mouseout(function() {
         $(this).removeClass('ui-state-hover');
      });
      initLinks();
      // $(window).resize(fixMenuAlignment);
   };
   
   /**
    * The mode objects in the order in which the buttons should appear in the toolbar.
    */
   var modes = [ResetMode, EditMode, MoveMode, DeleteMode, PropertyMode, GalleryMode, StorageMode, SaveMode, PublishMode, SitemapMode];
   
   /**
    * Gets a mode by mode name.
    * @param {Object} modeName
    */
   var getMode = function(modeName) {
      return modeMap[modeName];
   }
   
   /**
    * Builds a map from a list of objects by using a key function to generate the property name under which the object should be stored.
    * @param {Object} items the list of objects
    * @param {Object} keyFunction the function to generate a key from an object
    */
   var buildMap = function(items, keyFunction) {
      var result = {};
      for (var i = 0; i < items.length; i++) {
         var item = items[i];
         result[keyFunction(item)] = item;
      }
      return result;
   }
   
   /**
    * The map of modes, using the mode names as keys.
    */
   var modeMap = buildMap(modes, function(mode) {
      return mode.name;
   });
   modeMap[''] = NullMode;
   
   /**
    * The list of mode names.
    */
   var modeNames = $.map(modes, function(mode) {
      return mode.name;
   })
   
   
})(cms);

