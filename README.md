## HttpRequestMediator

基于`Retofit`与`OkHttp`封装的网络请求库，利用kotlin特性简化网络请求api调用。

- 支持RxJava3
- 支持管理多种http请求配置
- 动态替换baseUrl
- 文件下载

## 项目导入





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

- 管理多种`Okhttp`与`Retrofit`配置，根据需要使用特定配置
- 支持动态切换BaseUrl

### 多http配置管理

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
}
```

> - 默认添加了**日志打印**拦截器（网络层）、**动态替换baseUrl**的拦截器（应用层）。
>
> - 默认仅支持gson解析，`RxJava3`回调支持（kotlin协程自带支持，不需要额外添加）



### 动态替换baseUrl

核心类为`UrlRedirectHelper`，管理所有需要替换的baseUrl

**stage 1**

需要通过`UrlRedirectHelper.putRedirectUrl`添加对应重定向url键值对映射。

**stage 2**

在网络请求API声明处，添加`@Headers("${REDIRECT_HOST_HEAD_PREFIX}${URL_KEY}")`请求头

``` kotlin
@Headers("${UrlRedirectHelper.REDIRECT_HOST_HEAD_PREFIX}user")
@GET("v1/user/get")
fun getUserData(...)
```

#### 替换策略

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
  
  

可通过**实现`UrlReplaceable`接口创建新的url替换策略**，并调用`UrlRedirectHelper.setUrlReplacer`方法设置新替换策略

#### httpUrl文段替换规则

>
> size = 0，表示只替换域名，新文段拼接在原始请求文段之前
> * 原始请求url： `https://www.baidu.com/user/sss?user_id=111` ,
> * 重定向baseUrl：`https://www.google.com/test1/list/`,
> * 新url：`https://www.google.com/user/test1/list/sss?user_id=111`,
>
> size = 1，只替换域名后的第一个文段。
>
> * 原始请求url：`https://www.baidu.com/user/sss?user_id=111`,
> * 重定向baseUrl：`https://www.google.com/test1/list/`,
> * 新url：`https://www.google.com/test1/list/sss?user_id=111`,
>
> 以此类推...



## TODO

- 简化`kotlin`协程的使用，提取通用协程response处理

- 优化下载功能，尝试添加断点续传

  

## thanks

[RetrofitUrlManager](https://github.com/JessYanCoding/RetrofitUrlManager)

