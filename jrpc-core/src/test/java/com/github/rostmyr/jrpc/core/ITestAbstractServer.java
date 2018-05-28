package com.github.rostmyr.jrpc.core;

import org.junit.ClassRule;

/**
 * Rostyslav Myroshnychenko
 * on 27.05.2018.
 */
public class ITestAbstractServer {

    @ClassRule
    public static final TestServerRule SERVER = new TestServerRule(4040);
}
