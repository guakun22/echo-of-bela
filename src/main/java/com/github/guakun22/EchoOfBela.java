package com.github.guakun22;

import com.github.zxh.classpy.classfile.ClassFile;
import com.github.zxh.classpy.classfile.ClassFileParser;
import com.github.zxh.classpy.classfile.MethodInfo;
import com.github.zxh.classpy.classfile.bytecode.Instruction;
import com.github.zxh.classpy.classfile.bytecode.InstructionCp2;
import com.github.zxh.classpy.classfile.bytecode.Sipush;
import com.github.zxh.classpy.classfile.constant.*;

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
                    ConstantPool constantPool = pcRegister.getTopFrameClassConstantPool();
                    ConstantFieldrefInfo fieldrefInfo = constantPool.getFieldrefInfo(fieldIndex);
                    ConstantClassInfo classInfo = fieldrefInfo.getClassInfo(constantPool);
                    ConstantNameAndTypeInfo nameAndTypeInfo = fieldrefInfo.getFieldNameAndTypeInfo(constantPool);

                    String className = constantPool.getUtf8String(classInfo.getNameIndex());
                    String fieldName = nameAndTypeInfo.getName(constantPool);

                    if ("java/lang/System".equals(className) && "out".equals(fieldName)) {
                        Object field = System.out;
                        pcRegister.getTopFrame().pushObjectToOperandStack(field);
                    } else {
                        throw new IllegalStateException("还没支持呢！");
                    }
                }
                break;
                case invokestatic: {
                    String className = getClassNameFromInvokeInstruction(instruction, pcRegister.getTopFrameClassConstantPool());
                    String methodName = getMethodNameFromInvokeInstruction(instruction, pcRegister.getTopFrameClassConstantPool());

                    ClassFile classFile = loadClassFromClassPath(className);
                    MethodInfo targetMethodInfo = classFile.getMethod(methodName).get(0);

                    Object[] targetLocalVariables = new Object[targetMethodInfo.getMaxStack()];
                    StackFrame newFrame = new StackFrame(targetLocalVariables, targetMethodInfo, classFile);

                    // TODO 应该分析方法的参数，从操作数栈上弹出对应数量的参数放在新栈帧的局部变量表中
                    methodStack.push(newFrame);
                }
                break;
                case sipush: {
                    Sipush sipush = (Sipush) instruction;
                    pcRegister.getTopFrame().pushObjectToOperandStack(sipush.getOperand());
                }
                break;
                case ireturn: {
                    Object returnValue = pcRegister.getTopFrame().popFromOperandStack();
                    pcRegister.popFrameFromMethodStack();
                    pcRegister.getTopFrame().pushObjectToOperandStack(returnValue);
                }
                break;
                case invokevirtual: {
                    String className = getClassNameFromInvokeInstruction(instruction, pcRegister.getTopFrameClassConstantPool());
                    String methodName = getMethodNameFromInvokeInstruction(instruction, pcRegister.getTopFrameClassConstantPool());

                    if ("java/io/PrintStream".equals(className) && "println".equals(methodName)) {

                        Object param = pcRegister.getTopFrame().popFromOperandStack();
                        Object thisObject = pcRegister.getTopFrame().popFromOperandStack();

                        System.out.println(param);
                    } else {
                        throw new IllegalStateException("贝拉还没支持呢！");
                    }
                }
                break;
                case _return: {
                    pcRegister.popFrameFromMethodStack();
                }
                break;
                default:
                    throw new IllegalStateException("Opcode " + instruction.getOpcode() + ", 还没被贝拉支持!");
            }
        }
    }

    private String getClassNameFromInvokeInstruction(Instruction instruction, ConstantPool constantPool) {
        int methodIndex = InstructionCp2.class.cast(instruction).getTargetMethodIndex();
        ConstantMethodrefInfo methodrefInfo = constantPool.getMethodrefInfo(methodIndex);
        ConstantClassInfo classInfo = methodrefInfo.getClassInfo(constantPool);
        return constantPool.getUtf8String(classInfo.getNameIndex());
    }

    private String getMethodNameFromInvokeInstruction(Instruction instruction, ConstantPool constantPool) {
        int methodIndex = InstructionCp2.class.cast(instruction).getTargetMethodIndex();
        ConstantMethodrefInfo methodrefInfo = constantPool.getMethodrefInfo(methodIndex);
        ConstantClassInfo classInfo = methodrefInfo.getClassInfo(constantPool);
        return methodrefInfo.getMethodNameAndType(constantPool).getName(constantPool);
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

        public void popFrameFromMethodStack() {
            methodStack.pop();
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

        public Object popFromOperandStack() {
            return operandStack.pop();
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
