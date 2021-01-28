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

package org.springframework.web.servlet.mvc.method.annotation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.MatrixVariable;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.annotation.AbstractNamedValueMethodArgumentResolver;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Resolves arguments annotated with {@link MatrixVariable @MatrixVariable}.
 *
 * <p>If the method parameter is of type {@link Map} it will by resolved by
 * {@link MatrixVariableMapMethodArgumentResolver} instead unless the annotation
 * specifies a name in which case it is considered to be a single attribute of
 * type map (vs multiple attributes collected in a map).
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 3.2
 */
//处理Controller中action方法参数带有@MatrixVariable注解的参数，比@PathVariable要强大一些，从请求的url路径中(路径中，不包括参数)解析参数值

//   在Spring3.2 后，一个@MatrixVariable出现了，这个注解的出现拓展了URL请求地址的功能。
//   Matrix Variable中：
//   多个变量可以使用“;”（分号）分隔，例如：
//   /cars;color=red;year=2012
//   如果是一个变量的多个值那么可以使用“,”（逗号）分隔
//   color=red,green,blue
//   或者可以使用重复的变量名：
//   color=red;color=green;color=blue
//
//   下面来一个例子说明：
//   //请求URL: GET /pets/42;q=11;r=22
//   @RequestMapping(value = "/pets/{petId}", method = RequestMethod.GET)
//      public voidfindPet(@PathVariableString petId, @MatrixVariable int q) {
//      // petId == 42
//      // q == 11
//   }
//   再复杂一点就是这个例子：
//   // GET /owners/42;q=11/pets/21;q=22
//   @RequestMapping(value = "/owners/{ownerId}/pets/{petId}", method = RequestMethod.GET)
//   public voidfindPet(
//      @MatrixVariable(value="q", pathVar="ownerId") int q1,
//      @MatrixVariable(value="q", pathVar="petId") int q2) {
//      // q1 == 11
//      // q2 == 22
//   }
//   针对每一个Parh Variable绑定一个Matrix Variable，然后使用 value 和 pathVar属性就能找到该值。
//   另外，正对Matrix Variable也是可以指定自身的的属性，例如，是否必须，默认值。
//   下面这个例子说明：
//   // GET /pets/42
//   @RequestMapping(value = "/pets/{petId}", method = RequestMethod.GET)
//   public voidfindPet(@MatrixVariable(required=true, defaultValue="1") int q) {
//      // q == 1
//   }
//   最后说明一下，如果要开启Matrix Variable功能的话，必须设置 RequestMappingHandlerMapping 中的 removeSemicolonContent 为false.
//   一般情况不用你手动去设置这个属性，因为这个属性默认就是false ，如果你碰见Matrix Variable功能未开启的时候就可以看看是不是误设置这个属性为true了。
public class MatrixVariableMethodArgumentResolver extends AbstractNamedValueMethodArgumentResolver {

	public MatrixVariableMethodArgumentResolver() {
		super(null);
	}


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		if (!parameter.hasParameterAnnotation(MatrixVariable.class)) {
			return false;
		}
		if (Map.class.isAssignableFrom(parameter.nestedIfOptional().getNestedParameterType())) {
			MatrixVariable matrixVariable = parameter.getParameterAnnotation(MatrixVariable.class);
			return (matrixVariable != null && StringUtils.hasText(matrixVariable.name()));
		}
		return true;
	}

	@Override
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		MatrixVariable ann = parameter.getParameterAnnotation(MatrixVariable.class);
		Assert.state(ann != null, "No MatrixVariable annotation");
		return new MatrixVariableNamedValueInfo(ann);
	}

	@Override
	@SuppressWarnings("unchecked")
	@Nullable
	protected Object resolveName(String name, MethodParameter parameter, NativeWebRequest request) throws Exception {
		Map<String, MultiValueMap<String, String>> pathParameters = (Map<String, MultiValueMap<String, String>>)
				request.getAttribute(HandlerMapping.MATRIX_VARIABLES_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
		if (CollectionUtils.isEmpty(pathParameters)) {
			return null;
		}

		MatrixVariable ann = parameter.getParameterAnnotation(MatrixVariable.class);
		Assert.state(ann != null, "No MatrixVariable annotation");
		String pathVar = ann.pathVar();
		List<String> paramValues = null;

		if (!pathVar.equals(ValueConstants.DEFAULT_NONE)) {
			if (pathParameters.containsKey(pathVar)) {
				paramValues = pathParameters.get(pathVar).get(name);
			}
		}
		else {
			boolean found = false;
			paramValues = new ArrayList<>();
			for (MultiValueMap<String, String> params : pathParameters.values()) {
				if (params.containsKey(name)) {
					if (found) {
						String paramType = parameter.getNestedParameterType().getName();
						throw new ServletRequestBindingException(
								"Found more than one match for URI path parameter '" + name +
								"' for parameter type [" + paramType + "]. Use 'pathVar' attribute to disambiguate.");
					}
					paramValues.addAll(params.get(name));
					found = true;
				}
			}
		}

		if (CollectionUtils.isEmpty(paramValues)) {
			return null;
		}
		else if (paramValues.size() == 1) {
			return paramValues.get(0);
		}
		else {
			return paramValues;
		}
	}

	@Override
	protected void handleMissingValue(String name, MethodParameter parameter) throws ServletRequestBindingException {
		throw new ServletRequestBindingException("Missing matrix variable '" + name +
				"' for method parameter of type " + parameter.getNestedParameterType().getSimpleName());
	}


	private static class MatrixVariableNamedValueInfo extends NamedValueInfo {

		private MatrixVariableNamedValueInfo(MatrixVariable annotation) {
			super(annotation.name(), annotation.required(), annotation.defaultValue());
		}
	}

}
