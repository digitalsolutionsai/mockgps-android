# 商店详情 — 简体中文 (zh-CN)

## 应用名称 (≤ 30)
MockGPS – 模拟位置工具

## 简短说明 (≤ 80)
模拟 GPS、路线和摇杆移动，测试基于位置的应用。无广告。

## 详细说明 (≤ 4000)
MockGPS 是面向 Android 开发者和 QA 测试人员的位置模拟工具。选择一个地点、播放一条路线，或用摇杆操控，设备上的所有应用都会看到该位置：GPS、网络以及 Google Play services 的 Fused 位置提供程序都会报告同一位置，因此应用不会跳回你的真实位置。

地点
• 长按地图、搜索地址、粘贴坐标或 Google Maps 链接
• 将 Google Maps 中的地点直接分享到 MockGPS
• 保存收藏，一键跳转

路线
• 添加途经点，沿真实道路（步行、自行车、摩托车、汽车）或直线移动
• 导入 GPX 轨迹
• 设置速度、暂停和继续，并可选择到达终点后停止、循环或往返

摇杆
• 可悬浮在任意应用之上的摇杆，支持从步行到驾车的各种速度

专为开发者打造
• 完整的 adb 控制：在电脑或 CI 脚本中开始、移动、播放路线、调整速度和停止
• 可调整精度、海拔、更新频率和自然抖动
• 可选择要模拟的提供程序：GPS、网络、Fused
• 开发者选项分步设置向导，以及一行命令的 adb 设置

注重隐私
• 无广告、无统计分析、无账号，也没有我们自己的服务器
• 收藏和设置仅保存在你的设备上
• 地图由 OpenFreeMap 和 OpenStreetMap 提供；路线规划使用 OSRM（可使用自己的服务器）

注意事项
• 需要开启开发者选项，并将 MockGPS 选为“模拟位置信息应用”。
• Android 会将模拟位置标记为模拟（Location.isMock），因此会检查该标记的应用能够发现。MockGPS 仅用于测试你自己的应用，请勿用于违反其他服务的条款。

地图数据 © OpenStreetMap contributors。问题或反馈：loivo@dsai.vn
GitHub: https://github.com/digitalsolutionsai/mockgps-android
