10:27:32.853 [restartedMain] INFO com.alibaba.nacos.plugin.auth.spi.client.ClientAuthPluginManager -- [ClientAuthPluginManager] Load ClientAuthService com.alibaba.nacos.client.auth.impl.NacosClientAuthServiceImpl success.
10:27:32.853 [restartedMain] INFO com.alibaba.nacos.plugin.auth.spi.client.ClientAuthPluginManager -- [ClientAuthPluginManager] Load ClientAuthService com.alibaba.nacos.client.auth.ram.RamClientAuthServiceImpl success.
WARNING: A terminally deprecated method in sun.misc.Unsafe has been called
WARNING: sun.misc.Unsafe::objectFieldOffset has been called by com.alibaba.nacos.shaded.com.google.common.util.concurrent.AbstractFuture$UnsafeAtomicHelper (file:/D:/myrepository/com/alibaba/nacos/nacos-client/3.1.1/nacos-client-3.1.1.jar)
WARNING: Please consider reporting this to the maintainers of class com.alibaba.nacos.shaded.com.google.common.util.concurrent.AbstractFuture$UnsafeAtomicHelper
WARNING: sun.misc.Unsafe::objectFieldOffset will be removed in a future release
10:27:33.463 [restartedMain] INFO com.alibaba.nacos.common.ability.AbstractAbilityControlManager -- Ready to get current node abilities...
10:27:33.464 [restartedMain] INFO com.alibaba.nacos.common.ability.AbstractAbilityControlManager -- Ready to initialize current node abilities, support modes: [SDK_CLIENT]
10:27:33.464 [restartedMain] INFO com.alibaba.nacos.common.ability.AbstractAbilityControlManager -- Initialize current abilities finish...
10:27:33.464 [restartedMain] INFO com.alibaba.nacos.common.ability.discover.NacosAbilityManagerHolder -- [AbilityControlManager] Successfully initialize AbilityControlManager

. \_**\_ \_ ** \_ \_
/\\ / **_'_ ** \_ _(_)_ \_\_ \_\_ _ \ \ \ \
( ( )\_** | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/ \_**)| |_)| | | | | || (_| | ) ) ) )
' |\_**\_| .**|_| |_|_| |_\__, | / / / /
=========|_|==============|_\_\_/=/_/_/_/

:: Spring Boot :: (v4.1.1)

2026-08-28T10:27:34.619+08:00 INFO 32288 --- [goods-store-product-api] [ restartedMain] c.f.product.ProductApiApplication : Starting ProductApiApplication using Java 25.0.4 with PID 32288 (D:\.workspace\javaproject\goods-store-management-system-parent\goods-store-service\goods-store-product-api\target\classes started by 86198 in D:\.workspace\javaproject\goods-store-management-system-parent)
2026-08-28T10:27:34.620+08:00 INFO 32288 --- [goods-store-product-api] [ restartedMain] c.f.product.ProductApiApplication : No active profile set, falling back to 1 default profile: "default"
2026-08-28T10:27:34.651+08:00 WARN 32288 --- [goods-store-product-api] [ restartedMain] c.a.c.n.c.NacosConfigDataLoader : [Nacos Config] config[dataId=goods-store-product-api.yaml, group=DEFAULT_GROUP] is empty
2026-08-28T10:27:34.652+08:00 INFO 32288 --- [goods-store-product-api] [ restartedMain] .e.DevToolsPropertyDefaultsPostProcessor : Devtools property defaults active! Set 'spring.devtools.add-properties' to 'false' to disable
2026-08-28T10:27:34.652+08:00 INFO 32288 --- [goods-store-product-api] [ restartedMain] .e.DevToolsPropertyDefaultsPostProcessor : For additional web related logging consider setting the 'logging.level.web' property to 'DEBUG'
2026-08-28T10:27:35.075+08:00 WARN 32288 --- [goods-store-product-api] [ restartedMain] o.m.s.mapper.ClassPathMapperScanner : No MyBatis mapper was found in '[com.fengluan.product.repository]' package. Please check your configuration.
2026-08-28T10:27:35.077+08:00 INFO 32288 --- [goods-store-product-api] [ restartedMain] o.s.cloud.context.scope.GenericScope : BeanFactory id=5d6fa8c9-df3d-3029-9175-fbc2812e85cf
2026-08-28T10:27:35.318+08:00 INFO 32288 --- [goods-store-product-api] [ restartedMain] o.s.boot.tomcat.TomcatWebServer : Tomcat initialized with port 8084 (http)
2026-08-28T10:27:35.327+08:00 INFO 32288 --- [goods-store-product-api] [ restartedMain] o.apache.catalina.core.StandardService : Starting service [Tomcat]
2026-08-28T10:27:35.327+08:00 INFO 32288 --- [goods-store-product-api] [ restartedMain] o.apache.catalina.core.StandardEngine : Starting Servlet engine: [Apache Tomcat/11.0.24]
2026-08-28T10:27:35.360+08:00 INFO 32288 --- [goods-store-product-api] [ restartedMain] b.w.c.s.WebApplicationContextInitializer : Root WebApplicationContext: initialization completed in 708 ms
2026-08-28T10:27:35.460+08:00 INFO 32288 --- [goods-store-product-api] [ restartedMain] o.s.c.openfeign.FeignClientFactoryBean : For 'goods-store-brand-api' URL not provided. Will try picking an instance via load-balancing.
2026-08-28T10:27:35.461+08:00 WARN 32288 --- [goods-store-product-api] [ restartedMain] ConfigServletWebServerApplicationContext : Exception encountered during context initialization - cancelling refresh attempt: org.springframework.beans.factory.UnsatisfiedDependencyException: Error creating bean with name 'productController' defined in file [D:\.workspace\javaproject\goods-store-management-system-parent\goods-store-service\goods-store-product-api\target\classes\com\fengluan\product\api\ProductController.class]: Unsatisfied dependency expressed through constructor parameter 0: Error creating bean with name 'com.fengluan.product.service.feign.BrandFeignService': FactoryBean threw exception on object creation
2026-08-28T10:27:35.461+08:00 WARN 32288 --- [goods-store-product-api] [ restartedMain] s.c.a.AnnotationConfigApplicationContext : Exception thrown from ApplicationListener handling ContextClosedEvent

org.springframework.beans.factory.BeanCreationNotAllowedException: Error creating bean with name 'nacosGracefulShutdownDelegate': Singleton bean creation not allowed while singletons of this factory are in destruction (Do not request a bean from a BeanFactory in a destroy method implementation!)
at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.getSingleton(DefaultSingletonBeanRegistry.java:301) ~[spring-beans-7.0.9.jar:7.0.9]
at org.springframework.beans.factory.support.AbstractBeanFactory.doGetBean(AbstractBeanFactory.java:331) ~[spring-beans-7.0.9.jar:7.0.9]
at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(AbstractBeanFactory.java:201) ~[spring-beans-7.0.9.jar:7.0.9]
at org.springframework.context.event.AbstractApplicationEventMulticaster.retrieveApplicationListeners(AbstractApplicationEventMulticaster.java:266) ~[spring-context-7.0.9.jar:7.0.9]
at org.springframework.context.event.AbstractApplicationEventMulticaster.getApplicationListeners(AbstractApplicationEventMulticaster.java:222) ~[spring-context-7.0.9.jar:7.0.9]
at org.springframework.context.event.SimpleApplicationEventMulticaster.multicastEvent(SimpleApplicationEventMulticaster.java:140) ~[spring-context-7.0.9.jar:7.0.9]
at org.springframework.context.support.AbstractApplicationContext.publishEvent(AbstractApplicationContext.java:448) ~[spring-context-7.0.9.jar:7.0.9]
at org.springframework.context.support.AbstractApplicationContext.publishEvent(AbstractApplicationContext.java:454) ~[spring-context-7.0.9.jar:7.0.9]
at org.springframework.context.support.AbstractApplicationContext.publishEvent(AbstractApplicationContext.java:381) ~[spring-context-7.0.9.jar:7.0.9]
at org.springframework.context.support.AbstractApplicationContext.doClose(AbstractApplicationContext.java:1190) ~[spring-context-7.0.9.jar:7.0.9]
at org.springframework.context.support.AbstractApplicationContext.close(AbstractApplicationContext.java:1153) ~[spring-context-7.0.9.jar:7.0.9]
at org.springframework.cloud.context.named.NamedContextFactory.destroy(NamedContextFactory.java:116) ~[spring-cloud-context-5.0.3.jar:5.0.3]
at org.springframework.beans.factory.support.DisposableBeanAdapter.destroy(DisposableBeanAdapter.java:209) ~[spring-beans-7.0.9.jar:7.0.9]
at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.destroyBean(DefaultSingletonBeanRegistry.java:795) ~[spring-beans-7.0.9.jar:7.0.9]
at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.destroySingleton(DefaultSingletonBeanRegistry.java:745) ~[spring-beans-7.0.9.jar:7.0.9]
at org.springframework.beans.factory.support.DefaultListableBeanFactory.destroySingleton(DefaultListableBeanFactory.java:1507) ~[spring-beans-7.0.9.jar:7.0.9]
at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.destroySingletons(DefaultSingletonBeanRegistry.java:704) ~[spring-beans-7.0.9.jar:7.0.9]
at org.springframework.beans.factory.support.DefaultListableBeanFactory.destroySingletons(DefaultListableBeanFactory.java:1500) ~[spring-beans-7.0.9.jar:7.0.9]
at org.springframework.context.support.AbstractApplicationContext.destroyBeans(AbstractApplicationContext.java:1246) ~[spring-context-7.0.9.jar:7.0.9]
at org.springframework.context.support.AbstractApplicationContext.refresh(AbstractApplicationContext.java:644) ~[spring-context-7.0.9.jar:7.0.9]
at org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext.refresh(ServletWebServerApplicationContext.java:143) ~[spring-boot-web-server-4.1.1.jar:4.1.1]
at org.springframework.boot.SpringApplication.refresh(SpringApplication.java:756) ~[spring-boot-4.1.1.jar:4.1.1]
at org.springframework.boot.SpringApplication.refreshContext(SpringApplication.java:445) ~[spring-boot-4.1.1.jar:4.1.1]
at org.springframework.boot.SpringApplication.run(SpringApplication.java:321) ~[spring-boot-4.1.1.jar:4.1.1]
at org.springframework.boot.SpringApplication.run(SpringApplication.java:1365) ~[spring-boot-4.1.1.jar:4.1.1]
at org.springframework.boot.SpringApplication.run(SpringApplication.java:1354) ~[spring-boot-4.1.1.jar:4.1.1]
at com.fengluan.product.ProductApiApplication.main(ProductApiApplication.java:16) ~[classes/:na]
at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:104) ~[na:na]
at java.base/java.lang.reflect.Method.invoke(Method.java:565) ~[na:na]
at org.springframework.boot.devtools.restart.RestartLauncher.run(RestartLauncher.java:52) ~[spring-boot-devtools-4.1.1.jar:4.1.1]

2026-08-28T10:27:35.464+08:00 INFO 32288 --- [goods-store-product-api] [ restartedMain] o.apache.catalina.core.StandardService : Stopping service [Tomcat]
2026-08-28T10:27:35.477+08:00 INFO 32288 --- [goods-store-product-api] [ restartedMain] .s.b.a.l.ConditionEvaluationReportLogger :

Error starting ApplicationContext. To display the condition evaluation report re-run your application with 'debug' enabled.
2026-08-28T10:27:35.488+08:00 ERROR 32288 --- [goods-store-product-api] [ restartedMain] o.s.boot.SpringApplication : Application run failed

org.springframework.beans.factory.UnsatisfiedDependencyException: Error creating bean with name 'productController' defined in file [D:\.workspace\javaproject\goods-store-management-system-parent\goods-store-service\goods-store-product-api\target\classes\com\fengluan\product\api\ProductController.class]: Unsatisfied dependency expressed through constructor parameter 0: Error creating bean with name 'com.fengluan.product.service.feign.BrandFeignService': FactoryBean threw exception on object creation
at org.springframework.beans.factory.support.ConstructorResolver.createArgumentArray(ConstructorResolver.java:804) ~[spring-beans-7.0.9.jar:7.0.9]
at org.springframework.beans.factory.support.ConstructorResolver.autowireConstructor(ConstructorResolver.java:240) ~[spring-beans-7.0.9.jar:7.0.9]
at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.autowireConstructor(AbstractAutowireCapableBeanFactory.java:1380) ~[spring-beans-7.0.9.jar:7.0.9]
at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBeanInstance(AbstractAutowireCapableBeanFactory.java:1219) ~[spring-beans-7.0.9.jar:7.0.9]
at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.doCreateBean(AbstractAutowireCapableBeanFactory.java:565) ~[spring-beans-7.0.9.jar:7.0.9]
at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(AbstractAutowireCapableBeanFactory.java:525) ~[spring-beans-7.0.9.jar:7.0.9]
at org.springframework.beans.factory.support.AbstractBeanFactory.lambda$doGetBean$0(AbstractBeanFactory.java:333) ~[spring-beans-7.0.9.jar:7.0.9]
at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.getSingleton(DefaultSingletonBeanRegistry.java:371) ~[spring-beans-7.0.9.jar:7.0.9]
at org.springframework.beans.factory.support.AbstractBeanFactory.doGetBean(AbstractBeanFactory.java:331) ~[spring-beans-7.0.9.jar:7.0.9]
at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(AbstractBeanFactory.java:196) ~[spring-beans-7.0.9.jar:7.0.9]
at org.springframework.beans.factory.support.DefaultListableBeanFactory.instantiateSingleton(DefaultListableBeanFactory.java:1225) ~[spring-beans-7.0.9.jar:7.0.9]
at org.springframework.beans.factory.support.DefaultListableBeanFactory.preInstantiateSingleton(DefaultListableBeanFactory.java:1191) ~[spring-beans-7.0.9.jar:7.0.9]
at org.springframework.beans.factory.support.DefaultListableBeanFactory.preInstantiateSingletons(DefaultListableBeanFactory.java:1121) ~[spring-beans-7.0.9.jar:7.0.9]
at org.springframework.context.support.AbstractApplicationContext.finishBeanFactoryInitialization(AbstractApplicationContext.java:994) ~[spring-context-7.0.9.jar:7.0.9]
at org.springframework.context.support.AbstractApplicationContext.refresh(AbstractApplicationContext.java:621) ~[spring-context-7.0.9.jar:7.0.9]
at org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext.refresh(ServletWebServerApplicationContext.java:143) ~[spring-boot-web-server-4.1.1.jar:4.1.1]
at org.springframework.boot.SpringApplication.refresh(SpringApplication.java:756) ~[spring-boot-4.1.1.jar:4.1.1]
at org.springframework.boot.SpringApplication.refreshContext(SpringApplication.java:445) ~[spring-boot-4.1.1.jar:4.1.1]
at org.springframework.boot.SpringApplication.run(SpringApplication.java:321) ~[spring-boot-4.1.1.jar:4.1.1]
at org.springframework.boot.SpringApplication.run(SpringApplication.java:1365) ~[spring-boot-4.1.1.jar:4.1.1]
at org.springframework.boot.SpringApplication.run(SpringApplication.java:1354) ~[spring-boot-4.1.1.jar:4.1.1]
at com.fengluan.product.ProductApiApplication.main(ProductApiApplication.java:16) ~[classes/:na]
at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:104) ~[na:na]
at java.base/java.lang.reflect.Method.invoke(Method.java:565) ~[na:na]
at org.springframework.boot.devtools.restart.RestartLauncher.run(RestartLauncher.java:52) ~[spring-boot-devtools-4.1.1.jar:4.1.1]
Caused by: org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'com.fengluan.product.service.feign.BrandFeignService': FactoryBean threw exception on object creation
at org.springframework.beans.factory.support.FactoryBeanRegistrySupport.doGetObjectFromFactoryBean(FactoryBeanRegistrySupport.java:209) ~[spring-beans-7.0.9.jar:7.0.9]
at org.springframework.beans.factory.support.FactoryBeanRegistrySupport.getObjectFromFactoryBean(FactoryBeanRegistrySupport.java:149) ~[spring-beans-7.0.9.jar:7.0.9]
at org.springframework.beans.factory.support.AbstractBeanFactory.getObjectForBeanInstance(AbstractBeanFactory.java:1879) ~[spring-beans-7.0.9.jar:7.0.9]
at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.getObjectForBeanInstance(AbstractAutowireCapableBeanFactory.java:1302) ~[spring-beans-7.0.9.jar:7.0.9]
at org.springframework.beans.factory.support.AbstractBeanFactory.doGetBean(AbstractBeanFactory.java:343) ~[spring-beans-7.0.9.jar:7.0.9]
at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(AbstractBeanFactory.java:201) ~[spring-beans-7.0.9.jar:7.0.9]
at org.springframework.beans.factory.config.DependencyDescriptor.resolveCandidate(DependencyDescriptor.java:229) ~[spring-beans-7.0.9.jar:7.0.9]
at org.springframework.beans.factory.support.DefaultListableBeanFactory.doResolveDependency(DefaultListableBeanFactory.java:1769) ~[spring-beans-7.0.9.jar:7.0.9]
at org.springframework.beans.factory.support.DefaultListableBeanFactory.resolveDependency(DefaultListableBeanFactory.java:1658) ~[spring-beans-7.0.9.jar:7.0.9]
at org.springframework.beans.factory.support.ConstructorResolver.resolveAutowiredArgument(ConstructorResolver.java:912) ~[spring-beans-7.0.9.jar:7.0.9]
at org.springframework.beans.factory.support.ConstructorResolver.createArgumentArray(ConstructorResolver.java:791) ~[spring-beans-7.0.9.jar:7.0.9]
... 24 common frames omitted
Caused by: java.lang.IllegalStateException: No Feign Client for loadBalancing defined. Did you forget to include spring-cloud-starter-loadbalancer?
at org.springframework.cloud.openfeign.FeignClientFactoryBean.loadBalance(FeignClientFactoryBean.java:441) ~[spring-cloud-openfeign-core-5.0.3.jar:5.0.3]
at org.springframework.cloud.openfeign.FeignClientFactoryBean.getTarget(FeignClientFactoryBean.java:486) ~[spring-cloud-openfeign-core-5.0.3.jar:5.0.3]
at org.springframework.cloud.openfeign.FeignClientFactoryBean.getObject(FeignClientFactoryBean.java:461) ~[spring-cloud-openfeign-core-5.0.3.jar:5.0.3]
at org.springframework.beans.factory.support.FactoryBeanRegistrySupport.doGetObjectFromFactoryBean(FactoryBeanRegistrySupport.java:203) ~[spring-beans-7.0.9.jar:7.0.9]
... 34 common frames omitted

2026-08-28T10:27:35.489+08:00 INFO 32288 --- [goods-store-product-api] [ Thread-4] c.a.n.common.executor.ThreadPoolManager : [ThreadPoolManager] Start destroying ThreadPool
2026-08-28T10:27:35.489+08:00 INFO 32288 --- [goods-store-product-api] [ Thread-10] c.a.n.common.http.HttpClientBeanHolder : [HttpClientBeanHolder] Start destroying common HttpClient

进程已结束，退出代码为 0
