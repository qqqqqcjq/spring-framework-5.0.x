package testmetadata;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;

import java.io.IOException;

/**
 * @date 2020/10/3 15:52
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
//像这些元数据，在框架设计时候很多时候我们都希望从File(Resource)里得到，而不是从Class文件里获取，所以就是MetadataReader和MetadataReaderFactory。下面我也给出使用案例：
//因为MetadataReader的实现类都是包级别的访问权限，所以它的实例只能来自工厂
public class GetMetadataFromFileNotClass {

    public static void main(String[] args) throws IOException {
        CachingMetadataReaderFactory readerFactory = new CachingMetadataReaderFactory();
        // 下面两种初始化方式都可，效果一样
        //MetadataReader metadataReader = readerFactory.getMetadataReader(MetaDemo.class.getName());
        MetadataReader metadataReader = readerFactory.getMetadataReader(new ClassPathResource("/testmetadata/MetaDemo.class"));

        ClassMetadata classMetadata = metadataReader.getClassMetadata();
        AnnotationMetadata annotationMetadata = metadataReader.getAnnotationMetadata();
        Resource resource = metadataReader.getResource();

        System.out.println(classMetadata); // org.springframework.core.type.classreading.AnnotationMetadataReadingVisitor@79079097
        System.out.println(annotationMetadata); // org.springframework.core.type.classreading.AnnotationMetadataReadingVisitor@79079097
        System.out.println(resource); // class path resource [com/fsx/maintest/MetaDemo.class]

    }
}