package com.github.guakun22;

import com.github.zxh.classpy.classfile.ClassFile;
import com.github.zxh.classpy.classfile.ClassFileParser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * 哈利波特中的贝拉，每个贝拉将模拟一个简陋的 JVM
 */
public class EchoOfBela {

    private String mainClass;

    private String[] classPathEntries;

    /**
     * 贝拉：一个迷你 JVM，使用指定的 classpath 和 main class
     *
     * @param mainClass        主类的全限定类名
     * @param classPathEntries 启动时的 classpath 使用 {@link java.io.File#pathSeparator} 分割，支持文件夹
     */
    public EchoOfBela(String mainClass, String classPathEntries) {
        this.mainClass = mainClass;
        this.classPathEntries = classPathEntries.split(File.pathSeparator);
    }

    public static void main(String[] args) {
        new EchoOfBela("target/classes", "com.github.guakun22.SimpleClass").start();
    }

    /**
     * 启动并运行该虚拟机
     */
    public void start() {
        // 加载主类
        ClassFile mainClassFile = loadClassFromClassPath(mainClass);

        // 加载主方法

        // 执行主方法
    }

    private ClassFile loadClassFromClassPath(String fqcn) {
        return Stream.of(classPathEntries)
                .map(entry -> tryLoad(entry, fqcn))
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new RuntimeException(new ClassNotFoundException(fqcn)));
    }

    private ClassFile tryLoad(String entry, String fqcn) {
        try {
            byte[] bytes = Files.readAllBytes(
                    new File(entry, fqcn.replace('.', '/') + ".class").toPath()
            );
            return new ClassFileParser().parse(bytes);
        } catch (IOException e) {
            throw null;
        }
    }
}
