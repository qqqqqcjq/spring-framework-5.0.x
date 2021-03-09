我们可以通过是实现ImportAware获取注解的值等
@Override
public void setImportMetadata(AnnotationMetadata importMetadata) {
		//根据注解名字获取注解的属性
		Map tempMap = importMetadata.getAnnotationAttributes(EnabelRedissionHttpSessiontest.class.getName());
		this.keeptime = ((Integer) tempMap.get("keeptime")).intValue();
		this.key =  (String) tempMap.get("key");
	}
}