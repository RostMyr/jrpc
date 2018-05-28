package com.github.rostmyr.jrpc.core.parser;

import io.netty.buffer.ByteBuf;
import org.junit.Test;

import com.github.rostmyr.jrpc.common.io.Resource;
import com.github.rostmyr.jrpc.core.service.MethodDefinition;
import com.github.rostmyr.jrpc.core.service.reader.MethodDefinitionReader;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Rostyslav Myroshnychenko
 * on 27.05.2018.
 */
public class TestMethodDefinitionReader {
    private final MethodDefinitionReader reader = new MethodDefinitionReader();

    @Test
    public void shouldReturnEmptyDefinitionListForEmptyClass() {
        // WHEN
        List<MethodDefinition> methodDefinitions = reader.read(EmptyClass.class);

        // THEN
        assertThat(methodDefinitions).isEmpty();
    }

    @Test
    public void shouldReturnEmptyDefinitionListForClassWithPrivateMethods() {
        // WHEN
        List<MethodDefinition> methodDefinitions = reader.read(PrivateMethodsOnly.class);

        // THEN
        assertThat(methodDefinitions).isEmpty();
    }

    @Test
    public void shouldReturnEmptyDefinitionListForClassWithInvalidSignature() {
        // WHEN
        List<MethodDefinition> methodDefinitions = reader.read(InvalidSignature.class);

        // THEN
        assertThat(methodDefinitions).isEmpty();
    }

    @Test
    public void shouldReturnMethodDefinitions() throws NoSuchMethodException, IllegalAccessException {
        // GIVEN
        Class<ValidClass> clazz = ValidClass.class;
        Method voidMethodA = clazz.getMethod("voidMethodA", NullResource.class);
        Method voidMethodB = clazz.getMethod("voidMethodB", NullResource.class);
        Method voidMethodC = clazz.getMethod("voidMethodC", NullResource.class);
        MethodHandles.Lookup lookup = MethodHandles.lookup();

        // WHEN
        List<MethodDefinition> methodDefinitions = reader.read(clazz);

        // THEN
        assertThat(methodDefinitions).containsExactly(
            new MethodDefinition(
                0, voidMethodA, lookup.unreflect(voidMethodA), NullResource.class, NullResource._resourceId),
            new MethodDefinition(
                1, voidMethodB, lookup.unreflect(voidMethodB), NullResource.class, NullResource._resourceId),
            new MethodDefinition(2, voidMethodC, lookup.unreflect(voidMethodC), NullResource.class, NullResource._resourceId)
        );
    }


    private static class EmptyClass {

    }

    private static class PrivateMethodsOnly {
        private void methodA() {

        }

        private boolean methodB() {
            return false;
        }

        private void methodC(int a, int b) {

        }
    }

    private abstract static class InvalidSignature {
        public static void staticMethod(NullResource resource) {

        }

        public void invalidParameter(String string) {

        }

        public void invalidArgsLength(NullResource resourceA, NullResource resourceB) {
        }
    }

    public static class ValidClass {
        public void voidMethodA(NullResource resource) {

        }

        public void voidMethodC(NullResource resource) {

        }

        public void voidMethodB(NullResource resource) {

        }
    }

    public static class NullResource implements Resource {
        public static final int _resourceId = 0;

        public static Supplier<NullResource> create() {
            return NullResource::new;
        }

        @Override
        public int getResourceId() {
            return 0;
        }

        @Override
        public void read(ByteBuf in) {

        }

        @Override
        public void write(ByteBuf out) {

        }
    }
}