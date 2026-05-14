# 苍穹外卖 - 文件上传接口实现文档

## 一、整体架构

文件上传功能采用了**策略模式**的思想，支持两种上传方式：**阿里云 OSS** 和 **SFTP**。当前项目实际使用的是 SFTP 方式。

```
前端 → CommonController(/admin/common/upload) → SftpUtil → SFTP服务器
```

---

## 二、涉及文件清单

| 文件 | 作用 |
|------|------|
| `sky-server/.../controller/admin/CommonController.java` | 上传接口 Controller |
| `sky-server/.../config/StorageConfiguration.java` | 注入 SftpUtil 和 AliOssUtil Bean |
| `sky-common/.../utils/SftpUtil.java` | SFTP 上传工具类（核心逻辑） |
| `sky-common/.../utils/AliOssUtil.java` | 阿里云 OSS 上传工具类（备用） |
| `sky-common/.../properties/SftpProperties.java` | SFTP 配置属性类 |
| `sky-common/.../properties/AliOssProperties.java` | 阿里云 OSS 配置属性类（备用） |
| `sky-server/.../resources/application.yml` | 主配置文件（含 SFTP 连接信息、文件大小限制） |
| `sky-common/.../result/Result.java` | 统一响应结果类 |

---

## 三、Controller 层 — CommonController

**路径**: `sky-server/src/main/java/com/sky/controller/admin/CommonController.java`

- 接口地址: `POST /admin/common/upload`
- 参数: `MultipartFile file`（Spring MVC 自动绑定前端上传的文件）
- 返回: `Result<String>`，其中 `data` 为文件的可访问 URL

**处理流程**:

1. 从 `MultipartFile` 获取原始文件名
2. 提取文件扩展名（如 `.jpg`）
3. 用 `UUID.randomUUID()` 生成唯一文件名，拼接扩展名，防止文件名冲突
4. 调用 `sftpUtil.upload(file.getBytes(), objectName)` 上传
5. 返回上传后的访问 URL

```java
@PostMapping("/upload")
public Result<String> upload(MultipartFile file) {
    String originalFilename = file.getOriginalFilename();
    String extension = "";
    if (originalFilename != null && originalFilename.contains(".")) {
        extension = originalFilename.substring(originalFilename.lastIndexOf("."));
    }
    String objectName = UUID.randomUUID().toString() + extension;

    String url = sftpUtil.upload(file.getBytes(), objectName);
    return Result.success(url);
}
```

---

## 四、SFTP 上传工具类 — SftpUtil（核心）

**路径**: `sky-common/src/main/java/com/sky/utils/SftpUtil.java`

使用 **JSch** 库（`com.jcraft.jsch`）实现 SFTP 协议上传。

**核心方法**: `upload(byte[] bytes, String objectName)`

**执行流程**:

1. 创建 `JSch` 实例
2. 通过 `jsch.getSession(username, host, port)` 建立 SSH 会话
3. 设置密码，关闭严格主机密钥检查（`StrictHostKeyChecking=no`）
4. 调用 `session.connect(10000)` 连接（超时 10 秒）
5. 打开 SFTP 通道: `session.openChannel("sftp")`
6. 调用 `ensureDir()` 递归创建上传目录（不存在则逐级创建）
7. 拼接完整路径 `uploadDir + "/" + objectName`，调用 `channel.put()` 上传
8. 拼接访问 URL: `accessBaseUrl + "/" + objectName`（需配合 Nginx 静态文件服务）
9. `finally` 中关闭 channel 和 session

**ensureDir 方法**: 将目录按 `/` 拆分后逐级检查 `channel.stat()`，不存在则 `channel.mkdir()` 创建。

**关键依赖**: 构造函数通过 `@AllArgsConstructor` 接收 6 个参数：

| 参数 | 说明 |
|------|------|
| host | SFTP 服务器 IP |
| port | SSH 端口，默认 22 |
| username | SSH 用户名 |
| password | SSH 密码 |
| uploadDir | 文件上传目标目录 |
| accessBaseUrl | 文件访问的基础 URL（Nginx 提供） |

---

## 五、配置注入 — StorageConfiguration

**路径**: `sky-server/src/main/java/com/sky/config/StorageConfiguration.java`

Spring `@Configuration` 类，分别创建两个 Bean：

- `sftpUtil(SftpProperties)`: 从 `SftpProperties` 读取配置，构造 `SftpUtil`
- `aliOssUtil(AliOssProperties)`: 从 `AliOssProperties` 读取配置，构造 `AliOssUtil`

---

## 六、配置属性类

### SftpProperties

**路径**: `sky-common/src/main/java/com/sky/properties/SftpProperties.java`

通过 `@ConfigurationProperties(prefix = "sky.sftp")` 绑定 `application.yml` 中以 `sky.sftp` 开头的配置。

```java
@Component
@ConfigurationProperties(prefix = "sky.sftp")
@Data
public class SftpProperties {
    private String host;
    private int port = 22;
    private String username;
    private String password;
    private String uploadDir;
    private String accessBaseUrl;
}
```

### AliOssProperties (备用)

**路径**: `sky-common/src/main/java/com/sky/properties/AliOssProperties.java`

通过 `@ConfigurationProperties(prefix = "sky.alioss")` 绑定配置，当前配置文件中已注释掉。

---

## 七、配置文件

### application.yml 关键配置

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 10MB      # 单文件最大 10MB
      max-request-size: 10MB    # 单次请求最大 10MB

sky:
  sftp:
    host: 140.143.170.29
    port: 22
    username: root
    password: 116013.jm
    upload-dir: /data/blog/uploads
    access-base-url: http://140.143.170.29/uploads
```

> 说明：文件通过 SFTP 上传到服务器的 `/data/blog/uploads` 目录，通过 Nginx 将 `http://140.143.170.29/uploads` 映射到该目录，实现 HTTP 访问。

---

## 八、阿里云 OSS 工具类 — AliOssUtil（备用）

**路径**: `sky-common/src/main/java/com/sky/utils/AliOssUtil.java`

使用阿里云 OSS SDK 实现上传，逻辑与 SftpUtil 类似：

1. 通过 `OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret)` 创建客户端
2. 调用 `ossClient.putObject(bucketName, objectName, new ByteArrayInputStream(bytes))` 上传
3. 拼接返回 URL 格式: `https://{bucketName}.{endpoint}/{objectName}`
4. `finally` 中调用 `ossClient.shutdown()` 关闭客户端

当前项目未启用 OSS（配置已注释），如需切换只需：
1. 在 `application.yml` 中配置 `sky.alioss` 参数
2. 在 `CommonController` 中将 `sftpUtil` 替换为 `aliOssUtil`

---

## 九、统一响应结果 — Result

**路径**: `sky-common/src/main/java/com/sky/result/Result.java`

```java
public class Result<T> {
    private Integer code;   // 1=成功, 0=失败
    private String msg;     // 错误信息
    private T data;         // 数据
}
```

上传成功返回: `{ "code": 1, "msg": null, "data": "http://140.143.170.29/uploads/xxx.jpg" }`

---

## 十、完整调用链路图

```
HTTP POST /admin/common/upload
  │
  ├─ Spring MVC 将文件封装为 MultipartFile
  │
  ├─ CommonController.upload(file)
  │   ├─ 提取原文件名 → 取扩展名
  │   ├─ UUID 生成新文件名
  │   └─ sftpUtil.upload(file.getBytes(), objectName)
  │
  ├─ SftpUtil.upload(bytes, objectName)
  │   ├─ JSch 建立 SSH 会话
  │   ├─ 打开 SFTP Channel
  │   ├─ ensureDir(channel, uploadDir) → 递归创建目录
  │   ├─ channel.put() → 上传文件到服务器
  │   ├─ 拼接 url = accessBaseUrl + "/" + objectName
  │   └─ 返回 url
  │
  ├─ 返回 Result.success(url) 给前端
  │
  └─ 前端拿到 URL 后通过 Nginx 访问静态文件
```

---

## 十一、关键设计点

1. **文件名防冲突**: 使用 `UUID + 原扩展名` 作为新文件名，避免同名文件覆盖
2. **目录自动创建**: `ensureDir()` 逐级检查并创建不存在的目录，无需手动创建
3. **两种存储策略**: SftpUtil 和 AliOssUtil 都实现了 `upload(byte[], String)` 方法，可灵活切换
4. **文件大小限制**: 通过 `spring.servlet.multipart.max-file-size` 限制为 10MB
5. **分离存储与访问**: 上传目录与访问 URL 分离，后续可切换到 CDN 或独立静态文件服务