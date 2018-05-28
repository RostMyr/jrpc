package com.github.rostmyr.jrpc.core.server.registry;

import com.github.rostmyr.jrpc.common.io.Resource;
import com.github.rostmyr.jrpc.common.utils.Contract;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Rostyslav Myroshnychenko
 * on 27.05.2018.
 */
public class MutableResourceRegistry implements ResourceRegistry {
    private final Map<Integer, Supplier<? extends Resource>> resourceByIds = new HashMap<>();
    private final Map<Integer, Class<? extends Resource>> resourceClassesByIds = new HashMap<>();

    @Override
    public Resource get(int id) {
        Supplier<? extends Resource> supplier = resourceByIds.get(id);
        Contract.checkState(supplier, () -> "There is no resource with id: " + id);
        return supplier.get();
    }

    @Override
    public void add(int id, Class<? extends Resource> clazz) {
        Contract.checkArg(clazz, "Resource can't be null");
        Class<? extends Resource> parsed = resourceClassesByIds.get(id);
        if (parsed != null) {
            Contract.checkState(parsed.equals(clazz), "There is already a class " + clazz + " with id " + id);
            return;
        }
        resourceByIds.put(id, getSupplier(clazz));
        resourceClassesByIds.put(id, clazz);
    }

    @SuppressWarnings("unchecked")
    private static Supplier<? extends Resource> getSupplier(Class<? extends Resource> clazz) {
        try {
            Method method = clazz.getDeclaredMethod("create");
            return (Supplier<? extends Resource>) method.invoke(null); // static
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("There is no auto-generated method 'create' in " + clazz);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Can't invoke auto-generated method 'create' in " + clazz);
        }
    }
}
