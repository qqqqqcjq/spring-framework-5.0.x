package maintest.importaware.simmulateredis;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.type.AnnotationMetadata;

import java.util.Map;

/**
 * @date 2020/1/22 14:46
 * @author chengjiaqing
 * @version : 0.1
 */ 

//实现ImportAware接口，并且加上@Configuration注解，这样spring才会处理
@Configuration
public class RedissionHttpSessionConfiguirationtest implements ImportAware {

	public RedissionHttpSessionConfiguirationtest() {
		System.out.println("RedissionHttpSessionConfiguirationtest  create");
	}

	private int keeptime;
	private String key;

	public int getKeeptime() {
		return keeptime;
	}

	public void setKeeptime(int keeptime) {
		this.keeptime = keeptime;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	@Override
	public void setImportMetadata(AnnotationMetadata importMetadata) {
		//根据注解名字获取注解的属性
		Map tempMap = importMetadata.getAnnotationAttributes(EnabelRedissionHttpSessiontest.class.getName());
		this.keeptime = ((Integer) tempMap.get("keeptime")).intValue();
		this.key =  (String) tempMap.get("key");
	}
}