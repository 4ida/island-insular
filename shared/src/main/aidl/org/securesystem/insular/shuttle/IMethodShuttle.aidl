package org.securesystem.insular.shuttle;

import org.securesystem.insular.shuttle.MethodInvocation;

interface IMethodShuttle {
    void invoke(inout MethodInvocation invocation);
}
