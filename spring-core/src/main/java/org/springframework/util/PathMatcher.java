/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.util;

import java.util.Comparator;
import java.util.Map;

/**
 * Strategy interface for {@code String}-based path matching.
 *
 * <p>Used by {@link org.springframework.core.io.support.PathMatchingResourcePatternResolver},
 * {@link org.springframework.web.servlet.handler.AbstractUrlHandlerMapping},
 * and {@link org.springframework.web.servlet.mvc.WebContentInterceptor}.
 *
 * <p>The default implementation is {@link AntPathMatcher}, supporting the
 * Ant-style pattern syntax.
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see AntPathMatcher
 */
// PathMatcher是Spring的一个概念模型接口，该接口抽象建模了概念"路径匹配器"，一个"路径匹配器"是一个用于路径匹配的工具。它的使用者是 :
// org.springframework.core.io.support.PathMatchingResourcePatternResolver
// org.springframework.web.servlet.handler.AbstractUrlHandlerMapping
// org.springframework.web.servlet.mvc.WebContentInterceptor
// Spring框架自身对概念模型接口也提供了一个缺省的实现AntPathMatcher,用于匹配Ant风格的路径。

// AntPathMatcher使用例子
// AntPathMatcher antPathMatcher = new AntPathMatcher();
// antPathMatcher.isPattern("/user/001");// 返回 false
// antPathMatcher.isPattern("/user/*"); // 返回 true
// antPathMatcher.match("/user/001","/user/001");// 返回 true
// antPathMatcher.match("/user/*","/user/001");// 返回 true
// antPathMatcher.matchStart("/user/*","/user/001"); // 返回 true
// antPathMatcher.matchStart("/user/*","/user"); // 返回 true
// antPathMatcher.matchStart("/user/*","/user001"); // 返回 false
// antPathMatcher.extractPathWithinPattern("uc/profile*","uc/profile.html"); // 返回 profile.html
// antPathMatcher.combine("uc/*.html","uc/profile.html"); // uc/profile.html
public interface PathMatcher {

	/**
	 * Does the given {@code path} represent a pattern that can be matched
	 * by an implementation of this interface?
	 * <p>If the return value is {@code false}, then the {@link #match}
	 * method does not have to be used because direct equality comparisons
	 * on the static path Strings will lead to the same result.
	 * @param path the path String to check
	 * @return {@code true} if the given {@code path} represents a pattern
	 */
	//	判断指定的路径 path 是否是一个 pattern(模式)
    //	如果返回值是 false，也就是说 path 不是一个模式，而是一个静态路径(真正的路径字符串),
    //	那么就不用调用方法 #match 了，因为对于静态路径的匹配，直接使用字符串等号比较就足够了
	boolean isPattern(String path);

	/**
	 * Match the given {@code path} against the given {@code pattern},
	 * according to this PathMatcher's matching strategy.
	 * @param pattern the pattern to match against
	 * @param path the path String to test
	 * @return {@code true} if the supplied {@code path} matched,
	 * {@code false} if it didn't
	 */
	//  根据当前 PathMatcher 的匹配策略，检查指定的路径 path 和指定的模式 pattern 是否匹配
    //	用于检测路径字符串是否匹配于某个模式时所用的模式
    //	@param path 需要被检测的路径字符串
    //	@return true 表示匹配， false 表示不匹配
	boolean match(String pattern, String path);

	/**
	 * Match the given {@code path} against the corresponding part of the given
	 * {@code pattern}, according to this PathMatcher's matching strategy.
	 * <p>Determines whether the pattern at least matches as far as the given base
	 * path goes, assuming that a full path may then match as well.
	 * @param pattern the pattern to match against
	 * @param path the path String to test
	 * @return {@code true} if the supplied {@code path} matched,
	 * {@code false} if it didn't
	 */
	// 根据当前 PathMatcher 的匹配策略，检查指定的路径 path 和指定的模式 pattern 是否之间是否为前缀匹配
    // true 表示匹配， false 表示不匹配
	boolean matchStart(String pattern, String path);

	/**
	 * Given a pattern and a full path, determine the pattern-mapped part.
	 * <p>This method is supposed to find out which part of the path is matched
	 * dynamically through an actual pattern, that is, it strips off a statically
	 * defined leading path from the given full path, returning only the actually
	 * pattern-matched part of the path.
	 * <p>For example: For "myroot/*.html" as pattern and "myroot/myfile.html"
	 * as full path, this method should return "myfile.html". The detailed
	 * determination rules are specified to this PathMatcher's matching strategy.
	 * <p>A simple implementation may return the given full path as-is in case
	 * of an actual pattern, and the empty String in case of the pattern not
	 * containing any dynamic parts (i.e. the {@code pattern} parameter being
	 * a static path that wouldn't qualify as an actual {@link #isPattern pattern}).
	 * A sophisticated implementation will differentiate between the static parts
	 * and the dynamic parts of the given path pattern.
	 * @param pattern the path pattern
	 * @param path the full path to introspect
	 * @return the pattern-mapped part of the given {@code path}
	 * (never {@code null})
	 */
	// 给定一个模式 pattern 和一个全路径 path，判断路径中和模式匹配的部分。
    // 该方法用于发现路径中的哪一部分是和模式能动态匹配上的部分。它会去除路径中开头静态部分，
    // 仅仅返回那部分真正和模式匹配的上的部分。
    // 例子 : "myroot/*.html" 为 pattern , "myroot/myfile.html" 为路径，
    // 则该方法返回 "myfile.html".
    // 具体的检测规则根据当前 PathMatcher 的匹配策略来顶。
	String extractPathWithinPattern(String pattern, String path);

	/**
	 * Given a pattern and a full path, extract the URI template variables. URI template
	 * variables are expressed through curly brackets ('{' and '}').
	 * <p>For example: For pattern "/hotels/{hotel}" and path "/hotels/1", this method will
	 * return a map containing "hotel"->"1".
	 * @param pattern the path pattern, possibly containing URI templates
	 * @param path the full path to extract template variables from
	 * @return a map, containing variable names as keys; variables values as values
	 */
	// 给定一个模式和一个路径，提取其中的 URI 模板变量信息。URI模板变量表达式格式为 "{variable}"
    // 例子 : pattern  为 "/hotels/{hotel}" ，路径为 "/hotels/1", 则该方法会返回一个 map ，内容为 : "hotel"->"1".
	Map<String, String> extractUriTemplateVariables(String pattern, String path);

	/**
	 * Given a full path, returns a {@link Comparator} suitable for sorting patterns
	 * in order of explicitness for that path.
	 * <p>The full algorithm used depends on the underlying implementation,
	 * but generally, the returned {@code Comparator} will
	 * {@linkplain java.util.List#sort(java.util.Comparator) sort}
	 * a list so that more specific patterns come before generic patterns.
	 * @param path the full path to use for comparison
	 * @return a comparator capable of sorting patterns in order of explicitness
	 */
	// 给定一个完整的路径，返回一个{@link Comparator}，该比较器适合于按照路径的明确性对patterns进行排序。
	Comparator<String> getPatternComparator(String path);

	/**
	 * Combines two patterns into a new pattern that is returned.
	 * <p>The full algorithm used for combining the two pattern depends on the underlying implementation.
	 * @param pattern1 the first pattern
	 * @param pattern2 the second pattern
	 * @return the combination of the two patterns
	 * @throws IllegalArgumentException when the two patterns cannot be combined
	 */
    // 将两个模式组合成一个返回的新模式。
    // <p>用于组合这两种模式的完整算法取决于底层实现。
	String combine(String pattern1, String pattern2);

}
