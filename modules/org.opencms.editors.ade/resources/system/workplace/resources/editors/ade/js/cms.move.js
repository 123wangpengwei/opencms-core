(function(cms) {
   var $ = jQuery;
   var M = cms.messages;
   
   /** class for normal move-related hover borders */
   var HOVER_NORMAL = cms.move.HOVER_NORMAL = 'cms-hover-normal';
   /** class for hover borders for new items */
   var HOVER_NEW = cms.move.HOVER_NEW = 'cms-hover-new';
   
   /**
    * Status of the current move-process.<p>
    */
   var moveState = null;
   cms.move.zIndexMap = {};
   
   /**
    * Status object for move-process
    */
   var MoveState = function() {
      this.currentResourceId = null;
      this.currentContainerId = null;
      this.element = null;
      this.hoverList = null;
      this.isLoading = false;
      this.runningRequest = null;
      this.loadingResourceId = null;
      this.hasStopped = false;
      this.origPlaceholder = null;
      this.startId = null;
      this.over = null;
      this.overflowElem = null;
      this.preparing = true;
      this.movingSubcontainer = false;
      this.sortingData = {};
      
      this.isMoveFromFavorites = function() {
         return this.startId == cms.html.favoriteListId;
      }
      
      this.isMoveFromMenu = function() {
         return isMenuContainer(this.startId);
      }
      
      this.isMoveFromResultList = function() {
         return this.startId.indexOf(cms.html.galleryResultListPrefix) >= 0;
      }
      
      this.isMoveFromNew = function() {
         return this.startId == cms.html.galleryTypeListId;
      }
      
      this.isMoveToFavorites = function() {
         return this.currentContainerId == cms.html.favoriteDropListId;
      }
      
      
      this.shouldAddToRecent = function() {
         return !(this.isMoveToFavorites() || this.isMoveFromNew());
      }
      
      /**
       * Loads a new element for drag-and-drop
       *
       * @param {Object} tempId Id of the element that should be displayed while the new element is loaded
       * @param {Object} loadId Id of the element to load
       * @param {Object} ui the sortable object
       */
      this.loadElement = function(tempId, loadId, ui) {
         var self = this;
         self.isLoading = true;
         self.loadingResourceId = loadId;
         self.currentResourceId = tempId;
         self.runningRequest = cms.data.loadElements([self.loadingResourceId], function() {
         
            if (self.hasStopped) {
               self.currentResourceId = self.loadingResourceId;
               self.element = cms.data.elements[self.loadingResourceId];
               $('#' + self.currentContainerId + ' .cms-element[rel="' + self.loadingResourceId + '"]').replaceWith(self.element.getContent(self.currentContainerId))
               moveState = null;
            } else {
               self.currentResourceId = self.loadingResourceId;
               self.element = cms.data.elements[self.loadingResourceId];
               self.movingSubcontainer = self.element != null && self.element.subItems != null;
               if (cms.toolbar.editingSubcontainerId != null) {
                  initContainerForDrag(ui.self, cms.data.containers[cms.toolbar.editingSubcontainerId]);
               } else {
                  for (var container_name in cms.data.containers) {
                     initContainerForDrag(ui.self, cms.data.containers[container_name]);
                  }
               }
               self.over = false;
               
               // container highlighting needs to be refreshed *before* helper positions
               refreshContainerHighlighting(self.hoverList, 2);
               refreshHelperPositions(ui.self);
               
            }
            self.isLoading = false;
            self.runningRequest = null;
            
         });
         self.element = cms.data.elements[tempId];
      }
      
   }
   
   
   /**
    * Checks whether given id matches a menu-container.<p>
    *
    * @param {string} id
    * @return boolean
    */
   var isMenuContainer = cms.move.isMenuContainer = function(id) {
      //#
      return id == cms.html.favoriteListId || id == cms.html.recentListId || id == cms.html.newListId || id == cms.html.searchListId || id == cms.html.favoriteDropListId || id == cms.html.galleryTypeListId || id.indexOf(cms.html.galleryResultListPrefix) >= 0;
   }
   
   /**
    * Checks whether the given container has reached its max-elements number.<p>
    *
    * @param {Object} container container-object
    */
   var isOverflowContainer = cms.move.isOverflowContainer = function(container) {
      // in case maxElem of the container is <= 0 no limit will be set
      if (container && (container.maxElem > 0)) {
         return container.elements.length >= container.maxElem;
      }
      return false;
   }
   
   
   /**
    * Things to do after element dragging is done.<p>
    *
    * @param {Object} event
    * @param {Object} ui
    */
   var onDeactivateDrag = cms.move.onDeactivateDrag = function(event, ui) {
      var handleDiv = ui.self.currentItem.children('.cms-handle');
      if (handleDiv) {
         cms.toolbar.initHandleDiv(handleDiv, ui.self.currentItem, cms.toolbar.timer.adeMode);
      }
      if ('move' != cms.toolbar.timer.adeMode) {
         //$(this).hide();
         handleDiv.children().hide();
         handleDiv.children('.cms-' + cms.toolbar.timer.adeMode).show();
      }
      $('#' + cms.html.favoriteDropListId + ' li').hide(200);
      $('#' + cms.html.favoriteDropMenuId).css('display', 'none');
      $('.cms-handle').show();
      if ($.browser.msie) {
         setTimeout(function() {
            $('.cms-element').css('display', 'block');
         }, 50);
      }
   }
   
   /**
    * Puts z-index of given container into the z-index-map
    *
    * @param {Object} containerId
    */
   var saveZIndex = cms.move.saveZIndex = function(containerId) {
      cms.move.zIndexMap[containerId] = $('#' + containerId).css('z-index');
   }
   
   /**
    * Checks whether an element is compatible with a container with the given id.<p>
    *
    * A subcontainer is compatible with a container if all its elements are.
    *
    */
   var isCompatibleWithContainer = cms.move.isCompatibleWithContainer = /*boolean*/ function(/*Object*/element, /*String*/ containerId) {
      var containerType = cms.data.containers[containerId].type;
      return !!(element.contents[containerType])
   }
   
   
   /**
    * Creates the sortable-helper elment for the given container.<p>
    *
    * @param {Object} sortable
    * @param {Object} container
    */
   var initContainerForDrag = cms.move.initContainerForDrag = function(sortable, container) {
      var containerType = container.type;
      //skip incompatible containers
      if (!isCompatibleWithContainer(cms.data.elements[moveState.currentResourceId], container.name)) {
         return;
      }
      saveZIndex(container.name);
      
      if (container.name != moveState.startId) {
         moveState.hoverList += ', #' + container.name;
         var helperElem = moveState.element.getContent(containerType);
         
         moveState.helpers[container.name] = helperElem.css({
            'display': 'none',
            'position': 'absolute',
            'zIndex': sortable.options.zIndex
         }).addClass('ui-sortable-helper cms-element').attr('rel', moveState.element.id).appendTo('#' + container.name);
         cms.toolbar.addHandles(moveState.helpers[container.name], moveState.currentResourceId, cms.toolbar.timer.adeMode ? cms.toolbar.timer.adeMode : 'move', true);
         // to increase visibility of the helper
         if (!helperElem.hasClass(cms.html.subcontainerClass)) {
            if (helperElem.css('background-color') == 'transparent' && helperElem.css('background-image') == 'none') {
               helperElem.addClass('cms-helper-background');
            }
            if (!helperElem.css('border') || !helperElem.css('border') == 'none' || helperElem.css('border') == '') {
               helperElem.addClass('cms-helper-border');
            }
         }
         // check if helper element is floated and add cms-float-container class
         if ((/left|right/).test(moveState.helpers[container.name].css('float'))) {
            $('#' + container.name).addClass('cms-float-container');
         }
      } else {
         moveState.helpers[container.name] = sortable.helper;
         // to increase visibility of the helper
         if (!sortable.helper.hasClass(cms.html.subcontainerClass)) {
            if (sortable.helper.css('background-color') == 'transparent' && sortable.helper.css('background-image') == 'none') {
               sortable.helper.addClass('cms-helper-background');
            }
            if (!sortable.helper.css('border') || sortable.helper.css('border') == 'none' || sortable.helper.css('border') == '') {
               sortable.helper.addClass('cms-helper-border');
            }
         }
         moveState.over = true;
         
         // check if helper element is floated and add cms-float-container class
         if ((/left|right/).test(sortable.placeholder.css('float'))) {
            $('#' + container.name).addClass('cms-float-container');
         }
      }
      
   }
   
   
   /**
    * Preparations for a drag from the menu.<p>
    *
    * @param {Object} sortable
    */
   var startDragFromMenu = cms.move.startDragFromMenu = function(sortable) {
      sortable.scrollParent = $(document);
      moveState.helpers[moveState.startId] = sortable.helper;
      var elem = $(document.createElement('div')).addClass("placeholder" + " ui-sortable-placeholder box").css('display', 'none');
      sortable.placeholder.replaceWith(elem);
      sortable.placeholder = elem;
      
      $('.cms-additional', sortable.currentItem).hide();
      
      sortable.helper.appendTo(cms.toolbar.dom.appendBox).css('z-index', 10000);
      cms.toolbar.dom.appendBox.css('position', 'absolute');
      $('#' + moveState.startId).closest('.cms-menu').css('display', 'none');
      moveState.over = false;
   }
   
   
   /**
    * Preparations for dragging from a container.<p>
    *
    * @param {Object} sortable
    */
   var startDragFromNormalContainer = cms.move.startDragFromNormalContainer = function(sortable) {
      // prepare handles for move
      if (cms.toolbar.timer.id) {
         clearTimeout(cms.toolbar.timer.id);
         cms.toolbar.timer.id = null;
      }
      var thisHandleDiv = sortable.currentItem.children('.cms-handle').unbind('mouseenter').unbind('mouseleave');
      thisHandleDiv.children().removeClass('ui-corner-all ui-state-default');
      removeAllHighlighting();
      $('div.cms-handle').not(thisHandleDiv).hide();
      thisHandleDiv.removeClass('ui-widget-header').children('*:not(.cms-move)').hide();
      
      moveState.hoverList += '#' + moveState.startId;
      cms.util.fixZIndex(moveState.startId, cms.move.zIndexMap);
      // show drop zone for new favorites
      var list_item = cms.html.formatFavListItem(moveState.element).append('<a class="cms-handle cms-move"></a>');
      
      // shouldn't be able to drag new items to favorites before a resource is created 
      if (moveState.element.status == cms.data.STATUS_NEWCONFIG) {
         return;
      }
      //#
      moveState.helpers[cms.html.favoriteDropListId] = $(list_item).appendTo('#' + cms.html.favoriteDropListId).css({
         'display': 'none',
         'position': 'absolute',
         'zIndex': sortable.options.zIndex
      }).addClass('ui-sortable-helper');
      //#
      $('#' + cms.html.favoriteDropMenuId).show();
   }
   
   
   
   
   
   /**
    * Initializing the move/drag-process.<p>
    *
    * @param {Object} event
    * @param {Object} ui
    */
   var onStartDrag = cms.move.onStartDrag = function(event, ui) {
   
   
      $('.' + cms.move.HOVER_NEW).remove();
      
      moveState = new MoveState();
      moveState.startId = ui.self.currentItem.parent().attr('id');
      moveState.hoverList = '';
      
      moveState.currentContainerId = moveState.startId;
      moveState.loadingResourceId = moveState.currentResourceId = ui.self.currentItem.attr('rel');
      if (moveState.isMoveFromNew()) {
         var typeElem = cms.data.elements[moveState.currentResourceId];
         if (typeElem) {
            var newItem = moveState.element = typeElem.cloneAsNew(); // ui.self.cmsItem = cms.util.createInstanceForNewItem(typeElem.id);
            moveState.currentResourceId = newItem.id;
         }
      } else {
         // the element may be undefined 
         moveState.element = cms.data.elements[moveState.currentResourceId];
      }
      moveState.movingSubcontainer = moveState.element != null && moveState.element.subItems != null;
      // save the current offset. this is needed by ui.sortable._mouseStop for the reverting-animation in case the move is canceled
      ui.self.cmsStartOffset = {
         top: ui.self.offset.top,
         left: ui.self.offset.left
      };
      
      moveState.helpers = {};
      
      moveState.origPlaceholder = origPlaceholder = ui.placeholder.clone().insertBefore(ui.placeholder);
      moveState.origPlaceholder.css({
         'background-color': 'gray',
         'display': 'none'
      });
      
      cms.move.zIndexMap = {};
      
      if (!cms.toolbar.getCurrentMode().isEdit || isMenuContainer(moveState.startId)) {
         startDragFromMenu(ui.self);
      } else {
         startDragFromNormalContainer(ui.self);
      }
      
      var sortingData = {};
      sortingData['helperCss'] = {
         'width': ui.self.currentItem.width() + 'px',
         'height': ''
      };
      if (moveState.movingSubcontainer && !moveState.isMoveFromNew()) {
         var backgroundData = ui.self.currentItem.data('backgroundData');
         sortingData['placeholderCss'] = {
            'margin-top': backgroundData.dimension.top + 'px',
            'margin-left': backgroundData.dimension.left + 'px',
            'margin-right': (backgroundData.parentDimension.width - backgroundData.dimension.width - backgroundData.dimension.left) + 'px',
            'margin-bottom': (backgroundData.parentDimension.height - backgroundData.dimension.height - backgroundData.dimension.top) + 'px',
            'height': backgroundData.dimension.height + 'px',
            'width': ''
         };
      } else {
         sortingData['placeholderCss'] = {
            'height': ui.self.currentItem.height() + 'px'
         }
         
      }
      ui.self.placeholder.css(sortingData['placeholderCss']);
      moveState.origPlaceholder.css(sortingData['placeholderCss']);
      ui.self.placeholder.addClass(ui.self.currentItem.attr('class'));
      moveState.origPlaceholder.addClass(ui.self.placeholder.attr('class'));
      ui.self.placeholder.css({
         'background-color': 'blue',
         'border': 'solid 2px black'
      });
      moveState.sortingData[moveState.currentContainerId] = sortingData;
      ui.self.currentItem.data('sortingData', sortingData);
      
      if (!moveState.element) {
         if (moveState.isMoveFromResultList()) {
            var resourceType = ui.self.currentItem.data('type');
            moveState.loadElement(resourceType, moveState.currentResourceId, ui);
         }
         refreshHelperPositions(ui.self);
      } else if (moveState.element.partial) {
         moveState.loadElement(moveState.currentResourceId, moveState.currentResourceId, ui);
         refreshHelperPositions(ui.self);
      } else {
         if (cms.toolbar.editingSubcontainerId != null) {
            initContainerForDrag(ui.self, cms.data.containers[cms.toolbar.editingSubcontainerId]);
         } else {
            for (var container_name in cms.data.containers) {
               initContainerForDrag(ui.self, cms.data.containers[container_name]);
            }
         }
         refreshContainerHighlighting(moveState.hoverList, 2);
         refreshHelperPositions(ui.self);
      }
   }
   
   
   
   /**
    * Sets the cancel-flag if dragging stops outside the containers.<p>
    *
    * @param {Object} event
    * @param {Object} ui
    */
   var beforeStopFunction = cms.move.beforeStopFunction = function(event, ui) {
      moveState.cancel = !moveState.over;
   }
   
   /**
    * Gets all element-ids from the given container an updates the container-object with these.<p>
    *
    * @param {Object} id container-id
    */
   var updateContainer = cms.move.updateContainer = function(id) {
      if (isMenuContainer(id)) {
         return;
      }
      var newContents = [];
      $('#' + id + ' > *:visible').each(function() {
         newContents.push($(this).attr("rel"));
      });
      
      cms.data.containers[id].elements = newContents;
   }
   
   
   /**
    * Removes the helpers after a move operation.<p>
    *
    * @param {Object} helpers the helpers map
    * @param {Object} startContainer the name of the start container
    * @param {Object} endContainer the name of the end container
    */
   var _removeHelpers = function(helpers, startContainer, endContainer) {
      for (var containerName in helpers) {
         // remove cms-float-container class and reset min-height
         var container = $('#' + containerName);
         container.css('min-height', '');
         if (container.hasClass('cms-float-container')) {
            container.removeClass('cms-float-container');
         }
         var helper = helpers[containerName];
         if (containerName == endContainer) {
            // don't remove helper from end container
            continue;
         }
         if (containerName == startContainer && isMenuContainer(endContainer)) {
            // don't remove helper from start container if element was dragged into menu
            continue;
         }
         
         if (isMenuContainer(containerName)) {
            // don't remove helper from menu
            continue;
         }
         helper.remove();
         
         
      }
   }
   
   
   var addSubcontainerBackground = cms.move.addSubcontainerBackground = function(elem) {
      var dimension = cms.util.getInnerDimensions(elem);
      var elemPos = cms.util.getElementPosition(elem);
      var hWidth = 2;
      var inner = {
         top: dimension.top - (elemPos.top + hWidth),
         left: dimension.left - (elemPos.left + hWidth),
         height: dimension.height + 2 * hWidth,
         width: dimension.width + 2 * hWidth
      };
      var backgroundDiv = addBackgroundDiv(elem, inner, 'cms-subcontainer-background');
      var backData = {
         "dimension": inner,
         "parentDimension": {
            "width": elem.width(),
            "height": elem.height()
         }
      };
      elem.data('backgroundData', backData);
      return backData;
   }
   
   var addBackgroundDiv = cms.move.addBackgroundDiv = function(elem, dimension, classAttr) {
      return $('<div class="' + classAttr + '" style="position: absolute; z-index:0; top: ' +
      dimension.top +
      'px; left: ' +
      dimension.left +
      'px; height: ' +
      dimension.height +
      'px; width: ' +
      dimension.width +
      'px;"></div>').prependTo(elem);
   }
   
   
   /**
    * Finishing the drag process.<p>
    *
    * @param {Object} event
    * @param {Object} ui
    */
   var onStopDrag = cms.move.onStopDrag = function(event, ui) {
      cms.util.fixZIndex(null, cms.move.zIndexMap);
      var helpers = moveState.helpers;
      var origPlaceholder = moveState.origPlaceholder;
      var startContainer = moveState.startId;
      var endContainer = moveState.currentContainerId;
      var currentItem = ui.self.currentItem;
      
      if (moveState.cancel) {
         if (!cms.toolbar.getCurrentMode().isEdit || moveState.isMoveFromMenu()) {
            // show favorite list again after dragging a favorite from it.
            $('#' + cms.toolbar.currentMenu).css('display', 'block');
            if (moveState.isLoading && moveState.runningRequest) {
               cms.comm.removeRequest(moveState.runningRequest);
               moveState.runningRequest.abort();
               moveState.isLoading = false;
            }
         }
         
         $(this).sortable('cancel');
         origPlaceholder.remove();
      } else {
         var changed = false;
         if (!cms.toolbar.getCurrentMode().isEdit || moveState.isMoveFromMenu()) {
            // replace placeholder in the menu with the helper, i.e. the original item
            var startHelper = helpers[startContainer];
            origPlaceholder.replaceWith(startHelper);
            startHelper.removeClass('ui-sortable-helper');
            cms.util.clearAttributes(startHelper.get(0).style, ['width', 'height', 'top', 'left', 'position', 'opacity', 'zIndex', 'display']);
            $('div.cms-handle', currentItem).remove();
            $('button.ui-state-active').trigger('click');
            changed = true;
         } else {
            // check if there has been a real change. if the item has not moved to another container and is next to the original-placeholder, the page hasn't changed
            if (startContainer != endContainer || !((currentItem.next('.cms-placeholder').length > 0) || (currentItem.prev('.cms-placeholder').length > 0))) {
               changed = true;
            }
            origPlaceholder.remove();
         }
         
         if (moveState.isMoveToFavorites()) {
            changed = false;
            $('#' + cms.html.favoriteDropListId).children().remove();
            cms.util.addToElementList(cms.toolbar.favorites, moveState.isLoading ? moveState.loadingResourceId : moveState.currentResourceId, 9999);
            cms.data.persistFavorites(function(ok) {
               if (!ok) {
                              // TODO
               }
            });
         }
         
         if (changed && cms.toolbar.editingSubcontainerId == null) {
            // page has changed, enable saveButton
            cms.toolbar.setPageChanged(true);
         }
         if (moveState.shouldAddToRecent()) {
            cms.toolbar.addToRecent(moveState.isLoading ? moveState.loadingResourceId : moveState.currentResourceId);
         }
      }
      
      if (moveState.isMoveToFavorites()) {
         helper = helpers[startContainer];
         var helperStyle = helper.get(0).style;
         helper.removeClass('ui-sortable-helper cms-helper-border cms-helper-background');
         helper.removeData('sortingData');
         // reset position (?) of helper that was dragged to favorites,
         // but don't remove it
         cms.util.clearAttributes(helperStyle, ['width', 'height', 'top', 'left', 'opacity', 'zIndex', 'display']);
         
         // reset handles
         var handleDiv = $('div.cms-handle', helper);
         cms.toolbar.initHandleDiv(handleDiv, helper, cms.toolbar.timer.adeMode);
         if ('move' != cms.toolbar.timer.adeMode) {
            handleDiv.children('.cms-move').hide();
            handleDiv.children('.cms-' + cms.toolbar.timer.adeMode).show();
         }
         
         helperStyle.position = 'relative';
         if ($.browser.msie) {
            helperStyle.removeAttribute('filter');
         }
      }
      
      _removeHelpers(helpers, startContainer, endContainer);
      
      //$(ui.self.cmsHoverList).removeClass('show-sortable');
      
      removeAllHighlighting();
      
      cms.util.clearAttributes(currentItem.get(0).style, ['top', 'left', 'zIndex', 'display', 'opacity']);
      currentItem.removeClass('cms-helper-border cms-helper-background');
      if ($.browser.msie) {
         currentItem.get(0).style.removeAttribute('filter');
         
      } else if (currentItem) {
         currentItem.get(0).style.opacity = '';
      }
      currentItem.removeData('sortingData');
      if (!moveState.cancel) {
      
         // check if end-container is overflowing
         if (startContainer != endContainer && isOverflowContainer(cms.data.containers[endContainer])) {
            var overflowElement = $('.cms-overflow-element', $('#' + endContainer));
            cms.toolbar.addToRecent(overflowElement.attr('rel'));
            overflowElement.remove();
            // just in case: remove leftover cms-overflow-element class
            $('.cms-overflow-element').removeClass('cms-overflow-element');
         }
         if (cms.toolbar.getCurrentMode().isEdit) {
            updateContainer(startContainer);
         }
         updateContainer(endContainer);
         if (moveState.isMoveFromNew()) {
            $('button[name="edit"]').trigger('click');
            removeBorder(currentItem, '.' + HOVER_NEW, $('#' + endContainer));
            drawBorder(currentItem, 2, HOVER_NEW, $('#' + endContainer));
         }
      }
      
      if (moveState.isLoading) {
         moveState.hasStopped = true;
         currentItem.attr('rel', moveState.loadingResourceId)
      } else {
         moveState = null;
      }
      if (cms.toolbar.editingSubcontainerId == null) {
         refreshNewElementHighlighting();
      }
   }
   
   /**
    * Dom-operations necessary to show the appropriate helper and place-holder while dragging over a container.<p>
    *
    * @param {Object} event
    * @param {Object} ui
    */
   var onDragOverContainer = cms.move.onDragOverContainer = function(event, ui) {
      var elem = event.target ? event.target : event.srcElement;
      var containerId = $(elem).attr('id');
      var reDoHover = !moveState.over;
      if (moveState.startId != containerId) {
         // show placeholder in start container if element moves over a different container
         moveState.origPlaceholder.css({
            'display': 'block',
            'border': 'dotted 2px black'
         });
         // check whether current container is overflowing
         if (moveState.helpers[containerId] && isOverflowContainer(cms.data.containers[containerId])) {
            $('.cms-element:not(.ui-sortable-helper, .cms-placeholder):last', elem).addClass('cms-overflow-element');
         }
      } else {
         // hide placeholder (otherwise both the gray and blue boxes would be
         // shown)
         moveState.origPlaceholder.css('display', 'none');
      }
      if (moveState.helpers[containerId]) {
         cms.util.fixZIndex(containerId, cms.move.zIndexMap);
         ui.placeholder.css('display', 'block');
         moveState.over = true;
         if (containerId != moveState.currentContainerId) {
            moveState.currentContainerId = containerId;
            reDoHover = true;
            // hide dragged helper, display helper for container instead
            setHelper(ui.self, containerId);
         }
         setSortableSizes(ui.self.helper, ui.placeholder);
         // in case of a subcontainer use inner height for placeholder and set a margin
      } else {
         ui.placeholder.css('display', 'none');
         moveState.over = false;
      }
      
      // this bit of code is needed to prevent the bug where the display of the placeholder
      // goes out of sync with the over/out events
      if ((containerId == cms.html.favoriteDropListId) && (ui.placeholder.parent().attr('id') != containerId)) {
         ui.placeholder.appendTo(elem);
      }
      if (reDoHover && moveState.hoverList != '') {
         refreshContainerHighlighting(moveState.hoverList, 2);
      }
      
   }
   
   /**
    * Dom-operations necessary to show the appropriate helper and place-holder while dragging outside the containers.<p>
    *
    * @param {Object} event
    * @param {Object} ui
    */
   var onDragOutOfContainer = cms.move.onDragOutOfContainer = function(event, ui) {
      var elem = event.target ? event.target : event.srcElement;
      var containerId = $(elem).attr('id');
      
      if (moveState.over && ui.self.helper && containerId == moveState.currentContainerId) {
         if (moveState.startId != moveState.currentContainerId) {
            moveState.currentContainerId = moveState.startId;
            cms.util.fixZIndex(moveState.startId, cms.move.zIndexMap);
            setHelper(ui.self, moveState.currentContainerId);
            // check whether current container was overflowing
            if (isOverflowContainer(cms.data.containers[containerId])) {
               $('.cms-overflow-element', elem).removeClass('cms-overflow-element');
            }
         }
         ui.placeholder.css('display', 'none');
         moveState.origPlaceholder.css({
            'display': 'block',
            'border': 'solid 2px black'
         });
         
         moveState.over = false;
         if (moveState.hoverList != '') {
            refreshContainerHighlighting(moveState.hoverList, 2);
            refreshHelperPositions(ui.self);
         }
      }
      
   }
   
   /**
    * Highlighting of the given elment.<p>
    *
    * @param {Object} elem dom-elment
    * @param {Object} hOff highlighting-offset
    */
   var hightlightElement = cms.move.hightlightElement = function(elem) {
      if (elem.filter(':not(:empty)').hasClass('cms-subcontainer')) {
         drawInnerBorder(elem, 2, true, HOVER_NORMAL)
      } else {
         drawBorder(elem, 2, HOVER_NORMAL, elem.parent());
      }
   }
   
   var refreshNewElementHighlighting = cms.move.refreshNewElementHighlighting = function() {
      $('.' + HOVER_NEW).remove();
      $('.cms-new-element').each(function() {
         var elem = $(this);
         highlightNewElement(elem);
      });
   }
   
   var highlightNewElement = cms.move.highlightNewElement = function(elem) {
      drawBorder(elem, 2, HOVER_NEW + ' cms-highlight-' + elem.attr('rel'), elem.parent());
   }
   
   var removeNewElementHighlighting = cms.move.removeNewElementHighlighting = function(elem) {
      if (elem) {
         $('.cms-highlight-' + elem.attr('rel')).remove();
      } else {
         $('.' + HOVER_NEW).remove();
      }
   }
   
   var removeElementHighlighting = cms.move.removeElementHighlighting = function() {
      $('.' + HOVER_NORMAL).remove();
   }
   
   
   
   /**
    * Generalized version of hoverIn which draws animated borders around an element.<p>
    *
    * The elements constituting the border are given a CSS class.
    *
    * @param {Object} elem the element for which a border should be displayed
    * @param {Object} hOff the offset of the border
    * @param {Object} additionalClass the class which should be given to the elements of the border
    * @param {Object} appendToElem the element the border div's will be appended to, if <code>null</code> the body element will be used
    */
   var drawBorder = cms.move.drawBorder = function(elem, hOff, additionalClass, appendToElem) {
      var tHeight = elem.outerHeight();
      var tWidth = elem.outerWidth();
      var hWidth = 2;
      var lrHeight = tHeight + 2 * (hOff + hWidth);
      var btWidth = tWidth + 2 * (hOff + hWidth);
      if (!additionalClass) {
         additionalClass = '';
      }
      if (appendToElem == null) {
         appendToElem == $(document.body);
      }
      
      // if (elem.css('position') == 'relative') {
      if (elem == appendToElem) {
         // if position relative highlighting div's are appended to the element itself
         var tlrTop = -(hOff + hWidth);
         var tblLeft = -(hOff + hWidth);
         // top
         $('<div class="cms-hovering cms-hovering-top"></div>').addClass(additionalClass).height(hWidth).width(btWidth).css('top', tlrTop).css('left', tblLeft).appendTo(elem);
         
         // right
         $('<div class="cms-hovering cms-hovering-right"></div>').addClass(additionalClass).height(lrHeight).width(hWidth).css('top', tlrTop).css('left', tWidth + hOff).appendTo(elem);
         // left
         $('<div class="cms-hovering cms-hovering-left"></div>').addClass(additionalClass).height(lrHeight).width(hWidth).css('top', tlrTop).css('left', tblLeft).appendTo(elem);
         // bottom
         $('<div class="cms-hovering cms-hovering-bottom"></div>').addClass(additionalClass).height(hWidth).width(btWidth).css('top', tHeight + hOff).css('left', tblLeft).appendTo(elem);
      } else {
         // if position not relative highlighting div's are appended to the body element
         var cssPosition = appendToElem.css('position');
         if (cssPosition != 'relative' || cssPosition != 'absolute' || cssPosition != 'fixed') {
            appendToElem.css('position', 'relative');
         }
         var elemOffset = elem.offset();
         var appendOffset = appendToElem.offset();
         var tlrTop = elemOffset.top - appendOffset.top - (hOff + hWidth);
         var tblLeft = elemOffset.left - appendOffset.left - (hOff + hWidth);
         // top
         $('<div class="cms-hovering cms-hovering-top"></div>').addClass(additionalClass).height(hWidth).width(btWidth).css('top', tlrTop).css('left', tblLeft).appendTo(appendToElem);
         // right
         $('<div class="cms-hovering cms-hovering-right"></div>').addClass(additionalClass).height(lrHeight).width(hWidth).css('top', tlrTop).css('left', elemOffset.left - appendOffset.left + tWidth + hOff).appendTo(appendToElem);
         // left
         $('<div class="cms-hovering cms-hovering-left"></div>').addClass(additionalClass).height(lrHeight).width(hWidth).css('top', tlrTop).css('left', tblLeft).appendTo(appendToElem);
         // bottom
         $('<div class="cms-hovering cms-hovering-bottom"></div>').addClass(additionalClass).height(hWidth).width(btWidth).css('top', elemOffset.top - appendOffset.top + tHeight + hOff).css('left', tblLeft).appendTo(appendToElem);
      }
      
      // sometimes the filter property stays set and somehow prevents the hover images from showing
      if ($.browser.msie) {
         elem.css('filter', '');
      }
   }
   
   /**
    * Generalized version of hoverInner, drawing a border around all visible elements inside the given element.<p>
    *
    *  This functions adds a given CSS class to the elements of the border.
    *
    * @param {Object} elem the element for
    * @param {Object} hOff
    * @param {Object} showBackground
    * @param {Object} additionalClass the CSS class for the border elements
    */
   var drawInnerBorder = cms.move.drawInnerBorder = function(elem, hOff, showBackground, additionalClass) {
      elem = $(elem);
      var dimension = cms.util.getInnerDimensions(elem, 25);
      var elemPos = cms.util.getElementPosition(elem);
      var hWidth = 2;
      var inner = {
         top: dimension.top - (elemPos.top + hOff),
         left: dimension.left - (elemPos.left + hOff),
         height: dimension.height + 2 * hOff,
         width: dimension.width + 2 * hOff
      };
      if (showBackground) {
         // inner
         var classAttr = 'cms-highlight-container' + (additionalClass ? (' ' + additionalClass) : '');
         
         addBackgroundDiv(elem, inner, classAttr);
         
      }
      if (elem.hasClass('ui-sortable')) {
         elem.css('min-height', inner.height);
      }
      var cssPosition = elem.css('position');
      if (cssPosition == 'relative' || cssPosition == 'absolute' || cssPosition == 'fixed') {
         // top
         $('<div class="cms-hovering cms-hovering-top"></div>').addClass(additionalClass).height(hWidth).width(inner.width + 2 * hWidth).css('top', inner.top - hWidth).css('left', inner.left - hWidth).appendTo(elem);
         // right
         $('<div class="cms-hovering cms-hovering-right"></div>').addClass(additionalClass).height(inner.height + 2 * hWidth).width(hWidth).css('top', inner.top - hWidth).css('left', inner.left + inner.width + hOff).appendTo(elem);
         // left
         $('<div class="cms-hovering cms-hovering-left"></div>').addClass(additionalClass).height(inner.height + 2 * hWidth).width(hWidth).css('top', inner.top - hWidth).css('left', inner.left - hWidth).appendTo(elem);
         // bottom
         $('<div class="cms-hovering cms-hovering-bottom"></div>').addClass(additionalClass).height(hWidth).width(inner.width + 2 * hWidth).css('top', inner.top + inner.height).css('left', inner.left - hWidth).appendTo(elem);
      } else {
         // top
         $('<div class="cms-hovering cms-hovering-top"></div>').addClass(additionalClass).height(hWidth).width(dimension.width + 2 * (hOff + hWidth)).css('top', dimension.top - (hOff + hWidth)).css('left', dimension.left - (hOff + hWidth)).appendTo(document.body);
         // right
         $('<div class="cms-hovering cms-hovering-right"></div>').addClass(additionalClass).height(dimension.height + 2 * (hOff + hWidth)).width(hWidth).css('top', dimension.top - (hOff + hWidth)).css('left', dimension.left + dimension.width + hOff).appendTo(document.body);
         // left
         $('<div class="cms-hovering cms-hovering-left"></div>').addClass(additionalClass).height(dimension.height + 2 * (hOff + hWidth)).width(hWidth).css('top', dimension.top - (hOff + hWidth)).css('left', dimension.left - (hOff + hWidth)).appendTo(document.body);
         // bottom
         $('<div class="cms-hovering cms-hovering-bottom"></div>').addClass(additionalClass).height(hWidth).width(dimension.width + 2 * (hOff + hWidth)).css('top', dimension.top + dimension.height + hOff).css('left', dimension.left - (hOff + hWidth)).appendTo(document.body);
      }
   }
   
   /**
    *  This function draws a border around the following siblings of the given element.
    *
    * @param {Object} elem the element for
    * @param {Object} hOff
    * @param {Object} showBackground
    * @param {Object} additionalClass the CSS class for the border elements
    */
   var drawSiblingBorder = cms.move.drawSiblingBorder = function(elem, hOff, stopAtClass, showBackground, additionalClass) {
      elem = $(elem);
      var dimension = cms.util.getSiblingsDimensions(elem, 25, stopAtClass);
      var elemPos = cms.util.getElementPosition(elem.parents('.cms-element'));
      var hWidth = 2;
      var inner = {
         top: dimension.top - (elemPos.top + hOff),
         left: dimension.left - (elemPos.left + hOff),
         height: dimension.height + 2 * hOff,
         width: dimension.width + 2 * hOff
      };
      $('div.cms-directedit-buttons', elem).css({
         top: inner.top + hOff,
         left: inner.left + inner.width - 60
      }).hover(function() {
         $(this).nextAll('.cms-hovering').css('display', 'block');
      }, function() {
         $(this).nextAll('.cms-hovering').css('display', 'none');
      });
      $('a.cms-edit-enabled', elem).click(cms.toolbar.openSubelementDialog);
      $('a.cms-new', elem).click(cms.toolbar.openEditNewDialog);
      $('a.cms-delete', elem).click(cms.toolbar.directDeleteItem);
      if (showBackground) {
         // inner
         
         $('<div class="cms-highlight-container" style="position: absolute; z-index:0; top: ' +
         inner.top +
         'px; left: ' +
         inner.left +
         'px; height: ' +
         inner.height +
         'px; width: ' +
         inner.width +
         'px;"></div>').addClass(additionalClass).appendTo(elem);
      }
      
      // top
      $('<div class="cms-hovering cms-hovering-top"></div>').addClass(additionalClass).height(hWidth).width(inner.width + 2 * hWidth).css('top', inner.top - hWidth).css('left', inner.left - hWidth).appendTo(elem);
      // right
      $('<div class="cms-hovering cms-hovering-right"></div>').addClass(additionalClass).height(inner.height + 2 * hWidth).width(hWidth).css('top', inner.top - hWidth).css('left', inner.left + inner.width + hOff).appendTo(elem);
      // left
      $('<div class="cms-hovering cms-hovering-left"></div>').addClass(additionalClass).height(inner.height + 2 * hWidth).width(hWidth).css('top', inner.top - hWidth).css('left', inner.left - hWidth).appendTo(elem);
      // bottom
      $('<div class="cms-hovering cms-hovering-bottom"></div>').addClass(additionalClass).height(hWidth).width(inner.width + 2 * hWidth).css('top', inner.top + inner.height).css('left', inner.left - hWidth).appendTo(elem);
      
   }
   
   /**
    * Drawing a border around all visible elements and placing a background behind them inside the elements matching the selector.<p>
    *
    * @param {String} selector the selector
    * @param {Object} highlightingOffset the border offset
    */
   var refreshContainerHighlighting = cms.move.refreshContainerHighlighting = function(selector, highlightingOffset) {
      $(selector).each(function() {
         var elem = $(this);
         var elemId = elem.attr('id');
         var hoverClass = 'cms-hovering-' + elemId;
         removeBorder(null, '.' + hoverClass);
         drawInnerBorder(elem, highlightingOffset, true, hoverClass);
      });
   }
   
   
   /**
    * Generalized version of hoverOut which filters the border elements with a given filter expression.
    *
    * @param {Object} context the parent element from which the border elements should be removed
    * @param {Object} filterString the JQuery filter string which the items to be removed should match
    */
   var removeBorder = cms.move.removeBorder = function(context, filterString) {
      var $toRemove;
      if (context == null || !context.length) {
         $toRemove = $('div.cms-hovering, div.cms-highlight-container');
      } else {
         $toRemove = $('div.cms-hovering, div.cms-highlight-container', context);
      }
      if (filterString) {
         $toRemove = $toRemove.filter(filterString);
      }
      $toRemove.remove();
   };
   
   var replaceHelperElements = function(sortable) {
      var reDoHover = false;
      for (var container_name in cms.data.containers) {
         var containerType = cms.data.containers[container_name].type;
         //skip incompatible containers
         if (!isCompatibleWithContainer(cms.data.elements[moveState.currentResourceId], container_name) || container_name == moveState.startId) {
            continue;
         }
         if (!moveState.helpers[container_name]) {
            initContainerForDrag(sortable, cms.data.containers[container_name]);
            reDoHover = true;
         } else {
            var helperContent = moveState.element.getContent(containerType);
            helperContent.attr('class', moveState.helpers[container_name].attr('class'));
            helperContent.attr('style', moveState.helpers[container_name].attr('style'));
            helperContent.removeClass('cms-new-element');
            moveState.helpers[container_name].replaceWith(helperContent);
            moveState.helpers[container_name] = helperContent;
         }
         
         if (moveState.currentContainerId == container_name) {
            sortable.helper = helperContent;
            sortable.currentItem = helperContent;
            refreshHelperPositions(sortable);
         }
      }
      if (reDoHover) {
         refreshContainerHighlighting(moveState.hoverList, 2);
      }
   }
   
   
   /**
    * Removes the highlighting within a given context.<p>
    */
   var removeAllHighlighting = cms.move.removeAllHighlighting = function() {
      $('div.cms-hovering, div.cms-highlight-container').remove();
   }
   
   
   /**
    * Switches helpers in the dragging process.<p>
    *
    * @param {Object} sortable
    * @param {Object} id
    */
   var setHelper = cms.move.setHelper = function(sortable, id) {
      sortable.helper.css('display', 'none');
      sortable.helper = moveState.helpers[id].css('display', 'block');
      sortable.currentItem = moveState.helpers[id];
      sortable.placeholder.attr('class', sortable.currentItem.attr('class') + ' cms-placeholder').removeClass('ui-sortable-helper');
      refreshHelperPositions(sortable);
   };
   
   var setSortableSizes = function(helper, placeholder) {
      var sortingData = moveState.sortingData[moveState.currentContainerId];//  helper.data('sortingData');
      if (sortingData != null) {
         // helper.css(sortingData['helperCss']);
         placeholder.css(sortingData['placeholderCss']);
      } else {
         sortingData = {};
         if (moveState.movingSubcontainer && !isMenuContainer(moveState.currentContainerId)) {
            var backgroundData = helper.data('backgroundData');
            if (backgroundData == null) {
               placeholder.css({
                  'height': '',
                  'width': '',
                  'margin-top': '',
                  'margin-left': '',
                  'margin-right': '',
                  'margin-bottom': ''
               });
               sortingData['helperCss'] = {
                  'width': placeholder.width() + 'px',
                  'height': ''
               };
               helper.css(sortingData['helperCss']);
               backgroundData = addSubcontainerBackground(helper);
               var rightPos = backgroundData['parentDimension']['width'] - backgroundData['dimension']['left'] - backgroundData['dimension']['width'];
               helper.children('.cms-handle').css('right', rightPos + 'px');
            } else {
               sortingData['helperCss'] = {
                  'width': backgroundData['parentDimension']['width'] + 'px',
                  'height': ''
               };
               helper.css(sortingData['helperCss']);
            }
            sortingData['placeholderCss'] = {
               'margin-top': backgroundData.dimension.top + 'px',
               'margin-left': backgroundData.dimension.left + 'px',
               'margin-right': (backgroundData.parentDimension.width - backgroundData.dimension.width - backgroundData.dimension.left) + 'px',
               'margin-bottom': (backgroundData.parentDimension.height - backgroundData.dimension.height - backgroundData.dimension.top) + 'px',
               'height': backgroundData.dimension.height + 'px',
               'width': ''
            };
            placeholder.css(sortingData['placeholderCss']);
         } else {
            sortingData['helperCss'] = {
               'width': placeholder.width() + 'px',
               'height': ''
            };
            helper.css(sortingData['helperCss']);
            sortingData['placeholderCss'] = {
               'height': helper.height() + 'px'
            }
            placeholder.css(sortingData['placeholderCss']);
         }
         moveState.sortingData[moveState.currentContainerId] = sortingData;// helper.data('sortingData', sortingData);
      }
   }
   
   
   /**
    * Forces a recalculation of the helper-position in the dragging-process.<p>
    *
    * @param {Object} sortable
    */
   var refreshHelperPositions = cms.move.refreshHelperPositions = function(sortable) {
      sortable._cacheHelperProportions();
      sortable._adjustOffsetFromHelper(sortable.options.cursorAt);
      sortable.refreshPositions(true);
   };
   
})(cms);
