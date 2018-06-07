package com.github.rostmyr.jrpc.fibers.bytecode;

import com.github.rostmyr.jrpc.common.utils.Contract;
import com.github.rostmyr.jrpc.fibers.Fiber;
import com.github.rostmyr.jrpc.fibers.FiberManager;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import static java.util.stream.Collectors.toList;
import static org.objectweb.asm.ClassReader.SKIP_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_FRAMES;
import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

/**
 * Rostyslav Myroshnychenko
 * on 02.06.2018.
 */
public class FiberTransformer {
    // join points
    private static final String POINT_RESULT = "result";
    private static final String POINT_CALL = "call";
    private static final String CALL_METHOD_WITH_FIBER_ARG_DESCRIPTOR =
        getMethodDescriptor(getType(Object.class), getType(Fiber.class));

    private static String FIBER_CLASS_NAME = getInternalName(Fiber.class);
    private static String FIBER_MANAGER_CLASS_NAME = getInternalName(FiberManager.class);
    private static String FIBER_RETURN_TYPE = getDescriptor(Fiber.class);

    private Class<?> clazz;
    private byte[] clazzBytes;
    private final boolean debug;

    public FiberTransformer(Class<?> clazz, boolean debug) {
        this.clazz = clazz;
        this.debug = debug;
    }

    public FiberTransformer(byte[] clazz, boolean debug) {
        this.clazzBytes = clazz;
        this.debug = debug;
    }

    public FiberTransformerResult instrument() throws IOException {
        FiberTransformerResult result = new FiberTransformerResult();
        ClassWriter cw = new ClassWriter(COMPUTE_FRAMES);
        FiberClassAdapter cv = new FiberClassAdapter(cw, result);
        ClassReader cr = clazz == null ? new ClassReader(clazzBytes) : new ClassReader(clazz.getName());
        cr.accept(cv, SKIP_FRAMES);

        if (cv.isInstrumented) {
            result.setMainClass(cw.toByteArray());
        }
        return result;
    }

    private final class FiberClassAdapter extends ClassNode {
        private FiberTransformerResult result;
        private boolean isInstrumented;

        FiberClassAdapter(ClassVisitor cv, FiberTransformerResult result) {
            super(ASM6);
            this.result = result;
            if (debug) {
                this.cv = new CheckClassAdapter(new TraceClassVisitor(cv, new PrintWriter(System.out)));
            } else {
                this.cv = new CheckClassAdapter(cv);
            }
        }

        @Override
        public void visitEnd() {
            List<MethodNode> methodsForInstrumentation = methods.stream()
                .filter(method -> getReturnType(method.desc).getDescriptor().equals(FIBER_RETURN_TYPE))
                .filter(this::hasResultMethodInvocation)
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
                insertInnerClassConstructor(method, innerClassName, cw);
                insertUpdateMethod(cw, innerClassName, method);
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

                MethodVisitor mv = visitMethod(
                    m.access,
                    m.name,
                    m.desc,
                    m.signature,
                    m.exceptions.toArray(new String[0])
                );
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

        private void insertInnerClassConstructor(MethodNode method, String className, ClassWriter cw) {
            String[] localVarsNames = new String[method.localVariables.size() - 1]; // skip this param
            for (int i = 1; i < method.localVariables.size(); i++) {
                LocalVariableNode var = method.localVariables.get(i);
                cw.visitField(ACC_PRIVATE, var.name, var.desc, var.signature, null).visitEnd();
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
        }

        private void insertUpdateMethod(ClassWriter cw, String innerClassName, MethodNode method) {
            String outerClassName = "L" + name + ";";

            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "update", "()I", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, innerClassName, "state", "I");

            int casesCount = 0;
            InsnList instructions = method.instructions;
            for (int i = 0; i < instructions.size(); i++) {
                AbstractInsnNode insnNode = instructions.get(i);
                if (isJoinPointMethodInvocation(insnNode)) {
                    casesCount++;
                    // if we pass fiber as argument we need to add a new case to wait until it's ready
                    if (isMethodWithFiberArgumentInvocation(insnNode)) {
                        casesCount++;
                    }
                }
            }

            Label[] labels = new Label[casesCount + 1];
            for (int i = 0; i < casesCount; i++) {
                labels[i] = new Label();
            }
            labels[casesCount] = new Label(); // default label

            int[] values = new int[casesCount];
            for (int i = 0; i < casesCount; i++) {
                values[i] = i;
            }

            mv.visitLookupSwitchInsn(labels[casesCount], values, Arrays.copyOf(labels, labels.length - 1));

            // create a map with instructions by cases
            Map<Integer, List<AbstractInsnNode>> instByCases = new HashMap<>();
            for (int i = 0; i < casesCount; i++) {
                instByCases.put(i, new ArrayList<>());
            }

            // split whole method by label nodes and join points
            boolean foundPoint = false;
            for (int i = 0, j = 0; i < instructions.size() && j < casesCount; i++) {
                AbstractInsnNode inst = instructions.get(i);
                if (isJoinPointMethodInvocation(inst)) {
                    if (isMethodWithFiberArgumentInvocation(inst)) {
                        List<AbstractInsnNode> instNodes = instByCases.get(j);

                        // store fiber to the result field
                        instNodes.add(new FieldInsnNode(PUTFIELD, innerClassName, "result", "Ljava/lang/Object;"));

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
                        if (inst.getOpcode() == ASTORE) {
                            i++;
                            field = method.localVariables.get(((VarInsnNode) inst).var);
                        }

                        instNodes.add(new VarInsnNode(ALOAD, 0));
                        instNodes.add(
                            new FieldInsnNode(
                                GETFIELD,
                                innerClassName,
                                "scheduler",
                                "L" + FIBER_MANAGER_CLASS_NAME + ";"
                            ));
                        instNodes.add(new VarInsnNode(ALOAD, 0));
                        instNodes.add(new FieldInsnNode(GETFIELD, innerClassName, "result", "Ljava/lang/Object;"));
                        instNodes.add(new TypeInsnNode(CHECKCAST, FIBER_CLASS_NAME));
                        instNodes.add(new MethodInsnNode(INVOKEVIRTUAL, FIBER_MANAGER_CLASS_NAME, "schedule",
                            "(L" + FIBER_CLASS_NAME + ";)V", false
                        ));
                        j++;

                        instNodes = instByCases.get(j);
                        instNodes.add(new VarInsnNode(ALOAD, 0));
                        instNodes.add(new FieldInsnNode(GETFIELD, innerClassName, "result", "Ljava/lang/Object;"));
                        instNodes.add(new TypeInsnNode(CHECKCAST, FIBER_CLASS_NAME));
                        instNodes.add(new MethodInsnNode(INVOKEVIRTUAL, FIBER_CLASS_NAME, "isReady", "()Z", false));

                        LabelNode isReadyLabel = new LabelNode();
                        instNodes.add(new JumpInsnNode(IFNE, isReadyLabel));
                        instNodes.add(new InsnNode(getPushInst(j).getOpcode()));
                        instNodes.add(new InsnNode(NOP)); // marker
                        instNodes.add(new InsnNode(IRETURN));

                        instNodes.add(isReadyLabel);
                        instNodes.add(new VarInsnNode(ALOAD, 0));
                        instNodes.add(new VarInsnNode(ALOAD, 0));
                        instNodes.add(new FieldInsnNode(GETFIELD, innerClassName, "result", "Ljava/lang/Object;"));
                        instNodes.add(new TypeInsnNode(CHECKCAST, FIBER_CLASS_NAME));
                        instNodes.add(
                            new MethodInsnNode(
                                INVOKEVIRTUAL,
                                FIBER_CLASS_NAME,
                                "getResult",
                                "()Ljava/lang/Object;",
                                false
                            ));
                        instNodes.add(new TypeInsnNode(CHECKCAST, varDesc));
                        Contract.checkNotNull(field, "Missing field assignment");
                        instNodes.add(new FieldInsnNode(PUTFIELD, innerClassName, field.name, field.desc));

                        j++;
                        continue;

                    }
                    foundPoint = true;
                    continue;
                }

                instByCases.get(j).add(inst);
                if (inst instanceof LabelNode && foundPoint && inst.getNext() != null) {
                    foundPoint = false;
                    j++;
                }
            }

            // post process instructions
            for (int i = 0; i < casesCount; i++) {
                List<AbstractInsnNode> instNodes = instByCases.get(i);
                LinkedList<AbstractInsnNode> processedNodes = new LinkedList<>();
                LinkedList<AbstractInsnNode> buffer = new LinkedList<>();
                for (AbstractInsnNode inst : instNodes) {
                    if (inst.getOpcode() == INVOKEVIRTUAL && name.equals(((MethodInsnNode) inst).owner)) {
                        for (int j = 0; j < getArgumentTypes(inst).length; j++) {
                            buffer.addFirst(processedNodes.pollLast());
                        }
                        processedNodes.add(new VarInsnNode(ALOAD, 0));
                        processedNodes.add(new FieldInsnNode(GETFIELD, innerClassName, "this$0", outerClassName));
                        processedNodes.addAll(buffer);
                        buffer.clear();
                    } else if (inst.getOpcode() >= Opcodes.ILOAD && inst.getOpcode() <= SALOAD) {
                        // load vars from the class fields
                        int fieldIndex = ((VarInsnNode) inst).var;
                        if (fieldIndex > 0) { // not this
                            LocalVariableNode field = method.localVariables.get(fieldIndex);
                            processedNodes.add(new VarInsnNode(ALOAD, 0)); // we need this before get
                            processedNodes.add(new FieldInsnNode(GETFIELD, innerClassName, field.name, field.desc));
                            continue;
                        }
                    } else if (inst.getOpcode() >= ISTORE && inst.getOpcode() <= ASTORE) {
                        // store vars into the class fields
                        LocalVariableNode field = method.localVariables.get(((VarInsnNode) inst).var);
                        processedNodes.add(new FieldInsnNode(PUTFIELD, innerClassName, field.name, field.desc));
                        continue;
                    }

                    processedNodes.add(inst);
                }
                instByCases.put(i, processedNodes);
            }

            for (int i = 0, newState = 1; i < casesCount; i++, newState++) {
                mv.visitLabel(labels[i]);
                mv.visitVarInsn(ALOAD, 0);

                List<AbstractInsnNode> abstractInstNodes = instByCases.get(i);
                for (int e = 0; e < abstractInstNodes.size(); e++) {
                    AbstractInsnNode inst = abstractInstNodes.get(e);
                    int opcode = inst.getOpcode();

                    // result method invocation
                    if (opcode >= IRETURN && opcode <= ARETURN && abstractInstNodes.get(e - 1).getOpcode() != NOP) {
                        mv.visitFieldInsn(PUTFIELD, innerClassName, "result", "Ljava/lang/Object;"); //set result
                        mv.visitInsn(ICONST_M1);
                        mv.visitInsn(IRETURN);
                        break;
                    }

                    if (opcode == NOP) {
                        continue;
                    }

                    inst.accept(mv);
                }

                // return next state value
                mv.visitInsn(getPushInst(newState).getOpcode());
                mv.visitInsn(IRETURN);
            }

            // handle default case with exception
            mv.visitLabel(labels[casesCount]);
            mv.visitTypeInsn(NEW, "java/lang/IllegalStateException");
            mv.visitInsn(DUP);
            mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
            mv.visitLdcInsn("Unknown state: ");
            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "append",
                "(Ljava/lang/String;)Ljava/lang/StringBuilder;",
                false
            );
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, innerClassName, "state", "I");
            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "append",
                "(I)Ljava/lang/StringBuilder;",
                false
            );
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
            mv.visitMethodInsn(
                INVOKESPECIAL,
                "java/lang/IllegalStateException",
                "<init>",
                "(Ljava/lang/String;)V",
                false
            );
            mv.visitInsn(ATHROW);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        private boolean hasResultMethodInvocation(MethodNode method) {
            ListIterator<AbstractInsnNode> iterator = method.instructions.iterator();
            while (iterator.hasNext()) {
                if (isResultMethodInvocation(iterator.next())) {
                    return true;
                }
            }
            return false;
        }

        private boolean isJoinPointMethodInvocation(AbstractInsnNode inst) {
            MethodInsnNode methodNode = getStaticMethodInvocation(inst);
            if (methodNode != null) {
                String name = methodNode.name;
                return FIBER_CLASS_NAME.equals(methodNode.owner) && (POINT_CALL.equals(name) || POINT_RESULT.equals(name));
            }
            return false;
        }

        private boolean isResultMethodInvocation(AbstractInsnNode inst) {
            MethodInsnNode methodNode = getStaticMethodInvocation(inst);
            if (methodNode != null) {
                return POINT_RESULT.equals(methodNode.name) && FIBER_CLASS_NAME.equals(methodNode.owner);
            }
            return false;
        }

        private boolean isCallMethodInvocation(AbstractInsnNode inst) {
            MethodInsnNode methodNode = getStaticMethodInvocation(inst);
            if (methodNode != null) {
                return POINT_CALL.equals(methodNode.name) && FIBER_CLASS_NAME.equals(methodNode.owner);
            }
            return false;
        }

        private boolean isMethodWithFiberArgumentInvocation(AbstractInsnNode inst) {
            MethodInsnNode methodNode = getStaticMethodInvocation(inst);
            if (methodNode != null) {
                return POINT_CALL.equals(methodNode.name)
                    && FIBER_CLASS_NAME.equals(methodNode.owner)
                    && CALL_METHOD_WITH_FIBER_ARG_DESCRIPTOR.equals(methodNode.desc);
            }
            return false;
        }
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
}
