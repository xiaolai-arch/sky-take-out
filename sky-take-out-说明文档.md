# 苍穹外卖
### 项目结构
#### 模块划分--整体
1. sky-take-out maven父工程，统一管理依赖版本，聚合其他子模块
2. sky-common 子模块，存放公共类，eg：工具类、常量类、异常类。
3. sky-server 子模块，后端服务，存放配置文件、Controller、Service、Mapper等。
   1. 存放配置文件、配置类、拦截器、controller、service、mapper、启动类等。
4. sky-pojo 子模块，存放实体类。
   1. Entity 实体，通常和数据库中表对应
   2. DTO数据传输对象，通常用于程序各层之间传递数据
   3. VO视图对象，为前端展示数据提供的对象
   4. POJO普通java对象，只有数学和对应的getter和setter

---

## 环境依赖

| 组件 | 版本 | 路径/说明 |
|------|------|-----------|
| JDK | Oracle JDK 21.0.11 | `/Library/Java/JavaVirtualMachines/jdk-21.jdk` |
| Maven | 3.9.15 | `/usr/local/Cellar/maven/3.9.15` |
| MySQL | 8.0.46 | `/usr/local/mysql-8.0.46-macos15-x86_64/bin/mysql` |
| Redis | - | `localhost:6379`（需密码认证） |
| nginx | 1.29.8 | `/usr/local/opt/nginx/bin/nginx`（Homebrew 安装） |
| Spring Boot | 2.7.3 | 项目父工程 |

---

## 数据库环境搭建

### MySQL 连接信息

| 配置项 | 值 |
|--------|-----|
| 主机 | localhost |
| 端口 | 3306 |
| 数据库名 | sky_take_out |
| 用户名 | root |
| 密码 | 116013.jm |

配置文件：`sky-server/src/main/resources/application-dev.yml`

通过数据库建表语句创建数据库结构（数据库 `sky_take_out` 已存在）。

---

## 后端启动

### 启动类
`sky-server/src/main/java/com/sky/SkyApplication.java`

### 运行端口
`8080`

### Maven 构建命令
```bash
# 使用 JDK 21 编译（重要：Maven 默认可能指向其他 JDK 版本）
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
mvn clean install -DskipTests
```

### 启动命令
```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
cd sky-server && mvn spring-boot:run
```

或在 IDEA 中运行 SkyApplication main 方法（需将项目 SDK 设为 JDK 21）。

### 关键配置项

| 配置 | 值 | 说明 |
|------|-----|------|
| 服务端口 | 8080 | `application.yml` - server.port |
| 数据库驱动 | com.mysql.cj.jdbc.Driver | Druid 连接池 |
| 数据库连接池 | Druid 1.2.1 | Alibaba |
| MyBatis 驼峰映射 | true | 自动下划线转驼峰 |
| JWT 秘钥 | itcast | `application.yml` - sky.jwt.admin-secret-key |
| JWT 过期时间 | 7200000ms（2小时） | sky.jwt.admin-ttl |
| JWT 令牌名 | token | 前端请求头中的令牌名称 |

---

## 前端

### 文件位置
`nginx-1.20.2/html/sky/`（Vue.js SPA 编译产物）

### 访问地址
**http://localhost:8088**

### nginx 配置

| 配置项 | 值 |
|--------|-----|
| 监听端口 | 8088 |
| 前端根路径 | `/Users/mac/IdeaProjects/sky-take-out/nginx-1.20.2/html/sky` |
| API 代理 | `/api/` → `localhost:8080/admin/` |
| 用户端代理 | `/user/` → `localhost:8080/user/`（upstream webservers） |
| WebSocket | `/ws/` → 后端，HTTP 升级 + 3600s 超时 |
| 配置文件 | `/usr/local/etc/nginx/nginx.conf` |

### nginx 操作命令
```bash
# 测试配置
/usr/local/opt/nginx/bin/nginx -t

# 启动
/usr/local/opt/nginx/bin/nginx

# 重新加载
/usr/local/opt/nginx/bin/nginx -s reload

# 停止
/usr/local/opt/nginx/bin/nginx -s stop
```

### 请求链路

```
浏览器 http://localhost:8088
  │
  ├─ /index.html、/js/*、/css/*  → nginx 返回前端静态文件
  │
  ├─ /api/xxx     → nginx 反向代理 → localhost:8080/admin/xxx（后端）
  ├─ /user/xxx    → nginx 反向代理 → localhost:8080/user/xxx（后端）
  └─ /ws/         → nginx WebSocket 代理 → 后端
```

---

## 登录信息

- 访问地址：http://localhost:8088
- 默认账号：admin / 123456

---

## 本次环境搭建变更记录

| 变更 | 文件 | 说明 |
|------|------|------|
| 数据库密码 | `application-dev.yml` | `root` → `116013.jm` |
| Lombok 升级 | `pom.xml` | `1.18.20` → `1.18.36`（兼容 JDK 21） |
| Lombok scope | 3 个子模块 pom.xml | 添加 `<scope>provided</scope>` |
| 编译器插件 | `pom.xml` | 新增 maven-compiler-plugin 3.11.0 + Lombok 注解处理器路径 |
| nginx 端口 | `/usr/local/etc/nginx/nginx.conf` | `80` → `8088`（避免 sudo） |
| nginx 前端路径 | `/usr/local/etc/nginx/nginx.conf` | 改为绝对路径指向 `html/sky` |

#### 完善登陆功能

1. 将密码进行加密存储提升安全性
2. 使用MD5加密方式对明文密码加密
```
  // 对前端明文密码进行md5加密处理
  password = DigestUtils.md5DigestAsHex(password.getBytes());
```

#### 导入接口文档

将课程文件提供的项目接口导入YApi。

---                                                                                                                                                                 
第一步：注册/登录 Apifox

访问 https://app.apifox.com，用手机号、邮箱或微信扫码注册登录。网页版即可，无需下载客户端。

---                                                                                                                                                                 
第二步：创建团队和项目

1. 登录后，左上角点击 "新建团队"（如果还没有团队的话），输入团队名称如"苍穹外卖"
2. 进入团队后，点击 "新建项目"
3. 项目名称填入如"苍穹外卖接口"，点击确定

  ---                                                                                                                                                                 
第三步：导入 JSON 文件

1. 进入你创建的项目
2. 点击右上角的 "导入" 按钮（或左侧菜单找到"数据管理 → 导入"）
3. 选择 "手动导入"
4. 导入方式选择 "YApi"

    - 分别选择两个 JSON 文件：                                                                                                                                        
        - 苍穹外卖-管理端接口.json                                                                                                                                    
      - 苍穹外卖-用户端接口.json                                                                                                                                      
5. 导入模式建议选 "智能合并"，点击确定

  ---                                                                                                                                                                 
第四步：查看和调试接口

导入完成后，左侧会显示接口分组：

- "分类相关接口" — 管理端分类 CRUD
- "C端-分类接口" — 用户端分类查询
- 其他接口分组…

点击任意接口，右边会显示：
- 接口路径、请求方法（GET/POST/PUT/DELETE）
- 请求参数（Query、Body、Header）
- 响应示例
- 点击 "运行" 可以直接调试接口，填入参数和地址发请求

---                                                                                                                                                                 
第五步（可选）：设置环境

如果后续要联调，可以在 "环境管理" 里配置：
- Base URL：如 http://localhost:8080
- 公共 Header：如 token 等

---  

#### Swagger介绍和使用方法

介绍：使用Swagger你需要按照他的规范去定义接口相关信息，就可以生成接口文档，以及在线接口测试页面。
官网：https://swagger.io/
Knife4j是Java MVC框架集成Swagger生成Api文档的增强解决方案

导入依赖
```xml
   <dependency>
       <groupId>com.github.xiaoymin</groupId>
       <artifactId>knife4j-spring-boot-starter</artifactId>
       <version>${knife4j}</version>
   </dependency>
```

##### 使用方式

1. 导入Kinfe4j的maven的坐标
2. 再配置类中加入knife4j相关配置
3. 设置静态资源映射，否则接口文档页面无法访问

```java
    /**
     * 通过knife4j生成接口文档
     * @return
     */
    @Bean
    public Docket docket() {
        ApiInfo apiInfo = new ApiInfoBuilder()
                .title("苍穹外卖项目接口文档")
                .version("2.0")
                .description("苍穹外卖项目接口文档")
                .build();
        Docket docket = new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo)
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.sky.controller"))
                .paths(PathSelectors.any())
                .build();
        return docket;
    }

         /**
          * 设置静态资源映射
          * @param registry
          */
         protected void addResourceHandlers(ResourceHandlerRegistry registry) {
            registry.addResourceHandler("/doc.html").addResourceLocations("classpath:/META-INF/resources/");
            registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
         }
```

访问地址：http://localhost:8080/doc.html

#### 常用注解

@Api：修饰整个类，描述Controller的作用
@ApiModel：用在类上，例如entity，DTO，VO
@ApiModelProperty：用在属性、getter、setter方法上，描述这个属性的含义，以及是否必填
@ApiOperation：用在方法上，描述这个方法的作用

## 员工管理

#### 一、新增员工

需求分析和设计：
- 录入：账号纯数字
- 员工姓名
- 手机号：11位数字
- 性别：0-女，1-男
- 身份证号：合法18位身份证号码
- 默认密码123456、

post请求，json数据格式

本项目约定：
- 管理端发出的请求，统一使用/admin做前缀
- 用户端发出的请求，统一使用/user做前缀
数据库设计（employee表）

1. controller层
```java
    /**
     * 新增员工
     * @param employeeDTO
     * @return
     * */
    @PostMapping
    @ApiOperation(value = "新增员工")
    public Result save(@RequestBody EmployeeDTO employeeDTO) {
        log.info("新增员工，员工数据：{}", employeeDTO);
        employeeService.save(employeeDTO);
        return Result.success();
    }
```

2. service层：讲前端传入数据保存在数据库
```java
    public void save(EmployeeDTO employeeDTO){
        Employee employee = new Employee();

        // 对象的属性的拷贝,从前面拷贝到后面
        // 属性名称需要一致
        BeanUtils.copyProperties(employeeDTO, employee);

        // 设置账号状态
        employee.setStatus(StatusConstant.ENABLE);

        // 设置密码，默认密码
        employee.setPassword(DigestUtils.md5DigestAsHex(DEFAULT_PASSWORD.getBytes()));

        // 创建时间
        employee.setCreateTime(LocalDateTime.now());

        // 修改时间
        employee.setUpdateTime(LocalDateTime.now());

        // 设置创建人的ID
        employee.setCreateUser(BaseContext.getCurrentId());
        employee.setUpdateUser(BaseContext.getCurrentId());

        //
        employeeMapper.insert(employee);
    }
```

3. mapper层
```java
    /**
     * 插入员工数据
     * */
    @Insert("insert into employee (username, name, password, phone, sex, id_number, status, create_time, update_time, create_user, update_user) " +
            "values " +
            "(#{username}, #{name}, #{password}, #{phone}, #{sex}, #{idNumber}, #{status}, #{createTime}, #{updateTime}, #{createUser}, #{updateUser})")
    void insert(Employee employee);
```

4. 处理重复员工账户
```java
    /**
     * 处理sql异常
     * */
    @ExceptionHandler
    public Result exceptionHandler(SQLIntegrityConstraintViolationException ex){
        String message = ex.getMessage();
        log.error("异常信息：{}", ex.getMessage());
        if (message.contains("Duplicate entry")){
            String[] split = message.split(" ");
            String msg = split[2] + MessageConstant.ALREADY_EXISTS;
            return Result.error(msg);
        }else {
            return Result.error(MessageConstant.UNKNOWN_ERROR);
        }
    }
```

5. 线程内存储ID，JWT解析出ID

```
BaseContext.setCurrentId(empId);
```

```
// 设置创建人的ID
employee.setCreateUser(BaseContext.getCurrentId());
employee.setUpdateUser(BaseContext.getCurrentId());
```