## HttpRequestMediator

基于`Retofit`与`OkHttp`封装的网络请求库，利用kotlin dsl特性简化网络请求api调用。

- ~~支持RxJava3，简化RxJava对于网络请求的统一处理~~（v1.0.5版本后已移除对于RxJava的依赖）
  
- 提取对于网络请求的统一处理类，由外部注入具体处理策略

- 可自定义配置的网络请求日志输出

- 管理全局公共参数与请求头

- 统一的网络状态监听回调入口，兼容6.0之前版本

- 提供文件上传，文件下载的进度监听的功能
  
- 管理多种`Okhttp`与`Retrofit`配置，根据需要使用特定网络请求配置

- 支持动态切换BaseUrl

  

## 依赖方式

[![](https://jitpack.io/v/com.gitee.yupfeg/http_request_mediator.svg)](https://jitpack.io/#com.gitee.yupfeg/http_request_mediator)

``` groovy
//root project build.gradle
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
//module build.gradle
dependencies {
    implementation 'com.gitee.yupfeg:http_request_mediator:{$lastVersion}'
}
```

## 更新日志
- v1.0.9
  1. 优化网络状态变化重复分发问题
- v1.0.8
  1. 新增`OkHttpClient`线程调度器的快捷配置
- v1.0.7
  1. 新增`OkHttpClient`对于`cookies`持久化以及自动重定向的快捷配置
- v1.0.6
  1. 移除内部提供的线程池，方便外部项目统一线程池调度
  2. 在`HttpRequestConfig`中提供`OkHttpClient.Builder`配置，优先级最高，用于其他不常用参数配置
- v1.0.5
  1. 移除RxJava依赖
- v1.0.4
  1. 优化默认实现的日志输出拦截器，开放子类集成
  2. 移除默认添加的日志拦截器，完全交由外部配置添加

## 基础使用

- 先通过`addDefaultHttpClientFactory`或者`addHttpClientFactory`添加网络请求的工厂对象。

- 调用`getRetrofitInstance`获取`Retrofit`实例，来创建请求api，或者直接调用`createRequestApi`创建请求api对象。

### kotlin

利用`kotlin`的`by`关键字交由代理类创建`Retrofit`的api接口对象

1. 继承`BaseRequestApiDelegator`实现委托类的`addHttpRequestConfig`方法，添加对应key映射的网络请求配置

``` kotlin
class SimpleHttpApiDelegator<T>(clazz: Class<T>)
    : BaseRequestApiDelegator<T>(clazz, HttpRequestMediator.DEFAULT_CLIENT_KEY){
    override fun addHttpRequestConfig(configKey: String) {
        addDslRemoteConfig(configKey) {
            baseUrl = "https://www.baidu.com/"
            connectTimeout = 5
            readTimeout = 10
            writeTimeout = 15
            isAllowProxy = true
        }
    }
}

```

2. 创建内联函数，在外部调用可直接由by关键字委托使用

``` kotlin
inline fun <reified T> httpApiDelegate() = SimpleHttpApiDelegator(clazz = T::class.java)

class Test{
    private val testApi : TestApi by httpApiDelegate()
    
    fun getTestData(){
        testApi.getTest()
        ...
    }
}

interface TestApi{
    @GET("...")
    fun getTest()
}

```

### java

> `java`使用则还是按照原有逻辑处理即可

``` java
class DataSource {
    
    private volatile TestApi testApi;
    private String clientKey = "key";
        
    public void getTestData(){
        testApi.getTest()
            ...
    }    
    
    private TestApi getApiInstance(){
        if(testApi == null){
           synchronized(this){
               if(testApi == null){
                   initHttpClient();
                   testApi = HttpRequestMediator.createRequestApi(clientKey,TestApi.class)
               }
           }
        }
        return testApi;
    }
    
    private void initHttpClient(){
        HttpRequestConfig config = new HttpRequestConfig()
        ...    
        HttpRequestMediator.addDefaultHttpClientFactory(clientKey,config)    
    }
}
```



## 进阶使用





### 日志输出

内部已实现`HttpLogInterceptor`（v1.0.4版本后已开放子类继承），用于打印输出网络请求`request`与`response`的日志

> 推荐在设置网络配置时，设置给`networkInterceptors`，添加至**网络层拦截器的末尾**，避免遗漏重要日志。
> 1.0.4版本后，需要在配置时手动添加，不再内置任何不受外部控制的拦截器
```kotlin
addDslRemoteConfig(key){
    networkInterceptors.add(HttpLogInterceptor(LoggerHttpLogPrinterImpl()))
}
```

在创建`HttpLogInterceptor`时，需设置`HttpLogPrinter`，以自定义网络请求日志的输出策略。

> 推荐外部调用时设置为统一的日志输出管理类，方便管理日志输出。

### 统一网络请求返回处理

内部实现了`GlobalHttpResponseProcessor`的单例类（从v1.0.5版本开始）

step 1 : 设置全局的网络请求业务处理类，实现`HttpResponseHandler`接口（推荐只在全局注册一个)
```kotlin

@JvmStatic
fun setResponseHandler(handler: HttpResponseHandler)

```

step 2 : 在客户端的网络请求响应返回实体类实现`HttpResponseParsable`接口，
同时在网络请求响应返回时先调用

```kotlin
GlobalHttpResponseProcessor.preHandleHttpResponse(response)
```

方法，通过前面实现`HttpResponseHandler`接口的类，判断是否满足接口执行成功状态（如响应状态码code == 200，具体看业务需求）。
而在网络请求中出现的异常，则调用
```kotlin
GlobalHttpResponseProcessor.handleHttpError(throwable)
```




### 网络状态监听

内置了对网络状态变化的监听`NetWorkStatusHelper`
> ~~目前暂时使用RxJava3 的`BehaviorSubject`类分发网络状态变化，待后续分离对于RxJava的依赖~~（v1.0.5以后已移除RxJava3依赖）
> v1.0.5以后采用接口回调方式，实现`NetworkStatusChangeListener`接口
> 已兼容android 6.0以下

step 1 : 注册网络状态监听（推荐只在全局注册一个）


``` kotlin

@JvmStatic
fun registerNetWorkStatusChange(context: Context,listener: NetworkStatusChangeListener)

```

step 2 : 在应用结束时，要注意取消注册网络状态监听

``` kotlin

@JvmStatic
fun unRegisterNetworkStatusChange(context : Context)
    
```



### 公共请求参数管理

1. 提供了`BaseCommParamsInterceptor`基础拦截器类，只需要实现`commHeaders`与`commBodyParams`，分别添加固定的公共请求头与公共body参数
2. 直接继承`Interceptor`，正常添加okhttp拦截器方式



### 预处理请求回调

> ~~目前仅支持RxJava3封装，对于kotlin协程需要外部手动封装~~（v1.0.5版本后已移除，可在sample模块查看）

通过RxJava的`compose`操作符，创建`GlobalHttpTransformer`对象。

实现`onNextErrorIntercept`与`doOnErrorConsumer`两个kotlin高阶函数，分别处理请求回调与出现错误时的逻辑。

> 也可在外部进一步封装为`top-level`函数，根据项目的网络请求实际情况处理对应逻辑。

example：

``` kotlin
fun <T> preProcessHttpResponse() : GlobalHttpTransformer<T> 
	= GlobalHttpTransformer(
        onNextErrorIntercept = {
            //预处理onSuccess()、onNext()逻辑
            ...
        },
        doOnErrorConsumer = {
            //预处理onError()逻辑
            ...
        }
    )
```





### 多种http配置管理

`HttpRequestMediator`内部基于`HashMap`管理所有网络。而具体`Retrofit`与`OkHttpClient`对象创建，已抽取为独立的工厂类。

每个key对应映射为一个工厂类，也即是每个key对应一种http配置

外部通过`HttpRequestMediator.addHttpClientFactory`方法添加`HttpClientFactory`的实现类。

还可利用`kotlin dsl`方式，通过`HttpRequestConfig`配置默认工厂实现类`DefaultHttpClientFactoryImpl`

``` kotlin
class HttpRequestConfig{
    /**api域名*/
    var baseUrl : String ?= null
    /**链接超时时间，单位s*/
    var connectTimeout : Long = 5
    /**读取超时时间，单位s*/
    var readTimeout : Long = 10
    /**写入上传的超时时间，单位s*/
    var writeTimeout : Long = 15

    /**是否允许代理*/
    var isAllowProxy : Boolean = false
    /**是否在连接失败后自动重试，默认为false，在外部自行处理重试逻辑*/
    var isRetryOnConnectionFailure : Boolean = false

    /**
     * 网络响应的本地缓存
     * * `Cache(file = 缓存文件,size = 最大缓存大小)`
     * */
    var responseCache : Cache ?= null

    /**应用层拦截器*/
    var applicationInterceptors : MutableList<Interceptor> = mutableListOf()
    /**网络层拦截器*/
    var networkInterceptors : MutableList<Interceptor> = mutableListOf()

    /**retrofit解析器的集合*/
    var converterFactories : MutableList<Converter.Factory> = mutableListOf()
    /**retrofit回调支持的集合*/
    var callAdapterFactories : MutableList<CallAdapter.Factory> = mutableListOf()

    /**https的ssl证书校验配置*/
    var sslSocketConfig : SSLSocketTrustConfig ?= null

    /**网络请求事件监听，可监听网络请求指标数据*/
    var eventListenerFactory : EventListener.Factory? = null
}
```

> - ~~默认添加了**日志打印**拦截器（网络层）~~（v1.0.5版本后已移除）、默认添加**支持动态替换baseUrl**的拦截器（应用层）。
>
> - ~~默认仅支持gson解析，`RxJava3`回调支持（kotlin协程自带支持，不需要额外添加）~~（v1.0.5版本后已移除）



### 动态替换baseUrl

核心类为`UrlRedirectHelper`，管理所有需要替换的baseUrl

**stage 1**

需要通过`UrlRedirectHelper.putRedirectUrl`添加对应重定向url键值对映射。

**stage 2**

在网络请求API声明处，添加`@Headers("${UrlRedirectHelper.REDIRECT_HOST_HEAD_PREFIX}${URL_KEY}")`请求头
> 注意`UrlRedirectHelper.REDIRECT_HOST_HEAD_PREFIX`为必须标识符，只有这个标识才能确定开启动态替换url功能

``` kotlin
@Headers("${UrlRedirectHelper.REDIRECT_HOST_HEAD_PREFIX}user")
@GET("v1/user/get")
fun getUserData(...)
```

#### Url替换策略

默认的动态替换策略为`DefaultUrlReplacer`

> 1. 移除原始请求url的所有文段
>
> 2. 添加待替换的baseUrl内的所有文段
>
> 3. 将原始请求url的剩余文段拼接到重定向url后面，默认剩余文段为所有原始url的文段，由
>
>    即不忽略文段，所有待替换baseUrl中的文段直接添加在原始url文段前面，替换原始baseUrl。
>
> 4. 将所有文段添加到HttpUrl，并替换host



- 需要在部分请求单独忽略url内的某些文段

	则可在网络请求API声明处，额外添加`@Headers("${REDIRECT_SEGMENT_SIZE_HEAD_PREFIX}${SIZE}"`请求头

    ``` kotlin
    @Headers(
      "${UrlRedirectHelper.REDIRECT_HOST_HEAD_PREFIX}user",
      "${UrlRedirectHelper.REDIRECT_SEGMENT_SIZE_HEAD_PREFIX}2"
    )
    @GET("v1/user/get")
    fun getUserData(...)
    ```

	即可忽略原始文段内的`v1/user/`文段，只保留`get`文段。
	
	
	
- 需要所有网络请求都忽略文段（通常用不到）

  直接设置`UrlRedirectHelper.globalReplaceSegmentSize`数量，即可全局忽略所有网络请求的部分文段
  
  
- 更换url替换策略

1. 可通过**实现`UrlReplaceable`接口创建新的url替换策略**，
2. 调用`UrlRedirectHelper.setUrlReplacer`方法设置新替换策略

#### httpUrl文段替换规则

>
> size = 0，表示只替换域名，新文段拼接在原始请求文段之后
> * 原始请求url： `https://www.baidu.com/user/sss?user_id=111` ,
> * 重定向baseUrl：`https://www.google.com/test1/list/`,
> * 新url：`https://www.google.com/user/test1/list/sss?user_id=111`,
>
> size = 1，只替换域名后的第一个文段，新文段拼接在后面
>
> * 原始请求url：`https://www.baidu.com/user/sss?user_id=111`,
> * 重定向baseUrl：`https://www.google.com/test1/list/`,
> * 新url：`https://www.google.com/test1/list/sss?user_id=111`,
>
> 以此类推...


## 替换网络请求线程池

在实际开发中，我们可能希望收敛项目中运行的线程池资源，避免维护多个线程池，导致线程数过多。
而默认情况下，如果不配置时，会默认使用`OkHttpClient`内部自带的线程池（一个最大）进行网络请求调度。
在外部可以通过`OkHttpClient`创建配置时，设置`setDispatcher`方法进行替换。

在v1.0.8版本后，`HttpRequestConfig`类中提供了`executorService`属性，快捷设置线程池属性，
只要设置了该属性，在内部创建`OkHttpClient`对象时，就调用`setDispatcher`方法设置网络请求执行的线程池。

> 需要注意的是：
> 
> 在`OkHttpClient`实际发起请求的`RealCall`对象内，发起异步请求会创建`AsyncCall`对象，并执行调用内联函数`threadName`。
> 
> 将当前运行线程名称修改为`OkHttp ${request.url.redact()}`，然后在执行所有拦截器操作完成后修改回来。
> 
> 所以如果在日志中输出`Thread.currentThread.name`方法会出现不符合预期的情况，这是正常情况，实际线程还是由外部设置的线程来维护管理。
> 
> 上述逻辑，会发生在调用`call.enqueue`方法时，比如使用`suspend`修饰的网络请求api，默认就会调用`enqueue`发起异步请求。
> 
> 又比如`RxJava3CallAdapterFactory.create()`创建的`RxJava3CallAdapter`解析器，同样也会在内部调用`enqueue`发起异步请求。
> 
> 而`RxJava3CallAdapterFactory.createSynchronous()`则会在内部调用`execute`发起同步请求，而不会修改线程名称。
 

## 代码混淆配置

内部依赖了`gson`、`okHttp`、`retrofit`

添加官方给出的混淆配置即可

```
#----------- gson ----------------
-keep class com.google.gson.** {*;}
-keep class com.google.**{*;}
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.stream.** { *; }
-keep class com.google.gson.examples.android.model.** { *; }
#--------------------okhttp3----------------------
-dontwarn com.squareup.okhttp3.**
-keep class com.squareup.okhttp3.** { *;}
-dontwarn com.squareup.**
-dontwarn okio.**
-keep public class org.codehaus.* { *; }
-keep public class java.nio.* { *; }
#--------------------retrofit2---------------------
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
# Platform calls Class.forName on types which do not exist on Android to determine platform.
-dontnote retrofit2.Platform
## Platform used when running on Java 8 VMs. Will not be used at runtime.
-dontwarn retrofit2.Platform$Java8
## Retain generic type information for use by reflection by converters and adapters.
-keepattributes Signature
## Retain declared checked exceptions for use by a Proxy instance.
-keepattributes Exceptions
# Retrofit does reflection on method and parameter annotations.
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
# Retain service method parameters when optimizing.
-keepclassmembers,allowshrinking,allowobfuscation interface * {
 @retrofit2.http.* <methods>;
}
# Ignore annotation used for build tooling.
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# Ignore JSR 305 annotations for embedding nullability information.
-dontwarn javax.annotation.**

#解决使用Retrofit+RxJava联网时，在6.0系统出现java.lang.InternalError奔溃的问题:
#http://blog.csdn.net/mp624183768/article/details/79242147
-keepclassmembers class rx.internal.util.unsafe.*ArrayQueue*Field* {
    long producerIndex;
    long consumerIndex;
}
-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueProducerNodeRef {
    rx.internal.util.atomic.LinkedQueueNode producerNode;
}
-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueConsumerNodeRef {
    rx.internal.util.atomic.LinkedQueueNode consumerNode;
}
```



## TODO

- ~~简化`kotlin`协程的使用，提取通用协程response预处理操作~~（已提取response预处理）
- ~~逐步替换RxJava为kotlin协程kotlin flow~~(v1.0.5版本后已移除Rxjava)
- 优化下载功能，添加断点续传功能
- ~~分离RxJava依赖~~（v1.0.5版本后已移除RxJava）
- 增加简化WebSocket的支持

## thanks

[RetrofitUrlManager](https://github.com/JessYanCoding/RetrofitUrlManager)

