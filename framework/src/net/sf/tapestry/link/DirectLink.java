//
// Tapestry Web Application Framework
// Copyright (c) 2000-2002 by Howard Lewis Ship
//
// Howard Lewis Ship
// http://sf.net/projects/tapestry
// mailto:hship@users.sf.net
//
// This library is free software.
//
// You may redistribute it and/or modify it under the terms of the GNU
// Lesser General Public License as published by the Free Software Foundation.
//
// Version 2.1 of the license should be included with this distribution in
// the file LICENSE, as well as License.html. If the license is not
// included with this distribution, you may find a copy at the FSF web
// site at 'www.gnu.org' or 'www.fsf.org', or you may write to the
// Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139 USA.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied waranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//

package net.sf.tapestry.link;

import java.util.List;

import javax.servlet.http.HttpSession;

import net.sf.tapestry.BindingException;
import net.sf.tapestry.IActionListener;
import net.sf.tapestry.IBinding;
import net.sf.tapestry.IDirect;
import net.sf.tapestry.IEngineService;
import net.sf.tapestry.IRequestCycle;
import net.sf.tapestry.RequestCycleException;
import net.sf.tapestry.RequiredParameterException;
import net.sf.tapestry.StaleSessionException;
import net.sf.tapestry.Tapestry;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 *  A component for creating a link using the direct service; used for actions that
 *  are not dependant on dynamic page state.
 *
 *  [<a href="../../../../../ComponentReference/DirectLink.html">Component Reference</a>]
 *
 * @author Howard Lewis Ship
 * @version $Id$
 *
 **/

public class DirectLink extends GestureLink implements IDirect
{
    private static final Logger LOG = LogManager.getLogger(DirectLink.class);

    private IBinding _listenerBinding;
    private Object _parameters;
    private IBinding _statefulBinding;
    private boolean _warning = true;

    public void setStatefulBinding(IBinding value)
    {
        _statefulBinding = value;
    }

    public IBinding getStatefulBinding()
    {
        return _statefulBinding;
    }

    /**
     *  Returns true if the stateful parameter is bound to
     *  a true value.  If stateful is not bound, also returns
     *  the default, true.  May be invoked when not renderring.
     *
     **/

    public boolean isStateful()
    {
        if (_statefulBinding == null)
            return true;

        return _statefulBinding.getBoolean();
    }

    /**
     *  Returns {@link IEngineService#DIRECT_SERVICE}.
     *
     **/

    protected String getServiceName()
    {
        return IEngineService.DIRECT_SERVICE;
    }

    protected Object[] getServiceParameters(IRequestCycle cycle)
    {
        return constructServiceParameters(_parameters);
    }

    /**
     *  @deprecated To be removed in 2.3, use 
     *  {@link #constructServiceParameters(Object)}
     *  instead.
     * 
     **/

    public static Object[] constructContext(Object contextValue)
    {
        return constructServiceParameters(contextValue);
    }

    /**
     *  Converts a service parameters value to an array
     *  of objects.  
     *  This is used by the {@link DirectLink}, {@link ServiceLink}
     *  and {@link ExternalLink}
     *  components.
     *
     *  @param parameterValue the input value which may be
     *  <ul>
     *  <li>null  (returns null)
     *  <li>An array of Object (returns the array)
     *  <li>A {@link List} (returns an array of the values in the List})
     *  <li>A single object (returns the object as a single-element array)
     *  </ul>
     * 
     *  @return An array representation of the input object.
     * 
     *  @since 2.2
     **/

    public static Object[] constructServiceParameters(Object parameterValue)
    {
        if (parameterValue == null)
            return null;

        if (parameterValue instanceof Object[])
            return (Object[]) parameterValue;

        if (parameterValue instanceof List)
        {
            List list = (List) parameterValue;

            return list.toArray();
        }

        return new Object[] { parameterValue };
    }

    /**
     *  Invoked by the direct service to trigger the application-specific
     *  action by notifying the {@link IActionListener listener}.
     *
     *  @throws StaleSessionException if the component is stateful, and
     *  the session is new.
     * 
     **/

    public void trigger(IRequestCycle cycle) throws RequestCycleException
    {
        if (isStateful())
        {
            HttpSession session = cycle.getRequestContext().getSession();

            if (session == null || session.isNew())
                throw new StaleSessionException();
        }

        IActionListener listener = getListener(cycle);

        listener.actionTriggered(this, cycle);
    }

    public IBinding getListenerBinding()
    {
        return _listenerBinding;
    }

    public void setListenerBinding(IBinding value)
    {
        _listenerBinding = value;
    }

    /**
     *  Need to use the listener binding, since this method gets called even when the
     *  component is not rendering.
     * 
     **/

    private IActionListener getListener(IRequestCycle cycle) throws RequestCycleException
    {
        IActionListener result;

        try
        {
            result = (IActionListener) _listenerBinding.getObject("listener", IActionListener.class);

        }
        catch (BindingException ex)
        {
            throw new RequestCycleException(this, ex);
        }

        if (result == null)
            throw new RequiredParameterException(this, "listener", _listenerBinding);

        return result;
    }

    /** @since 2.2 **/

    public Object getParameters()
    {
        return _parameters;
    }

    /** @since 2.2. **/

    public void setParameters(Object context)
    {
        _parameters = context;
    }

    /**
     *  @deprecated To be removed in 2.3, use {@link #getParameters()}.
     * 
     **/

    public Object getContext()
    {
        return getParameters();
    }

    /**
     *  @deprecated To be removed in 2.3, use {@link #setParameters(Object)}.
     * 
     **/

    public void setContext(Object context)
    {
        if (_warning)
        {
            LOG.warn(Tapestry.getString("deprecated-component-param", getExtendedId(), "context", "parameters"));

            _warning = false;
        }

        setParameters(context);
    }
}