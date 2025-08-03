package com.lsj.tingshu.common.service.minio.annotation;

import com.lsj.tingshu.common.service.minio.config.MinioAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(MinioAutoConfiguration.class)
public @interface EnableMinioManagement {
}
