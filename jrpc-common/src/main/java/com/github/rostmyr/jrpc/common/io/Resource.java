package com.github.rostmyr.jrpc.common.io;

import com.github.rostmyr.jrpc.common.annotation.ResourceId;
import com.github.rostmyr.jrpc.common.io.serialization.BinarySerializable;

/**
 * Rostyslav Myroshnychenko
 * on 21.05.2018.
 */
public interface Resource extends BinarySerializable {

    /**
     * Resource unique identifier
     *
     * @see ResourceId
     */
    int getResourceId();
}
