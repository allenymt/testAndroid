# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述
这是一个 Android 测试项目，用于演示和测试各种 Android 开发技术和功能。主包名为 `com.vdian.android.lib.testforgradle`。

## 构建和运行命令

### 基本构建命令
```bash
./gradlew assembleDebug    # 构建Debug版本
./gradlew assembleRelease  # 构建Release版本
./gradlew clean           # 清理构建目录
```

### 测试命令
```bash
./gradlew testDebug       # 运行单元测试
./gradlew connectedAndroidTest  # 运行仪器测试
```

### 其他有用命令
```bash
./gradlew lint            # 代码检查
./gradlew dependencies    # 查看依赖关系
./gradlew testTask1       # 自定义测试任务
```

## 技术栈和依赖关键信息

### 核心版本
- Kotlin: 1.6.10
- Android Gradle Plugin: 4.1.3
- compileSdkVersion: 31
- minSdkVersion: 21
- targetSdkVersion: 31

### 主要架构组件
- **Jetpack组件**: Lifecycle, ViewModel, LiveData, DataBinding, ViewBinding, Room, Paging3, WorkManager, DataStore
- **协程**: kotlinx-coroutines-android (1.3.9)
- **网络**: Retrofit (2.9.0), OkHttp logging
- **UI框架**: Material Design (1.4.0)

### 特殊配置
- 启用了 DataBinding 和 ViewBinding
- 配置了 NoArg 插件用于数据类
- 使用了 PrivacySentry 隐私保护框架
- 支持 MultiDex

## 代码架构

### 主要功能模块
项目按功能模块组织，位于 `app/src/main/java/com/vdian/android/lib/testforgradle/` 下：

- **ktl/**: Kotlin 语言特性测试
  - `coroutines/`: 协程相关测试和示例
  - `collections/`: 集合操作测试
  - `extensions/`: 扩展函数测试
- **surface/**: Surface 和 OpenGL 相关功能
- **room/**: Room 数据库测试
- **pageing3/**: Paging 3 分页测试
- **datastore/**: DataStore 数据存储测试
- **workmanager/`: WorkManager 后台任务测试
- **binder/**: AIDL 和 Binder 通信测试
- **nfc/**: NFC 功能测试
- **thread/**: 多线程和并发测试
- **memory/**: 内存管理测试
- **rom/**: 设备ROM检测和模拟器检测

### 入口点
- **App.java**: Application 类，处理全局初始化
- **MainActivity.java**: 主活动，包含所有功能模块的导航入口

### 测试架构
- 单元测试位于 `src/test/java/`
- 仪器测试位于 `src/androidTest/java/`
- 使用 JUnit 4.12 作为测试框架

## 开发注意事项

### 签名配置
项目配置了自定义签名，使用 `../privacy` 文件作为keystore。

### 权限要求
应用需要以下关键权限：
- READ_PHONE_STATE
- ACCESS_NETWORK_STATE  
- CAMERA
- NFC 相关权限

### 特殊功能
- 支持全面屏适配 (Android P+)
- 实现了系统按键拦截 (Home、Back键)
- 包含防止应用被杀机制
- 支持多进程和AIDL通信

### 调试和日志
- 使用自定义 LogUtil 进行日志管理
- 支持线程dump分析
- 包含内存泄漏检测工具