package com.github.guakun22;

/**
 * 哈利波特中的贝拉，每个贝拉将模拟一个简陋的 JVM
 */
public class EchoOfBela {

    private String mainClass;

    private String[] classPathEntries;

    /**
     * 贝拉：一个迷你 JVM，使用指定的 classpath 和 main class
     *
     * @param mainClass 主类的全限定类名
     *
     * @param classPathEntries 启动时的 classpath 使用 {@link java.io.File#pathSeparator} 分割，支持文件夹
     */
    public EchoOfBela(String mainClass, String[] classPathEntries) {
        this.mainClass = mainClass;
        this.classPathEntries = classPathEntries;
    }

    public static void main(String[] args) {
    }
}
