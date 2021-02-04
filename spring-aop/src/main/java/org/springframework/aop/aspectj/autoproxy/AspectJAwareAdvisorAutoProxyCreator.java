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

package org.springframework.aop.aspectj.autoproxy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.aopalliance.aop.Advice;
import org.aspectj.util.PartialOrder;
import org.aspectj.util.PartialOrder.PartialComparable;

import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AbstractAspectJAdvice;
import org.springframework.aop.aspectj.AspectJPointcutAdvisor;
import org.springframework.aop.aspectj.AspectJProxyUtils;
import org.springframework.aop.framework.autoproxy.AbstractAdvisorAutoProxyCreator;
import org.springframework.aop.interceptor.ExposeInvocationInterceptor;
import org.springframework.core.Ordered;
import org.springframework.util.ClassUtils;

/**
 * {@link org.springframework.aop.framework.autoproxy.AbstractAdvisorAutoProxyCreator}
 * subclass that exposes AspectJ's invocation context and understands AspectJ's rules
 * for advice precedence when multiple pieces of advice come from the same aspect.
 *
 * @author Adrian Colyer
 * @author Juergen Hoeller
 * @author Ramnivas Laddad
 * @since 2.0
 */
@SuppressWarnings("serial")
// 主要为AspectJ切面服务。具体的针对如何创建代理，是在父类中(AbstractAutoProxyCreator#createProxy)实现
// 这个类可以为Xml定义的AspectJ切面和@AspectJ定义的切面服务
// 使用@AspectJ注解的话就使用其子类AnnotationAwareAspectJAutoProxyCreator
public class AspectJAwareAdvisorAutoProxyCreator extends AbstractAdvisorAutoProxyCreator {

    //比较Advisor切面的顺序
	private static final Comparator<Advisor> DEFAULT_PRECEDENCE_COMPARATOR = new AspectJPrecedenceComparator();


	/**
	 * Sort the rest by AspectJ precedence. If two pieces of advice have
	 * come from the same aspect they will have the same order.
	 * Advice from the same aspect is then further ordered according to the
	 * following rules:
	 * <ul>
	 * <li>if either of the pair is after advice, then the advice declared
	 * last gets highest precedence (runs last)</li>
	 * <li>otherwise the advice declared first gets highest precedence (runs first)</li>
	 * </ul>
	 * <p><b>Important:</b> Advisors are sorted in precedence order, from highest
	 * precedence to lowest. "On the way in" to a join point, the highest precedence
	 * advisor should run first. "On the way out" of a join point, the highest precedence
	 * advisor should run last.
	 */
	@Override
	@SuppressWarnings("unchecked")
    //对传进来的List<Advisor> advisors进行排序，偏序排序，就是集合中有的元素是没有顺序关系的，有些元素是有顺序关系的
    //使用PartialOrder和PartiallyComparableAdvisorHolder对传进来的List<Advisor> advisors进行偏序排序
	protected List<Advisor> sortAdvisors(List<Advisor> advisors) {
		List<PartiallyComparableAdvisorHolder> partiallyComparableAdvisors = new ArrayList<>(advisors.size());
		for (Advisor element : advisors) {
			partiallyComparableAdvisors.add(
					new PartiallyComparableAdvisorHolder(element, DEFAULT_PRECEDENCE_COMPARATOR));
		}
		List<PartiallyComparableAdvisorHolder> sorted = PartialOrder.sort(partiallyComparableAdvisors);
		if (sorted != null) {
			List<Advisor> result = new ArrayList<>(advisors.size());
			for (PartiallyComparableAdvisorHolder pcAdvisor : sorted) {
				result.add(pcAdvisor.getAdvisor());
			}
			return result;
		}
		else {
			return super.sortAdvisors(advisors);
		}
	}

	/**
	 * Adds an {@link ExposeInvocationInterceptor} to the beginning of the advice chain.
	 * These additional advices are needed when using AspectJ expression pointcuts and when using AspectJ-style advice.
	 */
	@Override
    // 增加ExposeInvocationInterceptor这个Advice到advice chain的首部
    // 当使用AspectJ表达式切入点和使用AspectJ风格的通知时，需要这个额外的Advice
	protected void extendAdvisors(List<Advisor> candidateAdvisors) {
		AspectJProxyUtils.makeAdvisorChainAspectJCapableIfNecessary(candidateAdvisors);
	}

	@Override
    //判断一个bean是否应该跳过，不创建代理
	protected boolean shouldSkip(Class<?> beanClass, String beanName) {
		// TODO: Consider optimization by caching the list of the aspect names
        //得到所有的切面Advisor
		List<Advisor> candidateAdvisors = findCandidateAdvisors();
		for (Advisor advisor : candidateAdvisors) {
			if (advisor instanceof AspectJPointcutAdvisor &&
					((AspectJPointcutAdvisor) advisor).getAspectName().equals(beanName)) {
				return true;
			}
		}
		//super.shouldSkip(beanClass, beanName)直接返回false
		return super.shouldSkip(beanClass, beanName);
	}


	/**
	 * Implements AspectJ PartialComparable interface for defining partial orderings.
	 */
	// 实现偏序排序
	private static class PartiallyComparableAdvisorHolder implements PartialComparable {

		private final Advisor advisor;

		private final Comparator<Advisor> comparator;

		public PartiallyComparableAdvisorHolder(Advisor advisor, Comparator<Advisor> comparator) {
			this.advisor = advisor;
			this.comparator = comparator;
		}

		@Override
		public int compareTo(Object obj) {
			Advisor otherAdvisor = ((PartiallyComparableAdvisorHolder) obj).advisor;
			return this.comparator.compare(this.advisor, otherAdvisor);
		}

		@Override
		public int fallbackCompareTo(Object obj) {
			return 0;
		}

		public Advisor getAdvisor() {
			return this.advisor;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			Advice advice = this.advisor.getAdvice();
			sb.append(ClassUtils.getShortName(advice.getClass()));
			sb.append(": ");
			if (this.advisor instanceof Ordered) {
				sb.append("order ").append(((Ordered) this.advisor).getOrder()).append(", ");
			}
			if (advice instanceof AbstractAspectJAdvice) {
				AbstractAspectJAdvice ajAdvice = (AbstractAspectJAdvice) advice;
				sb.append(ajAdvice.getAspectName());
				sb.append(", declaration order ");
				sb.append(ajAdvice.getDeclarationOrder());
			}
			return sb.toString();
		}
	}

}

/**
 * 关系
 * "关系"是数字中的一个很基本的概念，站在集合的角度来看关系是怎么样的呢？
 * 假设集合A={a,b,c,d,e,f}，集合A中的元素表示6个人。其中a是b和c的爸爸，b是d的爸爸，c是e和f的爸爸。
 * 那么集合A中的元素中符合父子关系的两个人可以用有序偶(a,b)、(a,c)、(b,d)、(c,e)、(c,f)表示。那么集合R={(a,b)、(a,c)、(b,d)、(c,e)、(c,f)}可以完整地描述出集合A中元素的父子管子，我们称R为集合A上的一个关系。
 * 再举个栗子，集合A={1,2,3}上的小于关系可以表示为R={(1,2)、(1,3)、(2,3)}
 * 现在我们把集合的个数扩充到两个
 * 有两个集合A={a,b,c,d}，B={x,y,z}。其中A表示a,b,c,d四位教师，x,y,z表示语文、数学和英语三门课程。a和b是语文老师、c是数学老师、d是英语老师。那么集合R={(a,x),(b,x),(c,y),(d,z)}表示教师和其所授课程之间的关系。
 *
 * 关系的性质
 * 自反性：R是A上的二元关系，如果对于A中每一个元素x，都有(x,x)属于R，则称R是自反的，也称R具有自反性。
 * 例1:A={a,b,c},A上的二元关系R={(a,a),(b,b),(c,c),(a,c),(c,b)}，则R是自反的二元关系。
 * 例2:设A={1,2,3,4},A上的二元关系R={(1,1),(2,2),(3,4),(4,2)},因为3∈A,但(3,3),所以R不是A上的自反关系.
 *
 * 反自反性：R是A上的二元关系，如果对于A中每一个元素x,都有(x,x)不属于R,则称R是反自反的，也称R具有反自反性。
 * 例1:设A={a,b,c},R={(a,c),(b,a),(b,c),(b,b)}。因为(b,b)属于R,则R不是A上的反自反关系。
 * 例2:设A={1，2，3}，R是A上的小于关系，即Ｒ＝｛(1,2),(1,3),(2,3)}。由于(1,1),(2,2),(3,3)都不属于R，所以R是A上的反自反关系。
 *
 * 对称性：R是A上的二元关系，每当(x,y)属于R时，就一定有(y,x)属于R，则称R是对称的，也称R具有对称性。
 * 例1:设A={a,b,c},R={(a,b),(b,a),(a,c),(c,a)}，所以R是对称的。
 * 例2:设A={1,2,3,4}上的二元关系R={(1,1),(1,2),(2,1),(3.3),(4,3),(4,4)},因为(4,3)属于R但(3,4)不属于。所以R不是对称的。
 *
 * 反对称性：定义R是A上的二元关系，当x≠y时，如果(x,y)属于R，则必有(y,x)不属于R，称R是反对称的，也称R具有反对称性。
 * 例1:A={1,2,3}上的关系R={(1,2),(2,2),(3,1)},则R是反对称的。但S={(1,2),(1,3),(2,2),(3,1)}不是反对称的.因为1≠3但(1,3)属于S,且(3,1)属于S。
 *
 * 对称关系和反对称关系不是两个相互否定的概念.
 * 存在既是对称的也是反对称的二元关系,也存在既不是对称的也不是反对称的二元关系。
 * 设A={a,b,c,d}上的关系R={(a,a),(b,c),(c,d),(d,c)},S={(a,a),(b,b),(d,d)}，
 * 则R既不是对称的(因为(b,c)∈R但(c,b)),也不是反对称的(因为(c,d)∈R且(d,c)∈R)
 * 而S既是对称的，也是反对称的。
 *
 * 可传递性：设R是A上的二元关系，每当(x,y)属于R且(y,z)属于R时，必有(x,z)属于R，则称R是可传递的，也称R具有可传递性。
 * 在重新认识了"关系"之后，来看看两种关系，"偏序关系"和"全序关系"。
 *
 * 偏序关系：设R是非空集合A上的关系，如果R是自反的，反对称的，和可传递的，则称R是A上的偏序关系。
 * 常常把集合A以及A上的偏序关系R合在一起统称为偏序集，记作（A，R）。
 * 由于偏序关系是自反的、反对称的、传递的二元关系，所以一般用符号"≤"表示偏序。
 *
 * 这里的符号"≤"可不要当做是小于等于哦。为了避免与≤（小于等于号）相混，后续表示偏序关系时，会加上双引号。即"≤"表示偏序。
 *
 * 偏序关系下，当(a, b)∈R，时，常记作a"≤"b，偏序集也常记作（A，"≤"）。
 * 比如小于等于关系,、字典顺序、整除关系和包含关系都是相应集合的偏序关系。
 *
 * 与偏序相关的一个概念是全序。
 *
 * 全序关系：如果R是A上的偏序关系，那么对于任意的A集合上的 x,y，都有 x"≤"y，或者 y"≤"x，二者必居其一，那么则称R是A上的全序关系。全序关系中，任意两个元素都是有关系的。全序也叫做线性次序。
 * 比如在整数集合中，大于等于关系是线性序关系。
 * 偏序关系是自反的，反对称的，和可传递的，这种关系里面可能存在2个元素没有任何关系，比如R={(a,b),(b,c),(a,c),(a,a),(b,b),(c,c),(m,m)},这个关系集中m就和其他元素没有任何顺序关系
 *
 * 看了教科书式的定义，那么我们怎么简单地去理解偏序关系和全序关系呢？
 *
 * 偏序关系表示了集合内只有部分元素之间在这个关系下是可以比较的（局部）。
 * 全序关系表示了集合内任何一对元素在在这个关系下都是相互可比较的（全局）。
 * 全序关系也是一种偏序关系。
 * 偏序排序
 * 知道了偏序关系，偏序排序就很简单，按照偏序关系进行排序就是偏序排序啦。
 *
 * 我们日常说的排序一般都是指全序排序。比如说给定数组[8,2,5,1,111,50,0]，通过快排使数组有序。这里其实就是指全序排序，而全序关系就是指两个元素的大小关系，因为我们认为数组中任意两个元素都是可以比较大小的。
 */
