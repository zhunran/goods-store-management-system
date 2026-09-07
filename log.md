2026-09-07T19:42:24.524+08:00 ERROR 47128 --- [goods-store-auth-api] [ restartedMain] t.s.DeferredServletContainerInitializers : Error starting Tomcat context. Exception: org.springframework.beans.factory.UnsatisfiedDependencyException. Message: Error creating bean with name 'jwtAuthenticationFilter' defined in file [D:\.workspace\javaproject\goods-store-management-system-parent\goods-store-service\goods-store-auth-api\target\classes\com\fengluan\auth\config\JwtAuthenticationFilter.class]: Unsatisfied dependency expressed through constructor parameter 0: Error creating bean with name 'jwtUtil' defined in file [D:\.workspace\javaproject\goods-store-management-system-parent\goods-store-service\goods-store-auth-api\target\classes\com\fengluan\auth\util\JwtUtil.class]: Unexpected exception during bean creation
2026-09-07T19:42:24.540+08:00 INFO 47128 --- [goods-store-auth-api] [ restartedMain] o.apache.catalina.core.StandardService : Stopping service [Tomcat]
2026-09-07T19:42:24.564+08:00 WARN 47128 --- [goods-store-auth-api] [ restartedMain] ConfigServletWebServerApplicationContext : Exception encountered during context initialization - cancelling refresh attempt: org.springframework.context.ApplicationContextException: Unable to start web server
2026-09-07T19:42:24.570+08:00 INFO 47128 --- [goods-store-auth-api] [ restartedMain] .s.b.a.l.ConditionEvaluationReportLogger :

Error starting ApplicationContext. To display the condition evaluation report re-run your application with 'debug' enabled.
2026-09-07T19:42:24.582+08:00 ERROR 47128 --- [goods-store-auth-api] [ restartedMain] o.s.boot.SpringApplication : Application run failed

org.springframework.context.ApplicationContextException: Unable to start web server
at org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext.onRefresh(ServletWebServerApplicationContext.java:167) ~[spring-boot-web-server-4.1.1.jar:4.1.1]
at org.springframework.context.support.AbstractApplicationContext.refresh(AbstractApplicationContext.java:615) ~[spring-context-7.0.9.jar:7.0.9]
at org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext.refresh(ServletWebServerApplicationContext.java:143) ~[spring-boot-web-server-4.1.1.jar:4.1.1]
at org.springframework.boot.SpringApplication.refresh(SpringApplication.java:756) ~[spring-boot-4.1.1.jar:4.1.1]
at org.springframework.boot.SpringApplication.refreshContext(SpringApplication.java:445) ~[spring-boot-4.1.1.jar:4.1.1]
at org.springframework.boot.SpringApplication.run(SpringApplication.java:321) ~[spring-boot-4.1.1.jar:4.1.1]
at org.springframework.boot.SpringApplication.run(SpringApplication.java:1365) ~[spring-boot-4.1.1.jar:4.1.1]
at org.springframework.boot.SpringApplication.run(SpringApplication.java:1354) ~[spring-boot-4.1.1.jar:4.1.1]
at com.fengluan.AuthApiApplication.main(AuthApiApplication.java:14) ~[classes/:na]
at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:104) ~[na:na]
at java.base/java.lang.reflect.Method.invoke(Method.java:565) ~[na:na]
at org.springframework.boot.devtools.restart.RestartLauncher.run(RestartLauncher.java:52) ~[spring-boot-devtools-4.1.1.jar:4.1.1]
Caused by: org.springframework.boot.web.server.WebServerException: Unable to start embedded Tomcat
at org.springframework.boot.tomcat.TomcatWebServer.initialize(TomcatWebServer.java:150) ~[spring-boot-tomcat-4.1.1.jar:4.1.1]
at org.springframework.boot.tomcat.TomcatWebServer.<init>(TomcatWebServer.java:110) ~[spring-boot-tomcat-4.1.1.jar:4.1.1]
at org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory.getTomcatWebServer(TomcatServletWebServerFactory.java:429) ~[spring-boot-tomcat-4.1.1.jar:4.1.1]
at org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory.getWebServer(TomcatServletWebServerFactory.java:167) ~[spring-boot-tomcat-4.1.1.jar:4.1.1]
at org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext.createWebServer(ServletWebServerApplicationContext.java:190) ~[spring-boot-web-server-4.1.1.jar:4.1.1]
at org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext.onRefresh(ServletWebServerApplicationContext.java:164) ~[spring-boot-web-server-4.1.1.jar:4.1.1]
... 11 common frames omitted
Caused by: org.springframework.beans.factory.UnsatisfiedDependencyException: Error creating bean with name 'jwtAuthenticationFilter' defined in file [D:\.workspace\javaproject\goods-store-management-system-parent\goods-store-service\goods-store-auth-api\target\classes\com\fengluan\auth\config\JwtAuthenticationFilter.class]: Unsatisfied dependency expressed through constructor parameter 0: Error creating bean with name 'jwtUtil' defined in file [D:\.workspace\javaproject\goods-store-management-system-parent\goods-store-service\goods-store-auth-api\target\classes\com\fengluan\auth\util\JwtUtil.class]: Unexpected exception during bean creation
at org.springframework.beans.factory.support.ConstructorResolver.createArgumentArray(ConstructorResolver.java:804) ~[spring-beans-7.0.9.jar:7.0.9]
at org.springframework.beans.factory.support.ConstructorResolver.autowireConstructor(ConstructorResolver.java:240) ~[spring-beans-7.0.9.jar:7.0.9]
at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.autowireConstructor(AbstractAutowireCapableBeanFactory.java:1380) ~[spring-beans-7.0.9.jar:7.0.9]
at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBeanInstance(AbstractAutowireCapableBeanFactory.java:1219) ~[spring-beans-7.0.9.jar:7.0.9]
at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.doCreateBean(AbstractAutowireCapableBeanFactory.java:565) ~[spring-beans-7.0.9.jar:7.0.9]
at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(AbstractAutowireCapableBeanFactory.java:525) ~[spring-beans-7.0.9.jar:7.0.9]
at org.springframework.beans.factory.support.AbstractBeanFactory.lambda$doGetBean$0(AbstractBeanFactory.java:333) ~[spring-beans-7.0.9.jar:7.0.9]
	at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.getSingleton(DefaultSingletonBeanRegistry.java:371) ~[spring-beans-7.0.9.jar:7.0.9]
	at org.springframework.beans.factory.support.AbstractBeanFactory.doGetBean(AbstractBeanFactory.java:331) ~[spring-beans-7.0.9.jar:7.0.9]
	at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(AbstractBeanFactory.java:201) ~[spring-beans-7.0.9.jar:7.0.9]
	at org.springframework.boot.web.servlet.ServletContextInitializerBeans.getOrderedBeansOfType(ServletContextInitializerBeans.java:231) ~[spring-boot-4.1.1.jar:4.1.1]
	at org.springframework.boot.web.servlet.ServletContextInitializerBeans.addAsRegistrationBean(ServletContextInitializerBeans.java:185) ~[spring-boot-4.1.1.jar:4.1.1]
	at org.springframework.boot.web.servlet.ServletContextInitializerBeans.addAsRegistrationBean(ServletContextInitializerBeans.java:180) ~[spring-boot-4.1.1.jar:4.1.1]
	at org.springframework.boot.web.servlet.ServletContextInitializerBeans.addAdaptableBeans(ServletContextInitializerBeans.java:165) ~[spring-boot-4.1.1.jar:4.1.1]
	at org.springframework.boot.web.servlet.ServletContextInitializerBeans.<init>(ServletContextInitializerBeans.java:97) ~[spring-boot-4.1.1.jar:4.1.1]
	at org.springframework.boot.web.context.servlet.WebApplicationContextInitializer.initialize(WebApplicationContextInitializer.java:53) ~[spring-boot-4.1.1.jar:4.1.1]
	at org.springframework.boot.tomcat.servlet.DeferredServletContainerInitializers.onStartup(DeferredServletContainerInitializers.java:55) ~[spring-boot-tomcat-4.1.1.jar:4.1.1]
	at org.apache.catalina.core.StandardContext.startInternal(StandardContext.java:4573) ~[tomcat-embed-core-11.0.24.jar:11.0.24]
	at org.apache.catalina.util.LifecycleBase.start(LifecycleBase.java:170) ~[tomcat-embed-core-11.0.24.jar:11.0.24]
	at org.apache.catalina.core.ContainerBase$StartChild.call(ContainerBase.java:1204) ~[tomcat-embed-core-11.0.24.jar:11.0.24]
at org.apache.catalina.core.ContainerBase$StartChild.call(ContainerBase.java:1200) ~[tomcat-embed-core-11.0.24.jar:11.0.24]
	at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:328) ~[na:na]
	at org.apache.tomcat.util.threads.InlineExecutorService.execute(InlineExecutorService.java:92) ~[tomcat-embed-core-11.0.24.jar:11.0.24]
	at java.base/java.util.concurrent.AbstractExecutorService.submit(AbstractExecutorService.java:149) ~[na:na]
	at org.apache.catalina.core.ContainerBase.startInternal(ContainerBase.java:732) ~[tomcat-embed-core-11.0.24.jar:11.0.24]
	at org.apache.catalina.core.StandardHost.startInternal(StandardHost.java:794) ~[tomcat-embed-core-11.0.24.jar:11.0.24]
	at org.apache.catalina.util.LifecycleBase.start(LifecycleBase.java:170) ~[tomcat-embed-core-11.0.24.jar:11.0.24]
	at org.apache.catalina.core.ContainerBase$StartChild.call(ContainerBase.java:1204) ~[tomcat-embed-core-11.0.24.jar:11.0.24]
at org.apache.catalina.core.ContainerBase$StartChild.call(ContainerBase.java:1200) ~[tomcat-embed-core-11.0.24.jar:11.0.24]
	at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:328) ~[na:na]
	at org.apache.tomcat.util.threads.InlineExecutorService.execute(InlineExecutorService.java:92) ~[tomcat-embed-core-11.0.24.jar:11.0.24]
	at java.base/java.util.concurrent.AbstractExecutorService.submit(AbstractExecutorService.java:149) ~[na:na]
	at org.apache.catalina.core.ContainerBase.startInternal(ContainerBase.java:732) ~[tomcat-embed-core-11.0.24.jar:11.0.24]
	at org.apache.catalina.core.StandardEngine.startInternal(StandardEngine.java:201) ~[tomcat-embed-core-11.0.24.jar:11.0.24]
	at org.apache.catalina.util.LifecycleBase.start(LifecycleBase.java:170) ~[tomcat-embed-core-11.0.24.jar:11.0.24]
	at org.apache.catalina.core.StandardService.startInternal(StandardService.java:433) ~[tomcat-embed-core-11.0.24.jar:11.0.24]
	at org.apache.catalina.util.LifecycleBase.start(LifecycleBase.java:170) ~[tomcat-embed-core-11.0.24.jar:11.0.24]
	at org.apache.catalina.core.StandardServer.startInternal(StandardServer.java:871) ~[tomcat-embed-core-11.0.24.jar:11.0.24]
	at org.apache.catalina.util.LifecycleBase.start(LifecycleBase.java:170) ~[tomcat-embed-core-11.0.24.jar:11.0.24]
	at org.apache.catalina.startup.Tomcat.start(Tomcat.java:452) ~[tomcat-embed-core-11.0.24.jar:11.0.24]
	at org.springframework.boot.tomcat.TomcatWebServer.initialize(TomcatWebServer.java:131) ~[spring-boot-tomcat-4.1.1.jar:4.1.1]
	... 16 common frames omitted
Caused by: org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'jwtUtil' defined in file [D:\.workspace\javaproject\goods-store-management-system-parent\goods-store-service\goods-store-auth-api\target\classes\com\fengluan\auth\util\JwtUtil.class]: Unexpected exception during bean creation
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(AbstractAutowireCapableBeanFactory.java:538) ~[spring-beans-7.0.9.jar:7.0.9]
	at org.springframework.beans.factory.support.AbstractBeanFactory.lambda$doGetBean$0(AbstractBeanFactory.java:333) ~[spring-beans-7.0.9.jar:7.0.9]
	at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.getSingleton(DefaultSingletonBeanRegistry.java:371) ~[spring-beans-7.0.9.jar:7.0.9]
	at org.springframework.beans.factory.support.AbstractBeanFactory.doGetBean(AbstractBeanFactory.java:331) ~[spring-beans-7.0.9.jar:7.0.9]
	at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(AbstractBeanFactory.java:201) ~[spring-beans-7.0.9.jar:7.0.9]
	at org.springframework.beans.factory.support.DefaultListableBeanFactory.resolveBean(DefaultListableBeanFactory.java:1232) ~[spring-beans-7.0.9.jar:7.0.9]
	at org.springframework.beans.factory.support.DefaultListableBeanFactory.doResolveDependency(DefaultListableBeanFactory.java:1711) ~[spring-beans-7.0.9.jar:7.0.9]
	at org.springframework.beans.factory.support.DefaultListableBeanFactory.resolveDependency(DefaultListableBeanFactory.java:1658) ~[spring-beans-7.0.9.jar:7.0.9]
	at org.springframework.beans.factory.support.ConstructorResolver.resolveAutowiredArgument(ConstructorResolver.java:912) ~[spring-beans-7.0.9.jar:7.0.9]
	at org.springframework.beans.factory.support.ConstructorResolver.createArgumentArray(ConstructorResolver.java:791) ~[spring-beans-7.0.9.jar:7.0.9]
	... 56 common frames omitted
Caused by: org.springframework.util.PlaceholderResolutionException: Could not resolve placeholder 'JWT_SECRET' in value "${JWT_SECRET}" <-- "${jwt.secret}"
	at org.springframework.util.PlaceholderResolutionException.withValue(PlaceholderResolutionException.java:81) ~[spring-core-7.0.9.jar:7.0.9]
	at org.springframework.util.PlaceholderParser$ParsedValue.resolve(PlaceholderParser.java:296) ~[spring-core-7.0.9.jar:7.0.9]
at org.springframework.util.PlaceholderParser.replacePlaceholders(PlaceholderParser.java:129) ~[spring-core-7.0.9.jar:7.0.9]
at org.springframework.util.PropertyPlaceholderHelper.replacePlaceholders(PropertyPlaceholderHelper.java:96) ~[spring-core-7.0.9.jar:7.0.9]
at org.springframework.core.env.AbstractPropertyResolver.doResolvePlaceholders(AbstractPropertyResolver.java:286) ~[spring-core-7.0.9.jar:7.0.9]
at org.springframework.core.env.AbstractPropertyResolver.resolveRequiredPlaceholders(AbstractPropertyResolver.java:257) ~[spring-core-7.0.9.jar:7.0.9]
at org.springframework.context.support.PropertySourcesPlaceholderConfigurer.lambda$processProperties$0(PropertySourcesPlaceholderConfigurer.java:184) ~[spring-context-7.0.9.jar:7.0.9]
at org.springframework.beans.factory.support.AbstractBeanFactory.resolveEmbeddedValue(AbstractBeanFactory.java:959) ~[spring-beans-7.0.9.jar:7.0.9]
at org.springframework.beans.factory.support.DefaultListableBeanFactory.doResolveDependency(DefaultListableBeanFactory.java:1679) ~[spring-beans-7.0.9.jar:7.0.9]
at org.springframework.beans.factory.support.DefaultListableBeanFactory.resolveDependency(DefaultListableBeanFactory.java:1658) ~[spring-beans-7.0.9.jar:7.0.9]
at org.springframework.beans.factory.support.ConstructorResolver.resolveAutowiredArgument(ConstructorResolver.java:912) ~[spring-beans-7.0.9.jar:7.0.9]
at org.springframework.beans.factory.support.ConstructorResolver.createArgumentArray(ConstructorResolver.java:791) ~[spring-beans-7.0.9.jar:7.0.9]
at org.springframework.beans.factory.support.ConstructorResolver.autowireConstructor(ConstructorResolver.java:240) ~[spring-beans-7.0.9.jar:7.0.9]
at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.autowireConstructor(AbstractAutowireCapableBeanFactory.java:1380) ~[spring-beans-7.0.9.jar:7.0.9]
at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBeanInstance(AbstractAutowireCapableBeanFactory.java:1219) ~[spring-beans-7.0.9.jar:7.0.9]
at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.doCreateBean(AbstractAutowireCapableBeanFactory.java:565) ~[spring-beans-7.0.9.jar:7.0.9]
at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(AbstractAutowireCapableBeanFactory.java:525) ~[spring-beans-7.0.9.jar:7.0.9]
... 65 common frames omitted

2026-09-07T19:42:24.588+08:00 INFO 47128 --- [goods-store-auth-api] [ Thread-10] c.a.n.common.http.HttpClientBeanHolder : [HttpClientBeanHolder] Start destroying common HttpClient
2026-09-07T19:42:24.588+08:00 INFO 47128 --- [goods-store-auth-api] [ Thread-12] c.a.nacos.common.notify.NotifyCenter : [NotifyCenter] Start destroying Publisher
2026-09-07T19:42:24.588+08:00 INFO 47128 --- [goods-store-auth-api] [ Thread-4] c.a.n.common.executor.ThreadPoolManager : [ThreadPoolManager] Start destroying ThreadPool
2026-09-07T19:42:24.588+08:00 INFO 47128 --- [goods-store-auth-api] [ Thread-12] c.a.nacos.common.notify.NotifyCenter : [NotifyCenter] Completed destruction of Publisher

进程已结束，退出代码为 0
