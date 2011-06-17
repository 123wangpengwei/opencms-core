(function(cms) {
   var M = cms.messages;
   
   /*util*/
   var log = cms.util.log = function(s) {
      $("body").append("<p>" + s + "</p>");
   }
   
   /* util */
   var dump = cms.util.dump = function(s) {
      $("<pre></pre>").text($.dump(s)).appendTo("body");
   }
   
   var addUnique = cms.util.addUnique = function(list, item, maxlen) {
      for (var i = 0; i < list.length; i++) {
         if (list[i] == item) {
            list.splice(i, 1);
            break;
         }
      }
      list.splice(0, 0, item);
      if (maxlen) {
         var delLength = list.length - maxlen;
         if (delLength > 0) 
            list.splice(maxlen, delLength);
      }
   }
   
   var addToList = cms.util.addToList = function(resource_id, list, max_size) {
      var newList = [resource_id];
      for (var i = 0; i < list.length; i++) {
         if (resource_id != list[i]) {
            newList.push(list[i]);
         }
         if (max_size && newList.length >= max_size) {
            break;
         }
      }
      return newList;
   }
   
   var clearAttributes = cms.util.clearAttributes = function(elem, attrs) {
      var ie = $.browser.msie;
      for (var i = 0; i < attrs.length; i++) {
         if (ie) {
            elem.removeAttribute(attrs[i]);
         } else {
            elem[attrs[i]] = '';
         }
      }
   }
   
   var getElementPosition = cms.util.getElementPosition = function(elem) {
      var position = {
         left: 0,
         top: 0
      };
      var offset = elem.offset();
      var cssPosition = $(document.body).css('position');
      if (cssPosition == 'relative' || cssPosition == 'absolute') {
         var bodyOffset=$(document.body).offset();
         position.left = offset.left - bodyOffset.left;
         position.top = offset.top - bodyOffset.top;
      } else {
         position.left = offset.left;
         position.top = offset.top;
      }
      return position;
   }
   
   /**
    * Calculates dimensions of the visible children of the given element.<p>
    *
    * @param {object} elem jquery-object
    * @param {integer} minHeight default height
    * @return {object} hashmap containing dimension-data as left, top, bottom, right, height, width
    */
   var getInnerDimensions = cms.util.getInnerDimensions = function(elem, minHeight) {
      var dimension = {
         left: 'x',
         top: 'x',
         bottom: 'x',
         right: 'x',
         height: '',
         width: ''
      
      };
      //      var bottom = 'x';
      //      var right = 'x';
      var contentElements = elem.children('*:visible:not(.ui-sortable-helper, div.cms-handle)');
      contentElements = contentElements.add(contentElements.filter('.cms-subcontainer').children('*:visible')).not('.cms-subcontainer:not(.cms-placeholder)');
      if (contentElements.length) {
         contentElements.each(function() {
            var el = $(this);
            var pos = cms.util.getElementPosition(el);
            dimension.left = (dimension.left == 'x' || pos.left < dimension.left) ? pos.left : dimension.left;
            dimension.top = (dimension.top == 'x' || pos.top < dimension.top) ? pos.top : dimension.top;
            dimension.bottom = (dimension.bottom == 'x' ||
            dimension.bottom <
            (pos.top +
            el.outerHeight())) ? pos.top +
            el.outerHeight() : dimension.bottom;
            dimension.right = (dimension.right == 'x' ||
            dimension.right <
            (pos.left +
            el.outerWidth())) ? pos.left +
            el.outerWidth() : dimension.right;
            
         });
      } else {
          elem.filter('.ui-sortable').css('min-height','');
          var height = elem.outerHeight();
          height = (height > minHeight) ? height : minHeight;
         var elemPos = getElementPosition(elem);
         dimension.top = elemPos.top;
         dimension.left = elemPos.left;
         dimension.right = dimension.left + elem.innerWidth();
         dimension.bottom = dimension.top + height;
      }
      dimension.height = dimension.bottom - dimension.top;
      dimension.width = dimension.right - dimension.left;
      return dimension;
   }
   
   var getScrollXY = cms.util.getScrollXY = function() {
      var scrOfX = 0, scrOfY = 0;
      if (typeof(window.pageYOffset) == 'number') {
         //Netscape compliant
         scrOfY = window.pageYOffset;
         scrOfX = window.pageXOffset;
      } else if (document.body && (document.body.scrollLeft || document.body.scrollTop)) {
         //DOM compliant
         scrOfY = document.body.scrollTop;
         scrOfX = document.body.scrollLeft;
      } else if (document.documentElement && (document.documentElement.scrollLeft || document.documentElement.scrollTop)) {
         //IE6 standards compliant mode
         scrOfY = document.documentElement.scrollTop;
         scrOfX = document.documentElement.scrollLeft;
      }
      return { 'scrollX': scrOfX, 'scrollY': scrOfY };
   }
   
   
   /**
    * Calculates dimensions for the following siblings of the given element.<p>
    *
    * @param {object} elem jquery-object
    * @param {integer} minHeight default height
    * @param {string} stopAtClass only calculate dimensions until next sibling with this class
    * @return {object} hashmap containing dimension-data as left, top, bottom, right, height, width
    */
   var getSiblingsDimensions = cms.util.getSiblingsDimensions = function(elem, minHeight, stopAtClass) {
      var dimension = {
         left: 'x',
         top: 'x',
         bottom: 'x',
         right: 'x',
         height: '',
         width: ''
      
      };
      var current = elem.next('*:visible:not(.' + stopAtClass + ', .cms-handle)');
      if (current.length) {
         while (current.length) {
            var pos = cms.util.getElementPosition(current);
            dimension.left = (dimension.left == 'x' || pos.left < dimension.left) ? pos.left : dimension.left;
            dimension.top = (dimension.top == 'x' || pos.top < dimension.top) ? pos.top : dimension.top;
            dimension.bottom = (dimension.bottom == 'x' ||
            dimension.bottom <
            (pos.top +
            current.outerHeight())) ? pos.top +
            current.outerHeight() : dimension.bottom;
            dimension.right = (dimension.right == 'x' ||
            dimension.right <
            (pos.left +
            current.outerWidth())) ? pos.left +
            current.outerWidth() : dimension.right;
            
            current = current.next('*:visible:not(.' + stopAtClass + ', .cms-handle)');
         }
      } else {
         var elemPos = getElementPosition(elem);
         dimension.top = elemPos.top;
         dimension.left = elemPos.left;
         dimension.right = dimension.left + elem.innerWidth();
         dimension.bottom = dimension.top + minHeight;
      }
      dimension.height = dimension.bottom - dimension.top;
      dimension.width = dimension.right - dimension.left;
      return dimension;
   }
   
   var getWindowDimensions = cms.util.getWindoDimensions = function() {
      var myWidth = 0, myHeight = 0;
      if (typeof(window.innerWidth) == 'number') {
         //Non-IE
         myWidth = window.innerWidth;
         myHeight = window.innerHeight;
      } else if (document.documentElement && (document.documentElement.clientWidth || document.documentElement.clientHeight)) {
         //IE 6+ in 'standards compliant mode'
         myWidth = document.documentElement.clientWidth;
         myHeight = document.documentElement.clientHeight;
      } else if (document.body && (document.body.clientWidth || document.body.clientHeight)) {
         //IE 4 compatible
         myWidth = document.body.clientWidth;
         myHeight = document.body.clientHeight;
      }
      return {
         'height': myHeight,
         'width': myWidth
      };
   }
   
   
   var fixZIndex = cms.util.fixZIndex = function(currentId, zmap) {
      if (!$.browser.msie) {
         return;
      }
      var z;
      for (var key in zmap) {
         if (key == currentId) {
            z = 9999;
         } else {
            z = zmap[key];
         }
         setZIndex(key, z);
      }
   }
   
   var setZIndex = function(id, z) {
      $('#' + id).css('z-index', z);
   }
   
   /**
    * Creates a selector by replaing the '%' character in a selector template
    * with each of a list of strings, and joining all the resulting strings
    * with commas.
    */
   var makeCombinedSelector = cms.util.makeCombinedSelector = function(strs, template) {
      return $.map(strs, function(s) {
         return template.replace("%", s);
      }).join(", ");
   }
   
   var getKeys = cms.util.getKeys = function(obj) {
      var keys = [];
      for (var key in obj) {
         keys.push(key);
      }
      return keys;
   }
   
   /**
    * Returns a selector that matches all containers on the page.
    */
   var getContainerSelector = cms.util.getContainerSelector = function() {
      return cms.util.makeCombinedSelector(cms.util.getKeys(cms.data.containers), '#%')
   }
   
   
   
   var replaceListElements = function(list, oldElem, newElem) {
      var result = [];
      for (var i = 0; i < list.length; i++) {
         result[i] = list[i] == oldElem ? newElem : list[i];
         
      }
      return result;
   }
   
   
   var replaceNewElement = cms.util.replaceNewElement = function(oldName, newName) {
      for (var containerName in cms.data.containers) {
         var container = cms.data.containers[containerName];
         container.elements = replaceListElements(container.elements, oldName, newName);
      }
   }
   
   
      /**
    * Opens the leave page confirm dialog (if page has changed)
    *
    * @param {String} target the target page to open
    * @param {Function} saveFunction function to save the page
    * @param {Function} leaveFunction function to leave the page (optional)
    */
   var leavePageDialog = cms.util.leavePageDialog = function(target, saveFunction, leaveFunction) {
      var buttons = {};
      buttons[M.GUI_LEAVE_PAGE_SAVE_0] = function() {
         $(this).dialog('destroy');
         saveFunction(function(ok) {
            if (ok) {
               $.isFunction(leaveFunction) ? leaveFunction(target) : window.location.href=target;
            }
         });
      };
      
      buttons[M.GUI_LEAVE_PAGE_OK_0] = function() {
         $(this).dialog('destroy');
         $.isFunction(leaveFunction) ? leaveFunction(target) : window.location.href=target;
      };
      
      buttons[M.GUI_LEAVE_PAGE_CANCEL_0] = function() {
         $(this).dialog('destroy');
      };
      var $dialogDiv = $('#cms-leave-dialog');
      if (!$dialogDiv.length){
          $dialogDiv=$('<div id="cms-leave-dialog" style="display: none;">' + M.GUI_LEAVE_PAGE_CONFIRM_0 + '</div>').appendTo('body');
      }
      $dialogDiv.dialog({
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
   
   var loadFavoriteAndRecentElements = function(afterLoad) {
      var toLoad = {};
      for (var fav in cms.toolbar.favorites) {
         if (!cms.data.elements[fav]) {
            toLoad[fav] = true;
         }
      }
      for (var recent in cms.toolbar.recent) {
         if (!cms.data.elements[recent]) {
            toLoad[recent] = true;
         }
      }
      cms.data.reloadItems(getKeys(toLoad), afterLoad);
      
   };
   
   /**
    * Converts a jquery object to HTML.<p>
    * Only use this for jquery objects which don't have elements attached to the DOM.
    * @param {Object} $jquery the jquery object to convert to html.
    */
   var jqueryToHtml = cms.util.jqueryToHtml = function($jquery) {
      return $jquery.appendTo("<div></div>").parent().html();
   }
   
   
   var validateForm = cms.util.validateForm = function($form) {
   
      $('span.ade-error', $form).remove();
      var hasError = false;
      
      jQuery.each($('ol.ade-forms li.ade-required', $form), function() {
         var $item = $(this);
         if ($item.hasClass('ade-grouping')) {
            var numSelected = $item.find('input:checked').length;
            if (numSelected == 0) {
               var labelText = $('legend', $item).text();
               labelText = labelText.replace(' *', '');
               $item.append('<span class="ade-error">' + cms.util.format(M.FIELD_CANT_BE_EMPTY, labelText) + '</span>');
               hasError = true;
            }
         } else {
            if (jQuery.trim($('input, textarea', $item).val()) == '') {
               var labelText = $('label', $item).text();
               labelText = labelText.replace(' *', '');
               $item.append('<span class="ade-error">' + cms.util.format(M.FIELD_CANT_BE_EMPTY, labelText) + '</span>');
               hasError = true;
            }
         }
      });
      return (!hasError);
   };
   
   /**
    * Aligns the element's left side with a given coordinate
    */
   var setLeft = cms.util.setLeft = function(element, x) {
      var $elem = $(element);
      if ($elem.css('left')) {
         $elem.css('left', x);
      } else {
         $elem.css('right', x + $elem.width());
      }
   }
   
   
   /**
    * Replacement for alert using a jQuery dialog.<p>
    *
    * @param {String} text the text to display in the dialog
    * @param {String} title the title of the dialog
    */
   var dialogAlert = cms.util.dialogAlert = function(/**String*/text, /**String*/ title) {
      var $dialog = $('<div id="cms-alert-dialog" style="display: none"></div>');
      $dialog.appendTo('body');
      $dialog.text(text);
      var buttons = {};
      buttons[M.ALERT_OK] = function() {
         $dialog.dialog('destroy');
         $dialog.remove();
      }
      $dialog.dialog({
         zIndex: 9999,
         title: title,
         modal: true,
         close: function() {
            $dialog.dialog('destroy');
            $dialog.remove();
         },
         buttons: buttons
      });
   }
   
   
   var dialogConfirm = cms.util.dialogConfirm = function(text, title, yesLabel, noLabel, callback) {
      var $dlg = $('<div/>').text(text).appendTo('body');
      var buttons = {};
      buttons[noLabel] = function() {
         $dlg.dialog('destroy').remove();
         callback(false);
      }
      buttons[yesLabel] = function() {
         $dlg.dialog('destroy').remove();
         callback(true);
      }
      
      $dlg.dialog({
         autoOpen: true,
         zIndex: 9999,
         buttons: buttons,
         title: title,
         modal: true,
         close: function() {
            $dlg.dialog('destroy').remove();
            callback(false);
         }
      });
   }
   
   var dialogConfirmCancel = cms.util.dialogConfirmCancel = function(text, title, yesLabel, noLabel, cancelLabel, callback) {
      var $dlg = $('<div/>').text(text).appendTo('body');
      var buttons = {};
      buttons[cancelLabel] = function() {
         $dlg.dialog('destroy').remove();        
      }
      buttons[noLabel] = function() {
         $dlg.dialog('destroy').remove();
         callback(false);
      }
      buttons[yesLabel] = function() {
         $dlg.dialog('destroy').remove();
         callback(true);
      }
            
      $dlg.dialog({
         autoOpen: true,
         zIndex: 9999,
         buttons: buttons,
         title: title,
         modal: true,
         close: function() {
            $dlg.dialog('destroy').remove();            
         }
      });
   }
   
   
   var addToElementList = cms.util.addToElementList = function(list, item, maxLen) {
      var rId = getResourceId(item);
      var pos = findFirstWithPrefix(list, rId);
      if (pos != -1) {
         list.splice(pos, 1);
      }
      list.splice(0, 0, item);
      if (list.length > maxLen) {
         list.splice(maxLen, list.length - maxLen);
      }
   }
   
   var findFirstWithPrefix = function(list, prefix) {
      for (var i = 0; i < list.length; i++) {
         if (list[i].match('^' + prefix)) {
            return i;
         }
      }
      return -1;
   };
   
   var getResourceId = function(id) {
      var sepPos = id.indexOf('#');
      if (sepPos == -1) {
         return id;
      } else {
         return id.substring(0, sepPos);
      }
   }
   
   /**
    * Checks whether Firebug is active.
    */
   var isFirebugActive = cms.util.isFirebugActive = function() {
      return $('#_firebugConsole').size() > 0;
   }
   
   /**
    * Inserts other strings into a message string.
    * If you call this function with the parameters format(message, arg0, arg1,...,argN),
    * the occurrences of {i} in the message, where i is a number, will be replaced by arg_i
    */
   var format = cms.util.format = function() {
      var message = arguments[0];
      for (var i = 0; i < arguments.length - 1; i++) {
         message = message.replace('{' + i + '}', arguments[i + 1]);
      }
      return message;
   }
   
   
   
   /**
    * Converts a function and a list of parameters to a new function which, if called with other parameters,
    * will call the original function, with the parameter list obtained by concatenating the original parameter
    * list and the call's argument list.
    *
    * For example, bindFn(f, [x1, x2])(x3, x4) will call f(x1, x2, x3, x4).
    *
    * The 'this' parameter is passed through to the original function.
    *
    * @param {Object} fn the function to bind parameters to
    * @param {Object} args the list of initial parameters that should be bound
    */
   var bindFn = cms.util.bindFn = function(fn, args) {
      return function() {
         return fn.apply(this, args.concat(arguments));
      };
   }
   
   /**
    * Convert an event callback expecting its argument in the "this" variable to a function taking a normal parameter
    * @param {Object} fn the function to convert
    */
   var targetToParam = cms.util.targetToParam = function(fn) {
      return function() {
         return fn.apply(this, [this].concat(arguments));
      }
   }
   
   
   /**
    * Converts an array of strings to an object which will have a property named x if x is in the array.
    * @param {Object} arr the array of strings
    */
   var stringArrayToObject = cms.util.stringArrayToObject = function(arr) {
      var result = {};
      $.each(arr, function() {
         result[this] = true;
      });
      return result;
   }
   
   /**
    * JQuery extension function that depending on the value of a boolean flag sets one of two css classes on the matched elements.
    * @param {Object} flag the boolean flag
    * @param {Object} classTrue if the flag is true, this class will be added, else it will be removed
    * @param {Object} classFalse if the flag is false, this class will be added, else it will be removed
    */
   $.fn.chooseClass = function(flag, classTrue, classFalse) {
      this.each(function() {
         if (flag) {
            $(this).addClass(classTrue).removeClass(classFalse);
         } else {
            $(this).addClass(classFalse).removeClass(classTrue);
         }
      })
   }
   
   /**
    * Replacement for the jQuery hover function which takes just one function with a boolean argument instead of two functions.
    * @param {Object} handler
    */
   $.fn.hoverBoolean = function(handler) {
      return this.hover(function() {
         handler.call(this, true)
      }, function() {
         handler.call(this, false);
      })
   }
   
   /**
    * Shows the element passed as a parameter when hovering over one of the elements in the jQuery object, else hides it.
    */
   $.fn.hoverSetVisible = function($elem) {
      return this.hover(function() {
         $elem.show();
      }, function() {
         $elem.hide();
      });
   }
   
   $.fn.addData = function(key, value) {
      return this.each(function() {
         $(this).data(key, value).addClass('has-data-' + key);
      });
   }
   
   
   /**
    * Function which returns the matched element's associated data, and sets it to an empty array if there isn't already
    * associated data.
    *
    * @param {Object} key
    */
   $.fn.dataList = function(key) {
      var data = this.data(key);
      if (!data) {
         data = [];
         this.data(key, data);
      }
      return data;
   }
   
   /**
    * Installs a generic jQuery live event handler which takes the real handler from the event target's associated data.
    * @param {Object} eventType the event type string
    * @param {Object} key the key which should be used to retrieve the real event handler(s).
    */
   $.fn.liveData = function(eventType, key) {
      this.live(eventType, function() {
         var data = $(this).data(key);
         if (typeof data == 'function') {
            return fn.call(this);
         } else if (typeof data == 'object' && data.constructor == Array) {
            var b = true;
            for (var i = 0; i < data.length; i++) {
               b = data[i].call(this);
               if (!b) {
                  return false;
               }
            }
            return b;
         }
      });
   }
   
   /**
    * Converts a function to a "deferred" function that executes the original function as a timeout.
    *
    * This can be used to create an event handler that is executed after other live event handlers.
    * @param {Object} fn the function to convert
    */
   var defer = cms.util.defer = function(fn) {
      return function() {
         var original = this;
         window.setTimeout(function() {
            fn.call(original)
         }, 0);
      }
   }
   cms.util.checkboxClassName = 'cms-checkbox';
   cms.util.checkboxDataName = 'cms-checkbox';
   $(function() {
      $('.' + cms.util.checkboxClassName).live('click', function() {
         var checkbox = $(this).data(cms.util.checkboxDataName);
         checkbox.setCheckedIfEnabled(!checkbox.checked);
      });
      $('.' + cms.util.checkboxClassName).live('mouseover', function() {
         $(this).chooseClass(true, 'cms-checkbox-hover', 'cms-checkbox-nohover');
      });
      $('.' + cms.util.checkboxClassName).live('mouseout', function() {
         $(this).chooseClass(false, 'cms-checkbox-hover', 'cms-checkbox-nohover');
      });
      
   });
   
   /**
    * Checkbox constructor function.
    * @param {Object} $dom the DOM element to use for the checkbox
    */
   var Checkbox = cms.util.Checkbox = function($dom) {
      if (!$dom) {
         $dom = $('<div/>');
      }
      var self = this;
      self.$dom = $dom.addClass(cms.util.checkboxClassName);
      self.$dom.data(cms.util.checkboxDataName, self);
      self.setChecked(false);
      self.setEnabled(true);
   }
   
   Checkbox.fromJQuery = function($jq) {
      return $jq.data(cms.util.checkboxDataName);
   }
   
   /**
    * Helper function for getting the checkbox objects from a set of DOM elements
    * @param {Object} $dom the DOM elements representing the checkboxes
    */
   var _getCheckboxes = function($dom) {
      var result = [];
      $dom.each(function() {
         result.push($.data(this, cms.util.checkboxDataName));
      })
      return result;
   }
   
   /**
    * Gets all checkbox objects from a given jQuery context
    * @param {Object} context the jQuery context
    */
   Checkbox.getCheckboxes = function(context) {
      return _getCheckboxes($('.' + cms.util.checkboxClassName, context));
   }
   
   /**
    * Gets all unchecked checkbox objects from a given jQuery context
    * @param {Object} context the jQuery context
    */
   Checkbox.getUncheckedCheckboxes = function(context) {
      return _getCheckboxes($('.cms-checkbox-unchecked', context));
   }
   
   
   
   
   
   
   Checkbox.prototype = {
      /**
       * Enables or disables the checkbox.
       * @param {Object} enabled if true, the checkbox will be enabled, else disabled
       */
      setEnabled: function(enabled) {
         this.enabled = enabled;
         this.$dom.chooseClass(enabled, 'cms-checkbox-enabled', 'cms-checkbox-disabled');
      },
      
      /**
       * Returns true if the checkbox is enabled.
       */
      getEnabled: function() {
         return this.enabled;
      },
      
      /**
       * Checks or unchecks the checkbox
       * @param {Object} checked if true, the checkbox will be checked, else unchecked
       */
      setChecked: function(checked) {
         this.checked = checked;
         this.$dom.chooseClass(checked, 'cms-checkbox-checked', 'cms-checkbox-unchecked');
      },
      
      /**
       * Check or uncheck the checkbox, but only if it is enabled
       * @param {Object} checked
       */
      setCheckedIfEnabled: function(checked) {
         if (this.enabled) {
            this.setChecked(checked);
         }
      },
      
      /**
       * Returns true if the checkbox is checked.
       */
      getChecked: function() {
         return this.checked;
      },
      
      addClickHandler: function(handler) {
         this.$dom.dataList('click').push(handler);
      }
   }
   
})(cms);
