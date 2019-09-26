package com.saantiaguilera.gradle.plugin.fusrodah

import javassist.CtClass
import javassist.Modifier

interface Processor {

    fun process(ctClass: CtClass)

}

class FusRoDahProcessor : Processor {

    override fun process(ctClass: CtClass) {
        System.out.println("Processing ${ctClass.simpleName}")
        ctClass.declaredMethods.forEach {
            if (!it.isEmpty && !Modifier.isNative(it.modifiers)) {
                it.insertAfter(
                    """
                        throw new java.lang.RuntimeException("FUS RO DAH!");
                    """.trimIndent()
                , true)
            }
        }
    }

}