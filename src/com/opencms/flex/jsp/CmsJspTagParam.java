/*
 * File   : $Source: /alkacon/cvs/opencms/src/com/opencms/flex/jsp/Attic/CmsJspTagParam.java,v $
 * Date   : $Date: 2002/12/16 13:20:36 $
 * Version: $Revision: 1.1 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * Copyright (C) 2002  The OpenCms Group
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * For further information about OpenCms, please see the
 * OpenCms Website: http://www.opencms.org
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * 
 * This file is based on:
 * org.apache.taglibs.standard.tag.common.core.ParamSupport
 * from the Apache JSTL 1.0 implmentation.
 * 
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights 
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer. 
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:  
 *       "This product includes software developed by the 
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written 
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */ 

package com.opencms.flex.jsp;

import com.opencms.core.A_OpenCms;
import com.opencms.util.Encoder;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.BodyTagSupport;
import javax.servlet.jsp.tagext.Tag;

/**
 * A handler for &lt;param&gt; that accepts attributes as Strings
 * and evaluates them as expressions at runtime.<p>
 *
 * @author Shawn Bayern
 */

public class CmsJspTagParam extends BodyTagSupport {

    protected String name;        
    protected String value;

    /**
     * There used to be an 'encode' attribute; I've left this as a
     * vestige in case custom subclasses want to use our functionality
     * but NOT encode parameters.
     */
    protected boolean encode = false;
    
	public CmsJspTagParam() {
		super();
		init();
	}

	private void init() {
		name = value = null;
	}

	// for tag attribute
	public void setName(String name) throws JspTagException {
		this.name = name;
	}

	// for tag attribute
	public void setValue(String value) throws JspTagException {
		this.value = value;
	}

	// simply send our name and value to our appropriate ancestor
	public int doEndTag() throws JspException {
		Tag t = findAncestorWithClass(this, I_CmsJspTagParamParent.class);
		if (t == null)
			throw new JspTagException("Parameter Tag <param> without parent found!");

		// take no action for null or empty names
		if (name == null || name.equals(""))
			return EVAL_PAGE;

		// send the parameter to the appropriate ancestor
		I_CmsJspTagParamParent parent = (I_CmsJspTagParamParent) t;
		String value = this.value;
		if (value == null) {
			if (bodyContent == null || bodyContent.getString() == null)
				value = "";
			else
				value = bodyContent.getString().trim();
		}
		if (encode) {
			parent.addParameter(
				Encoder.encode(name, A_OpenCms.getDefaultEncoding(), true),
				Encoder.encode(value, A_OpenCms.getDefaultEncoding(), true));
		} else
			parent.addParameter(name, value);

		return EVAL_PAGE;
	}

    // Releases any resources we may have (or inherit)
    public void release() {
    	init();
    }

}
