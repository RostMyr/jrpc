package com.github.rostmyr.jrpc.fibers.bytecode;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.util.*;

import com.github.rostmyr.jrpc.common.utils.Contract;
import com.github.rostmyr.jrpc.fibers.Fiber;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.Future;

import static java.util.stream.Collectors.toList;
import static org.objectweb.asm.ClassWriter.COMPUTE_FRAMES;
import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

/**
 * Rostyslav Myroshnychenko
 * on 11.06.2018.
 */
public class FiberClassNodeAdapter extends ClassNode {
    private static final String AWAIT_FIBER_DESC =
        getMethodDescriptor(getType(int.class), getType(Fiber.class));
    private static final String AWAIT_FUTURE_DESC =
        getMethodDescriptor(getType(int.class), getType(Future.class));
    private static final String INTERNAL_WITH_LITERAL_ARG =
        getMethodDescriptor(getType(int.class), getType(Object.class));
    private static final String INTERNAL_NOTHING = getMethodDescriptor(getType(int.class));

    private static String FIBER_CLASS_NAME = getInternalName(Fiber.class);
    private static String FIBER_RETURN_TYPE = getDescriptor(Fiber.class);

    private FiberTransformerResult result;
    private boolean debug;
    private boolean isInstrumented;

    FiberClassNodeAdapter(ClassVisitor cv, boolean debug, FiberTransformerResult result) {
        super(ASM6);
        this.result = result;
        if (debug) {
            this.cv = new TraceClassVisitor(cv, new PrintWriter(System.out));
        } else {
            this.cv = cv;
        }
    }

    boolean isInstrumented() {
        return isInstrumented;
    }

    @Override
    public void visitEnd() {
        List<MethodNode> methodsForInstrumentation = methods.stream()
            .filter(method -> getReturnType(method.desc).getDescriptor().equals(FIBER_RETURN_TYPE))
            .filter(FiberClassNodeAdapter::hasTerminateMethodInvocation)
            .collect(toList());

        // generate inner classes
        for (MethodNode method : methodsForInstrumentation) {
            String innerClassName = name + "$" + method.name + "_Fiber";
            String fiberClassName = getInternalName(Fiber.class);
            String methodReturnType = method.signature.substring(method.signature.indexOf(")") + 1);

            visitInnerClass(innerClassName, name, method.name + "_Fiber", ACC_PUBLIC);
            ClassWriter cw = new ClassWriter(COMPUTE_FRAMES);
            cw.visit(V1_8, ACC_PUBLIC + ACC_SUPER, innerClassName, methodReturnType, fiberClassName, null);
            cw.visitInnerClass(innerClassName, name, method.name + "_Fiber", ACC_PUBLIC);
            insertInnerClass(cw, innerClassName, method);
            insertUpdateMethod(innerClassName, method);
            cw.visitEnd();

            ClassReader cr = new ClassReader(cw.toByteArray());
            if (debug) {
                cr.accept(new CheckClassAdapter(new TraceClassVisitor(new PrintWriter(System.out))), 0);
            }

            String className = this.name.substring(this.name.lastIndexOf("/") + 1) + "$" + method.name + "_Fiber";
            result.addFiber(className, cw.toByteArray());
        }

        // replace original method with a constructor invocation of a new fiber
        for (MethodNode m : methodsForInstrumentation) {
            String innerClassName = name + "$" + m.name + "_Fiber";
            String outerClassDesc = "L" + name + ";";
            String ctrInputDescriptor = "(" + outerClassDesc + substringBetween(m.signature, "(", ")") + ")V";

            methods.remove(m);

            MethodVisitor mv = visitMethod(m.access, m.name, m.desc, m.signature, m.exceptions.toArray(new String[0]));
            mv.visitCode();
            mv.visitTypeInsn(NEW, innerClassName);
            mv.visitInsn(DUP);
            mv.visitVarInsn(ALOAD, 0);

            Type[] methodInputParams = Type.getArgumentTypes(m.desc);
            for (int i = 0; i < methodInputParams.length; i++) {
                mv.visitVarInsn(methodInputParams[i].getOpcode(ILOAD), i + 1);
            }

            mv.visitMethodInsn(INVOKESPECIAL, innerClassName, "<init>", ctrInputDescriptor, false);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(5, 3);
            mv.visitEnd();
        }

        if (!methodsForInstrumentation.isEmpty()) {
            accept(cv);
            isInstrumented = true;
        }
    }

    private void insertInnerClass(ClassWriter cw, String className, MethodNode method) {
        String[] localVarsNames = new String[method.localVariables.size() - 1]; // skip this param
        for (int i = 1; i < method.localVariables.size(); i++) {
            LocalVariableNode var = method.localVariables.get(i);
            cw.visitField(ACC_PUBLIC, var.name, var.desc, var.signature, null).visitEnd();
            localVarsNames[i - 1] = var.name;
        }

        String outerClassDesc = "L" + name + ";";
        cw.visitField(ACC_FINAL + ACC_SYNTHETIC, "this$0", outerClassDesc, null, null).visitEnd();

        String ctrDescriptor = "(" + outerClassDesc + substringBetween(method.signature, "(", ")") + ")V";
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", ctrDescriptor, null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1); // push `this` to the operand stack
        mv.visitFieldInsn(PUTFIELD, className, "this$0", outerClassDesc);

        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, FIBER_CLASS_NAME, "<init>", "()V", false);

        // set class fields values
        Type[] argTypes = Type.getArgumentTypes(method.desc);
        for (int i = 0; i < argTypes.length; i++) {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(argTypes[i].getOpcode(ILOAD), i + 2);
            mv.visitFieldInsn(PUTFIELD, className, localVarsNames[i], argTypes[i].getDescriptor());
        }

        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        insertFiberUpdateMethod(cw, className, method);
    }

    private void insertUpdateMethod(String innerClassName, MethodNode method) {
        String outerClassName = "L" + name + ";";

        // debug
//        Printer p = new Textifier(ASM6) {
//            @Override public void visitMethodEnd() {
//                print(new PrintWriter(System.out)); // print it after it has been visited
//            }
//        };
//        MethodVisitor mv = new TraceMethodVisitor(visitMethod(ACC_PROTECTED, method.name + "_FiberUpdate", "(L" + innerClassName + ";)I", null, null), p);

        MethodVisitor mv = visitMethod(ACC_PROTECTED, method.name + "_FiberUpdate", "(L" + innerClassName + ";)I", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 1);
        mv.visitFieldInsn(GETFIELD, innerClassName, "state", "I");

        // create a map with instructions by cases
        Map<Integer, List<AbstractInsnNode>> instByCases = new HashMap<>();

        // method instructions
        int switchCase = 0;
        InsnList instructions = method.instructions;
        for (int i = 0; i < instructions.size(); i++) {
            AbstractInsnNode inst = instructions.get(i);
            MethodInsnNode methodInvocation = getStaticMethodInvocation(inst);
            if (isCallMethodWithFiberArg(methodInvocation)) {
                putInsnAfterLineNumber(instByCases, switchCase, new VarInsnNode(ALOAD, 1));
                putInsnAfterLineNumber(instByCases, switchCase, new InsnNode(NOP));
                putInsn(instByCases, switchCase, new MethodInsnNode(INVOKEVIRTUAL, innerClassName, "awaitFor", AWAIT_FIBER_DESC, false));
                putInsn(instByCases, switchCase, new InsnNode(IRETURN));

                // add next case
                switchCase++;
                putInsn(instByCases, switchCase, new MethodInsnNode(INVOKEVIRTUAL, innerClassName, "awaitFiber", getMethodDescriptor(getType(int.class)), false));
                putInsn(instByCases, switchCase, new InsnNode(IRETURN));

                // add next case
                switchCase++;
                // skip check cast since we store a var in result field
                inst = inst.getNext();
                String varDesc = null;
                if (inst.getOpcode() == CHECKCAST) {
                    i++;
                    varDesc = ((TypeInsnNode) inst).desc;
                }

                // skip storing it to the local var
                inst = inst.getNext();
                LocalVariableNode field = null;
                if (inst.getOpcode() >= ISTORE && inst.getOpcode() <= ASTORE) {
                    i++;
                    field = method.localVariables.get(((VarInsnNode) inst).var);
                }
                putInsn(instByCases, switchCase, new InsnNode(NOP));
                putInsn(instByCases, switchCase, new VarInsnNode(ALOAD, 1));
                putInsn(instByCases, switchCase, new FieldInsnNode(GETFIELD, innerClassName, "result", "Ljava/lang/Object;"));
                putInsn(instByCases, switchCase, new TypeInsnNode(CHECKCAST, varDesc));
                Contract.checkNotNull(field, "Missing field assignment");
                putInsn(instByCases, switchCase, new FieldInsnNode(PUTFIELD, innerClassName, field.name, field.desc));

                putInsn(instByCases, switchCase, getPushInst(switchCase + 1));
                putInsn(instByCases, switchCase, new InsnNode(IRETURN));
                switchCase++;
                continue;
            }
            if (isCallMethodWithFutureArg(methodInvocation)) {
                // replace it with call to internal method
                putInsn(instByCases, switchCase, new MethodInsnNode(INVOKEVIRTUAL, innerClassName, "awaitFor", AWAIT_FUTURE_DESC, false));
                putInsn(instByCases, switchCase, new InsnNode(IRETURN));

                // add next case
                switchCase++;
                putInsn(instByCases, switchCase, new MethodInsnNode(INVOKEVIRTUAL, innerClassName, "awaitFuture", getMethodDescriptor(getType(int.class)), false));
                putInsn(instByCases, switchCase, new InsnNode(IRETURN));

                // add next case
                switchCase++;
                // skip check cast since we store a var in result field
                inst = inst.getNext();
                String varDesc = null;
                if (inst.getOpcode() == CHECKCAST) {
                    i++;
                    varDesc = ((TypeInsnNode) inst).desc;
                }

                // skip storing it to the local var
                inst = inst.getNext();
                LocalVariableNode field = null;
                if (inst.getOpcode() >= ISTORE && inst.getOpcode() <= ASTORE) {
                    i++;
                    field = method.localVariables.get(((VarInsnNode) inst).var);
                }
                putInsn(instByCases, switchCase, new InsnNode(NOP));
                putInsn(instByCases, switchCase, new VarInsnNode(ALOAD, 1));
                putInsn(instByCases, switchCase, new InsnNode(NOP));
                putInsn(instByCases, switchCase, new VarInsnNode(ALOAD, 1));
                putInsn(instByCases, switchCase, new FieldInsnNode(GETFIELD, innerClassName, "result", "Ljava/lang/Object;"));
                putInsn(instByCases, switchCase, new TypeInsnNode(CHECKCAST, varDesc));
                Contract.checkNotNull(field, "Missing field assignment");
                putInsn(instByCases, switchCase, new FieldInsnNode(PUTFIELD, innerClassName, field.name, field.desc));

                putInsn(instByCases, switchCase, getPushInst(switchCase + 1));
                putInsn(instByCases, switchCase, new InsnNode(IRETURN));
                switchCase++;
                continue;
            }
            if (isCallMethodWithLiteralArg(methodInvocation)) {
                AbstractInsnNode next = inst.getNext();
                while (next != null && !(next instanceof LabelNode)) {
                    i++;
                    putInsn(instByCases, switchCase, next);
                    next = next.getNext();
                }
                putInsn(instByCases, switchCase, getPushInst(switchCase + 1));
                putInsn(instByCases, switchCase, new InsnNode(IRETURN));
                switchCase++;
                continue;
            }
            if (isReturnMethodWithFiberArg(methodInvocation)) {
                putInsn(instByCases, switchCase, new MethodInsnNode(INVOKEVIRTUAL, innerClassName, "awaitFor", AWAIT_FIBER_DESC, false));
                putInsn(instByCases, switchCase, new InsnNode(IRETURN));

                // add last case
                switchCase++;
                putInsn(instByCases, switchCase, new MethodInsnNode(INVOKEVIRTUAL, innerClassName, "waitForFiberResult", getMethodDescriptor(getType(int.class)), false));
                putInsn(instByCases, switchCase, new InsnNode(IRETURN));
                break;
            }
            if (isReturnMethodWithFutureArg(methodInvocation)) {
                putInsn(instByCases, switchCase, new MethodInsnNode(INVOKEVIRTUAL, innerClassName, "awaitFor", AWAIT_FUTURE_DESC, false));
                putInsn(instByCases, switchCase, new InsnNode(IRETURN));

                // add last case
                switchCase++;
                putInsn(instByCases, switchCase, new MethodInsnNode(INVOKEVIRTUAL, innerClassName, "waitForFutureResult", getMethodDescriptor(getType(int.class)), false));
                putInsn(instByCases, switchCase, new InsnNode(IRETURN));
                break;
            }
            if (isReturnMethodWithLiteralArg(methodInvocation)) {
                putInsnAfterLineNumber(instByCases, switchCase, new VarInsnNode(ALOAD, 1));
                putInsnAfterLineNumber(instByCases, switchCase, new InsnNode(NOP));
                putInsn(instByCases, switchCase, new MethodInsnNode(INVOKEVIRTUAL, innerClassName, "resultLiteral", INTERNAL_WITH_LITERAL_ARG, false));
                putInsn(instByCases, switchCase, new InsnNode(IRETURN));
                break;
            }
            if (isReturnNothingMethod(methodInvocation)) {
                putInsn(instByCases, switchCase, new MethodInsnNode(INVOKEVIRTUAL, innerClassName, "nothingInternal", INTERNAL_NOTHING, false));
                putInsn(instByCases, switchCase, new InsnNode(IRETURN));
                break;
            }
            putInsn(instByCases, switchCase, inst);
        }

        switchCase++;
        // insert switch table
        Label[] labels = new Label[switchCase + 1];
        for (int i = 0; i < switchCase; i++) {
            labels[i] = new Label();
        }
        labels[switchCase] = new Label(); // default label

        int[] values = new int[switchCase];
        for (int i = 0; i < switchCase; i++) {
            values[i] = i;
        }
        mv.visitLookupSwitchInsn(labels[switchCase], values, Arrays.copyOf(labels, labels.length - 1));

        // post process instructions
        for (int i = 0; i < switchCase; i++) {
            List<AbstractInsnNode> instNodes = instByCases.get(i);
            List<AbstractInsnNode> processedNodes = new ArrayList<>();

            AbstractInsnNode inst;
            for (int j = 0; j < instNodes.size(); j++) {
                inst = instNodes.get(j);
                int opcode = inst.getOpcode();
                if (opcode >= Opcodes.ILOAD && opcode <= SALOAD) {
                    int prevOpcode = (j - 1) >= 0 ? instNodes.get(j - 1).getOpcode() : Integer.MIN_VALUE;
                    // load vars from the class fields (if it's not a local to frame)
                    int fieldIndex = ((VarInsnNode) inst).var;
                    if (fieldIndex > 0 && fieldIndex < method.localVariables.size() && prevOpcode != NOP) {
                        LocalVariableNode field = method.localVariables.get(fieldIndex);
                        processedNodes.add(new VarInsnNode(ALOAD, 1));
                        processedNodes.add(new FieldInsnNode(GETFIELD, innerClassName, field.name, field.desc));
                        continue;
                    }
                } else if (opcode >= ISTORE && opcode <= ASTORE) {
                    // store vars into the class fields (if it's not a local to frame)
                    int index = ((VarInsnNode) inst).var;
                    if (index < method.localVariables.size()) {
                        LocalVariableNode field = method.localVariables.get(index);
                        putInsnAfterLineNumber(processedNodes, new VarInsnNode(ALOAD, 1));
                        processedNodes.add(new FieldInsnNode(PUTFIELD, innerClassName, field.name, field.desc));
                        continue;
                    }
                }
                processedNodes.add(inst);
            }
            instByCases.put(i, processedNodes);
        }

        // add switch cases
        for (int i = 0; i < switchCase; i++) {
            mv.visitLabel(labels[i]);
            mv.visitVarInsn(ALOAD, 1);
            instByCases.get(i).stream()
                .filter(inst -> inst.getOpcode() != NOP)
                .forEach(inst -> inst.accept(mv));
        }

        // handle default case with exception
        mv.visitLabel(labels[switchCase]);
        mv.visitTypeInsn(NEW, "java/lang/IllegalStateException");
        mv.visitInsn(DUP);
        mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
        mv.visitLdcInsn("Unknown state: ");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitFieldInsn(GETFIELD, innerClassName, "state", "I");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(I)Ljava/lang/StringBuilder;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/IllegalStateException", "<init>", "(Ljava/lang/String;)V", false);
        mv.visitInsn(ATHROW);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void insertFiberUpdateMethod(ClassWriter cw, String innerClassName, MethodNode method) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "update", "()I", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, innerClassName, "this$0", "L" + name + ";");
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEVIRTUAL, name, method.name + "_FiberUpdate", "(L" + innerClassName + ";)I", false);
        mv.visitInsn(IRETURN);
        mv.visitMaxs(2, 1);
        mv.visitEnd();
    }

    /* utils */
    private static void putInsn(Map<Integer, List<AbstractInsnNode>> instByCases, int switchCase, AbstractInsnNode node) {
        instByCases
            .computeIfAbsent(switchCase, k -> new ArrayList<>())
            .add(node);
    }

    private static void putInsnBeforeLast(Map<Integer, List<AbstractInsnNode>> instByCases, int switchCase, AbstractInsnNode node) {
        List<AbstractInsnNode> insnList = instByCases.computeIfAbsent(switchCase, k -> new ArrayList<>());
        if (insnList.size() == 0) {
            insnList.add(node);
        } else {
            insnList.add(insnList.size() - 1, node);
        }
    }

    private static void putInsnAfterLineNumber(Map<Integer, List<AbstractInsnNode>> instByCases, int switchCase, AbstractInsnNode node) {
        putInsnAfterLineNumber(instByCases.computeIfAbsent(switchCase, k -> new ArrayList<>()), node);
    }

    private static void putInsnAfterLineNumber(List<AbstractInsnNode> insnList, AbstractInsnNode node) {
        if (insnList.size() == 0) {
            insnList.add(node);
        } else {
            for (int i = insnList.size() - 1; i >= 0; i--) {
                AbstractInsnNode insn = insnList.get(i);
                if (insn instanceof LineNumberNode) {
                    insnList.add(i + 1, node);
                    break;
                }
            }
        }
    }

    private static boolean isPointerToThis(AbstractInsnNode insn) {
        return insn instanceof VarInsnNode && ((VarInsnNode) insn).var == 0;
    }

    private static boolean isCallMethodWithFiberArg(MethodInsnNode node) {
        return isCallMethodWithArg(node, Fiber.class);
    }

    private static boolean isCallMethodWithFutureArg(MethodInsnNode node) {
        return isCallMethodWithArg(node, Future.class);
    }

    private static boolean isCallMethodWithLiteralArg(MethodInsnNode node) {
        return isCallMethodWithArg(node, Object.class);
    }

    private static boolean isCallMethodWithArg(MethodInsnNode node, Class clazz) {
        if (node == null) {
            return false;
        }
        return "call".equals(node.name)
            && FIBER_CLASS_NAME.equals(node.owner)
            && getMethodDescriptor(getType(Object.class), getType(clazz)).equals(node.desc);
    }

    private static boolean isReturnMethodWithFiberArg(MethodInsnNode node) {
        return isReturnMethodWithArg(node, Fiber.class);
    }

    private static boolean isReturnMethodWithFutureArg(MethodInsnNode node) {
        return isReturnMethodWithArg(node, Future.class);
    }

    private static boolean isReturnMethodWithLiteralArg(MethodInsnNode node) {
        return isReturnMethodWithArg(node, Object.class);
    }

    private static boolean isReturnMethodWithArg(MethodInsnNode node, Class clazz) {
        if (node == null) {
            return false;
        }
        return "result".equals(node.name)
            && FIBER_CLASS_NAME.equals(node.owner)
            && getMethodDescriptor(getType(Fiber.class), getType(clazz)).equals(node.desc);
    }

    private static boolean isReturnNothingMethod(MethodInsnNode node) {
        if (node == null) {
            return false;
        }
        return "nothing".equals(node.name)
            && FIBER_CLASS_NAME.equals(node.owner)
            && getMethodDescriptor(getType(Fiber.class)).equals(node.desc);
    }

    private static boolean hasTerminateMethodInvocation(MethodNode method) {
        ListIterator<AbstractInsnNode> iterator = method.instructions.iterator();
        while (iterator.hasNext()) {
            if (isTerminateMethodInvocation(iterator.next())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isTerminateMethodInvocation(AbstractInsnNode inst) {
        MethodInsnNode methodNode = getStaticMethodInvocation(inst);
        if (methodNode != null) {
            String name = methodNode.name;
            return FIBER_CLASS_NAME.equals(methodNode.owner)
                && ("nothing".equals(name) || "result".equals(name));
        }
        return false;
    }

    private static Type[] getArgumentTypes(AbstractInsnNode node) {
        Contract.checkArg(node instanceof MethodInsnNode, () -> "Node is not an instance of MethodInsnNode");
        return Type.getArgumentTypes(((MethodInsnNode) node).desc);
    }

    private static MethodInsnNode getStaticMethodInvocation(AbstractInsnNode inst) {
        if (inst.getOpcode() == INVOKESTATIC && inst instanceof MethodInsnNode) {
            return ((MethodInsnNode) inst);
        }
        return null;
    }

    private static String substringBetween(String string, String left, String right) {
        int start = string.indexOf(left) + 1;
        int end = string.indexOf(right);
        return string.substring(start, end);
    }

    private static AbstractInsnNode getPushInst(int value) {
        if (value == -1) {
            return new InsnNode(Opcodes.ICONST_M1);
        }
        if (value == 0) {
            return new InsnNode(Opcodes.ICONST_0);
        }
        if (value == 1) {
            return new InsnNode(Opcodes.ICONST_1);
        }
        if (value == 2) {
            return new InsnNode(Opcodes.ICONST_2);
        }
        if (value == 3) {
            return new InsnNode(Opcodes.ICONST_3);
        }
        if (value == 4) {
            return new InsnNode(Opcodes.ICONST_4);
        }
        if (value == 5) {
            return new InsnNode(Opcodes.ICONST_5);
        }
        if ((value >= -128) && (value <= 127)) {
            return new IntInsnNode(Opcodes.BIPUSH, value);
        }
        if ((value >= -32768) && (value <= 32767)) {
            return new IntInsnNode(Opcodes.SIPUSH, value);
        }
        return new LdcInsnNode(value);
    }
}
