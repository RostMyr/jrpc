package com.github.rostmyr.jrpc.common.bytecode;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.util.CheckClassAdapter;

import com.github.rostmyr.jrpc.common.annotation.ResourceId;

import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import static org.objectweb.asm.ClassWriter.COMPUTE_FRAMES;
import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.getMethodDescriptor;

/**
 * Rostyslav Myroshnychenko
 * on 26.05.2018.
 */
public class ResourceClassTransformer {

    // constructor
    private static final String CTR_NAME = "<init>";
    private static final String CTR_DESC = "()V";

    // fields
    public static final String RESOURCE_ID_FIELD = "_resourceId";

    // methods
    public static final String RESOURCE_ID_SUPPLIER = "create";

    // descriptors
    private static final String RESOURCE_ID_DESC = Type.getDescriptor(ResourceId.class);
    private static final String INT_DESC = Type.getDescriptor(int.class);
    private static final String SUPPLIER_DESC = "()" + Type.getDescriptor(Supplier.class);

    // access flags
    private static final int PSF = ACC_PUBLIC + ACC_STATIC + ACC_FINAL;
    private static final int PS = ACC_PUBLIC + ACC_STATIC;

    private static final String LOOKUP_NAME = Type.getInternalName(MethodHandles.Lookup.class);
    private static final String METHOD_HANDLES_NAME = Type.getInternalName(MethodHandles.class);
    private static final String LAMBDA_METAFACTORY = Type.getInternalName(LambdaMetafactory.class);

    private boolean isChanged;

    private final byte[] clazz;

    public ResourceClassTransformer(byte[] clazz) {
        this.clazz = clazz;
    }

    /**
     * Transforms the current class
     *
     * @return modified class represented as byte array
     */
    public byte[] transform() {
        ClassWriter cw = new ClassWriter(COMPUTE_FRAMES);
        ClassVisitor ca = new MyClassAdapter(cw);
        ClassReader cr = new ClassReader(clazz);
        cr.accept(ca, 0);
        if (isChanged) {
            return cw.toByteArray();
        }
        return clazz;
    }

    private class MyClassAdapter extends ClassNode {
        MyClassAdapter(ClassVisitor classVisitor) {
            super(ASM6);
            this.cv = new CheckClassAdapter((classVisitor));
        }

        @Override
        public void visitEnd() {
            Integer resourceId = getResourceId();
            if (resourceId != null) {
                FieldVisitor fv = visitField(PSF, RESOURCE_ID_FIELD, INT_DESC, null, resourceId);
                if (fv != null) {
                    fv.visitEnd();
                }

                appendDefaultConstructor();
                appendResourceSupplier();
                appendGetResourceMethod(resourceId);
                accept(cv);
                isChanged = true;
            }
        }

        private void appendResourceSupplier() {
            try {
                visitInnerClass(LOOKUP_NAME, METHOD_HANDLES_NAME, "Lookup", PSF);
                MethodVisitor mv = visitMethod(
                    PS, RESOURCE_ID_SUPPLIER, SUPPLIER_DESC, "()Ljava/util/function/Supplier<L" + name + ";>;", null);
                mv.visitCode();
                String metafactoryDescriptor = getMethodDescriptor(
                    LambdaMetafactory.class.getMethod("metafactory", MethodHandles.Lookup.class, String.class,
                        MethodType.class, MethodType.class, MethodHandle.class, MethodType.class
                    ));
                mv.visitInvokeDynamicInsn(
                    "get", SUPPLIER_DESC,
                    new Handle(H_INVOKESTATIC, LAMBDA_METAFACTORY, "metafactory", metafactoryDescriptor, false),
                    new Object[]{Type.getType("()" + Type.getType(Object.class)), new Handle(
                        Opcodes.H_NEWINVOKESPECIAL, name, CTR_NAME, CTR_DESC, false), Type.getType("()L" + name + ";")}
                );
                mv.visitInsn(ARETURN);
                mv.visitMaxs(1, 0);
                mv.visitEnd();
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("Can't append a resource supplier: ", e);
            }
        }

        private void appendGetResourceMethod(Integer resourceId) {
            methods.removeIf(methodNode -> methodNode.name.equals("getResourceId"));
            MethodVisitor mv = visitMethod(ACC_PUBLIC, "getResourceId", "()" + INT_DESC, null, null);
            if (mv != null) {
                mv.visitCode();
                AbstractInsnNode pushInsn = getPushInsn(resourceId);
                pushInsn.accept(mv);
                mv.visitInsn(IRETURN);
                mv.visitMaxs(1, 1);
                mv.visitEnd();
            }
        }

        private void appendDefaultConstructor() {
            if (!hasDefaultConstructor()) {


                MethodVisitor mv = visitMethod(ACC_PUBLIC, CTR_NAME, CTR_DESC, null, null);
                mv.visitCode();
                mv.visitVarInsn(ALOAD, 0); // this
                if (Objects.equals(superName, Type.getInternalName(Object.class))) {
                    mv.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(Object.class), CTR_NAME, CTR_DESC, false);
                } else {
                    mv.visitMethodInsn(INVOKESPECIAL, superName, CTR_NAME, CTR_DESC, false);
                }
                mv.visitInsn(RETURN);
                mv.visitMaxs(1, 1);
                mv.visitEnd();
            }
        }

        private boolean hasDefaultConstructor() {
            return methods.stream()
                .filter(method -> CTR_NAME.equals(method.name))
                .anyMatch(method -> CTR_DESC.equals(method.desc));
        }

        private Integer getResourceId() {
            AnnotationNode resourceAnnotation = getAnnotation(RESOURCE_ID_DESC);
            if (resourceAnnotation == null) {
                return null;
            }
            return Integer.class.cast(resourceAnnotation.values.get(1));
        }

        private AnnotationNode getAnnotation(String annotationDesc) {
            List<AnnotationNode> annotations = visibleAnnotations;
            if (annotations == null) {
                return null;
            }
            return annotations.stream()
                .filter(annotationNode -> annotationDesc.equals(annotationNode.desc))
                .findFirst()
                .orElse(null);
        }
    }

    static AbstractInsnNode getPushInsn(int value) {
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
