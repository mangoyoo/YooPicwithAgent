## 项目介绍
网站地址： [**https://www.yoopic.space/**](https://www.yoopic.space/)

![](https://cdn.nlark.com/yuque/0/2025/png/26568050/1753111055232-26d87332-a692-4c12-90e0-43f11d21ce7e.png)

基于 Spring Boot、Vue、Spring AI、Spring AI Alibaba、Redis、RAG、WebSocket 的云端智能体图片素材库。平台包含公共图库、私有图库、团队共享图库和AI智能体四大模块。支持用户公开上传检索图片管理员审核及批量上传，私有空间批量管理，AI扩图，企业团队空间实时协同编辑。用户可通过智能体助手对话快速找到站内外素材，整合文字、图片资源并生成可视化网页。

## 项目架构图
![](https://cdn.nlark.com/yuque/0/2025/png/26568050/1753097638698-1089e069-f917-47a2-a5e2-64b003b2224c.png)

## YooPic-AI模块
![](https://cdn.nlark.com/yuque/0/2025/png/26568050/1753101322507-bdd4c5af-527a-40d2-91f3-94da23653e09.png)

### YooPic-Agent部分
本部分基于Spring AI、Spring AI Alibaba框架，采用ReAct模式，使用本地自定义工具结合外部MCP工具服务，**实现了一个能将复杂任务拆解成多个步骤解决的一个智能体**。特点是通过与**智能体的对话就能能够迅速找到符合条件的本站资源**。更深入地：结合自定义工具以及接入外部MCP服务，**能够迅速地将本站（YooPic）的资源和外部资源整合并且以可视化页面的形式呈现给用户。**本项目**采用了多专家工作流**，**针对特定的复杂任务会采用不同的垂直领域LLM专家**，比如需要生成代码时，就会让Code Expert进行代码生成的子任务。

#### ReAct模式架构图
![](https://cdn.nlark.com/yuque/0/2025/png/26568050/1753101281077-ca036335-11a1-4115-ba8a-75ec6e429466.png)

#### 复杂任务示例
> 帮我看看广州附近适合拍照打卡的徒步路线，帮我写一份攻略。应该包含徒步路线的图文介绍，不仅要包含丰富的文字介绍信息，还可以根据对应的徒步路线名称可以去网上搜索相关图片，还有该城市的天气信息，还可以适当插入一些本站的类别为表情包图片。最终把这份攻略内容整合以的html文件格式给我。
>

#### 复杂任务示例结果图
![](https://cdn.nlark.com/yuque/0/2025/png/26568050/1753108884964-9c3130d4-c14f-482e-8694-d57d38b1187a.png)



YooPic-Agent返回结果所提供的页面：[https://www.yoodns.yoopic.space/html/2025-07-21_y2rfeqXdAPem7xvA.html](https://www.yoodns.yoopic.space/html/2025-07-21_y2rfeqXdAPem7xvA.html)
<img width="1867" height="4398" alt="image" src="https://github.com/user-attachments/assets/de7e6d7c-f58a-4c5f-9ae9-1b62061e12b9" />

![](https://cdn.nlark.com/yuque/0/2025/png/26568050/1753109050363-78829596-be72-43f2-841d-fbd045db35bb.png)

如上图结果图所示，这个这个任务很好地体现了YooPic-Agent将站内站，站外资源各方位相关素材检索并整合的能力。

### 智能图像顾问部分，
这部分是一个集成多个工具以及专业知识库的图像分析专家，同时也是你快速获取本站相关素材的得力帮手！

example1：识别图片并根据知识库分析图片

![](https://cdn.nlark.com/yuque/0/2025/png/26568050/1753109556417-4bfcbc0f-f25f-476d-8d85-3ab8756613de.png)

example2：按照上传图片的色系来寻找本站中指定数量的类似色系风格的的图片

![](https://cdn.nlark.com/yuque/0/2025/png/26568050/1753109715462-0f625380-ecf8-4939-8053-bc023839f10a.png)

返回图片的连接：[https://www.yoodns.yoopic.space/public/1920742554046758914/2025-05-13_9XGm72FICllkEcLL.webp](https://www.yoodns.yoopic.space/public/1920742554046758914/2025-05-13_9XGm72FICllkEcLL.webp)

![](https://cdn.nlark.com/yuque/0/2025/png/26568050/1753109786913-eb3ac415-fd45-4c81-b0a6-915fc033e566.png)



example3：按照类别关键词描述来寻找指定数量的本站的图片

![](https://cdn.nlark.com/yuque/0/2025/png/26568050/1753109864953-f1bc6542-674c-44cf-97f5-e5a91880b7dd.png)

返回图片的链接：[https://www.yoodns.yoopic.space/public/1920742554046758914/2025-05-09_DEBAfVS2ityusx3Y.webp](https://www.yoodns.yoopic.space/public/1920742554046758914/2025-05-09_DEBAfVS2ityusx3Y.webp)

![](https://cdn.nlark.com/yuque/0/2025/png/26568050/1753109888659-0383c378-207c-4d41-9df8-eb608745b119.png)

example4：按照关键词在网络上搜索指定数量的图片

![](https://cdn.nlark.com/yuque/0/2025/png/26568050/1753110058294-87428ceb-a639-4303-8b52-b59a8feb4240.png)

返回结果链接：[https://file1.shop265.com/tk/2019/e04f40ad4ea622d6bca3fa7e54767ad1.jpg](https://file1.shop265.com/tk/2019/e04f40ad4ea622d6bca3fa7e54767ad1.jpg)



![](https://cdn.nlark.com/yuque/0/2025/png/26568050/1753110083445-c4c89785-03e1-4266-bcc5-94ca17ccccbe.png)





### 利用WebSocket实现团队空间中协同编辑图片的功能
![](https://cdn.nlark.com/yuque/0/2025/gif/26568050/1753146428803-d97e065a-f736-4d71-a8cd-396548b7b83c.gif)







以下为其它业务模块功能

### 用户模块
用户注册

用户登录

![](https://cdn.nlark.com/yuque/0/2025/png/26568050/1753110319369-6b8c5a70-fed3-44f3-9b4d-e13974c248cb.png)

用户中心

![](https://cdn.nlark.com/yuque/0/2025/png/26568050/1753110524298-e429aa0b-7c31-4e16-964b-091585f42f84.png)

分为普通用户和管理员两种角色

### 图片模块
普通上传图片（本地上传）

![](https://cdn.nlark.com/yuque/0/2025/png/26568050/1753110626292-1d6b762a-3a0c-4ffa-8f7c-aa95e2780af9.png)

普通上传图片（url上传）

![](https://cdn.nlark.com/yuque/0/2025/png/26568050/1753110638910-ab02d2c0-4af0-4489-ab6f-74e9ca055028.png)

管理员审核图片、管理图片

![](https://cdn.nlark.com/yuque/0/2025/png/26568050/1753110675786-d117a479-8864-4b12-bbf2-8a37b624e54d.png)

管理员批量上传图片（根据关键词搜索批量上传）

![](https://cdn.nlark.com/yuque/0/2025/png/26568050/1753110752145-f66a1bb1-8753-4cda-9ec1-dcc23bc096dc.png)



AI扩图

![](https://cdn.nlark.com/yuque/0/2025/png/26568050/1753111439932-ee094ab0-0788-4763-ad41-dc2505227c42.png)

### 空间模块




个人空间上传图片，管理图片，按照类别搜索图片，按照颜色搜索图片

![](https://cdn.nlark.com/yuque/0/2025/png/26568050/1753111566309-960700d7-3563-4815-a828-d8ffbff10d69.png)



团队空间上传图片

![](https://cdn.nlark.com/yuque/0/2025/png/26568050/1753111746659-f4d5cb80-e26e-4030-8c0e-93ba7172042b.png)

添加团队成员

![](https://cdn.nlark.com/yuque/0/2025/png/26568050/1753111845089-f37855e6-3130-47ce-8181-b4ee725f1b10.png)



## 


## 


