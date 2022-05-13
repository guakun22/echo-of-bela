package com.github.guakun22;

import com.github.zxh.classpy.classfile.ClassFile;
import com.github.zxh.classpy.classfile.ClassFileParser;
import com.github.zxh.classpy.classfile.MethodInfo;
import com.github.zxh.classpy.classfile.bytecode.Instruction;
import com.github.zxh.classpy.classfile.bytecode.InstructionCp2;
import com.github.zxh.classpy.classfile.constant.ConstantClassInfo;
import com.github.zxh.classpy.classfile.constant.ConstantFieldrefInfo;
import com.github.zxh.classpy.classfile.constant.ConstantNameAndTypeInfo;
import com.github.zxh.classpy.classfile.constant.ConstantPool;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;
import java.util.Stack;
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
    public EchoOfBela(String classPathEntries, String mainClass) {
        this.classPathEntries = classPathEntries.split(File.pathSeparator);
        this.mainClass = mainClass;
    }

    public static void main(String[] args) {
        new EchoOfBela("target/classes", "com.github.guakun22.SampleClass").start();
    }

    /**
     * 启动并运行该虚拟机
     */
    public void start() {
        // 加载主类
        ClassFile mainClassFile = loadClassFromClassPath(mainClass);

        // 加载主方法
        MethodInfo methodInfo = mainClassFile.getMethod("main").get(0);

        // 执行主方法
        Stack<StackFrame> methodStack = new Stack<>();

        Object[] localVariblesForMainStackFrame = new Object[methodInfo.getMaxStack()];
        localVariblesForMainStackFrame[0] = null;
        methodStack.push(new StackFrame(localVariblesForMainStackFrame, methodInfo, mainClassFile));

        PCRegister pcRegister = new PCRegister(methodStack);
        while (true) {
            Instruction instruction = pcRegister.getNextInstruction();

            if (Objects.isNull(instruction)) {
                break;
            }

            switch (instruction.getOpcode()) {
                case getstatic: {
                    int fieldIndex = InstructionCp2.class.cast(instruction).getTargetFieldIndex();
                    // 从栈顶元素所在的类获取常量池
                    ConstantPool constantPool = pcRegister.getTopFrameClassConstantPool();

                    ConstantFieldrefInfo fieldrefInfo = constantPool.getFieldrefInfo(fieldIndex);

                    ConstantClassInfo classInfo = fieldrefInfo.getClassInfo(constantPool);
                    ConstantNameAndTypeInfo fieldNameAndTypeInfo = fieldrefInfo.getFieldNameAndTypeInfo(constantPool);

                    String className = constantPool.getUtf8String(classInfo.getNameIndex());
                    String fieldName = fieldNameAndTypeInfo.getName(constantPool);

                    if ("java/lang/System".equals(className) && "out".equals(fieldName)) {
                        Object field = System.out;
                        pcRegister.getTopFrame().pushObjectToOperandStack(field);
                    } else {
                        throw new IllegalStateException("还没支持呢！");
                    }
                    System.out.println("fieldName = " + fieldName);
                }
                break;
                case invokestatic: {
                }
                break;
                case invokevirtual: {
                }
                break;
                case _return: {
                }
                break;
                default:
                    throw new IllegalStateException("Opcode " + instruction.getOpcode() + ", 还没被贝拉支持!");
            }
        }

        System.out.println("mainClassFile = " + mainClassFile);
    }

    static class PCRegister {
        Stack<StackFrame> methodStack;

        public PCRegister(Stack<StackFrame> methodStack) {
            this.methodStack = methodStack;
        }

        public Instruction getNextInstruction() {
            if (methodStack.isEmpty()) {
                return null;
            }
            StackFrame frameAtTop = methodStack.peek();
            return frameAtTop.getNextInstruction();
        }

        public StackFrame getTopFrame() {
            return methodStack.peek();
        }

        public ConstantPool getTopFrameClassConstantPool() {
            return getTopFrame().getClassFile().getConstantPool();
        }
    }

    static class StackFrame {
        Object[] localVariables;

        Stack<Object> operandStack = new Stack<>();

        MethodInfo methodInfo;

        ClassFile classFile;

        public StackFrame(Object[] localVariables, MethodInfo methodInfo, ClassFile classFile) {
            this.localVariables = localVariables;
            this.methodInfo = methodInfo;
            this.classFile = classFile;
        }

        public ClassFile getClassFile() {
            return classFile;
        }

        int currentInstructionIndex = 0;

        public Instruction getNextInstruction() {
            return methodInfo.getCode().get(currentInstructionIndex++);
        }

        public void pushObjectToOperandStack(Object object) {
            operandStack.push(object);
        }
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
