/*
 * Tapestry Web Application Framework
 * Copyright (c) 2001 by Howard Ship and Primix
 *
 * Primix
 * 311 Arsenal Street
 * Watertown, MA 02472
 * http://www.primix.com
 * mailto:hship@primix.com
 * 
 * This library is free software.
 * 
 * You may redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation.
 *
 * Version 2.1 of the license should be included with this distribution in
 * the file LICENSE, as well as License.html. If the license is not
 * included with this distribution, you may find a copy at the FSF web
 * site at 'www.gnu.org' or 'www.fsf.org', or you may write to the
 * Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139 USA.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 */

package com.primix.tapestry.bean;

import com.primix.tapestry.*;
import com.primix.tapestry.spec.*;
import java.util.*;
import com.primix.tapestry.util.pool.*;
import org.apache.log4j.*;

/**
 *  Basic implementation of the {@link IBeanProvider} interface.
 *
 *  @author Howard Ship
 *  @version $Id$
 *  @since 1.0.4
 */


public class BeanProvider implements IBeanProvider
{
	private static final Category CAT = Category.getInstance(BeanProvider.class);
	
	private boolean registered = false;
	private IComponent component;
	private IResourceResolver resolver;
	private Pool helperBeanPool;
	
	/**
	 *  Map of beans, keyed on name.
	 *
	 */
	
	private Map beans;
	
	public BeanProvider(IComponent component)
	{
		this.component = component;
		IEngine engine = component.getPage().getEngine();
		resolver = engine.getResourceResolver();
		helperBeanPool = engine.getHelperBeanPool();
		
		if (CAT.isDebugEnabled())
			CAT.debug("Created BeanProvider for " + component);
		
	}
	
	public Object getBean(String name)
	{
		Object bean = null;
		
		if (beans != null)
			bean = beans.get(name);
		
		if (bean != null)
			return bean;
		
		// Doesn't exist, so let's create it.
		
		BeanSpecification spec = component.getSpecification().getBeanSpecification(name);
		
		if (spec == null)
			throw new ApplicationRuntimeException(
				"Component " + component.getExtendedId() + " does not define a bean named " + name + ".");
		
		bean = instantiateBean(spec);
		
		BeanLifecycle lifecycle = spec.getLifecycle();
		
		if (lifecycle == BeanLifecycle.NONE)
			return bean;
		
		if (beans == null)
			beans = new HashMap();
		
		beans.put(name, bean);
		
		// The first time in a request that a REQUEST lifecycle bean is created,
		// register with the page to be notified at the end of the
		// request cycle.
		
		if (lifecycle == BeanLifecycle.REQUEST && !registered)
		{
			component.getPage().registerBeanProvider(this);
			registered = true;
		}
		
		// No need to register if a PAGE lifecycle bean; those can stick around
		// forever.
		
		return bean;
	}
	
	private Object instantiateBean(BeanSpecification spec)
	{
		String className = spec.getClassName();
		
		// See if there's one in the pool.
		
		Object bean = helperBeanPool.retrieve(className);
		
		if (bean != null)
		{
			if (CAT.isDebugEnabled())
				CAT.debug("Obtained " + bean + " from pool.");
		
			return bean;
		}
		
		if (CAT.isDebugEnabled())
			CAT.debug("Instantiating instance of " + className);
		
		// Do it the hard way!
		
		Class beanClass = resolver.findClass(className);
		
		try
		{
			return beanClass.newInstance();
		}
		catch (IllegalAccessException ex)
		{
			throw new ApplicationRuntimeException(ex);
		}
		catch (InstantiationException ex)
		{
			throw new ApplicationRuntimeException(ex);
		}
	}
	
	public void removeRequestBeans()
	{
		registered = false;
		ComponentSpecification spec = null;
		
		if (beans == null)
			return;
		
		Iterator i = beans.entrySet().iterator();
		while (i.hasNext())
		{
			Map.Entry e = (Map.Entry)i.next();
			String name = (String)e.getKey();
			
			if (spec == null)
				spec = component.getSpecification();
			
			BeanSpecification s = spec.getBeanSpecification(name);
			
			// If the bean has the REQUEST lifecycle, then remove
			// the key and bean from the map of beans.  Beans with
			// other lifecycles (i.e., page)
			// will stick around.
			
			if (s.getLifecycle() == BeanLifecycle.REQUEST)
			{
				Object bean = e.getValue();
				
				if (CAT.isDebugEnabled())
					CAT.debug("Removing REQUEST bean " + name + ": " + bean);
				
				i.remove();
				
				if (bean instanceof IPoolable)
					helperBeanPool.store(s.getClassName(), bean);
			}
		}
	}
}

