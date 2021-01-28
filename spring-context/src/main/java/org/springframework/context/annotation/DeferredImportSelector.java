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

package org.springframework.context.annotation;

import java.util.Objects;

import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.Nullable;

/**
 * A variation of {@link ImportSelector} that runs after all {@code @Configuration} beans
 * have been processed. This type of selector can be particularly useful when the selected
 * imports are {@code @Conditional}.
 *
 * <p>Implementations can also extend the {@link org.springframework.core.Ordered}
 * interface or use the {@link org.springframework.core.annotation.Order} annotation to
 * indicate a precedence against other {@link DeferredImportSelector}s.
 *
 * <p>Implementations may also provide an {@link #getImportGroup() imports group} which
 * can provide additional sorting and filtering logic across different selectors.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 4.0
 */
// 1. DeferredImportSelector是ImportSelector的一个扩展；
// 2. ImportSelector实例的selectImports方法的执行时机，是在@Configguration注解中的其他逻辑被处理之前，所谓的其他逻辑，包括对@ImportResource、@Bean这些注解的处理（注意，这里只是对@Bean修饰的方法的处理，并不是立即调用@Bean修饰的方法，这个区别很重要！）；=====processImports()方法处理
// 3. DeferredImportSelector实例的selectImports方法的执行时机，是在@Configguration注解中的其他逻辑被处理完毕之后，所谓的其他逻辑，包括对@ImportResource、@Bean这些注解的处理；=====processDeferredImportSelectors()方法处理
// 4. DeferredImportSelector的实现类可以用Order注解，或者实现Ordered接口来对selectImports的执行顺序排序；
public interface DeferredImportSelector extends ImportSelector {

	/**
	 * Return a specific imports group or {@code null} if no grouping is required.
	 * @return the imports group class or {@code null}
	 */
	@Nullable
	default Class<? extends Group> getImportGroup() {
		return null;
	}


	// spring 中只有一个Group实现类
    // ConfigurationClassParser.DefaultDeferredImportSelectorGroup.imports里面包含多个DeferredImportSelector.Group.Entry
    // 每一个DeferredImportSelector.Group.Entry都封装一个selectImports得到的全类名
    //	private static class DefaultDeferredImportSelectorGroup implements Group {
    //
    //		private final List<Entry> imports = new ArrayList<>();
    //
    //		@Override
    //      //selectImports得到的每一个全类名，被封装为DeferredImportSelector.Group.Entry
    //		public void process(AnnotationMetadata metadata, DeferredImportSelector selector) {
    //			for (String importClassName : selector.selectImports(metadata)) {
    //				this.imports.add(new Entry(metadata, importClassName));
    //			}
    //		}
    //
    //		@Override
    //      //可迭代接口Iterable里面定义了获取迭代器的方法
    //		public Iterable<Entry> selectImports() {
    //			return this.imports;
    //		}
    //	}
	/**
	 * Interface used to group results from different imports selectors.
	 */
	interface Group {

		/**
		 * Process the {@link AnnotationMetadata} of the importing @{@link Configuration}
		 * class using the specified {@link DeferredImportSelector}.
		 */
		void process(AnnotationMetadata metadata, DeferredImportSelector selector);

		/**
		 * Return the {@link Entry entries} of which class(es) should be imported for this
		 * group.
		 */
		//返回一个可迭代的类实现，迭代访问Entry类型的实例
		Iterable<Entry> selectImports();

		/**
		 * An entry that holds the {@link AnnotationMetadata} of the importing
		 * {@link Configuration} class and the class name to imports.
		 */
		class Entry {

			private final AnnotationMetadata metadata;

			private final String importClassName;

			public Entry(AnnotationMetadata metadata, String importClassName) {
				this.metadata = metadata;
				this.importClassName = importClassName;
			}

			/**
			 * Return the {@link AnnotationMetadata} of the importing
			 * {@link Configuration} class.
			 */
			public AnnotationMetadata getMetadata() {
				return this.metadata;
			}

			/**
			 * Return the fully qualified name of the class to imports.
			 */
			public String getImportClassName() {
				return this.importClassName;
			}

			@Override
			public boolean equals(Object o) {
				if (this == o) {
					return true;
				}
				if (o == null || getClass() != o.getClass()) {
					return false;
				}
				Entry entry = (Entry) o;
				return Objects.equals(this.metadata, entry.metadata) &&
						Objects.equals(this.importClassName, entry.importClassName);
			}

			@Override
			public int hashCode() {
				return Objects.hash(this.metadata, this.importClassName);
			}
		}
	}

}
