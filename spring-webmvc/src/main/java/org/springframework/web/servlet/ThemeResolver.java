/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.lang.Nullable;

/**
 * Interface for web-based theme resolution strategies that allows for
 * both theme resolution via the request and theme modification via
 * request and response.
 *
 * <p>This interface allows for implementations based on session,
 * cookies, etc. The default implementation is
 * {@link org.springframework.web.servlet.theme.FixedThemeResolver},
 * simply using a configured default theme.
 *
 * <p>Note that this resolver is only responsible for determining the
 * current theme name. The Theme instance for the resolved theme name
 * gets looked up by DispatcherServlet via the respective ThemeSource,
 * i.e. the current WebApplicationContext.
 *
 * <p>Use {@link org.springframework.web.servlet.support.RequestContext#getTheme()}
 * to retrieve the current theme in controllers or views, independent
 * of the actual resolution strategy.
 *
 * @author Jean-Pierre Pawlak
 * @author Juergen Hoeller
 * @since 17.06.2003
 * @see org.springframework.ui.context.Theme
 * @see org.springframework.ui.context.ThemeSource
 */
// ThemeResolver从名字就可以看出是解析主题用的
/*
   以前使用电脑的时候可能很多人都没注意过“主题”，不过随着智能手机的普及，主题已经成了一个不需要过多解释的名词。
   不同的主题其实就是换了一套图片、显示效果以及样式等。Spring MVC中一套主题对应一个properties文件，里面存放着跟当前主题相关的所有资源，如图片、css样式表的路径等
 */
/*
   另外，Spring MVC的主题也支持国际化，也就是说同一个主题不同的区域也可以显示不同的风格，比如，可以定义以下主题文件 :
   theme.properties
   theme_zh_CN.properties
   theme_en_US.properties
   这样即使同样使用theme的主题，不同的区域也会调用不同主题文件里的资源进行显示。
 */
/*
   ThemeResolver的作用是从request解析出主题名；ThemeSource则是根据主题名找到具体的主题；Theme是ThemeSource找出的一个具体的主题，可以通过它获取主题里具体的资源。
   ThemeResolver默认使用的是FixedThemeResolver，ThemeSource默认使用的是WebApplicationContext（这个类实现了ThemeSource接口，其实现方式是在内部封装了一个ThemeSource属性，然后将具体工作交给它去干。）
 */
public interface ThemeResolver {

	/**
	 * Resolve the current theme name via the given request.
	 * Should return a default theme as fallback in any case.
	 * @param request request to be used for resolution
	 * @return the current theme name
	 */
	String resolveThemeName(HttpServletRequest request);

	/**
	 * Set the current theme name to the given one.
	 * @param request request to be used for theme name modification
	 * @param response response to be used for theme name modification
	 * @param themeName the new theme name ({@code null} or empty to reset it)
	 * @throws UnsupportedOperationException if the ThemeResolver implementation
	 * does not support dynamic changing of the theme
	 */
	void setThemeName(HttpServletRequest request, @Nullable HttpServletResponse response, @Nullable String themeName);

}
