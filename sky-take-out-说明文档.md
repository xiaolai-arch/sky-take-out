# 苍穹外卖

## 后端环境配置

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

