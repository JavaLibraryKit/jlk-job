spring2.7版本已废弃spring.factories。
spring3.x版本使用新方式：
1. 新建文件 META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
2. 在新文件写入内容, 格式如下
```
cn.liujinnan.xxx.Class1
cn.liujinnan.xxx.Class2
```